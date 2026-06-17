/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge

import scala.collection.mutable
import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration.DurationInt

/** Red tests for ISS-678: core SgeExtension lifecycle.
  *
  * Pins the brand-new public contract that does not exist yet:
  *
  * {{{
  * trait SgeExtension {
  *   def name: String
  *   def load()(using Sge): Future[Unit] = Future.unit
  * }
  * object SgeExtension {
  *   def loadAll(extensions: Seq[SgeExtension])(using Sge, ExecutionContext): Future[Unit]
  * }
  * }}}
  *
  * `loadAll` folds the extensions SEQUENTIALLY, in order: each `load()` starts only after the previous extension's Future has completed, and the returned Future completes after the last one finishes.
  *
  * Pre-implementation expectation: `SgeExtension`/`loadAll` are absent, so this suite FAILS TO COMPILE (`Not found: SgeExtension`). That capability-absent compile failure is the honest red for a new
  * core API.
  */
class SgeExtensionLoadAllRedSuite extends munit.FunSuite {

  /** Single-thread EC so we can deterministically await loadAll's returned Future. */
  private val executor: java.util.concurrent.ExecutorService =
    java.util.concurrent.Executors.newSingleThreadExecutor()

  override def afterAll(): Unit = {
    executor.shutdownNow()
    super.afterAll()
  }

  private given Sge = SgeTestFixture.testSge()

  /** Recording extension: appends its name to a shared buffer when its load() Future resolves. */
  final private class RecordingExtension(val name: String, buffer: mutable.Buffer[String], async: Boolean)(using
    ec: ExecutionContext
  ) extends SgeExtension {
    override def load()(using Sge): Future[Unit] =
      if (async) {
        // Completes slightly later on the EC; loadAll must not start the next extension before this resolves.
        val p = Promise[Unit]()
        ec.execute(new Runnable {
          override def run(): Unit = {
            buffer += name
            p.success(())
          }
        })
        p.future
      } else {
        buffer += name
        Future.unit
      }
  }

  test("ISS-678 loadAll runs each extension's load() exactly once, sequentially in order, and completes") {
    given ec: ExecutionContext = ExecutionContext.fromExecutor(executor)

    val buffer = mutable.Buffer.empty[String]
    val a      = new RecordingExtension("a", buffer, async = true)
    val b      = new RecordingExtension("b", buffer, async = false)
    val c      = new RecordingExtension("c", buffer, async = true)

    // The returned Future must complete (bounded await) and must do so only after ALL loads finished.
    Await.result(SgeExtension.loadAll(Seq(a, b, c)), 5.seconds)

    // (1) each extension's load() ran exactly once
    assertEquals(buffer.toList.sorted, List("a", "b", "c"), "each extension's load() must run exactly once")
    assertEquals(buffer.size, 3, "no extension may be loaded more than once")

    // (2) sequential, in order: a before b before c, regardless of async timing
    assertEquals(buffer.toList, List("a", "b", "c"), "loadAll must load extensions sequentially in the given order")

    // (3) the returned Future completed only after all loads finished
    assertEquals(buffer.size, 3, "the returned Future must complete only after every extension has loaded")
  }

  test("ISS-678 loadAll over an empty sequence completes successfully") {
    given ec: ExecutionContext = ExecutionContext.fromExecutor(executor)

    Await.result(SgeExtension.loadAll(Seq.empty), 5.seconds)
  }
}
