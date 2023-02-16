package org.luartz.executor

import org.luartz.job.Job
import org.luartz.job.JobState
import java.time.Instant
import kotlin.random.Random
import kotlin.random.nextLong

class JobExecutor {
    fun execute(job: Job): Job {
        job.startedAt = Instant.now()
        job.state = JobState.RUNNING
        println("Executing job ${job.name} (${job.id}) at ${job.startedAt}")

        Thread.sleep(Random.nextLong(1000L..5000L))

        job.stoppedAt = Instant.now()
        job.state = JobState.SUCCEEDED

        println("Finished job ${job.name} (${job.id}) at ${job.stoppedAt}")

        return job
    }
}