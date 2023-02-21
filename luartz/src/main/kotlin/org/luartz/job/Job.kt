package org.luartz.job

import org.luartz.trigger.Trigger
import java.time.Instant

data class Job(
    val id: String,
    val name: String,
    val definition: JobDefinition,
    val payload: String,
    var state: JobState,
    val trigger: Trigger
) {
    var scheduledExecutionAt: Instant? = null
        private set
    var startedAt: Instant? = null
        private set
    var stoppedAt: Instant? = null
        private set
    var executionError: String? = null
        private set

    val printableId = "${name}:${id}"

    // ToDo: implement state machine invariant validation
    fun scheduleExecutionAt(time: Instant): Job {
        scheduledExecutionAt = time
        state = JobState.SCHEDULED
        return this
    }

    fun runAt(time: Instant): Job {
        startedAt = time
        state = JobState.RUNNING
        return this
    }

    fun succeedAt(time: Instant): Job {
        stoppedAt = time
        state = JobState.SUCCEEDED
        return this
    }

    fun failAt(time: Instant, error: String): Job {
        stoppedAt = time
        state = JobState.FAILED
        executionError = error
        return this
    }
}
