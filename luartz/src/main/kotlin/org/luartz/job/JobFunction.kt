package org.luartz.job

data class JobFunction(
    val name: String,
    val description: String = "",
    val deployment: DeploymentParams? = null
)