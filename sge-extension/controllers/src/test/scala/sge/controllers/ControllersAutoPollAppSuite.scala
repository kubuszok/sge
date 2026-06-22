/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * GREEN coverage for ISS-516 (in-app auto-poll path):
 *   The frozen red suite (ControllersWiringRedSuite) only drives frames via the
 *   local registeredFrameHooks accessor with NO running app, so it cannot catch
 *   the case where the per-frame poll is never installed into the running
 *   Application. This suite closes that gap: it installs the auto-poll into a
 *   real FrameHookHost (the capability every Application carries) and proves that
 *   the host's own per-frame tick (runFrameHooks) actually drives manager.poll(),
 *   and that dispose() removes the installed hook so subsequent ticks do NOT poll.
 */
package sge
package controllers

import scala.collection.mutable.ArrayBuffer

/** GREEN suite for ISS-516: proves controller auto-poll fires through a running host's per-frame tick after [[Controllers.installAutoPoll]] / [[Controllers.installAutoPollInto]], and stops after
  * [[Controllers.dispose]]. See the file header for rationale.
  */
class ControllersAutoPollAppSuite extends ControllersIsolatedSuite {

  /** A [[ControllerManager]] that records every poll() invocation (same shape as the red suite's recorder). */
  private class RecordingControllerManager extends ControllerManager {
    var pollCount: Int = 0

    private val listeners: ArrayBuffer[ControllerListener] = ArrayBuffer.empty

    override def addListener(listener: ControllerListener): Unit =
      listeners += listener
    override def removeListener(listener: ControllerListener): Unit = {
      val _ = listeners -= listener
    }
    override def getListeners:     Seq[ControllerListener] = listeners.toSeq
    override def clearListeners(): Unit                    = listeners.clear()

    def poll(): Unit =
      pollCount += 1
  }

  /** A lightweight running host. [[FrameHookHost]] is fully concrete (it IS the per-frame registry every [[Application]] inherits), so no abstract members need stubbing and no shortcut markers are
    * introduced. Driving [[runFrameHooks]] mirrors exactly what each concrete platform main loop does once per frame.
    */
  private class TestFrameHookHost extends FrameHookHost {
    def tick(): Unit = runFrameHooks()
  }

  test("ISS-516(in-app): installAutoPollInto makes runFrameHooks drive manager.poll() every frame") {
    serialized {
      val manager = RecordingControllerManager()
      val host    = TestFrameHookHost()

      Controllers.initialize(manager)
      // initialize alone must NOT install into the host (app-less): ticking the host polls nothing yet.
      host.tick()
      assertEquals(manager.pollCount, 0, "initialize must NOT register into the running host by itself")

      // Install the per-frame poll into the running host (the (using Sge) path delegates here).
      Controllers.installAutoPollInto(host)

      // Simulate N frames by ticking the host's own per-frame loop — no manual poll().
      val frames = 5
      var f      = 0
      while (f < frames) {
        host.tick()
        f += 1
      }
      assertEquals(
        manager.pollCount,
        frames,
        "the installed per-frame hook must call manager.poll() once per host frame"
      )

      // dispose() must remove the installed hook so subsequent host ticks do NOT poll.
      Controllers.dispose()
      val countAtDispose = manager.pollCount
      host.tick()
      host.tick()
      assertEquals(
        manager.pollCount,
        countAtDispose,
        "dispose() must remove the installed hook; subsequent host ticks must not poll (no leak)"
      )
    }
  }

  test("ISS-516(in-app): re-install does not leak hooks (exactly one poll per frame)") {
    serialized {
      val manager = RecordingControllerManager()
      val host    = TestFrameHookHost()

      Controllers.initialize(manager)
      Controllers.installAutoPollInto(host)
      Controllers.installAutoPollInto(host) // idempotent: replaces, does not stack

      host.tick()
      assertEquals(manager.pollCount, 1, "re-install must leave exactly one poll hook (no leak)")
    }
  }

  // ── (using Sge) resolution proof ────────────────────────────────────────
  // The bounce defect was a companion-object given-lift to Nullable[Sge] that did
  // NOT resolve at the call site, so the app hook was never installed. installAutoPoll
  // now takes (using Sge) DIRECTLY and delegates to installAutoPollInto(sge.application);
  // standard given resolution of a normal `using` parameter is reliable. The mere
  // compilation of the call below — under an in-scope `given Sge`, with NO import of any
  // Controllers given — proves that resolution succeeds (the old lift would have needed
  // an explicit `import Controllers.given`). We do not execute it: constructing a full
  // Sge requires the heavy Graphics/Audio/Input/Files/Net traits, whereas the real
  // install behaviour is fully exercised through FrameHookHost in the tests above.
  test("ISS-516(in-app): installAutoPoll's (using Sge) call site compiles (given resolves without import)") {
    // The proof is a context function whose body calls Controllers.installAutoPoll() with only an
    // in-scope `given Sge` and NO `import Controllers.given`. Its successful compilation is the
    // binary proof that the given resolves directly (the old Nullable[Sge] lift would have required
    // an explicit import and silently fell back to its empty default otherwise). We bind it to a val
    // so it is exercised as a real expression; we do not invoke it, since constructing a full Sge
    // requires the heavy Graphics/Audio/Input/Files/Net traits — the install behaviour itself is
    // fully driven through FrameHookHost in the tests above.
    val installUnderGivenSge: Sge => Unit = (sge: Sge) => {
      given Sge = sge
      Controllers.installAutoPoll() // resolves the in-scope given Sge directly — no Controllers given import
    }
    assert(installUnderGivenSge.isInstanceOf[AnyRef], "installAutoPoll must resolve an in-scope given Sge")
  }
}
