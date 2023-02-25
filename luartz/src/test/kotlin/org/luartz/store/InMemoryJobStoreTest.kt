package org.luartz.store

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.luartz.job.givenTestJob
import java.util.*
import java.util.UUID.randomUUID
import java.util.concurrent.Executors
import java.util.concurrent.Future

class InMemoryJobStoreTest {

    @Test
    fun testGetJobsByName() {
        // Given
        val store = InMemoryJobStore(2)
        val jobName = "jobName"
        val job1 = givenTestJob(id = "job1", name = jobName)
        val job2 = givenTestJob(id = "job2", name = jobName)

        // When
        store.save(job1)
        store.save(job2)

        // Then
        val jobs = store.getJobsByName(jobName)
        assertThat(jobs).containsExactly(job1, job2)
    }

    @Test
    fun testCapacityIsNotExceeded() {
        // Given
        val store = InMemoryJobStore(1)
        val job1 = givenTestJob(id = "job1")
        val job2 = givenTestJob(id = "job2")
        val job3 = givenTestJob(id = "job3")

        // When
        store.save(job1)
        store.save(job2)
        store.save(job3)

        // Then
        assertThat(store.getJob(job1.id)).isNull()
        assertThat(store.getJob(job2.id)).isNull()
        assertThat(store.getJob(job3.id)).isNotNull()

        assertThat(store.getAllJobs()).hasSize(1)
        assertThat(store.getJobsByName(job3.name)).hasSize(1)
    }

    @RepeatedTest(10)
    fun testThreadSafety() {
        // Given
        val store = InMemoryJobStore(10)

        val jobNameCount = 10
        val jobNames = (1..jobNameCount).map { "jobName$it" }.toList()

        val executor = Executors.newFixedThreadPool(4)
        val futures = mutableListOf<Future<*>>()

        // When
        // Init write tasks
        repeat(10) {
            val future: Future<*> = executor.submit {
                repeat(10) {
                    store.save(givenTestJob(id = randomUUID().toString(), name = jobNames[it % jobNameCount]))
                }
            }
            futures.add(future)
        }
        // Init read tasks
        repeat(10) {
            val future: Future<*> = executor.submit {
                repeat(10) {
                    store.getAllJobs()
                    store.getJobsByName(jobNames[it % jobNameCount])
                }
            }
            futures.add(future)
        }

        // Then
        // Expect no errors
        futures.forEach { it.get() }
    }

}