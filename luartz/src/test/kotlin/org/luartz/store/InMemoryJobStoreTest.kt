package org.luartz.store

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.luartz.job.givenTestJob
import java.time.Instant

class InMemoryJobStoreTest {

    @Test
    fun testCapacityIsNotExceeded() {
        // Given
        val store = InMemoryJobStore(1)
        val oldJob = givenTestJob(id = "old", name = "job", createdAt = Instant.ofEpochMilli(1))
        val newJob = givenTestJob(id = "new", name = "job", createdAt = Instant.ofEpochMilli(2))

        // When
        store.save(oldJob)
        store.save(newJob)

        // Then
        val oldJobResult = store.getJob("old")
        val newJobResult = store.getJob("new")
        val jobs = store.getJobsByName("job")

        assertThat(oldJobResult).isNull()
        assertThat(newJobResult).isNotNull()
        assertThat(jobs).containsExactly(newJob)
    }

}