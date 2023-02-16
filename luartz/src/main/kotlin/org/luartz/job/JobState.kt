package org.luartz.job

enum class JobState {
    CREATED,
    SUBMITTED,
    RUNNING,
    SUCCEEDED,
    FAILED
}