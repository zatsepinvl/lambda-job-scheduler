package scheduler

import org.junit.jupiter.api.Test
import org.luartz.executor.JobExecutor
import org.luartz.job.Job
import org.luartz.job.JobDefinition
import org.luartz.job.JobState
import org.luartz.scheduler.JobScheduleRequest
import org.luartz.scheduler.SchedulerImpl
import org.luartz.store.MutableJobStore
import org.luartz.trigger.NowTrigger
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import java.lang.Thread.sleep

class SchedulerImplTest {

    @Test
    fun smokeTest() {
        // Given
        val executor: JobExecutor = mock()
        val store: MutableJobStore = mock()

        whenever(executor.execute(any())).thenAnswer {
            val job = it.arguments[0] as Job
            job.state = JobState.SUCCEEDED
            job
        }
        val scheduler = SchedulerImpl(executor, store)

        // When
        scheduler.schedule(
            JobScheduleRequest(
                name = "test_name",
                definition = JobDefinition("test", "test"),
                payload = "test_payload",
                trigger = NowTrigger()
            )
        )
        scheduler.start()
        sleep(100)
        scheduler.shutdown()

        // Then
        verify(executor, times(1)).execute(any())
        // Save on submitted and then executed
        verify(store, times(2)).save(any())
    }
}