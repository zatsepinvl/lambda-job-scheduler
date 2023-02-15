package org.luartz.scheduler

object SchedulerFabric {
    private val scheduler = SchedulerImpl()

    fun default(): Scheduler {
        return scheduler
    }
}