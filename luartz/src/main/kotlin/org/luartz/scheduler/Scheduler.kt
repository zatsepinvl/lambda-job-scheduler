package org.luartz.scheduler

import org.luartz.job.Job
import org.luartz.job.definition.JobDefinition
import org.luartz.trigger.Trigger

interface Scheduler {
    fun schedule(definition: JobDefinition, trigger: Trigger): Job

    fun execute(definition: JobDefinition): Job
}