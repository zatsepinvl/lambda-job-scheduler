package org.luartz.scheduler

import org.luartz.store.JobStore

interface Scheduler {
    val jobStore: JobStore

    fun schedule(request: JobScheduleRequest)

    fun start()

    fun shutdown()
}