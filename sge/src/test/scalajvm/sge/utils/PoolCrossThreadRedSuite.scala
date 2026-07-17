/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-603 (sge.utils.Pool is not thread-safe while SGE-original
 * code already uses a Pool across threads).
 *
 * Pool.scala keeps its free list in an unsynchronized DynamicArray
 * (Pool.scala:53 `private val freeObjects = DynamicArray.createRef[A](...)`),
 * and obtain()/free() (Pool.scala:58-70) perform unguarded check-then-act
 * sequences on it. DynamicArray.pop() (lls DynamicArray.scala:264-272) does
 * `_size -= 1; mk.get(_items, _size)` and add() (DynamicArray.scala:100-105)
 * does `mk.set(_items, _size, value); _size += 1` — neither is atomic, so two
 * racing threads can hand out the SAME instance twice, lose frees, drive
 * `_size` negative (after which every obtain() throws
 * ArrayIndexOutOfBoundsException with a negative index, permanently poisoning
 * the pool), or pop an unwritten null slot.
 *
 * This is not a hypothetical misuse: SGE-original SgeHttpClient uses ONE Pool
 * from at least two threads by design:
 *   - obtain on the caller thread: SgeHttpClient.scala:67
 *     (`def obtainRequest(): SgeHttpRequest = requestPool.obtain()`)
 *   - free on an ExecutionContext.global worker: SgeHttpClient.scala:85-100
 *     (`future.onComplete { ... freeRequest(request, entry) }` with
 *     `private given ExecutionContext = ExecutionContext.global` at
 *     SgeHttpClient.scala:41), landing in `requestPool.free(request)` at
 *     SgeHttpClient.scala:167
 *   - free on the caller thread in cancel()/close(): SgeHttpClient.scala:130
 *     and SgeHttpClient.scala:154
 * The ISS-505 `claimFree` guard (SgeHttpClient.scala:174-181) only prevents
 * double-frees of one request; it does NOT serialize requestPool.obtain()
 * against requestPool.free() of different requests, so the pool's internal
 * DynamicArray is still mutated concurrently.
 *
 * For reference, the original LibGDX Pool is equally unsynchronized
 * (com/badlogic/gdx/utils/Pool.java:52-68) — but LibGDX's own NetJavaImpl
 * never pools requests across threads; it protects its cross-thread maps with
 * synchronized methods (NetJavaImpl.java:278-291). The cross-thread pooling
 * is an SGE-original design decision, so SGE must either synchronize Pool or
 * serialize every cross-thread call site.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the thread-safety contract the SGE-original call
 * sites already rely on.
 */
package sge
package utils

import java.util.concurrent.{ ConcurrentHashMap, ConcurrentLinkedQueue, Semaphore }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReferenceArray }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, DurationInt }

class PoolCrossThreadRedSuite extends munit.FunSuite {

  override def munitTimeout: Duration = 120.seconds

  /** Identity-only pooled object (no equals/hashCode override, so `eq` and ConcurrentHashMap keying are both by reference). Resettable via the noop Poolable fallback. */
  final private class Probe

  private def describe(t: Throwable): String = s"${t.getClass.getName}: ${t.getMessage}"

