package org.luartz.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

fun givenFixedClock(timeMillis: Long): Clock {
    return Clock.fixed(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
}