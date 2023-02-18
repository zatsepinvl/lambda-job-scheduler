package org.luartz.executor

import org.luartz.job.Job
import org.luartz.job.JobState
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest


class LambdaJobExecutor(private val lambdaClient: LambdaClient) : JobExecutor {

    override fun execute(job: Job): Job {
        val payload: SdkBytes = SdkBytes.fromUtf8String(job.payload)
        val request = InvokeRequest.builder()
            .functionName(job.definition.functionName)
            .payload(payload)
            .build()

        val response = lambdaClient.invoke(request)

        if (response.statusCode() == 200) {
            job.state = JobState.SUCCEEDED
        } else {
            job.state = JobState.FAILED
            job.executionError = response.functionError()
        }

        return job
    }
}