package org.luartz.store

import org.luartz.job.Job

interface JobStore {
    fun findJobsByName(jobName: String): List<Job>

    fun getAllJobs(): List<Job>
}