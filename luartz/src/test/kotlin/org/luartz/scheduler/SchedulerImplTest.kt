package org.luartz.scheduler

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.job.LambdaDefinition
import org.luartz.store.MutableJobStore
import org.luartz.trigger.NowTrigger
import org.luartz.trigger.Trigger
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import java.lang.Thread.sleep
import java.time.Instant
import java.util.concurrent.CompletableFuture.completedFuture

class SchedulerImplTest {

    private lateinit var executor: JobExecutor
    private lateinit var store: MutableJobStore
    private lateinit var scheduler: Scheduler

    @BeforeEach
    fun setup() {
        executor = mock()
        store = mock()
        scheduler = SchedulerImpl(executor, store)
    }

    @Test
    fun executorIsInvoked() {
        // Given
        executor.mockSuccessInvocation()
        val template = givenTestJobTemplate()

        // When
        scheduler.whenScheduleAndShutdown(template)

        // Then
        argumentCaptor<Job> {
            verify(executor).execute(capture())

            val job = firstValue
            assertThat(job.name).isEqualTo(template.jobName)
            assertThat(job.state).isEqualTo(JobState.RUNNING)
        }
    }

    @Test
    fun storeIsInvoked() {
        // Given
        executor.mockSuccessInvocation()
        val template = givenTestJobTemplate()

        // When
        scheduler.whenScheduleAndShutdown(template)

        // Then
        argumentCaptor<Job> {
            verify(store, times(3)).save(capture())

            // Save on scheduled, run and then executed successfully
            assertThat(allValues.map { it.state }).containsExactly(
                JobState.SCHEDULED,
                JobState.RUNNING,
                JobState.SUCCEEDED
            )
        }
    }

    private fun givenTestJobTemplate(trigger: Trigger = NowTrigger()): JobTemplate {
        return JobTemplate(
            id = "test_tempalte",
            jobName = "test_name",
            lambda = LambdaDefinition("test_function"),
            payload = "test_payload",
            trigger = trigger
        )
    }

    private fun JobExecutor.mockSuccessInvocation() {
        whenever(this.execute(any())).thenAnswer {
            val job = it.arguments[0] as Job
            completedFuture(job.succeedAt(Instant.now()))
        }
    }

    private fun Scheduler.whenScheduleAndShutdown(template: JobTemplate, awaitMillis: Long = 200) {
        this.schedule(template)
        this.start()
        // Make test not depend on explicit sleep
        sleep(awaitMillis)
        this.shutdown()
    }
}