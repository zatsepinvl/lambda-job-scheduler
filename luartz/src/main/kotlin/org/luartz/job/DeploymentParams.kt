package org.luartz.job

data class DeploymentParams(
    val codeZipPath: String,
    val handler: String,
    val runtime: String,
    val roleArn: String,
)