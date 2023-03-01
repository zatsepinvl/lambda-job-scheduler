package org.luartz.executor

import org.luartz.job.Job
import org.luartz.json.Json
import org.luartz.json.defaultJson
import org.luartz.util.defaultUtcClock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.time.Clock


class LambdaJobExecutor(
    private val lambda: LambdaClient,
    private val json: Json = defaultJson(),
    private val clock: Clock = defaultUtcClock()
) : JobExecutor {

    private val logger: Logger = LoggerFactory.getLogger(LambdaJobExecutor::class.java)

    override fun execute(job: Job): Job {
        val payload = job
            .toExecutionPayload()
            .toJsonString(json)
        val payloadBytes: SdkBytes = SdkBytes.fromUtf8String(payload)
        val request = InvokeRequest.builder()
            .functionName(job.function.name)
            .payload(payloadBytes)
            .build()

        val response =  lambda.invoke(request)
        val output = response.payload()?.asUtf8String() ?: "<empty>"
        logger.debug("Job ${job.id} execution output:\n${output}")

        return if (response.functionError() == null) {
            job.succeedAt(clock.instant())
        } else {
            job.failAt(clock.instant(), response.functionError())
        }
    }
}