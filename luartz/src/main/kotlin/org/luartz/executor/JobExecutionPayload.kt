package org.luartz.executor

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.luartz.job.Job
import org.luartz.json.Json

@Serializable
data class JobExecutionPayload(
    val job: JobExecutionData,
    @Contextual
    val payload: Any?
)

@Serializable
data class JobExecutionData(
    val id: String,
    val name: String,
    // Might be extended in the future
)

fun Job.toExecutionPayload(): JobExecutionPayload {
    return JobExecutionPayload(
        job = JobExecutionData(
            id = this.id,
            name = this.name
        ),
        payload = this.payload
    )
}

fun JobExecutionPayload.toJsonString(json: Json): String {
    return json.stringify(this)
}