package org.luartz.executor

import org.luartz.job.Job
import org.luartz.json.Json

data class FunctionInvocationPayload(
    val job: JobInvocationData,
    val payload: Any?
)

data class JobInvocationData(
    val id: String,
    val name: String,
    // Might be extended in the future
)

fun Job.toInvocationPayload(): FunctionInvocationPayload {
    return FunctionInvocationPayload(
        job = JobInvocationData(
            id = this.id,
            name = this.name
        ),
        payload = this.payload
    )
}

fun FunctionInvocationPayload.toJsonString(json: Json): String {
    return json.stringify(this)
}