package org.luartz.scheduler

import org.luartz.store.JobStore

interface Scheduler {
    fun schedule(request: JobScheduleRequest)
    fun submit(request: JobSubmitRequest)
    fun start()
    fun shutdown()
    fun getStore(): JobStore
}