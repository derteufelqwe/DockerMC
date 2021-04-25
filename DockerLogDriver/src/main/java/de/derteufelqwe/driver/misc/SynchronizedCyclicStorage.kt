package de.derteufelqwe.driver.misc

import java.util.concurrent.locks.ReentrantLock

/**
 * A DS, which makes it possible to endlessly cycle over its entries
 */
class SynchronizedCyclicStorage<T> {

    private val lock = ReentrantLock(true)

    private var size = 0
    private var root: CyclicStorageObj<T>? = null


    fun add(entry: T) {
        lock.lock()

        try {
            var root = this.root
            if (root == null) {
                root = CyclicStorageObj(entry)
                this.root = root

            } else {
                val oldPrevious = root.previous
                val newEntry = CyclicStorageObj(entry, root, oldPrevious)
                oldPrevious.next = newEntry
                root.previous = newEntry
            }
            size++

        } finally {
            lock.unlock()
        }
    }

    fun remove(entry: T) {
        lock.lock()

        try {
            val root = this.root ?: return // No operation required if no items are present
            var current = root

            // Special handling if the item to remove is the root item
            if (root.value == entry) {
                if (size == 1) {
                    this.root = null

                } else {
                    val previous = root.previous
                    val next = root.next

                    previous.next = next
                    next.previous = previous
                    this.root = root.next
                }

                size--
                return
            }

            // Otherwise check all other items
            do {
                if (current.value == entry) {
                    val previous = current.previous
                    val next = current.next

                    previous.next = next
                    next.previous = previous

                    size--
                    return
                }
                current = current.next

            } while (current != root)

        } finally {
            lock.unlock()
        }
    }

    /**
     * Returns the current element but doesn't rotate the storage
     */
    fun peek(): T? {
        lock.lock()

        try {
            this.root?.let {
                return it.value
            }

            return null

        } finally {
            lock.unlock()
        }
    }

    /**
     * Returns the current element and rotates the storage
     */
    fun get(): T? {
        lock.lock()

        try {
            this.root?.let {
                val value = it.value
                this.root = it.next
                return value
            }

            return null

        } finally {
            lock.unlock()
        }
    }


    private data class CyclicStorageObj<T>(val value: T) {
        var next: CyclicStorageObj<T> = this
        var previous: CyclicStorageObj<T> = this

        constructor(value: T, next: CyclicStorageObj<T>, previous: CyclicStorageObj<T>) : this(value) {
            this.next = next
            this.previous = previous
        }

    }

}