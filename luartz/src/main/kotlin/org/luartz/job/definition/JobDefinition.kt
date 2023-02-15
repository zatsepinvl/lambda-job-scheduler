package org.luartz.job.definition

data class JobDefinition(
    val id: String,
    val group: String,
    val lambdaArn: String
)
