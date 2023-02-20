package org.luartz.scheduler

import org.luartz.aws.LambdaClientFactory
import org.luartz.executor.JobExecutor
import org.luartz.executor.LambdaJobExecutor
import org.luartz.store.InMemoryJobStore
import org.luartz.store.MutableJobStore

object SchedulerFabric {

    fun create(store: MutableJobStore, executor: JobExecutor): Scheduler {
        return SchedulerImpl(executor, store)
    }

    fun createDefault(): Scheduler {
        val store = InMemoryJobStore()

        val lambdaClient = LambdaClientFactory.create()
        val executor = LambdaJobExecutor(lambdaClient)

        return create(store, executor)
    }
}