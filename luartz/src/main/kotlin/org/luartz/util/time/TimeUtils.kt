package org.luartz.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

internal fun defaultUtcClock(): Clock = Clock.systemUTC()

internal fun Instant.toUtcDate(): ZonedDateTime {
    return ZonedDateTime.ofInstant(this, ZoneId.of("UTC"))
}