package org.luartz.store

import org.luartz.job.Job
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

private const val DEFAULT_CAPACITY = 1000

/**
 * Thread safe implementation of job store component.
 * @param capacity defines max number of jobs to keep in memory. Earlier jobs push out older ones.
 */
class InMemoryJobStore(val capacity: Int = DEFAULT_CAPACITY) : MutableJobStore {
    private val logger: Logger = LoggerFactory.getLogger(InMemoryJobStore::class.java)

    private val jobsById: MutableMap<String, Job> = mutableMapOf()

    // Thread save list required for concurrent access
    private val jobsByName: MutableMap<String, CopyOnWriteArrayList<Job>> = mutableMapOf()
    private val jobAgeQueue: PriorityQueue<Job> = PriorityQueue(capacity, JobAgeComparator())

    override fun save(job: Job) {
        jobsById[job.id] = job
        jobsByName.getOrPut(job.name) { CopyOnWriteArrayList() }.add(job)

        // Ensure not exceeding capacity
        jobAgeQueue.add(job)
        if (jobAgeQueue.size > capacity) {
            // The oldest job is a head of the queue
            val jobToRemove = jobAgeQueue.poll()
            jobsById.remove(jobToRemove.id)
            jobsByName[jobToRemove.name]?.remove(jobToRemove)
        }

        logger.debug("Job ${job.id} saved in state ${job.state}")
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

private class JobAgeComparator : Comparator<Job> {
    override fun compare(a: Job, b: Job): Int {
        return (a.createdAt.toEpochMilli() - b.createdAt.toEpochMilli()).toInt()
    }
}