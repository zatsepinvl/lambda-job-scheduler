package org.luartz.job

data class JobFunction(
    val name: String,
    val description: String = "",
    /**
     * If specified, scheduler will try to deploy a lambda function first.
     * Otherwise, the function defined by name property should already be available for invocation.
     */
    val deployment: DeploymentParams? = null
)