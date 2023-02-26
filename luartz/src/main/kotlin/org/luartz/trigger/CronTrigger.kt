package org.luartz.trigger

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*


class CronTrigger(cronExpression: String) : Trigger {
    private val logger = LoggerFactory.getLogger(CronTrigger::class.java)
    private val executionTime: ExecutionTime

    init {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
        val parser = CronParser(cronDefinition)
        val cron = parser.parse(cronExpression)
        this.executionTime = ExecutionTime.forCron(cron)
        val descriptor = CronDescriptor.instance(Locale.US)
        val description = descriptor.describe(cron)
        logger.debug("New cron trigger created: $description")
    }

    override fun nextFireTime(clock: Clock): Instant {
        val date = ZonedDateTime.now(clock)
        return executionTime.nextExecution(date).get().toInstant()
    }
}