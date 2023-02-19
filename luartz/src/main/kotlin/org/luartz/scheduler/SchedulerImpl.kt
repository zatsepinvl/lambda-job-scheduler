package org.luartz.scheduler

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.store.JobStore
import org.luartz.store.MutableJobStore
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

    override fun schedule(request: JobScheduleRequest) {
        scheduledQueue.add(request)
    }

    override fun submit(request: JobSubmitRequest) {
        val job = request.toJobWithId(randomUUID().toString())
        submitJob(job)
    }

    override fun start() {
        schedulerThread.start()
        executorThread.start()
    }

    override fun shutdown() {
        schedulerThread.shutdown()
        executorThread.shutdown()
    }

    override fun getStore(): JobStore {
        return this.store
    }

    private fun submitJob(job: Job) {
        submittedQueue.add(job)
        job.state = JobState.SUBMITTED
        store.save(job)
    }

    private inner class SchedulerThread : Thread("LambdaJobScheduler") {
        private var terminated = false
        private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        override fun run() {
            try {
                while (true) {
                    val request = scheduledQueue.take()
                    scheduleJobRecurring(request)
                }
            } catch (exception: InterruptedException) {
                if (!terminated) throw SchedulerException("Unexpected error while scheduling jobs", exception)
                // else do nothing
            }
        }

        fun shutdown() {
            terminated = true
            scheduledExecutorService.shutdown()
            this.interrupt()
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
    }

    private inner class ExecutorThread : Thread("LambdaJobExecutor") {
        private var terminated = false
        private val executorService = Executors.newWorkStealingPool()

        override fun run() {
            try {
                while (true) {
                    val job = submittedQueue.take()
                    executeJobAsync(job)
                }
            } catch (exception: InterruptedException) {
                if (!terminated) throw SchedulerException("Unexpected error while executing jobs", exception)
                // else do nothing
            }
        }

        fun shutdown() {
            terminated = true
            executorService.shutdown()
            this.interrupt()
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