package org.luartz.scheduler.impl

import org.luartz.deployer.JobDeployer
import org.luartz.deployer.toDeploymentCommand
import org.luartz.executor.JobSubmitter
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.scheduler.JobTemplate
import org.luartz.scheduler.Scheduler
import org.luartz.scheduler.toJobWithId
import org.luartz.store.JobStore
import org.luartz.store.MutableJobStore
import org.luartz.util.WorkerThread
import org.luartz.util.defaultUtcClock
import org.luartz.util.shutdownGracefully
import org.luartz.util.toUtcDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

private const val SHUTDOWN_TIMEOUT_SECONDS = 60L

internal class SchedulerImpl(
    private val store: MutableJobStore,
    private val deployer: JobDeployer,
    private val executor: JobSubmitter,
    private val clock: Clock = defaultUtcClock()
) : Scheduler {
    private val logger: Logger = LoggerFactory.getLogger(SchedulerImpl::class.java)

    private val executorService = Executors.newWorkStealingPool()
    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val deploymentQueue = DeploymentQueue()
    private val scheduleQueue = ScheduleQueue()
    private val submissionQueue = SubmissionQueue()

    private val deploymentWorker = DeploymentWorker()
    private val schedulingWorker = SchedulingWorker()
    private val submissionWorker = SubmissionWorker()

    private val templates: MutableMap<String, JobTemplate> = ConcurrentHashMap()
    private var shutdown = false

    override fun schedule(template: JobTemplate) {
        ensureActive()
        if (templates.containsKey(template.id)) {
            throw IllegalArgumentException("Job template with id ${template.id} has already been added")
        }
        templates[template.id] = template
        if (template.function.deployment != null) {
            deploymentQueue.add(template)
        } else {
            scheduleQueue.add(template)
        }
    }

    override fun unschedule(templateId: String) {
        ensureActive()
        if (!templates.containsKey(templateId)) {
            throw IllegalArgumentException("Job template is not found by id $templateId")
        }
        templates.remove(templateId)

        logger.debug("Job template $templateId was unscheduled")
    }

    override fun start() {
        ensureActive()
        deploymentWorker.start()
        schedulingWorker.start()
        submissionWorker.start()
    }

    override fun shutdown() {
        ensureActive()

        deploymentWorker.shutdown()
        schedulingWorker.shutdown()
        submissionWorker.shutdown()

        executorService.shutdownGracefully(Duration.ofSeconds(SHUTDOWN_TIMEOUT_SECONDS))
        scheduledExecutorService.shutdownGracefully(Duration.ofSeconds(SHUTDOWN_TIMEOUT_SECONDS))

        shutdown = true
    }

    override fun getJobStore(): JobStore {
        return store
    }

    override fun getJobTemplates(): List<JobTemplate> {
        return templates.values.toList()
    }

    private fun newJobId(jobName: String): String {
        return "${jobName}:${UUID.randomUUID()}"
    }

    private fun isUnscheduled(templateId: String): Boolean {
        return !templates.containsKey(templateId)
    }

    private fun ensureActive() {
        if (shutdown) {
            throw IllegalStateException("Scheduler has been shut down")
        }
    }

    // Deployment
    private inner class DeploymentWorker : WorkerThread("JobDeploymentWorker") {
        override fun runInInfiniteLoop() {
            val template = deploymentQueue.take()
            deployAsync(template)
        }

        private fun deployAsync(template: JobTemplate) {
            executorService.submit {
                try {
                    deployer.deploy(template.function.toDeploymentCommand())
                    scheduleQueue.add(template)
                } catch (throwable: Throwable) {
                    logger.error(
                        "Unable to deploy function from template ${template.id} for job ${template.jobName}",
                        throwable
                    )
                }
            }
        }
    }

    // Scheduling
    private inner class SchedulingWorker : WorkerThread("JobSchedulingWorker") {
        override fun runInInfiniteLoop() {
            val template = scheduleQueue.take()
            scheduleJobRecurring(template)
        }

        private fun scheduleJobRecurring(template: JobTemplate) {
            if (isUnscheduled(template.id)) {
                return
            }

            val trigger = template.trigger
            if (!trigger.canFire()) {
                // means that trigger is now longer active
                return
            }

            // Calculate delay time to submit job
            val nextFireTime = trigger.nextFireTime(clock)
            val now = clock.instant()
            val delayMillis = max(nextFireTime.toEpochMilli() - now.toEpochMilli(), 0)

            // Create job from template
            val submissionAt = now.plusMillis(delayMillis)
            val job = newJobFromTemplate(template, submissionAt)

            // Schedule job submitting for submission
            scheduledExecutorService.schedule(
                {
                    try {
                        submissionQueue.add(job)
                        trigger.whenFired()
                    } catch (throwable: Throwable) {
                        logger.error("Error while submitting job ${job.id}", throwable)
                    } finally {
                        // Make scheduling recurring
                        scheduleQueue.add(template)
                    }
                },
                delayMillis, TimeUnit.MILLISECONDS
            )

            logger.debug("Job ${job.id} is scheduled for submission at ${submissionAt.toUtcDate()} in ${delayMillis / 1000} seconds")
            store.save(job)
        }

        private fun newJobFromTemplate(template: JobTemplate, scheduleSubmissionAt: Instant): Job {
            val jobId = newJobId(template.jobName)
            return template
                .toJobWithId(jobId, clock.instant())
                .scheduleSubmissionAt(scheduleSubmissionAt)
        }
    }

    // Submission
    private inner class SubmissionWorker : WorkerThread("JobSubmissionWorker") {
        override fun runInInfiniteLoop() {
            val job = submissionQueue.take()
            submitJobAsync(job)
        }

        private fun submitJobAsync(job: Job) {
            if (isUnscheduled(job.templateId)) {
                return
            }
            CompletableFuture
                .supplyAsync({ submitJob(job) }, executorService)
                .whenCompleteAsync({ submittedJob, throwable ->
                    if (throwable != null) {
                        handleJobSubmissionFailed(job, throwable)
                    } else {
                        handleJobSubmitted(submittedJob)
                    }
                }, executorService)
        }

        private fun submitJob(job: Job): Job {
            logger.info("Submitting job ${job.id}...")
            return executor.submit(job)
        }

        private fun handleJobSubmitted(job: Job) {
            store.save(job)

            if (job.state == JobState.SUBMITTED) {
                logger.info("Job ${job.id} was submitted successfully")
            } else {
                logger.error("Job ${job.id} submission failed with error ${job.submissionError}")
            }
        }

        private fun handleJobSubmissionFailed(job: Job, throwable: Throwable) {
            logger.error("Error while submitting job ${job.id}", throwable)
            val failedJob = job.submissionFailAt(clock.instant(), throwable.message ?: "unknown error")
            store.save(failedJob)
        }
    }
}