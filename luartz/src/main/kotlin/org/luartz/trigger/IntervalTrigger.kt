package org.luartz.trigger

import java.time.Clock
import java.time.Duration
import java.time.Instant

class IntervalTrigger(
    val startAt: Instant,
    val interval: Duration,
) : Trigger {
    override fun nextFireTime(clock: Clock): Instant {
        val now = clock.instant()
        return if (now.isBefore(startAt)) {
            startAt.plusMillis(interval.toMillis())
        } else {
            val k: Int = ((now.toEpochMilli() - startAt.toEpochMilli()) / interval.toMillis()).toInt()
            startAt.plusMillis(interval.toMillis() * (k + 1))
        }
    }
}
