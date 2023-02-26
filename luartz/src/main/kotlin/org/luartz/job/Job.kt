package org.luartz.job

import org.luartz.trigger.Trigger
import java.time.Instant

data class Job(
    val id: String,
    val name: String,
    val lambda: LambdaDefinition,
    val payload: Any?,
    val state: JobState,
    val trigger: Trigger,
    val createdAt: Instant,
    val scheduledExecutionAt: Instant? = null,
    val startedAt: Instant? = null,
    val stoppedAt: Instant? = null,
    val executionError: String? = null
) {
    // ToDo: implement state machine invariant validation
    fun scheduleExecutionAt(time: Instant): Job {
        return copy(state = JobState.SCHEDULED, scheduledExecutionAt = time)
    }

    fun runAt(time: Instant): Job {
        return copy(state = JobState.RUNNING, startedAt = time)
    }

    fun succeedAt(time: Instant): Job {
        return copy(state = JobState.SUCCEEDED, stoppedAt = time)
    }

    fun failAt(time: Instant, error: String): Job {
        return copy(state = JobState.FAILED, stoppedAt = time, executionError = error)
    }
}
