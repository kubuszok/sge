/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-504 (Timer lost wakeup).
 *
 * Original design (original-src/libgdx/gdx/src/com/badlogic/gdx/utils/
 * Timer.java): TimerThread.run() blocks in `threadLock.wait(waitMillis)`
 * (line 307) and scheduleTask / start wake it immediately via
 * `threadLock.notifyAll()` (lines 94, 117). A task scheduled with a 0.1s
 * delay therefore fires ~0.1s later, even if the thread was idling in its
 * 5000ms cap wait (line 292).
 *
 * The port (sge/src/main/scala/sge/utils/Timer.scala) keeps the notifyAll
 * calls (lines 67, 89) but nothing ever WAITS on threadLock: the loop sleeps
 * via TimerPlatformOps.runLoop — AsyncOperations.sleep on JVM, Thread.sleep
 * on Native, setTimeout on JS — none of which the notifyAll can interrupt.
 * With the idle cap waitMillis = 5000 (Timer.scala line 284), a task
 * scheduled while the loop sleeps can fire up to ~5s late.
 *
 * Test design (timing test, made deterministic):
 *   1. A zero-delay dummy task is scheduled; the moment it runs, the loop has
 *      just finished a step() with an empty task queue and is sleeping for
 *      the FULL 5000ms idle cap — regardless of who won the thread-startup
 *      race. This is the deterministic "loop is demonstrably idle" state.
 *   2. 250ms later (loop still has >=4.7s of its cap left) a 0.1s one-shot is
 *      scheduled; per the original it fires ~0.1s later, with the bug it
 *      cannot run before the cap expires (~4.75s). The 4s await times out (or
 *      the latency far exceeds it), and the assertion bound is 1500ms —
 *      generous against CI jitter, unreachable with the bug.
 *
 * Control: there is NO deterministic code path that schedules "before the
 * loop sleeps" — the only such path is racing the background loop's first
 * step() right after TimerThread creation, which is a coin flip. The
 * deterministic control used instead: once the loop has processed a REPEATING
 * task, update() returns waitMillis = intervalMillis (Timer.scala line 127),
 * so the gap between consecutive firings honours the interval even with the
 * bug. This proves the loop machinery itself works and isolates the failure
 * to the lost wakeup.
 *
 * Platform scope: JVM only.
 *   - JS: impossible — single-threaded event loop has no thread-wait
 *     semantics; a blocking pump-and-await harness cannot run there.
 *   - Native: Thread.sleep loop has the same bug, but this harness leans on
 *     concurrent latches + a pump thread interleaving with a Gears-free
 *     daemon thread; judged portable in principle, but left to the fixer to
 *     mirror once green — the JVM suite alone proves ISS-504.
 *
 * Tasks run via Application.postRunnable (main-loop semantics), simulated
 * here by PumpApplication: postRunnable enqueues, the test thread drains.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package utils

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.ArrayBuffer

class TimerWakeupRedSuite extends munit.FunSuite {

  /** Application whose postRunnable enqueues; the test thread pumps the queue, playing the role of the main loop thread. */
  final private class PumpApplication extends Application {
    private val queue = ArrayBuffer.empty[Runnable]

    def drainAndRun(): Unit = {
      val toRun = queue.synchronized {
        val copy = queue.toList
        queue.clear()
        copy
      }
      toRun.foreach(_.run())
    }

    def applicationListener:              ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                         Graphics                    = throw new UnsupportedOperationException
    def audio:                            Audio                       = throw new UnsupportedOperationException
    def input:                            Input                       = throw new UnsupportedOperationException
    def files:                            Files                       = throw new UnsupportedOperationException
    def net:                              Net                         = throw new UnsupportedOperationException
    def applicationType:                  Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                          Int                         = 0
    def javaHeap:                         Long                        = 0L
    def nativeHeap:                       Long                        = 0L
    def getPreferences(name: String):     Preferences                 = throw new UnsupportedOperationException
    def clipboard:                        Clipboard                   = throw new UnsupportedOperationException
    def postRunnable(runnable: Runnable): Unit                        = queue.synchronized {
      queue += runnable
      ()
    }
    def exit():                                               Unit = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit = ()
  }

