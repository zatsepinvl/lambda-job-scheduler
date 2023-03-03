package org.luartz.job

enum class JobState {
    CREATED,
    SCHEDULED,
    INVOKED,
    FAILED
}