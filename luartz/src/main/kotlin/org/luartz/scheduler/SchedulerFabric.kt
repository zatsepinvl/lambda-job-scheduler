package org.luartz.scheduler

import org.luartz.aws.LambdaClientFactory
import org.luartz.executor.JobExecutor
import org.luartz.executor.LambdaJobExecutor
import org.luartz.store.InMemoryJobStore
import org.luartz.store.MutableJobStore
import software.amazon.awssdk.services.lambda.LambdaAsyncClient

object SchedulerFabric {

    fun createDefault(): Scheduler {
        val lambdaClient = LambdaClientFactory.createAsync()
        return createDefaultWithLambdaClient(lambdaClient)
    }

    fun createDefaultWithLambdaClient(lambdaAsyncClient: LambdaAsyncClient): Scheduler {
        val store = InMemoryJobStore()
        val executor = LambdaJobExecutor(lambdaAsyncClient)

        return create(store, executor)
    }

    fun create(store: MutableJobStore, executor: JobExecutor): Scheduler {
        return SchedulerImpl(executor, store)
    }
}