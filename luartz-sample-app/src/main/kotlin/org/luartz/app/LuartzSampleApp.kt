package org.luartz.app

import org.luartz.job.JobDefinition
import org.luartz.scheduler.JobScheduleRequest
import org.luartz.scheduler.JobSubmitRequest
import org.luartz.scheduler.SchedulerFabric
import org.luartz.trigger.IntervalTrigger
import org.luartz.trigger.OneOffTrigger
import java.time.Duration
import java.time.Instant


fun main() {
    // Init
    val scheduler = SchedulerFabric.default()
    val store = scheduler.getStore()

    val jobDefinition = JobDefinition("d1", "test")

    // Schedule a recurrent job
    val trigger = IntervalTrigger(startAt = Instant.now(), interval = Duration.ofSeconds(2))
    scheduler.schedule(
        JobScheduleRequest(
            name = "RecurrentTestJob",
            definition = jobDefinition,
            payload = "test",
            trigger = trigger
        )
    )

    // Schedule a one-off job
    scheduler.schedule(
        JobScheduleRequest(
            name = "OneOffTestJob",
            definition = jobDefinition,
            payload = "test",
            trigger = OneOffTrigger(Instant.now().plusSeconds(4))
        )
    )

    // Start
    scheduler.start()

    Thread.sleep(5000)

    // Submit when already started
    scheduler.submit(
        JobSubmitRequest(
            name = "LateSubmittedJob",
            definition = jobDefinition,
            payload = "test"
        )
    )

    // Print jobs
    val jobs = store.getJobsByName("RecurrentTestJob")
    println(jobs)
}
