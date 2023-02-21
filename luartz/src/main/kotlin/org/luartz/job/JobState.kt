package org.luartz.job

enum class JobState {
    CREATED,
    SCHEDULED,
    RUNNING,
    SUCCEEDED,
    FAILED
}