package org.luartz.scheduler

import org.luartz.store.JobStore

interface Scheduler {
    /**
     * Schedule a job for execution. Do not take any effect until #start is invoked.
     */
    fun schedule(template: JobTemplate)

    /**
     * Unschedule a job from execution. Do not affect already running jobs.
     */
    fun unschedule(templateId: String)

    /**
     * Start scheduling and executing jobs. Do not block current thread.
     */
    fun start()

    /**
     * Shutdown scheduler. Might block current thread until all internal resources are terminated.
     */
    fun shutdown()

    /**
     * Returns instance of currently using job store.
     */
    fun getJobStore(): JobStore

    /**
     * Get the list of all currently scheduled job templates.
     */
    fun getJobTemplates(): List<JobTemplate>
}