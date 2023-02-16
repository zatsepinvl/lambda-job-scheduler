package org.luartz.store

import org.luartz.job.Job

interface MutableJobStore: JobStore {
    fun save(job: Job)
}