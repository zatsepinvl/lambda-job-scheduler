package org.luartz.scheduler

import org.luartz.job.Job
import org.luartz.job.definition.JobDefinition
import org.luartz.trigger.Trigger

class SchedulerImpl : Scheduler {
    override fun schedule(definition: JobDefinition, trigger: Trigger): Job {
        TODO("Not yet implemented")
    }

    override fun execute(definition: JobDefinition): Job {
        TODO("Not yet implemented")
    }
}