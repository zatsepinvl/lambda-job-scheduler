package org.luartz.scheduler

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.store.JobStore
import org.luartz.store.MutableJobStore
import org.luartz.util.WorkerThread
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID.randomUUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SchedulerImpl(
    private val executor: JobExecutor,
    private val store: MutableJobStore
) : Scheduler {
    private val logger: Logger = LoggerFactory.getLogger(SchedulerImpl::class.java)

    private val scheduledQueue: BlockingQueue<JobScheduleRequest> = LinkedBlockingQueue()
    private val submittedQueue: BlockingQueue<Job> = LinkedBlockingQueue()

    private val schedulerThread = SchedulerThread()
    private val executorThread = ExecutorThread()

    override val jobStore: JobStore = store

    override fun schedule(request: JobScheduleRequest) {
        scheduledQueue.add(request)
    }

    override fun start() {
        schedulerThread.start()
        executorThread.start()
    }

    override fun shutdown() {
        schedulerThread.shutdown()
        executorThread.shutdown()
    }

    private inner class SchedulerThread : WorkerThread("LambdaJobScheduler") {
        private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        override fun runInfinitely() {
            val request = scheduledQueue.take()
            scheduleJobRecurring(request)
        }

        override fun onShutdown() {
            scheduledExecutorService.shutdown()
        }

        private fun scheduleJobRecurring(request: JobScheduleRequest) {
            val trigger = request.trigger
            if (!trigger.mayFireAgain()) {
                // means that trigger is now longer active
                return
            }

            // Calculate delay time to submit job
            val now = Instant.now()
            val nextFireTime = trigger.nextFireTime()
            val delayMillis = max(nextFireTime.toEpochMilli() - now.toEpochMilli(), 0)

            // Schedule job submitting for execution
            scheduledExecutorService.schedule({
                val job = request.toJobWithId(randomUUID().toString())
                submitJob(job)
                trigger.updateAfterFired()
                // Make scheduling recurring
                scheduledQueue.add(request)
            }, delayMillis, TimeUnit.MILLISECONDS)
        }


        private fun submitJob(job: Job) {
            submittedQueue.add(job)
            job.state = JobState.SUBMITTED
            store.save(job)
        }
    }

    private inner class ExecutorThread : WorkerThread("LambdaJobExecutor") {
        private val executorService = Executors.newWorkStealingPool()

        override fun runInfinitely() {
            val job = submittedQueue.take()
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
                    logger.error("Error while executing job ${job.printableId}", throwable)
                }
            }
        }

        private fun executeJob(job: Job) {
            job.startedAt = Instant.now()
            logger.info("Starting executing job ${job.printableId}")
            val executedJob = executor.execute(job)
            job.stoppedAt = Instant.now()
            store.save(executedJob)

            if (job.state == JobState.SUCCEEDED) {
                logger.info("Job ${job.printableId} was executed successfully")
            } else {
                logger.error("Job ${job.printableId} execution failed with error ${job.executionError}")
            }
        }
    }
}