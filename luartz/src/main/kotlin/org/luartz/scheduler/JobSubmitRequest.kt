package org.luartz.scheduler

import org.luartz.job.Job
import org.luartz.job.JobDefinition
import org.luartz.job.JobState

data class JobSubmitRequest(
    val name: String,
    val definition: JobDefinition,
    val payload: String
)

fun JobSubmitRequest.toJobWithId(id: String): Job {
    return Job(
        id = id,
        name = this.name,
        definition = this.definition,
        payload = this.payload,
        state = JobState.CREATED,
        trigger = null
    )
}