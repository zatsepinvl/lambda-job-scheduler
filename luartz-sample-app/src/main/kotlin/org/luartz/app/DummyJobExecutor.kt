package org.luartz.app

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import kotlin.random.Random
import kotlin.random.nextLong

class DummyJobExecutor : JobExecutor {
    override fun execute(job: Job): Job {
        Thread.sleep(Random.nextLong(1000L..5000L))
        job.state = JobState.SUCCEEDED
        return job
    }
}