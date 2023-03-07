package org.luartz.job

enum class JobState {
    CREATED,
    SCHEDULED,
    SUBMITTED,
    SUBMISSION_FAILED
}