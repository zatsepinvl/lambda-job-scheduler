package org.luartz.scheduler

import org.luartz.store.JobStore

interface Scheduler {
    fun schedule(template: JobTemplate)

    fun unschedule(templateId: String)

    fun start()

    fun shutdown()

    fun getJobStore(): JobStore

    fun getJobTemplates(): List<JobTemplate>
}