package day2

import day1.Queue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.max
import kotlin.math.min

class FAABasedQueueSimplified<E> : Queue<E> {
    private val infiniteArray = AtomicReferenceArray<Any?>(1024) // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while (true) {
            val i = enqIdx.getAndIncrement()

            if (infiniteArray.compareAndSet(i.toInt(), null, element)) {
                return
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            if (!shouldTryToDequeue()) return null

            val i = deqIdx.getAndIncrement()

            if (infiniteArray.compareAndSet(i.toInt(), null, POISONED)) {
                continue
            }

            @Suppress("UNCHECKED_CAST")
            return infiniteArray.getAndUpdate(i.toInt()) { null } as E?
        }
    }

    private fun shouldTryToDequeue(): Boolean {
        while (true) {
            val deqIdx = deqIdx.get()
            val enqIdx = enqIdx.get()

            if (enqIdx != this.enqIdx.get()) continue

            return deqIdx < enqIdx
        }
    }

    override fun validate() {
        for (i in 0 until min(deqIdx.get().toInt(), enqIdx.get().toInt())) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `deqIdx = ${deqIdx.get()}` at the end of the execution"
            }
        }
        for (i in max(deqIdx.get().toInt(), enqIdx.get().toInt()) until infiniteArray.length()) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `enqIdx = ${enqIdx.get()}` at the end of the execution"
            }
        }
    }
}

private val POISONED = Any()
