package org.luartz.util

import java.util.concurrent.Callable
import java.util.concurrent.locks.ReadWriteLock

internal fun <T> ReadWriteLock.acquireToWrite(callable: Callable<T>): T {
    this.writeLock().lock()
    try {
        return callable.call()
    } finally {
        this.writeLock().unlock()
    }
}


internal fun <T> ReadWriteLock.acquireToRead(callable: Callable<T>): T {
    this.readLock().lock()
    try {
        return callable.call()
    } finally {
        this.readLock().unlock()
    }
}