package org.luartz.scheduler.impl

import org.luartz.job.Job
import org.luartz.scheduler.JobTemplate
import java.util.concurrent.LinkedBlockingQueue

// ToDo: review the choice of LinkedBlockingQueue
internal class DeploymentQueue : LinkedBlockingQueue<JobTemplate>()

internal class ScheduleQueue : LinkedBlockingQueue<JobTemplate>()

internal class SubmissionQueue : LinkedBlockingQueue<Job>()

