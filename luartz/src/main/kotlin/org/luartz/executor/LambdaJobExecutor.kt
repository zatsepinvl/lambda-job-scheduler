package org.luartz.executor

import org.luartz.job.Job
import org.luartz.json.Json
import org.luartz.json.defaultJson
import org.luartz.util.defaultUtcClock
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvocationType
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.time.Clock


class LambdaJobExecutor(
    private val lambda: LambdaClient,
    private val json: Json = defaultJson(),
    private val clock: Clock = defaultUtcClock()
) : JobExecutor {

    override fun execute(job: Job): Job {
        val payload = job
            .toExecutionPayload()
            .toJsonString(json)
        val payloadBytes: SdkBytes = SdkBytes.fromUtf8String(payload)
        val request = InvokeRequest.builder()
            .functionName(job.function.name)
            .payload(payloadBytes)
            .invocationType(InvocationType.EVENT)
            .build()

        // ToDo: try/catch with retry mechanism in case of recoverable error
        lambda.invoke(request)
        return job.invokedAt(clock.instant())
    }
}