package org.luartz.deployer

import org.luartz.job.JobFunction

data class DeploymentCommand(
    val functionName: String,
    val description: String,
    val codeZipPath: String,
    val handler: String,
    val runtime: String,
    val roleArn: String,
)

fun JobFunction.toDeploymentCommand(): DeploymentCommand {
    if (this.deployment == null) {
        throw IllegalArgumentException("Deployment params should be specified to deploy a function")
    }
    return DeploymentCommand(
        functionName = this.name,
        description = this.description,
        codeZipPath = this.deployment.codeZipPath,
        handler = this.deployment.handler,
        runtime = this.deployment.runtime,
        roleArn = this.deployment.roleArn
    )
}