package day3

import java.util.concurrent.atomic.AtomicReferenceArray

// This implementation never stores `null` values.
class AtomicArrayWithCAS2OnLockedState<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        while (true) {
            val value = array[index]
            if (value === LOCKED) continue
            @Suppress("UNCHECKED_CAST")
            return value as E
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

        if (!lockCell(idx1, exp1)) {
            return false
        }

        if (!lockCell(idx2, exp2)) {
            array[idx1] = exp1 // rollback
            return false
        }

        array[index1] = update1
        array[index2] = update2

        return true
    }

    private fun lockCell(index: Int, expected: E): Boolean {
        while (true) {
            val value = array[index]
            when (value) {
                expected -> {
                    if (array.compareAndSet(index, expected, LOCKED)) {
                        return true
                    }
                }

                LOCKED -> {
                    continue
                }

                else -> {
                    return false
                }
            }
        }
    }
}

private val LOCKED = "Locked"
