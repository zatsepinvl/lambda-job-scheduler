package org.luartz.trigger

import java.time.Clock
import java.time.Duration
import java.time.Instant

class IntervalTrigger(
    val startAt: Instant,
    val interval: Duration,
    val clock: Clock = Clock.systemDefaultZone(),
) : Trigger {
    override fun mayFireAgain() = true

    override fun nextFireTime(): Instant {
        val now = clock.instant()
        return if (now.isBefore(startAt)) {
            startAt.plusMillis(interval.toMillis())
        } else {
            val k: Int = ((now.toEpochMilli() - startAt.toEpochMilli()) / interval.toMillis()).toInt()
            startAt.plusMillis(interval.toMillis() * (k + 1))
        }
    }

    override fun updateAfterFired() {}
}
