package org.luartz.app

import org.luartz.job.JobDefinition
import org.luartz.scheduler.JobTemplate
import org.luartz.scheduler.SchedulerFabric
import org.luartz.trigger.IntervalTrigger
import org.luartz.trigger.OneOffTrigger
import java.time.Duration
import java.time.Instant


/**
 * Set AWS_ENDPOINT_URL=http://localhost:4566 environment variable for localstack deployment
 */
fun main() {
    // Init for test
    val scheduler = SchedulerFabric.create(InMemoryJobStore(), DummyJobExecutor())

    // Init for real lambda invocation
    //val scheduler = SchedulerFabric.createDefault()

    val store = scheduler.jobStore

    val jobDefinition = JobDefinition("TestSuccess")
    val payload = "{\"key\": \"value\"}"

    // Schedule a recurrent job
    val intervalTrigger = IntervalTrigger(startAt = Instant.now(), interval = Duration.ofSeconds(10))
    scheduler.schedule(
        JobTemplate(
            id = "RecurrentTestJob",
            jobName = "RecurrentTestJob",
            definition = jobDefinition,
            payload = payload,
            trigger = intervalTrigger
        )
    )

    // Schedule a one-off job
    scheduler.schedule(
        JobTemplate(
            id = "OneOffTestJob",
            jobName = "OneOffTestJob",
            definition = jobDefinition,
            payload = payload,
            trigger = OneOffTrigger(Instant.now().plusSeconds(10))
        )
    )

    // Start
    scheduler.start()

    // Get job templates
    println("Job templates: ${scheduler.jobTemplates}")

    println("Sleeping for 10 seconds...")
    Thread.sleep(10000)

    // Unschedule
    println("Unscheduling recurrent job")
    scheduler.unschedule("RecurrentTestJob")

    // Schedule when already started
    println("Scheduling another recurrent job")
    scheduler.schedule(
        JobTemplate(
            id = "LateSubmittedJob",
            jobName = "LateSubmittedJob",
            definition = jobDefinition,
            payload = payload,
            trigger = intervalTrigger
        )
    )

    // Print jobs
    val jobs = store.getJobsByName("RecurrentTestJob")
    println(jobs)

    println("Sleeping for 10 seconds...")
    Thread.sleep(5000)

    println("Shutting down scheduler...")
    scheduler.shutdown()
}