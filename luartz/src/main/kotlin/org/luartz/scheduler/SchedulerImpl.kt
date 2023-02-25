package org.luartz.scheduler

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.store.JobStore
import org.luartz.store.MutableJobStore
import org.luartz.util.WorkerThread
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.UUID.randomUUID
import java.util.concurrent.*
import kotlin.math.max

internal class SchedulerImpl(
    private val executor: JobExecutor,
    private val store: MutableJobStore,
    private val clock: Clock = Clock.systemDefaultZone()
) : Scheduler {
    private val logger: Logger = LoggerFactory.getLogger(SchedulerImpl::class.java)

    // ToDo: review the choice of LinkedBlockingQueue
    private val scheduleQueue: BlockingQueue<JobTemplate> = LinkedBlockingQueue()
    private val executionQueue: BlockingQueue<Job> = LinkedBlockingQueue()

    private val schedulerThread = SchedulerThread()
    private val executorThread = ExecutorThread()

    private val templates: MutableMap<String, JobTemplate> = ConcurrentHashMap()

    override val jobTemplates: List<JobTemplate> get() = templates.values.toList()
    override val jobStore: JobStore = store

    override fun schedule(template: JobTemplate) {
        if (templates.containsKey(template.id)) {
            throw IllegalArgumentException("Job template with id ${template.id} has already been added")
        }
        templates[template.id] = template
        scheduleQueue.add(template)
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
        schedulerThread.start()
        executorThread.start()
    }

    override fun shutdown() {
        schedulerThread.shutdown()
        executorThread.shutdown()
    }

    private fun newJobId(jobName: String): String {
        return "${jobName}:${randomUUID()}"
    }

    private inner class SchedulerThread : WorkerThread("LambdaJobScheduler") {
        private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        override fun runInInfiniteLoop() {
            val template = scheduleQueue.take()
            scheduleJobRecurring(template)
        }

        override fun onShutdown() {
            scheduledExecutorService.shutdown()
        }

        private fun scheduleJobRecurring(template: JobTemplate) {
            val trigger = template.trigger
            if (!trigger.mayFireAgain()) {
                // means that trigger is now longer active
                return
            }

            // Calculate delay time to submit job
            val now = clock.instant()
            val nextFireTime = trigger.nextFireTime()
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
                    }
                },
                delayMillis, TimeUnit.MILLISECONDS
            )

            // Store scheduled job
            store.save(job)
        }

        private fun newJobFromTemplate(template: JobTemplate, scheduleExecutionAt: Instant): Job {
            val jobId = newJobId(template.jobName)
            return template
                .toJobWithId(jobId, clock.instant())
                .scheduleExecutionAt(scheduleExecutionAt)
        }

        private fun submitForExecution(job: Job, template: JobTemplate) {
            if (!templates.containsKey(template.id)) {
                // Skip execution if template was unscheduled
                return
            }
            executionQueue.add(job)
            template.trigger.updateAfterFired()
            // Make scheduling recurring
            scheduleQueue.add(template)
        }
    }

    private inner class ExecutorThread : WorkerThread("LambdaJobExecutor") {
        // ToDo: better executor service?
        private val executorService = Executors.newWorkStealingPool()

        override fun runInInfiniteLoop() {
            val job = executionQueue.take()
            executeJobAsync(job)
        }

        override fun onShutdown() {
            executorService.shutdown()
        }

        private fun executeJobAsync(job: Job) {
            executorService.submit {
                try {
                    executeJob(job)
                } catch (throwable: Throwable) {
                    logger.error("Error while executing job ${job.id}", throwable)
                }
            }
        }

        private fun executeJob(scheduledJob: Job) {
            val job = scheduledJob.runAt(clock.instant())
            store.save(job)

            logger.info("Starting executing job ${job.id}")
            executor
                .execute(job)
                // Switch context to the current executor service to gain more control
                .thenApplyAsync({ executedJob ->
                    store.save(executedJob)

                    if (scheduledJob.state == JobState.SUCCEEDED) {
                        logger.info("Job ${scheduledJob.id} was executed successfully")
                    } else {
                        logger.error("Job ${scheduledJob.id} execution failed with error ${scheduledJob.executionError}")
                    }
                }, executorService)
        }
    }
}