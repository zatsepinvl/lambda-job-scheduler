package org.luartz.job

import org.mockito.kotlin.mock
import java.time.Instant

fun givenTestJob(
    id: String = "id",
    templateId: String = "templateId",
    name: String = "name",
    createdAt: Instant = Instant.now()
): Job {
    return Job(
        id = id,
        templateId = templateId,
        name = name,
        createdAt = createdAt,
        state = JobState.CREATED,
        trigger = mock(),
        lambda = LambdaDefinition(functionName = "testFunctionName"),
        payload = null
    )
}