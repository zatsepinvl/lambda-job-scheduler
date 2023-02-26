package org.luartz.trigger

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.luartz.util.givenFixedClock

class CronTriggerTest {

    @Test
    fun test() {
        // Given
        val cronExpression = "0 * * * * ?" // every minute
        val trigger = CronTrigger(cronExpression)

        val min0 = givenFixedClock(0)
        val minAlmost1 = givenFixedClock(59999)
        val min1 = givenFixedClock(60000)
        val min2 = givenFixedClock(120000)

        // Then
        assertThat(trigger.nextFireTime(min0))
            .isEqualTo(min1.instant())

        assertThat(trigger.nextFireTime(minAlmost1))
            .isEqualTo(min1.instant())

        assertThat(trigger.nextFireTime(min1))
            .isEqualTo(min2.instant())
    }
}