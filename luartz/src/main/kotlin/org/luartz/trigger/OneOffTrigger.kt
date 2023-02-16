package org.luartz.trigger

import java.time.Instant

class OneOffTrigger(
    val fireAt: Instant
) : Trigger {

    private var active = true

    override fun mayFireAgain(): Boolean {
        return active
    }

    override fun nextFireTime(): Instant {
        if (!active) {
            throw IllegalStateException("Trigger is no longer active as have already fired once")
        }
        return fireAt
    }

    override fun updateAfterFired() {
        active = false
    }
}
