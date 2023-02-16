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
import kotlin.concurrent.thread
import kotlin.math.max

class SchedulerImpl(
    private val executor: JobExecutor,
    private val store: MutableJobStore
) : Scheduler {
    private lateinit var schedulerThread: Thread
    private lateinit var executorThread: Thread

    private val submittedQueue: BlockingQueue<Job> = LinkedBlockingQueue()
    private val scheduledQueue: BlockingQueue<JobScheduleRequest> = LinkedBlockingQueue()

    private val executorService = Executors.newWorkStealingPool()
    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    override fun schedule(request: JobScheduleRequest) {
        scheduledQueue.add(request)
    }

    override fun submit(request: JobSubmitRequest) {
        val job = request.toJob()
        submitJob(job)
    }

    override fun start() {
        schedulerThread = thread(name = "LambdaJobScheduler") { scheduleJobs() }
        executorThread = thread(name = "LambdaJobExecutor") { executeSubmitted() }
    }

    override fun shutdown() {
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

    private fun scheduleJobs() {
        try {
            while (true) {
                val request = scheduledQueue.take()
                scheduleJobRecurring(request)
            }
        } catch (exception: InterruptedException) {
            // do nothing
        }
    }

    private fun scheduleJobRecurring(request: JobScheduleRequest) {
        val now = Instant.now()
        if (!request.trigger.mayFireAgain()) {
            // means that trigger is now longer active
            return
        }
        val nextFireTime = request.trigger.nextFireTime()
        val delay = max(nextFireTime.toEpochMilli() - now.toEpochMilli(), 0)
        scheduledExecutorService.schedule({
            val job = request.toJob()
            submitJob(job)
            request.trigger.updateAfterFired()
            scheduleJobRecurring(request)
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun executeSubmitted() {
        try {
            while (true) {
                val job = submittedQueue.take()
                executorService.submit {
                    val updatedJob = executor.execute(job)
                    store.save(updatedJob)
                }
            }
        } catch (exception: InterruptedException) {
            // do nothing
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