package org.luartz.store

import org.luartz.job.Job
import org.luartz.util.acquireToRead
import org.luartz.util.acquireToWrite
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

private const val DEFAULT_CAPACITY = 1000

/**
 * Thread safe implementation of job store component.
 * @param capacity defines max number of jobs to keep in memory. New jobs push out old ones.
 */
class InMemoryJobStore(val capacity: Int = DEFAULT_CAPACITY) : MutableJobStore {
    private val logger: Logger = LoggerFactory.getLogger(InMemoryJobStore::class.java)

    private val jobsById: MutableMap<String, Job> = mutableMapOf()
    private val jobsByName: MutableMap<String, MutableList<Job>> = mutableMapOf()
    private val jobAgeQueue: Queue<Job> = LinkedList()

    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    override fun save(job: Job) {
        lock.acquireToWrite {
            saveJob(job)
            ensureCapacityNotExceeded()
            logger.debug("Job ${job.id} saved in state ${job.state}")
        }
    }

    private fun saveJob(job: Job) {
        if (!jobsById.containsKey(job.id)) {
            jobAgeQueue.add(job)
        }
        jobsById[job.id] = job
        jobsByName.getOrPut(job.name) { mutableListOf() }.add(job)
    }

    private fun ensureCapacityNotExceeded() {
        while (jobAgeQueue.size > capacity) {
            // The oldest job is a head of the queue
            val jobToRemove = jobAgeQueue.poll()
            jobsById.remove(jobToRemove.id)
            jobsByName[jobToRemove.name]?.remove(jobToRemove)
        }
    }

    override fun getJob(jobId: String): Job? {
        return lock.acquireToRead {
            jobsById[jobId]
        }
    }

    override fun getJobsByName(jobName: String): List<Job> {
        return lock.acquireToRead {
            jobsByName[jobName] ?: emptyList()
        }
    }

    override fun getAllJobs(): List<Job> {
        return lock.acquireToRead {
            jobsById.values.toList()
        }
    }
}