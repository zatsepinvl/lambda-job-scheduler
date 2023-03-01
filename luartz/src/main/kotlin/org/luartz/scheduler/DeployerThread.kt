package org.luartz.scheduler

import org.luartz.deployer.JobDeployer
import org.luartz.deployer.toDeploymentCommand
import org.luartz.util.WorkerThread
import java.util.concurrent.ExecutorService

internal class DeployerThread(
    private val deploymentQueue: DeploymentQueue,
    private val scheduleQueue: ScheduleQueue,
    private val executor: ExecutorService,
    private val jobDeployer: JobDeployer,
) : WorkerThread("DeployerThread") {

    override fun runInInfiniteLoop() {
        val template = deploymentQueue.take()
        deployAsync(template)
    }

    private fun deployAsync(template: JobTemplate) {
        executor.submit {
            jobDeployer.deploy(template.function.toDeploymentCommand())
            scheduleQueue.add(template)
        }
    }
}