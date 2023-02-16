package org.luartz.store

import org.luartz.job.Job

interface JobStore {
    fun getJob(jobId: String): Job?

    fun getJobsByName(jobName: String): List<Job>

    fun getAllJobs(): List<Job>
}