package org.luartz.executor

import org.luartz.job.Job
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.time.Clock


class LambdaJobExecutor(
    private val lambdaClient: LambdaClient,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobExecutor {

    override fun execute(job: Job): Job {
        val payload: SdkBytes = SdkBytes.fromUtf8String(job.payload)
        val request = InvokeRequest.builder()
            .functionName(job.definition.functionName)
            .payload(payload)
            .build()

        val response = lambdaClient.invoke(request)

        return if (response.functionError() == null) {
            job.succeedAt(clock.instant())
        } else {
            job.failAt(clock.instant(), response.functionError())
        }
    }
}