package org.luartz.app

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import java.time.Instant
import kotlin.random.Random
import kotlin.random.nextLong

class DummyJobExecutor : JobExecutor {
    override fun execute(job: Job): Job {
        Thread.sleep(Random.nextLong(5000L..10000L))
        return job.invokedAt(Instant.now())
    }
}