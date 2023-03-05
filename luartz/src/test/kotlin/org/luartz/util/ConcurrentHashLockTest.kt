package org.luartz.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random

class ConcurrentHashLockTest {

    @Test
    fun ensureThreadSafety() {
        // Given
        val lock = ConcurrentHashLock()
        val executor = Executors.newWorkStealingPool()
        val iterations = 1000
        var a = 0
        var b = 0

        val incrementA = { lock.whenAcquire("a") { a += 1 } }
        val incrementB = { lock.whenAcquire("b") { b += 1 } }

        // When
        repeat(iterations) {
            executor.submit {
                Thread.sleep(Random.nextLong(1, 10))
                if (it % 2 == 0) {
                    incrementA(); incrementB()
                } else {
                    incrementB(); incrementA()
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(60, SECONDS)

        // Then
        // Assert atomic increment
        assertThat(a).isEqualTo(iterations)
        assertThat(b).isEqualTo(iterations)
    }
}