  test(
    "ISS-603: latch-coordinated obtain/free from two threads must not hand out duplicates, throw, or drive the free count negative"
  ) {
    // Small pool: contention always happens on the last few freeObjects slots,
    // which is exactly the SgeHttpClient steady state (a handful of pooled
    // requests cycling between the caller thread and EC.global workers).
    val pool = Pool.Default[Probe](() => new Probe, initialCapacity = 4, max = 4)
    // Seed one pooled object so round 1 already contends on freeObjects.pop().
    pool.free(new Probe)

    val rounds          = 40000
    val slots           = new AtomicReferenceArray[Option[Probe]](Array[Option[Probe]](None, None))
    val duplicateRounds = new AtomicInteger(0)
    val errors          = new ConcurrentLinkedQueue[Throwable]()

    // Monotonic 2-party SPIN barrier: a parked-thread barrier (CyclicBarrier)
    // wakes the two parties microseconds apart, which serializes the racing
    // pop()/add() and hides the bug; busy-spinning releases both threads
    // within nanoseconds of each other, inside pop()'s check-then-act window.
    val arrivals = new AtomicInteger(0)
    def spinAwait(target: Int): Unit = {
      arrivals.incrementAndGet()
      while (arrivals.get() < target) Thread.onSpinWait()
    }

    def worker(w: Int): Thread = {
      val t = new Thread(
        () => {
          var r = 0
          while (r < rounds) {
            // Phase 1: both threads obtain at the same instant.
            spinAwait(2 * (3 * r + 1))
            val obtained =
              try
                Some(pool.obtain())
              catch {
                case t: Throwable =>
                  errors.add(t)
                  None
              }
            slots.set(w, obtained)
            // Phase 2: compare what the two threads received.
            spinAwait(2 * (3 * r + 2))
            if (w == 0) {
              (slots.get(0), slots.get(1)) match {
                case (Some(a), Some(b)) if a eq b => duplicateRounds.incrementAndGet()
                case _                            => ()
              }
            }
            // Phase 3: both threads free at the same instant (racing add()).
            spinAwait(2 * (3 * r + 3))
            slots.get(w).foreach { p =>
              try
                pool.free(p)
              catch {
                case t: Throwable => errors.add(t)
              }
            }
            slots.set(w, None)
            r += 1
          }
        },
        s"iss603-worker-$w"
      )
      t.setDaemon(true)
      t
    }

    val w0 = worker(0)
    val w1 = worker(1)
    w0.start()
    w1.start()
    w0.join(90000)
    w1.join(90000)
    assert(!w0.isAlive && !w1.isAlive, "ISS-603 workers did not finish in time (barrier broken by pool corruption?)")

    val freeCount = pool.free
    val firstErr  = Option(errors.peek()).map(describe).getOrElse("none")
    assert(
      errors.isEmpty && duplicateRounds.get() == 0 && freeCount >= 0,
      s"ISS-603: Pool corrupted by 2-thread obtain/free — duplicate handouts in ${duplicateRounds.get()} of $rounds rounds, " +
        s"${errors.size} exceptions thrown from obtain/free (first: $firstErr), pool.free=$freeCount " +
        "(unsynchronized freeObjects: Pool.scala:53, obtain Pool.scala:58-59, free Pool.scala:64-70)"
    )
  }

  test(
    "ISS-603: SgeHttpClient-shaped usage — obtain on the caller thread, free on ExecutionContext.global — must never hand out an instance that is still checked out"
  ) {
    // Same pool sizing as SgeHttpClient.apply() (SgeHttpClient.scala:229-230:
    // poolCapacity = 16, poolMax = 64) and the same thread topology: the
    // "game" thread obtains (SgeHttpClient.scala:67), EC.global completion
    // callbacks free (SgeHttpClient.scala:41 + :85-100 + :167).
    val pool               = Pool.Default[Probe](() => new Probe, initialCapacity = 16, max = 64)
    given ExecutionContext = ExecutionContext.global
    val checkedOut         = ConcurrentHashMap.newKeySet[Probe]()
    val duplicates         = new AtomicInteger(0)
    val errors             = new ConcurrentLinkedQueue[Throwable]()
    val maxInFlight        = 256
    val inFlight           = new Semaphore(maxInFlight)
    val iterations         = 200000
    var i                  = 0
    while (i < iterations) {
      inFlight.acquire()
      try {
        val obj = pool.obtain()
        // A correct pool can never return an object that has not been freed
        // yet: the freeing side removes it from `checkedOut` BEFORE calling
        // pool.free, so a duplicate here proves the free list handed the same
        // instance to two owners (or popped a lost-add null slot — that NPEs
        // into `errors` below).
        if (!checkedOut.add(obj)) duplicates.incrementAndGet()
        Future {
          try {
            checkedOut.remove(obj)
            pool.free(obj)
          } catch {
            case t: Throwable => errors.add(t)
          } finally
            inFlight.release()
        }
      } catch {
        case t: Throwable =>
          errors.add(t)
          inFlight.release()
      }
      i += 1
    }
    inFlight.acquire(maxInFlight) // drain: wait for every EC.global free to finish

    val freeCount = pool.free
    val firstErr  = Option(errors.peek()).map(describe).getOrElse("none")
    assert(
      errors.isEmpty && duplicates.get() == 0 && freeCount >= 0 && freeCount <= 64,
      s"ISS-603: cross-thread obtain/free corrupted the pool — ${duplicates.get()} duplicate handouts over $iterations requests, " +
        s"${errors.size} exceptions (first: $firstErr), pool.free=$freeCount (valid range 0..64) — " +
        "the exact SgeHttpClient topology: obtain SgeHttpClient.scala:67, free-on-EC.global SgeHttpClient.scala:167"
    )
  }
}
