package org.luartz.aws

import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.LambdaClient
import java.net.URI

private const val AWS_LAMBDA_ENDPOINT_URL = "AWS_LAMBDA_ENDPOINT_URL"

object LambdaClientFactory {
    fun createAsync(): LambdaAsyncClient {
        return LambdaAsyncClient.builder()
            .apply { getEndpointUrl()?.let { endpointOverride(it) } }
            .build()
    }

    fun createSync(): LambdaClient {
        return LambdaClient.builder()
            .apply { getEndpointUrl()?.let { endpointOverride(it) } }
            .build()
    }

    private fun getEndpointUrl(): URI? {
        val endpointUrl = System.getenv(AWS_LAMBDA_ENDPOINT_URL)
        return if (endpointUrl != null) {
            URI.create(endpointUrl)
        } else {
            null
        }
    }
}