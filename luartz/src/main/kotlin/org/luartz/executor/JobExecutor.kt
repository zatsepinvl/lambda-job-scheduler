package org.luartz.executor

import org.luartz.job.Job

interface JobExecutor {
    fun execute(job: Job): Job
}