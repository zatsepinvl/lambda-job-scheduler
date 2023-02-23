package org.luartz.executor

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.luartz.job.Job
import org.luartz.job.JobDefinition
import org.luartz.job.JobState
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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

    private fun givenTestJob(): Job {
        return Job(
            id = "testId",
            name = "testName",
            definition = JobDefinition("testFunctionName"),
            payload = "{}",
            state = JobState.RUNNING,
            trigger = mock()
        )
    }
}