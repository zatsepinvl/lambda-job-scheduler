package org.luartz.scheduler

import org.luartz.executor.DummyJobExecutor
import org.luartz.store.InMemoryJobStore

object SchedulerFabric {

    private val scheduler = SchedulerImpl(
        DummyJobExecutor(),
        InMemoryJobStore()
    )

    fun default(): Scheduler {
        return scheduler
    }
}