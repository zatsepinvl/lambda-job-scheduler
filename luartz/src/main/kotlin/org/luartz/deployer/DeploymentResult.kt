package org.luartz.deployer

data class DeploymentResult(
    val status: DeploymentStatus
)


enum class DeploymentStatus {
    CREATED,
    SKIPPED,
    // ToDo: possible feature to update code and config if function already exists
    UPDATED
}