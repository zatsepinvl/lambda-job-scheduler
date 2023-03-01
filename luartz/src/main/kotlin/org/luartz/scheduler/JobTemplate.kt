package org.luartz.scheduler

import org.luartz.job.Job
import org.luartz.job.JobFunction
import org.luartz.job.JobState
import org.luartz.trigger.Trigger
import java.time.Instant

data class JobTemplate(
    val id: String,
    val jobName: String,
    val function: JobFunction,
    val trigger: Trigger,
    val payload: Any?,
)

fun JobTemplate.toJobWithId(id: String, createdAt: Instant): Job {
    return Job(
        id = id,
        templateId = this.id,
        name = this.jobName,
        createdAt = createdAt,
        function = this.function,
        payload = this.payload,
        state = JobState.CREATED,
        trigger = this.trigger
    )
}