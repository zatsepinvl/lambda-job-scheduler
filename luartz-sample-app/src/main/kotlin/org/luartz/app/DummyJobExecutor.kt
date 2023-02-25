package org.luartz.app

import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.random.nextLong

class DummyJobExecutor : JobExecutor {
    override fun execute(job: Job): CompletableFuture<Job> {
        val executor = CompletableFuture.delayedExecutor(Random.nextLong(5000L..10000L), TimeUnit.MILLISECONDS)
        return CompletableFuture.supplyAsync({ job.succeedAt(Instant.now()) }, executor)
    }
}