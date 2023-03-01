package org.luartz.scheduler

import org.luartz.deployer.JobDeployer
import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.store.JobStore
import org.luartz.store.MutableJobStore
import org.luartz.util.WorkerThread
import org.luartz.util.defaultUtcClock
import org.luartz.util.toUtcDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.UUID.randomUUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

internal class SchedulerImpl(
    private val store: MutableJobStore,
    deployer: JobDeployer,
    private val executor: JobExecutor,
    private val clock: Clock = defaultUtcClock()
) : Scheduler {
    private val logger: Logger = LoggerFactory.getLogger(SchedulerImpl::class.java)

    private val executorService = Executors.newWorkStealingPool()
    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val scheduleQueue = ScheduleQueue()
    private val executionQueue = ExecutionQueue()
    private val deploymentQueue = DeploymentQueue()

    private val schedulerThread = SchedulerThread()
    private val executorThread = ExecutorThread()
    private val deployerThread = DeployerThread(deploymentQueue, scheduleQueue, executorService, deployer)

    private val templates: MutableMap<String, JobTemplate> = ConcurrentHashMap()

    override fun schedule(template: JobTemplate) {
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
        val template = templates[templateId]
        if (template == null) {
            throw IllegalArgumentException("Job template is not found by id $templateId")
        }
        templates.remove(templateId)

        logger.debug("Job template $templateId was unscheduled")
    }

    override fun start() {
        deployerThread.start()
        schedulerThread.start()
        executorThread.start()
    }

    override fun shutdown() {
        // ToDo: shutdown service properly
        executorService.shutdown()
        scheduledExecutorService.shutdown()

        schedulerThread.shutdown()
        executorThread.shutdown()
    }

    override fun getJobStore(): JobStore {
        return store
    }

    override fun getJobTemplates(): List<JobTemplate> {
        return templates.values.toList()
    }

    private fun newJobId(jobName: String): String {
        return "${jobName}:${randomUUID()}"
    }

    private fun isUnscheduled(templateId: String): Boolean {
        return !templates.containsKey(templateId)
    }

    private inner class SchedulerThread : WorkerThread("LambdaJobScheduler") {

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
            val executionAt = now.plusMillis(delayMillis)
            val job = newJobFromTemplate(template, executionAt)

            // Schedule job submitting for execution
            scheduledExecutorService.schedule(
                {
                    try {
                        submitForExecution(job, template)
                    } catch (throwable: Throwable) {
                        logger.error("Error while submitting job ${job.id} for execution", throwable)
                    } finally {
                        // Make scheduling recurring
                        scheduleQueue.add(template)
                    }
                },
                delayMillis, TimeUnit.MILLISECONDS
            )

            logger.debug("Job ${job.id} is scheduled for submission at ${executionAt.toUtcDate()} in ${delayMillis / 1000} seconds")
            store.save(job)
        }

        private fun newJobFromTemplate(template: JobTemplate, scheduleExecutionAt: Instant): Job {
            val jobId = newJobId(template.jobName)
            return template
                .toJobWithId(jobId, clock.instant())
                .scheduleExecutionAt(scheduleExecutionAt)
        }

        private fun submitForExecution(job: Job, template: JobTemplate) {
            if (isUnscheduled(template.id)) {
                return
            }
            executionQueue.add(job)
            template.trigger.whenFired()
        }
    }

    private inner class ExecutorThread : WorkerThread("LambdaJobExecutor") {
        override fun runInInfiniteLoop() {
            val job = executionQueue.take()
            executeJobAsync(job)
        }

        private fun executeJobAsync(job: Job) {
            if (isUnscheduled(job.templateId)) {
                return
            }
            CompletableFuture
                .supplyAsync({ saveJobAsRun(job) }, executorService)
                .thenApplyAsync({ executeJob(it) }, executorService)
                .whenCompleteAsync({ executedJob, throwable ->
                    if (throwable != null) {
                        handleJobExecutionError(job, throwable)
                    } else {
                        handleJobExecuted(executedJob)
                    }
                }, executorService)
        }

        private fun saveJobAsRun(scheduledJob: Job): Job {
            val job = scheduledJob.runAt(clock.instant())
            store.save(job)
            return job
        }

        private fun executeJob(runningJob: Job): Job {
            logger.info("Starting executing job ${runningJob.id}")
            return executor.execute(runningJob)
        }

        private fun handleJobExecuted(job: Job) {
            store.save(job)

            if (job.state == JobState.SUCCEEDED) {
                logger.info("Job ${job.id} was executed successfully")
            } else {
                logger.error("Job ${job.id} execution failed with error ${job.executionError}")
            }
        }

        private fun handleJobExecutionError(job: Job, throwable: Throwable) {
            logger.error("Error while executing job ${job.id}", throwable)
            val failedJob = job.failAt(clock.instant(), throwable.message ?: "unknown execution error")
            store.save(failedJob)
            // ToDo: implement retry mechanism
        }
    }
}