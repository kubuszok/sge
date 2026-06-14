/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * RED reproducer for ISS-516 (controllers subsystem unwired):
 *   - No core frame-hook API exists, so the per-frame manager.poll() is never
 *     invoked automatically; getControllers only works after a manual poll().
 *   - GLFW uniqueId uses the model GUID (GlfwControllerJvm.scala:111-134), so two
 *     identical-GUID pads at different slots MERGE; the browser does it right via
 *     the slot index (BrowserControllerImpl.scala:66 -> s"gamepad-$gpIndex").
 *
 * This suite pins the CHOSEN fix design and is expected to FAIL on the current
 * branch (compile-fail RED: the targeted APIs do not exist yet).
 */
package sge
package controllers

import scala.collection.mutable.ArrayBuffer

/** RED suite for ISS-516. See the file header for the failure rationale. */
class ControllersWiringRedSuite extends munit.FunSuite {

  /** A [[ControllerManager]] that records every poll() invocation. Grounded in the real abstract base — mirrors the mock style of ControllersSuite's TestControllerOps/DefaultControllerManager but
    * records auto-poll calls.
    */
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

    /** The per-frame poll the auto-poll wiring is supposed to drive. */
    def poll(): Unit =
      pollCount += 1
  }

  // ── (a) Auto-poll wiring ────────────────────────────────────────────────
  // CHOSEN fix: Controllers.initialize(manager) registers a frame hook that calls
  // manager.poll(); each platform main loop invokes registered hooks once per frame.
  // After initialize, simulating N frames must call poll() exactly N times WITHOUT
  // any manual poll() call. We drive "frames" by invoking the registered frame hooks
  // exposed via the new Controllers.registeredFrameHooks accessor.
  //
  // RED NOW: neither Controllers.registeredFrameHooks nor the auto-registration of a
  // poll() frame hook exists (Controllers.scala:36-37 only stores the manager).
  test("ISS-516(a): initialize auto-registers a per-frame poll hook") {
    Controllers.dispose() // clean state
    val manager = RecordingControllerManager()
    Controllers.initialize(manager)

    // The chosen design registers exactly one frame hook (the poll() driver).
    val hooks = Controllers.registeredFrameHooks
    assert(hooks.nonEmpty, "Controllers.initialize must register a per-frame poll hook")

    // Simulate N frames by invoking the registered hooks — no manual poll().
    val frames = 5
    var f      = 0
    while (f < frames) {
      hooks.foreach(_())
      f += 1
    }
    assertEquals(
      manager.pollCount,
      frames,
      "the auto-registered frame hook must call manager.poll() once per frame"
    )

    Controllers.dispose()
  }

  // ── (c) Controllers.poll() facade ───────────────────────────────────────
  // CHOSEN fix item: a Controllers.poll() facade that delegates to the manager.
  // RED NOW: Controllers.poll does not exist (Controllers.scala has no such method).
  test("ISS-516(c): Controllers.poll() facade delegates to the manager") {
    Controllers.dispose()
    val manager = RecordingControllerManager()
    Controllers.initialize(manager)

    Controllers.poll()
    Controllers.poll()
    assertEquals(manager.pollCount, 2, "Controllers.poll() must delegate to manager.poll()")

    Controllers.dispose()
  }

  // ── (b) GLFW uniqueId slot-distinctness ─────────────────────────────────
  // CHOSEN fix: GLFW uniqueId must incorporate the joystick SLOT INDEX so two
  // identical-GUID pads at different slots get DISTINCT ids — mirror the browser's
  // s"gamepad-$gpIndex" (BrowserControllerImpl.scala:66).
  //
  // The GLFW JVM id is currently built inline as `uniqueId = guid`
  // (GlfwControllerJvm.scala:111-134), ignoring the slot index, so two pads with the
  // same GUID at slots 0 and 1 produce EQUAL ids and the manager MERGES them.
  //
  // The fix is expected to extract a pure helper GlfwControllerBackend.uniqueIdFor(guid, slot).
  // RED NOW: that helper does not exist (compile-fail).
  test("ISS-516(b): GLFW uniqueIdFor yields distinct ids for same GUID at different slots") {
    val guid = "030000005e0400008e02000010010000" // a real-looking shared GLFW GUID
    val id0  = GlfwControllerBackend.uniqueIdFor(guid, 0)
    val id1  = GlfwControllerBackend.uniqueIdFor(guid, 1)
    assertNotEquals(
      id0,
      id1,
      "two identical-GUID pads at different slots must get DISTINCT uniqueIds (currently merged)"
    )
  }
}
