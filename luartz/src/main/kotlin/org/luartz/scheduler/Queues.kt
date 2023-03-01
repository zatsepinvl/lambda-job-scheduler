package org.luartz.scheduler

import org.luartz.job.Job
import java.util.concurrent.LinkedBlockingQueue

// ToDo: review the choice of LinkedBlockingQueue
internal class DeploymentQueue : LinkedBlockingQueue<JobTemplate>()

internal class ScheduleQueue : LinkedBlockingQueue<JobTemplate>()

internal class ExecutionQueue : LinkedBlockingQueue<Job>()

