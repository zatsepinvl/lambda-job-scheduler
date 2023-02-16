package org.luartz.scheduler

import org.luartz.executor.JobExecutor
import org.luartz.store.InMemoryJobStore

object SchedulerFabric {

    private val scheduler = SchedulerImpl(
        JobExecutor(),
        InMemoryJobStore()
    )

    fun default(): Scheduler {
        return scheduler
    }
}