# Luartz - Lambda Job Scheduler

Quartz-like job scheduler based on AWS Lambda

🚀 Project Status: Proof of Concept development 🚀

## Feature Coverage

| Feature                        | Status            |
|--------------------------------|-------------------|
| Submitting job                 | ✅ Implemented     |
| Scheduling one-off job         | ✅ Implemented     |
| Scheduling recurring job       | ✅ Implemented     |
| Scheduling cron job            | 🏃 Coming soon... |
| Getting jobs statuses          | ✅ Implemented     |
| AWS Lambda throttling handling | 🏃 Coming soon... |
| Job execution events           | 🏃 Coming soon... |
| Misfire handling               | 🏃 Coming soon... |
| Job persistent store           | 🏃 Coming soon... |

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

Create a job definition:
```kotlin
val jobDefinition = JobDefinition(...)
```

Schedule a job:
```kotlin
sheudler.schedule(JobScheduleRequest(name = "MyJob", ...))
```

Get jobs statuses by name:
```kotlin
val jobs = scheduler.getJobsByName("MyJob")
```


## Example app
See [luartz-sample-app](luartz-sample-app/src/main/kotlin/org/luartz/app/LuartzSampleApp.kt) for a complete example.

## Architecture

![logical-architecture.png](assets/logical-architecture.png)

