package org.luartz.store

import org.luartz.job.Job
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryJobStore : MutableJobStore {
    private val logger: Logger = LoggerFactory.getLogger(InMemoryJobStore::class.java)

    private val jobsById: MutableMap<String, Job> = mutableMapOf()

    // Thread save list required for concurrent access
    private val jobsByName: MutableMap<String, CopyOnWriteArrayList<Job>> = mutableMapOf()

    override fun save(job: Job) {
        jobsById[job.id] = job
        jobsByName.getOrPut(job.name) { CopyOnWriteArrayList() }.add(job)

        logger.debug("Job ${job.printableId} saved in state ${job.state}")
    }

    override fun getJob(jobId: String): Job? {
        return jobsById[jobId]
    }

    override fun getJobsByName(jobName: String): List<Job> {
        return jobsByName[jobName] ?: emptyList()
    }

    override fun getAllJobs(): List<Job> {
        return jobsById.values.toList()
    }
}