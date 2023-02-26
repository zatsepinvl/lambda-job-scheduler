package org.luartz.trigger

import org.luartz.util.defaultUtcClock
import java.time.Instant

class NowTrigger(
    now: Instant = defaultUtcClock().instant(),
) : OneOffTrigger(now)
