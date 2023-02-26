package org.luartz.trigger

import java.time.Clock
import java.time.Instant

interface Trigger {
    fun nextFireTime(clock: Clock): Instant

    fun canFire(): Boolean = true

    fun whenFired() {}
}
