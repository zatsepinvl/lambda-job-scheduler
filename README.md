# Luartz - Lambda Job Scheduler

Quartz-like job scheduler based on AWS Lambda

🚀 Project Status: Proof of Concept development 🚀

## Feature Coverage

| Feature                                  | Status            |
|------------------------------------------|-------------------|
| Submitting job                           | ✅ Implemented     |
| Scheduling one-off job                   | ✅ Implemented     |
| Scheduling recurring job                 | ✅ Implemented     |
| Scheduling cron job                      | 🏃 Coming soon... |
| Unscheduling job                         | ✅ Implemented     |
| Accessing job details in lambda function | ✅ Implemented     |
| Getting jobs statuses                    | ✅ Implemented     |
| Getting job schedules                    | ✅ Implemented     |
| Listening for job status changes         | 🏃 Coming soon... |
| Lambda automatic deployment              | 🏃 Coming soon... |
| AWS Lambda throttling handling           | 🤔 Sometime       |
| Misfire handling                         | 🤔 Sometime       |
| Job persistent store                     | 🤔 Sometime       |

## Getting Started

Add dependency:

```groovy
dependencies {
    ...
    implementation "org.laurtz:luartz:1.0.0"
}
```

Create Scheduler instance:

```kotlin
val scheduler = SchedulerFabric.createDefault()
```

Create a trigger:

```kotlin
val trigger = IntervalTrigger(...) 
```
Create a job template:

```kotlin
val jobTemplate = JobTempalte(jobName = "MyJob", trigger = trigger, ...)
```

Schedule a job:

```kotlin
sheudler.schedule(jobTemplate)
```

Get jobs statuses by name:

```kotlin
val store = scheduler.jobStore
val jobs = store.getJobsByName("MyJob")
```

## Example app

See [luartz-sample-app](luartz-sample-app/src/main/kotlin/org/luartz/app/LuartzSampleApp.kt) for a complete example.

## Architecture

![logical-architecture.png](assets/logical-architecture.png)

