package org.luartz.deployer

interface JobDeployer {
    fun deploy(command: DeploymentCommand): DeploymentResult
}