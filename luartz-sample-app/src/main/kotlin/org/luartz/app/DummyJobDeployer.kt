package org.luartz.app

import org.luartz.deployer.DeploymentCommand
import org.luartz.deployer.DeploymentResult
import org.luartz.deployer.DeploymentStatus
import org.luartz.deployer.JobDeployer
import kotlin.random.Random
import kotlin.random.nextLong

class DummyJobDeployer : JobDeployer {
    override fun deploy(command: DeploymentCommand): DeploymentResult {
        Thread.sleep(Random.nextLong(5000L..10000L))
        return DeploymentResult(status = DeploymentStatus.SKIPPED)
    }
}