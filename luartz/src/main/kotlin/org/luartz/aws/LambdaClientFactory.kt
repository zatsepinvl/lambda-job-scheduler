package org.luartz.aws

import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import java.net.URI

private const val AWS_ENDPOINT_URL = "AWS_ENDPOINT_URL"

object LambdaClientFactory {
    fun createAsync(): LambdaAsyncClient {
        val endpointUrl = System.getenv(AWS_ENDPOINT_URL)
        return if (endpointUrl != null) {
            LambdaAsyncClient.builder()
                .endpointOverride(URI.create(endpointUrl))
                .build()
        } else {
            LambdaAsyncClient.create()
        }
    }
}