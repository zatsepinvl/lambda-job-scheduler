package org.luartz.executor

import org.luartz.job.Job
import java.util.concurrent.CompletableFuture

interface JobExecutor {
    fun execute(job: Job): CompletableFuture<Job>
}