package scheduler

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobDefinition
import org.luartz.job.JobState
import org.luartz.scheduler.JobScheduleRequest
import org.luartz.scheduler.Scheduler
import org.luartz.scheduler.SchedulerImpl
import org.luartz.store.MutableJobStore
import org.luartz.trigger.NowTrigger
import org.luartz.trigger.Trigger
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import java.lang.Thread.sleep

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
        val request = givenTestJobScheduleRequest()

        // When
        scheduler.whenScheduledAndAwaited(request)

        // Then
        verify(executor, times(1)).execute(any())
    }

    @Test
    fun storeIsInvoked() {
        // Given
        executor.mockSuccessInvocation()
        val request = givenTestJobScheduleRequest()

        // When
        scheduler.whenScheduledAndAwaited(request)

        // Save on scheduled, run and then executed
        verify(store, times(3)).save(any())
    }

    private fun givenTestJobScheduleRequest(trigger: Trigger = NowTrigger()): JobScheduleRequest {
        return JobScheduleRequest(
            name = "test_name",
            definition = JobDefinition("test", "test"),
            payload = "test_payload",
            trigger = trigger
        )
    }

    private fun JobExecutor.mockSuccessInvocation() {
        whenever(this.execute(any())).thenAnswer {
            val job = it.arguments[0] as Job
            job.state = JobState.SUCCEEDED
            job
        }
    }

    private fun Scheduler.whenScheduledAndAwaited(request: JobScheduleRequest, awaitMillis: Long = 200) {
        this.schedule(request)
        this.start()
        sleep(awaitMillis)
        this.shutdown()
    }
}