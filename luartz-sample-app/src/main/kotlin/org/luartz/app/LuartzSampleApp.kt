package org.luartz.app

import org.luartz.job.LambdaDefinition
import org.luartz.scheduler.JobTemplate
import org.luartz.scheduler.SchedulerFabric
import org.luartz.trigger.IntervalTrigger
import org.luartz.trigger.OneOffTrigger
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant


@SpringBootApplication
class LuartzSampleApp

fun main(args: Array<String>) {
    runApplication<LuartzSampleApp>(*args)
}

/**
 * Set AWS_ENDPOINT_URL=http://localhost:4566 environment variable for localstack deployment
 */
@Component
class AppRunner : CommandLineRunner {
    override fun run(vararg args: String?) {
        // Init for test
        // val scheduler = SchedulerFabric.create(InMemoryJobStore(), DummyJobExecutor())

        // Init for real lambda invocation
        val scheduler = SchedulerFabric.createDefault()

        val store = scheduler.jobStore

        val lambdaDefinition = LambdaDefinition("Sample")
        val payload = mapOf(
            "key" to "value"
        )

        // Schedule a recurrent job
        val intervalTrigger = IntervalTrigger(startAt = Instant.now(), interval = Duration.ofSeconds(10))
        scheduler.schedule(
            JobTemplate(
                id = "RecurrentTestJob",
                jobName = "RecurrentTestJob",
                lambda = lambdaDefinition,
                payload = payload,
                trigger = intervalTrigger
            )
        )

        // Schedule a one-off job
        scheduler.schedule(
            JobTemplate(
                id = "OneOffTestJob",
                jobName = "OneOffTestJob",
                lambda = lambdaDefinition,
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
                lambda = lambdaDefinition,
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
}