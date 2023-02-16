package org.luartz.scheduler

import org.luartz.job.JobDefinition
import org.luartz.trigger.Trigger

data class JobScheduleRequest(
    val name: String,
    val definition: JobDefinition,
    val payload: String,
    val trigger: Trigger
)