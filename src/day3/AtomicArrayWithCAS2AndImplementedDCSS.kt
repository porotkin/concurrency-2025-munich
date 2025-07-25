@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day3

import day3.AtomicArrayWithCAS2AndImplementedDCSS.Status.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

// This implementation never stores `null` values.
class AtomicArrayWithCAS2AndImplementedDCSS<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E {
        // TODO: Copy the implementation from `AtomicArrayWithCAS2Simplified`
        val cell = array[index]

        if (cell is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor) {
            val (expected, update) = if (index == cell.index1) {
                cell.expected1 to cell.update1
            } else {
                cell.expected2 to cell.update2
            }

            return when (cell.status.get()) {
                UNDECIDED, FAILED -> expected
                SUCCESS -> update
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
        val index1: Int,
        val expected1: E,
        val update1: E,
        val index2: Int,
        val expected2: E,
        val update2: E,
    ) {
        val status = AtomicReference(UNDECIDED)

        private val Any?.asDescriptor: AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor
            get() = this as? AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor
                ?: this@CAS2Descriptor

        private fun toSortedTriples(): List<Triple<Int, E, E>> =
            listOf(
                Triple(index1, expected1, update1),
                Triple(index2, expected2, update2),
            ).sortedBy { it.first }

        private fun toSortedPairs(): List<Pair<Int, E>> =
            toSortedTriples()
                .map { (idx, exp) -> idx to exp }

        fun apply() {
            val success = installDescriptor()
            applyLogically(success)
            applyPhysically()
        }

        private fun installDescriptor(): Boolean {
            // TODO: Install this descriptor to the cells,
            // TODO: returning `true` on success, and `false`
            // TODO: if one of the cells contained an unexpected value.
            val (first, second) = toSortedPairs()
            val (idx1, exp1) = first
            val (idx2, exp2) = second

            if (!tryToInstall(idx1, exp1)) {
                return false
            }

            if (!tryToInstall(idx2, exp2)) {
                return false
            }

            return true
        }

        private fun tryToInstall(
            index: Int,
            expected: E,
        ): Boolean {
            while (true) {
                val value = array[index]
                when (value) {
                    expected -> {
                        val dcss = dcss(
                            index = index,
                            expectedCellState = expected,
                            updateCellState = this,
                            statusReference = status,
                            expectedStatus = UNDECIDED,
                        )

                        if (array[index] is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor) {
                            continue
                        }

                        return dcss
                    }

                    is AtomicArrayWithCAS2AndImplementedDCSS<*>.CAS2Descriptor -> {
                        if (value !== this) {
                            if (value.status.get() == UNDECIDED) {
                                value.asDescriptor.apply()
                            } else {
                                value.asDescriptor.applyPhysically()
                            }

                            continue
                        } else {
                            return true
                        }
                    }

                    else -> {
                        return false
                    }
                }
            }
        }

        private fun applyLogically(success: Boolean) {
            val newStatus = if (success) SUCCESS else FAILED
            status.compareAndSet(UNDECIDED, newStatus)
        }

        private fun applyPhysically() {
            // TODO: Apply this operation physically
            // TODO: by updating the cells to either
            // TODO: update values (on success)
            // TODO: or back to expected values (on failure).
            val (first, second) = toSortedTriples()
            val (idx1, exp1, upd1) = first
            val (idx2, _, upd2) = second

            val status = status.get()
            when (status) {
                SUCCESS -> {
                    array.compareAndSet(idx2, this, upd2)
                    array.compareAndSet(idx1, this, upd1)
                }

                FAILED -> {
                    // rollback on failure
                    array.compareAndSet(idx1, this, exp1)
                }

                else -> {}
            }
        }
    }

    enum class Status {
        UNDECIDED, SUCCESS, FAILED
    }

    // TODO: Please use this DCSS implementation to ensure that
    // TODO: the status is `UNDECIDED` when installing the descriptor.
    fun dcss(
        index: Int,
        expectedCellState: Any?,
        updateCellState: Any?,
        statusReference: AtomicReference<*>,
        expectedStatus: Any?,
    ): Boolean =
        if (array[index] == expectedCellState && statusReference.get() == expectedStatus) {
            array[index] = updateCellState
            true
        } else {
            false
        }
}
