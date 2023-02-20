package org.luartz.trigger

import java.time.Clock

class NowTrigger(
    clock: Clock = Clock.systemDefaultZone(),
) : OneOffTrigger(clock.instant())
