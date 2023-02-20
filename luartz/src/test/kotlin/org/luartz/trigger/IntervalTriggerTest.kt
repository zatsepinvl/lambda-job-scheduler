package org.luartz.trigger

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class IntervalTriggerTest {

    @Test
    fun testFireTimeBeforeStartTime() {
        // Given
        val clock = givenFixedClock(0)
        val trigger = givenIntervalTrigger(clock, 10, 1)

        // When
        val fireTime = trigger.nextFireTime()

        // Then
        assertThat(fireTime.toEpochMilli()).isEqualTo(11)
    }

    @Test
    fun testFireTimeAfterStartTime() {
        // Given
        val clock = givenFixedClock(10)
        val trigger = givenIntervalTrigger(clock, 10, 1)

        // When
        val fireTime = trigger.nextFireTime()

        // Then
        assertThat(fireTime.toEpochMilli()).isEqualTo(11)
    }

    @Test
    fun testFireTimeAfterAWhile() {
        // Given
        val clock = givenFixedClock(22)
        val trigger = givenIntervalTrigger(clock, 10, 5)

        // When
        val fireTime = trigger.nextFireTime()

        // Then
        assertThat(fireTime.toEpochMilli()).isEqualTo(25)
    }

    private fun givenFixedClock(timeMillis: Long): Clock {
        return Clock.fixed(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
    }

    private fun givenIntervalTrigger(clock: Clock, startAtMillis: Long, intervalMillis: Long): IntervalTrigger {
        return IntervalTrigger(
            Instant.ofEpochMilli(startAtMillis),
            Duration.ofMillis(intervalMillis),
            clock
        )
    }
}