  /** Pumps posted runnables until the latch opens or timeoutMillis elapses. Returns true if the latch opened. */
  private def pumpUntil(app: PumpApplication, latch: CountDownLatch, timeoutMillis: Long): Boolean = {
    val deadlineNanos = System.nanoTime() + timeoutMillis * 1000000L
    var opened        = latch.getCount == 0L
    while (!opened && System.nanoTime() < deadlineNanos) {
      app.drainAndRun()
      opened = latch.await(10L, TimeUnit.MILLISECONDS)
    }
    app.drainAndRun()
    if (!opened) opened = latch.getCount == 0L
    opened
  }

  test("ISS-504 red: one-shot task scheduled while the loop idles fires ~0.1s later, not after the 5s idle cap") {
    Timer.disposeThread()
    val app   = new PumpApplication
    given Sge = SgeTestFixture.testSge(application = app)
    try {
      val timer = new Timer()

      // Phase 1: drive the loop into its idle-cap sleep deterministically.
      // Once the zero-delay dummy has run, the loop has just completed a
      // step() that emptied the task queue and is sleeping the full 5000ms
      // idle cap (whichever side won the startup race). Allow ~6.5s: with
      // the bug the dummy itself can take up to ~5s to fire.
      val dummyFired = new CountDownLatch(1)
      timer.scheduleTask(new Timer.Task {
        def run(): Unit = dummyFired.countDown()
      })
      assert(pumpUntil(app, dummyFired, 6500L), "setup: zero-delay dummy task never fired — timer loop appears dead")

      // The window between the loop posting the dummy (observed above within
      // ~10ms by the pump) and re-entering its sleep is microseconds; 250ms
      // leaves the loop demonstrably mid-sleep with >=4.7s of cap remaining.
      Thread.sleep(250L)

      // Phase 2: schedule a 0.1s one-shot while the loop sleeps. Java wakes
      // the thread immediately (notifyAll -> wait); the port's loop sleeps
      // out the remaining idle cap, so with the bug nothing can fire before
      // ~4.75s from now.
      val fireNanos = new AtomicLong(0L)
      val fired     = new CountDownLatch(1)
      val t0        = System.nanoTime()
      timer.scheduleTask(
        new Timer.Task {
          def run(): Unit = {
            fireNanos.set(System.nanoTime())
            fired.countDown()
          }
        },
        delaySeconds = Seconds(0.1f)
      )
      val firedInTime = pumpUntil(app, fired, 4000L)
      if (!firedInTime) {
        val elapsedMillis = (System.nanoTime() - t0) / 1000000L
        fail(
          s"task scheduled with 0.1s delay had not fired after ${elapsedMillis}ms — " +
            "lost wakeup: scheduleTask's notifyAll has no matching wait, the loop slept out its 5s idle cap"
        )
      }
      val latencyMillis = (fireNanos.get() - t0) / 1000000L
      assert(
        latencyMillis < 1500L,
        s"task scheduled with 0.1s delay fired after ${latencyMillis}ms (expected ~100ms, bound 1500ms) — lost wakeup"
      )
    } finally
      Timer.disposeThread()
  }

  test("control: consecutive firings of a repeating task honour the interval (loop machinery works)") {
    Timer.disposeThread()
    val app   = new PumpApplication
    given Sge = SgeTestFixture.testSge(application = app)
    try {
      val timer      = new Timer()
      val fireTimes  = ArrayBuffer.empty[Long]
      val twiceFired = new CountDownLatch(2)
      // repeatCount = 1 => runs exactly twice. The FIRST firing may be late
      // (it can lose the startup race and sit out the idle cap — that is the
      // red test's bug, not this control's subject). After the first firing,
      // update() returns waitMillis = intervalMillis, so the loop sleeps
      // exactly ~200ms before the second: the inter-firing gap is on time
      // on both buggy and fixed code.
      timer.scheduleTask(
        new Timer.Task {
          def run(): Unit = {
            fireTimes.synchronized {
              fireTimes += System.nanoTime()
              ()
            }
            twiceFired.countDown()
          }
        },
        delaySeconds = Seconds.zero,
        intervalSeconds = Seconds(0.2f),
        repeatCount = 1
      )
      assert(pumpUntil(app, twiceFired, 12000L), "control setup: repeating task did not fire twice within 12s")
      val gapMillis = fireTimes.synchronized((fireTimes(1) - fireTimes(0)) / 1000000L)
      assert(
        gapMillis < 1500L,
        s"control: inter-firing gap of a 0.2s-interval task was ${gapMillis}ms (expected ~200ms, bound 1500ms)"
      )
    } finally
      Timer.disposeThread()
  }
}
