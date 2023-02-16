package org.luartz.trigger

import java.time.Duration
import java.time.Instant

class IntervalTrigger(
    val startAt: Instant,
    val interval: Duration
) : Trigger {
    override fun mayFireAgain() = true

    override fun nextFireTime(): Instant {
        val now = Instant.now()
        val k: Int = ((now.toEpochMilli() - startAt.toEpochMilli()) / interval.toMillis()).toInt()
        return startAt.plusMillis(interval.toMillis() * (k + 1))
    }

    override fun updateAfterFired() {}
}
