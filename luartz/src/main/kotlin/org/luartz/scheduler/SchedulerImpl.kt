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
import java.util.UUID.randomUUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SchedulerImpl(
    private val executor: JobExecutor,
    private val store: MutableJobStore,
    private val clock: Clock = Clock.systemDefaultZone()
) : Scheduler {
    private val logger: Logger = LoggerFactory.getLogger(SchedulerImpl::class.java)

    private val scheduleQueue: BlockingQueue<JobTemplate> = LinkedBlockingQueue()
    private val executionQueue: BlockingQueue<Job> = LinkedBlockingQueue()

    private val schedulerThread = SchedulerThread()
    private val executorThread = ExecutorThread()

    private val templates: MutableMap<String, JobTemplate> = mutableMapOf()

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

            // Schedule job submitting for execution
            val jobId = newJobId(template.jobName)
            val job = template
                .toJobWithId(jobId)
                .scheduleExecutionAt(now.plusMillis(delayMillis))
            scheduledExecutorService.schedule({
                if (!templates.containsKey(template.id)) {
                    // Skip execution if template was unscheduled
                    return@schedule
                }
                submitJob(job)
                trigger.updateAfterFired()
                // Make scheduling recurring
                scheduleQueue.add(template)
            }, delayMillis, TimeUnit.MILLISECONDS)
            store.save(job)
        }


        private fun submitJob(job: Job) {
            executionQueue.add(job)
        }
    }

    private inner class ExecutorThread : WorkerThread("LambdaJobExecutor") {
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
            val executedJob = executor.execute(job)

            store.save(executedJob)

            if (scheduledJob.state == JobState.SUCCEEDED) {
                logger.info("Job ${scheduledJob.id} was executed successfully")
            } else {
                logger.error("Job ${scheduledJob.id} execution failed with error ${scheduledJob.executionError}")
            }
        }
    }
}