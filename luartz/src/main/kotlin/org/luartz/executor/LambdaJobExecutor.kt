package org.luartz.executor

import org.luartz.job.Job
import org.luartz.json.Json
import org.luartz.json.defaultJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.time.Clock
import java.util.concurrent.CompletableFuture


class LambdaJobExecutor(
    private val lambdaClient: LambdaAsyncClient,
    private val json: Json = defaultJson(),
    private val clock: Clock = Clock.systemDefaultZone()
) : JobExecutor {

    private val logger: Logger = LoggerFactory.getLogger(LambdaJobExecutor::class.java)

    override fun execute(job: Job): CompletableFuture<Job> {
        val payload = job
            .toExecutionPayload()
            .toJsonString(json)
        val payloadBytes: SdkBytes = SdkBytes.fromUtf8String(payload)
        val request = InvokeRequest.builder()
            .functionName(job.definition.functionName)
            .payload(payloadBytes)
            .build()

        return lambdaClient.invoke(request)
            .thenApply { response ->
                val output = response.payload()?.asUtf8String() ?: "<empty>"
                logger.debug("Job ${job.id} execution output:\n${output}")

                if (response.functionError() == null) {
                    job.succeedAt(clock.instant())
                } else {
                    job.failAt(clock.instant(), response.functionError())
                }
            }
    }
}