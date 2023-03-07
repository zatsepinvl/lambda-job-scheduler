package org.luartz.job

import org.luartz.trigger.Trigger
import java.time.Instant

data class Job(
    val id: String,
    val templateId: String,
    val name: String,
    val function: JobFunction,
    val payload: Any?,
    val state: JobState,
    val trigger: Trigger,
    val createdAt: Instant,
    val scheduledSubmissionAt: Instant? = null,
    val submittedAt: Instant? = null,
    val submissionError: String? = null
) {
    // ToDo: implement state machine invariant validation
    fun scheduleSubmissionAt(time: Instant): Job {
        return copy(state = JobState.SCHEDULED, scheduledSubmissionAt = time)
    }

    fun submitAt(time: Instant): Job {
        return copy(state = JobState.SUBMITTED, submittedAt = time)
    }

    fun submissionFailAt(time: Instant, error: String): Job {
        return copy(state = JobState.SUBMISSION_FAILED, submittedAt = time, submissionError = error)
    }
}
