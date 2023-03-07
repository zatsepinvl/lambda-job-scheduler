package org.luartz.app

import org.luartz.executor.JobSubmitter
import org.luartz.job.Job
import java.time.Instant
import kotlin.random.Random
import kotlin.random.nextLong

class DummyJobSubmitter : JobSubmitter {
    override fun submit(job: Job): Job {
        Thread.sleep(Random.nextLong(5000L..10000L))
        return job.submitAt(Instant.now())
    }
}