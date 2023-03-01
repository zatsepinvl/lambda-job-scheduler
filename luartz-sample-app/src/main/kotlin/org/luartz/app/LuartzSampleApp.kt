package org.luartz.app

import org.luartz.job.JobFunction
import org.luartz.scheduler.JobTemplate
import org.luartz.scheduler.SchedulerFabric
import org.luartz.trigger.CronTrigger
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
        //val scheduler = SchedulerFabric.create(InMemoryJobStore(), DummyJobExecutor(), DummyJobDeployer())

        // Init for real lambda invocation
        val scheduler = SchedulerFabric.createDefault()

        val store = scheduler.getJobStore()
        val functionDefinition = JobFunction("Sample")
        val payload = mapOf("key" to "value")

        // Schedule a recurrent job
        val intervalTrigger = IntervalTrigger(startAt = Instant.now(), interval = Duration.ofSeconds(5))
        scheduler.schedule(
            JobTemplate(
                id = "RecurrentTestJob",
                jobName = "RecurrentTestJob",
                function = functionDefinition,
                payload = payload,
                trigger = intervalTrigger
            )
        )

        // Schedule a one-off job
        scheduler.schedule(
            JobTemplate(
                id = "OneOffTestJob",
                jobName = "OneOffTestJob",
                function = functionDefinition,
                payload = payload,
                trigger = OneOffTrigger(Instant.now().plusSeconds(10))
            )
        )

        // Start
        scheduler.start()

        sleepForSeconds(10)

        // Unschedule
        println("Unscheduling recurrent job")
        scheduler.unschedule("RecurrentTestJob")

        // Schedule when already started
        println("Scheduling another recurrent job")
        scheduler.schedule(
            JobTemplate(
                id = "LateSubmittedJob",
                jobName = "LateSubmittedJob",
                function = functionDefinition,
                payload = payload,
                trigger = CronTrigger("*/2 * * * * ?")
            )
        )

        sleepForSeconds(10)

        // Print current state
        println("Job templates: ${scheduler.getJobTemplates()}")
        println("Jobs: ${store.getJobsByName("RecurrentTestJob")}")

        println("Shutting down scheduler...")
        scheduler.shutdown()
    }
}

private fun sleepForSeconds(seconds: Long) {
    println("Sleeping for $seconds seconds...")
    Thread.sleep(seconds * 1000)
}