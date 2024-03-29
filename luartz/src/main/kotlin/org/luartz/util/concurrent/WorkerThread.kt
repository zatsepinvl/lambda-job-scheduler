package org.luartz.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class WorkerThreadException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

internal abstract class WorkerThread(name: String) : Thread(name) {
    private val logger: Logger = LoggerFactory.getLogger(WorkerThread::class.java)

    @Volatile
    private var terminated = false

    override fun run() {
        while (!terminated) {
            try {
                runInInfiniteLoop()
            } catch (exception: InterruptedException) {
                if (!terminated) throw WorkerThreadException("Unexpected interrupted exception", exception)
                // else do nothing
            } catch (throwable: Throwable) {
                logger.error("Unexpected error while running", throwable)
            }
        }
    }

    fun shutdown() {
        terminated = true
        onShutdown()
        this.interrupt()
    }

    open fun onShutdown() {}

    abstract fun runInInfiniteLoop()
}