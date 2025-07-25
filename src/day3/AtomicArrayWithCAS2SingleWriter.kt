@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2SingleWriter.Status.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

// This implementation never stores `null` values.
class AtomicArrayWithCAS2SingleWriter<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        val cell = array[index]

        if (cell is AtomicArrayWithCAS2SingleWriter<*>.CAS2Descriptor) {
            return when (cell.status.get()) {
                UNDECIDED, FAILED -> if (index == cell.index1) cell.expected1 else cell.expected2
                SUCCESS -> if (index == cell.index1) cell.update1 else cell.update2
            } as E
        }

        return cell as E
    }

    fun cas2(
        index1: Int, expected1: E, update1: E,
        index2: Int, expected2: E, update2: E,
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        val descriptor = CAS2Descriptor(
            index1 = index1, expected1 = expected1, update1 = update1,
            index2 = index2, expected2 = expected2, update2 = update2
        )
        descriptor.apply()
        return descriptor.status.get() === SUCCESS
    }

    inner class CAS2Descriptor(
        val index1: Int, val expected1: E, val update1: E,
        val index2: Int, val expected2: E, val update2: E,
    ) {
        val status = AtomicReference(UNDECIDED)

        fun apply() {
            val installed = installDescriptor()
            updateStatus(installed)
            updateCells()
        }

        private fun installDescriptor(): Boolean {
            if (!array.compareAndSet(index1, expected1, this)) {
                return false
            }

            if (!array.compareAndSet(index2, expected2, this)) {
                return false
            }

            return true
        }

        private fun updateStatus(isInstalled: Boolean) {
            if (!isInstalled) {
                status.compareAndSet(UNDECIDED, FAILED)
                return
            }

            status.compareAndSet(UNDECIDED, SUCCESS)
        }

        private fun updateCells() {
            val status = status.get()

            // rollback on failure
            if (status === FAILED) {
                array.compareAndSet(index1, this, expected1)
                return
            }

            if (status === SUCCESS) {
                array[index1] = update1
                array[index2] = update2
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }
}
