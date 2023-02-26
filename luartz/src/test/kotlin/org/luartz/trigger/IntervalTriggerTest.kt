package org.luartz.trigger

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.luartz.util.givenFixedClock
import java.time.Duration
import java.time.Instant

class IntervalTriggerTest {

    @Test
    fun testFireTimeBeforeStartTime() {
        // Given
        val clock = givenFixedClock(0)
        val trigger = givenIntervalTrigger(10, 1)

        // When
        val fireTime = trigger.nextFireTime(clock)

        // Then
        assertThat(fireTime.toEpochMilli()).isEqualTo(11)
    }

    @Test
    fun testFireTimeAfterStartTime() {
        // Given
        val clock = givenFixedClock(10)
        val trigger = givenIntervalTrigger(10, 1)

        // When
        val fireTime = trigger.nextFireTime(clock)

        // Then
        assertThat(fireTime.toEpochMilli()).isEqualTo(11)
    }

    @Test
    fun testFireTimeAfterAWhile() {
        // Given
        val clock = givenFixedClock(22)
        val trigger = givenIntervalTrigger(10, 5)

        // When
        val fireTime = trigger.nextFireTime(clock)

        // Then
        assertThat(fireTime.toEpochMilli()).isEqualTo(25)
    }

    private fun givenIntervalTrigger(startAtMillis: Long, intervalMillis: Long): IntervalTrigger {
        return IntervalTrigger(
            Instant.ofEpochMilli(startAtMillis),
            Duration.ofMillis(intervalMillis),
        )
    }
}