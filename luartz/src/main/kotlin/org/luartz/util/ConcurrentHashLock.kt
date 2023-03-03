package org.luartz.util

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class ConcurrentHashLock {
    private val map = ConcurrentHashMap<String, ReentrantLock>()

    fun acquire(key: String) {
        map.getOrPut(key) { ReentrantLock() }.lock()
    }

    fun release(key: String) {
        map[key]!!.unlock()
    }

    fun <T> whenAcquire(key: String, callable: Callable<T>): T {
        acquire(key)
        try {
            return callable.call()
        } finally {
            release(key)
        }
    }
}