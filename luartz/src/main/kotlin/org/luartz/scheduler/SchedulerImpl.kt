package org.luartz.scheduler

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.store.JobStore
import org.luartz.store.MutableJobStore
import java.time.Instant
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max

class SchedulerImpl(
    private val executor: JobExecutor, private val store: MutableJobStore
) : Scheduler {
    private val schedulerThread = SchedulerThread()
    private val executorThread = ExecutorThread()
    private var terminated = false

    private val submittedQueue: BlockingQueue<Job> = LinkedBlockingQueue()
    private val scheduledQueue: BlockingQueue<JobScheduleRequest> = LinkedBlockingQueue()

    override fun schedule(request: JobScheduleRequest) {
        scheduledQueue.add(request)
    }

    override fun submit(request: JobSubmitRequest) {
        val job = request.toJob()
        submitJob(job)
    }

    override fun start() {
        schedulerThread.start()
        executorThread.start()
    }

    override fun shutdown() {
        terminated = true
        executorThread.interrupt()
        schedulerThread.interrupt()
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

        private fun scheduleJobRecurring(request: JobScheduleRequest) {
            val now = Instant.now()
            val trigger = request.trigger
            if (!trigger.mayFireAgain()) {
                // means that trigger is now longer active
                return
            }
            val nextFireTime = trigger.nextFireTime()
            val delay = max(nextFireTime.toEpochMilli() - now.toEpochMilli(), 0)
            scheduledExecutorService.schedule({
                val job = request.toJob()
                submitJob(job)
                trigger.updateAfterFired()
                scheduledQueue.add(request)
            }, delay, TimeUnit.MILLISECONDS)
        }
    }

    private inner class ExecutorThread : Thread("LambdaJobExecutor") {
        private val executorService = Executors.newWorkStealingPool()

        override fun run() {
            try {
                while (true) {
                    val job = submittedQueue.take()
                    executeJob(job)
                }
            } catch (exception: InterruptedException) {
                if (!terminated) throw SchedulerException("Unexpected error while executing jobs", exception)
                // else do nothing
            }
        }

        private fun executeJob(job: Job) {
            executorService.submit {
                job.startedAt = Instant.now()
                val executedJob = executor.execute(job)
                job.stoppedAt = Instant.now()
                store.save(executedJob)
            }
        }
    }
}

private fun JobScheduleRequest.toJob(): Job {
    return Job(
        id = UUID.randomUUID().toString(),
        name = this.name,
        definition = this.definition,
        payload = this.payload,
        state = JobState.CREATED,
        trigger = this.trigger
    )
}

private fun JobSubmitRequest.toJob(): Job {
    return Job(
        id = UUID.randomUUID().toString(),
        name = this.name,
        definition = this.definition,
        payload = this.payload,
        state = JobState.CREATED,
        trigger = null
    )
}