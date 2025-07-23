package day2

import java.util.concurrent.atomic.AtomicLong

class CountersSnapshot {
    val counter1 = AtomicLong(0)
    val counter2 = AtomicLong(0)
    val counter3 = AtomicLong(0)

    fun incrementCounter1() = counter1.getAndIncrement()
    fun incrementCounter2() = counter2.getAndIncrement()
    fun incrementCounter3() = counter3.getAndIncrement()

    fun countersSnapshot(): Triple<Long, Long, Long> {
        while (true) {
            val counter1 = this.counter1.get()
            val counter2 = this.counter2.get()
            val counter3 = this.counter3.get()

            if (counter1 != this.counter1.get() ||
                counter2 != this.counter2.get()
            ) {
                continue
            }

            return Triple(counter1, counter2, counter3)
        }
    }
}
