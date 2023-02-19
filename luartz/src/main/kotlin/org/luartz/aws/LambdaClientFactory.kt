package org.luartz.aws

import software.amazon.awssdk.services.lambda.LambdaClient
import java.net.URI

private const val AWS_ENDPOINT_ENV = "AWS_ENDPOINT_URL"

object LambdaClientFactory {
    fun create(): LambdaClient {
        val endpointUrl = System.getenv(AWS_ENDPOINT_ENV)
        return if (endpointUrl != null) {
            LambdaClient.builder()
                .endpointOverride(URI.create(endpointUrl))
                .build()
        } else {
            LambdaClient.create()
        }
    }
}