package org.luartz.scheduler

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobState
import org.luartz.job.LambdaDefinition
import org.luartz.store.MutableJobStore
import org.luartz.trigger.IntervalTrigger
import org.luartz.trigger.NowTrigger
import org.luartz.trigger.Trigger
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.*
import java.lang.Thread.sleep
import java.time.Duration
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

    @Test
    // ToDo: might be flaky due to high dependency on timing
    fun unscheduleScheduledJob() {
        // Given
        val template = givenTestJobTemplate(
            trigger = IntervalTrigger(Instant.now(), Duration.ofSeconds(1))
        )

        // When
        scheduler.schedule(template)
        scheduler.start()
        // Give time to schedule a job
        sleep(100)
        scheduler.unschedule(template.id)
        // Given time to invoke scheduled submit
        sleep(1000)
        scheduler.shutdown()

        // Then
        argumentCaptor<Job> {
            verify(store, times(1)).save(capture())
            assertThat(firstValue.state).isEqualTo(JobState.SCHEDULED)
        }

        verify(executor, never()).execute(any())
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