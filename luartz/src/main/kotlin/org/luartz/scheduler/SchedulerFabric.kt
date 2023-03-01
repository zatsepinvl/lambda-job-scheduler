package org.luartz.scheduler

import org.luartz.aws.LambdaClientFactory
import org.luartz.deployer.JobDeployer
import org.luartz.deployer.LambdaJobDeployer
import org.luartz.executor.JobExecutor
import org.luartz.executor.LambdaJobExecutor
import org.luartz.scheduler.impl.SchedulerImpl
import org.luartz.store.InMemoryJobStore
import org.luartz.store.MutableJobStore
import software.amazon.awssdk.services.lambda.LambdaClient

object SchedulerFabric {

    fun createDefault(): Scheduler {
        val lambdaClient = LambdaClientFactory.createSync()
        return createDefaultWithLambdaClient(lambdaClient)
    }

    fun createDefaultWithLambdaClient(lambdaClient: LambdaClient): Scheduler {
        val store = InMemoryJobStore()
        val executor = LambdaJobExecutor(lambdaClient)
        val deployer = LambdaJobDeployer(lambdaClient)

        return create(store, deployer, executor)
    }

    fun create(store: MutableJobStore, deployer: JobDeployer, executor: JobExecutor): Scheduler {
        return SchedulerImpl(store, deployer, executor)
    }
}