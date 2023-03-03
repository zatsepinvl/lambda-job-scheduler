package org.luartz.executor

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.luartz.job.JobState
import org.luartz.job.givenTestJob
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
        assertThat(executedJob.state).isEqualTo(JobState.INVOKED)
    }

    @Test
    fun executeWithFailure() {
        // Given
        val runningJob = givenTestJob()
        val executionError = "testExecutionError"
        whenLambdaInvokedWithError(any(), RuntimeException(executionError))

        // When
        assertThrows<RuntimeException>(executionError) { executor.execute(runningJob) }
    }

    @Test
    fun executeWithTheRightRequest() {
        // Given
        val runningJob = givenTestJob()
        val response = InvokeResponse.builder().statusCode(200).build()
        whenLambdaInvoked(any(), response)

        // When
        executor.execute(runningJob)

        // Then
        argumentCaptor<InvokeRequest> {
            verify(lambdaClient).invoke(capture())
            val request = firstValue

            // Verify lambda params
            assertThat(request.functionName()).isEqualTo(runningJob.function.name)

            // Verify payload
            val payload = defaultJson().parse(request.payload().asUtf8String(), JobExecutionPayload::class.java)
            assertThat(payload.job.id).isEqualTo(runningJob.id)
            assertThat(payload.job.name).isEqualTo(runningJob.name)
            assertThat(payload.payload).isEqualTo(runningJob.payload)
        }
    }

    private fun whenLambdaInvoked(request: InvokeRequest, response: InvokeResponse) {
        whenever(lambdaClient.invoke(request)).thenReturn(response)
    }

    private fun whenLambdaInvokedWithError(request: InvokeRequest, error: Throwable) {
        whenever(lambdaClient.invoke(request)).thenThrow(error)
    }
}