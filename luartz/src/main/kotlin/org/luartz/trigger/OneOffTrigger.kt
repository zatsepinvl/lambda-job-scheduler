package org.luartz.trigger

import java.time.Clock
import java.time.Instant

open class OneOffTrigger(
    val fireAt: Instant
) : Trigger {

    private var active = true

    override fun nextFireTime(clock: Clock): Instant {
        if (!active) {
            throw IllegalStateException("Trigger is no longer active as have already fired once")
        }
        return fireAt
    }

    override fun canFire(): Boolean {
        return active
    }

    override fun whenFired() {
        active = false
    }
}
