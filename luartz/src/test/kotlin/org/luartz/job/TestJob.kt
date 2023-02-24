package org.luartz.job

import org.mockito.kotlin.mock
import java.time.Instant

fun givenTestJob(
    id: String = "id",
    name: String = "name",
    createdAt: Instant = Instant.now()
): Job {
    return Job(
        id = id,
        name = name,
        createdAt = createdAt,
        state = JobState.CREATED,
        trigger = mock(),
        definition = JobDefinition(functionName = "testFunctionName"),
        payload = null
    )
}