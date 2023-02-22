package org.luartz.scheduler

import org.luartz.job.Job
import org.luartz.job.JobDefinition
import org.luartz.job.JobState
import org.luartz.trigger.Trigger

data class JobTemplate(
    val id: String,
    val jobName: String,
    val definition: JobDefinition,
    val payload: String,
    val trigger: Trigger
)

fun JobTemplate.toJobWithId(id: String): Job {
    return Job(
        id = id,
        name = this.jobName,
        definition = this.definition,
        payload = this.payload,
        state = JobState.CREATED,
        trigger = this.trigger
    )
}