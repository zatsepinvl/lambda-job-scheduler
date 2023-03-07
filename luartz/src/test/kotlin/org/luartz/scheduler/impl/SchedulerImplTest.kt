package org.luartz.scheduler.impl

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.luartz.deployer.JobDeployer
import org.luartz.executor.JobSubmitter
import org.luartz.job.Job
import org.luartz.job.JobFunction
import org.luartz.job.JobState
import org.luartz.scheduler.JobTemplate
import org.luartz.scheduler.Scheduler
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

class SchedulerImplTest {
    private lateinit var store: MutableJobStore
    private lateinit var deployer: JobDeployer
    private lateinit var executor: JobSubmitter
    private lateinit var scheduler: Scheduler

    @BeforeEach
    fun setup() {
        store = mock()
        deployer = mock()
        executor = mock()
        scheduler = SchedulerImpl(store, deployer, executor)
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
            verify(executor).submit(capture())

            val job = firstValue
            assertThat(job.name).isEqualTo(template.jobName)
            assertThat(job.state).isEqualTo(JobState.SCHEDULED)
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
            verify(store, times(2)).save(capture())

            // Save on scheduled, run and then executed successfully
            assertThat(allValues.map { it.state }).containsExactly(
                JobState.SCHEDULED,
                JobState.SUBMITTED
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

        verify(executor, never()).submit(any())
    }

    private fun givenTestJobTemplate(trigger: Trigger = NowTrigger()): JobTemplate {
        return JobTemplate(
            id = "test_tempalte",
            jobName = "test_name",
            function = JobFunction("test_function"),
            payload = "test_payload",
            trigger = trigger
        )
    }

    private fun JobSubmitter.mockSuccessInvocation() {
        whenever(this.submit(any())).thenAnswer {
            val job = it.arguments[0] as Job
            job.submitAt(Instant.now())
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