package org.luartz.executor

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.luartz.job.Job
import org.luartz.job.JobDefinition
import org.luartz.job.JobState
import org.luartz.json.defaultJson
import org.mockito.kotlin.*
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse

class LambdaJobExecutorTest {
    private lateinit var lambdaClient: LambdaClient
    private lateinit var executor: JobExecutor

    @BeforeEach
    fun setup() {
        lambdaClient = mock()
        executor = LambdaJobExecutor(lambdaClient)
    }

    @Test
    fun executeWithSuccess() {
        // Given
        val runningJob = givenTestJob()
        val response = InvokeResponse.builder()
            .statusCode(200)
            .build()
        whenever(lambdaClient.invoke(any<InvokeRequest>())).thenReturn(response)

        // When
        val executedJob = executor.execute(runningJob)

        // Then
        assertThat(executedJob.state).isEqualTo(JobState.SUCCEEDED)
    }

    @Test
    fun executeWithFailure() {
        // Given
        val runningJob = givenTestJob()
        val functionError = "testFunctionError"
        val response = InvokeResponse.builder()
            .statusCode(200)
            .functionError(functionError)
            .build()
        whenever(lambdaClient.invoke(any<InvokeRequest>())).thenReturn(response)

        // When
        val executedJob = executor.execute(runningJob)

        // Then
        assertThat(executedJob.state).isEqualTo(JobState.FAILED)
        assertThat(executedJob.executionError).isEqualTo(functionError)
    }

    @Test
    fun executeWithTheRightRequest() {
        // Given
        val runningJob = givenTestJob()
        val response = InvokeResponse.builder().statusCode(200).build()
        whenever(lambdaClient.invoke(any<InvokeRequest>())).thenReturn(response)

        // When
        executor.execute(runningJob)

        // Then
        argumentCaptor<InvokeRequest> {
            verify(lambdaClient).invoke(capture())
            val request = firstValue

            // Verify lambda params
            assertThat(request.functionName()).isEqualTo(runningJob.definition.functionName)

            // Verify payload
            val payload = defaultJson().parse(request.payload().asUtf8String(), JobExecutionPayload::class.java)
            assertThat(payload.job.id).isEqualTo(runningJob.id)
            assertThat(payload.job.name).isEqualTo(runningJob.name)
            assertThat(payload.payload).isEqualTo(runningJob.payload)
        }
    }

    private fun givenTestJob(): Job {
        return Job(
            id = "testId",
            name = "testName",
            definition = JobDefinition("testFunctionName"),
            payload = mapOf(
                "testKey" to "testValue"
            ),
            state = JobState.RUNNING,
            trigger = mock()
        )
    }
}