package org.luartz.executor

import org.luartz.job.Job

interface JobSubmitter {
    fun submit(job: Job): Job
}