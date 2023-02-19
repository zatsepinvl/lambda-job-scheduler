package org.luartz.scheduler

import org.luartz.aws.LambdaClientFactory
import org.luartz.executor.LambdaJobExecutor
import org.luartz.store.InMemoryJobStore

object SchedulerFabric {

    fun createDefault(): Scheduler {
        val store = InMemoryJobStore()

        val lambdaClient = LambdaClientFactory.create()
        val executor = LambdaJobExecutor(lambdaClient)

        return SchedulerImpl(executor, store)
    }
}