package org.luartz.app

import org.luartz.job.JobDefinition
import org.luartz.scheduler.JobScheduleRequest
import org.luartz.scheduler.JobSubmitRequest
import org.luartz.scheduler.SchedulerFabric
import org.luartz.trigger.IntervalTrigger
import org.luartz.trigger.OneOffTrigger
import java.time.Duration
import java.time.Instant


/**
 * Set AWS_ENDPOINT_URL=http://localhost:4566 environment variable for localstack deployment
 */
fun main() {
    // Init
    val scheduler = SchedulerFabric.createDefault()
    val store = scheduler.getStore()

    val jobDefinition = JobDefinition("d1", "TestSuccess")
    val payload = "{\"key\": \"value\"}"

    // Schedule a recurrent job
    val trigger = IntervalTrigger(startAt = Instant.now(), interval = Duration.ofSeconds(2))
    scheduler.schedule(
        JobScheduleRequest(
            name = "RecurrentTestJob",
            definition = jobDefinition,
            payload = payload,
            trigger = trigger
        )
    )

    // Schedule a one-off job
    scheduler.schedule(
        JobScheduleRequest(
            name = "OneOffTestJob",
            definition = jobDefinition,
            payload = payload,
            trigger = OneOffTrigger(Instant.now().plusSeconds(4))
        )
    )

    // Start
    scheduler.start()

    Thread.sleep(10000)

    // Submit when already started
    scheduler.submit(
        JobSubmitRequest(
            name = "LateSubmittedJob",
            definition = jobDefinition,
            payload = payload
        )
    )

    // Print jobs
    val jobs = store.getJobsByName("RecurrentTestJob")
    println(jobs)


    //Thread.sleep(100000)

    scheduler.shutdown()
}
