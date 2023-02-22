package org.luartz.util

class WorkerThreadException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

abstract class WorkerThread(name: String) : Thread(name) {
    private var terminated = false

    override fun run() {
        try {
            while (true) {
                runInInfiniteLoop()
            }
        } catch (exception: InterruptedException) {
            if (!terminated) throw WorkerThreadException("Unexpected interrupted exception", exception)
            // else do nothing
        }
    }

    fun shutdown() {
        terminated = true
        onShutdown()
        this.interrupt()
    }

    abstract fun onShutdown()

    abstract fun runInInfiniteLoop()
}