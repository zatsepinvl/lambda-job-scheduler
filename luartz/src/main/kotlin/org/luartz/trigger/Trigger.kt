package org.luartz.trigger

import java.time.Instant

interface Trigger {
    fun mayFireAgain(): Boolean

    fun nextFireTime(): Instant

    fun updateAfterFired()
}
