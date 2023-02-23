package org.luartz.executor

import org.luartz.job.Job
import org.luartz.json.Json
import org.luartz.json.defaultJson
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.time.Clock


class LambdaJobExecutor(
    private val lambdaClient: LambdaClient,
    private val json: Json = defaultJson(),
    private val clock: Clock = Clock.systemDefaultZone()
) : JobExecutor {

    override fun execute(job: Job): Job {
        val payload = job
            .toExecutionPayload()
            .toJsonString(json)
        val payloadBytes: SdkBytes = SdkBytes.fromUtf8String(payload)
        val request = InvokeRequest.builder()
            .functionName(job.definition.functionName)
            .payload(payloadBytes)
            .build()

        val response = lambdaClient.invoke(request)

        return if (response.functionError() == null) {
            job.succeedAt(clock.instant())
        } else {
            job.failAt(clock.instant(), response.functionError())
        }
    }
}