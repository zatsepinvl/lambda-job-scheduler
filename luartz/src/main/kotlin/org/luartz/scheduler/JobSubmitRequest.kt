package org.luartz.scheduler

import org.luartz.job.JobDefinition

data class JobSubmitRequest(
    val name: String,
    val definition: JobDefinition,
    val payload: String
)