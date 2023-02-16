package org.luartz.store

import org.luartz.job.Job

class InMemoryJobStore : MutableJobStore {
    private val jobsById: MutableMap<String, Job> = mutableMapOf()
    private val jobsByName: MutableMap<String, MutableList<Job>> = mutableMapOf()

    override fun save(job: Job) {
        jobsById[job.id] = job
        jobsByName.getOrPut(job.name) { mutableListOf() }.add(job)
    }

    override fun findJobsByName(jobName: String): List<Job> {
        return jobsByName[jobName] ?: emptyList()
    }

    override fun getAllJobs(): List<Job> {
        return jobsById.values.toList()
    }
}