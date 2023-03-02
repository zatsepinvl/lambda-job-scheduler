package org.luartz.util

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * Proper way to shut down executor service.
 * See example for more details: https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
 */
fun ExecutorService.shutdownGracefully(timeout: Duration) {
    // Disable new tasks from being submitted
    this.shutdown()

    try {
        // Wait a while for existing tasks to terminate
        var terminated = this.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)
        if (!terminated) {
            // Cancel currently executing tasks
            this.shutdownNow()
            terminated = this.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)
            // Wait a while for tasks to respond to being cancelled
            if (!terminated) {
                throw RuntimeException("Unable to terminated executor service properly withing giving time frame")
            }
        }
    } catch (interruptedException: InterruptedException) {
        // (Re-)Cancel if current thread also interrupted
        this.shutdownNow()
        // Preserve interrupt status
        Thread.currentThread().interrupt()
    }
}