package day3

import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// This implementation never stores `null` values.
class AtomicArrayWithCas2OnReentrantLock<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<E>(size)
    private val locks = Array(size) { ReentrantLock() }

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        locks[index].withLock {
            return array[index]
        }
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E,
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }

        val (pair1, pair2) = arrayOf(
            index1 to expected1,
            index2 to expected2,
        ).sortedBy { it.first }

        val (idx1, exp1) = pair1
        val (idx2, exp2) = pair2

        locks[idx1].withLock {
            if (array[idx1] != exp1) return false
        }

        locks[idx2].withLock {
            if (array[idx2] != exp2) return false

            array.set(index1, update1)
            array.set(index2, update2)
            return true
        }
    }
}
