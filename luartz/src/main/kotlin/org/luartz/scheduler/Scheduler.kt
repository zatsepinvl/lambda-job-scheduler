package org.luartz.scheduler

import org.luartz.store.JobStore

interface Scheduler {
    val jobStore: JobStore

    val jobTemplates: List<JobTemplate>

    fun schedule(template: JobTemplate)

    fun unschedule(templateId: String)

    fun start()

    fun shutdown()
}