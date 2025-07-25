package day4

import day1.*
import org.junit.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.*


/* TODO: Let's write your first Lincheck test.
         1. Add a `test()` function and annotate it with `@Test` to use JUnit.
         2. In this function, create an integer non-atomic counter
            and launch two threads that increment this counter.
         3. In the test thread, wait until the launched threads are finished
            and check that both the increments have been applied.
         4. Run the test -- it will likely succeed even though
            the increments are not atomic.
         5. Wrap the test code with `Lincheck.runConcurrentTest { ... }`
            and re-run the test. The test should fail.
         6. Debug the test with the plugin.
*/
class CounterTest {
    @Test
    fun test() {
        var counter = 0
        val t1 = thread {
            counter++
        }
        val t2 = thread {
            counter++
        }
        t1.join()
        t2.join()
        assert(counter == 2) { "The counter should be equal to 2" }
    }
}

/* TODO: Here is a copy of [FlatCombiningQueueTest.testHelping]
         with a couple of additional checks
         that tests a flat combining queue implementation with a tricky bug
         (see [FlatCombiningQueueWithTrickyBug]).

   TODO: 1. Run this test and make sure it fails with
            "At least one operation acquired the lock twice".
         2. Try to understand the error for 5 minutes.
         3. After you failed with the error investigation,
            transform this test into a Lincheck test.
            3.1. Reduce `THREADS` and `ENQ_DEQ_PAIRS_PER_THREAD` to some
                 small constants (e.g., 2 and 2).
            3.2. Wrap the whole test with `Lincheck.runConcurrentTest { .. }`.
         4. Run the transformed test. Make sure it fails with the same error
            but now with a provided interleaving trace.
         5. Use the Lincheck plugin to debug the interleaving trace.
         6. Find the bug and fix it.
*/
class FlatCombiningQueueWithTrickyBugTest {
    @Test
    fun testHelping() {
        val lockReleases = AtomicInteger(0)
        val q = object : FlatCombiningQueueWithTrickyBug<Int>() {
            override fun releaseLock() {
                super.releaseLock()
                lockReleases.incrementAndGet()
            }
        }
        runParallelQueueOperations(q)
        check(lockReleases.get() <= THREADS * ENQ_DEQ_PAIRS_PER_THREAD) {
            "At least one operation acquired the lock twice"
        }
        check(lockReleases.get() < THREADS * ENQ_DEQ_PAIRS_PER_THREAD) {
            "The combiner does not help other threads"
        }
        lockReleases.set(0)
        runParallelQueueOperations(q)
        check(lockReleases.get() <= THREADS * ENQ_DEQ_PAIRS_PER_THREAD) {
            "At least one operation acquired the lock twice"
        }
        check(lockReleases.get() < THREADS * ENQ_DEQ_PAIRS_PER_THREAD) {
            "The combiner helped other threads during the first execution, " + "but did not help during the second one. " + "Probably, you clean the array slots incorrectly."
        }
    }

    private fun runParallelQueueOperations(q: Queue<Int>) {
        (1..THREADS).map {
            thread {
                repeat(ENQ_DEQ_PAIRS_PER_THREAD) {
                    q.enqueue(it)
                    q.dequeue()
                }
            }
        }.forEach { it.join() }
    }

    private val THREADS = 6
    private val ENQ_DEQ_PAIRS_PER_THREAD = 1000_000
}

/* TODO: Write a Lincheck test for ConcurrentLinkedDeque
         and reveal a concurrent bug. Please use the same API
         for data structures as we use in the course, and check
         all the peek/poll/add first/last operations on the deque.

   TODO: See the guide for instructions:
         https://kotlinlang.org/docs/introduction.html#next-step
 */
class ConcurrentLinkedDequeTest {
}