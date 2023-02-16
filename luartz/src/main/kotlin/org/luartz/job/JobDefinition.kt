package org.luartz.job

data class JobDefinition(
    val id: String,
    val lambdaArn: String
)
