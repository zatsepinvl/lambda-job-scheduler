package org.luartz.job

import org.luartz.trigger.Trigger
import java.time.Instant

data class Job(
    val id: String,
    val name: String,
    val definition: JobDefinition,
    val payload: String,
    var state: JobState,
    val trigger: Trigger
) {
    var startedAt: Instant? = null
    var stoppedAt: Instant? = null
    var executionError: String? = null

    val printableId = "${name}:${id}"
}
