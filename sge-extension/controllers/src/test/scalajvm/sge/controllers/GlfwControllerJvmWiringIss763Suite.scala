/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * RED reproducer for ISS-763 (JVM desktop gamepad polling never reaches GLFW).
 *
 * Structural comparison (what the implementer must mirror):
 *
 *   - Scala Native (the faithful implementation): GlfwControllerNative.scala:16-26
 *     binds the seven GLFW joystick symbols via `@link("glfw3") @extern`, so symbol
 *     availability is guaranteed at LINK time — sge-core already links glfw3 into
 *     every Native binary. Consequently GlfwControllerNativeInit.init()
 *     (GlfwControllerNative.scala:43-44) is INFALLIBLE: it only assigns
 *     GlfwControllerBackend.pollControllerImpl. Polling
 *     (GlfwControllerNative.scala:46-82) then always reaches GLFW; an uninitialized
 *     GLFW answers glfwJoystickPresent == 0 (GLFW_NOT_INITIALIZED) and the poll
 *     degrades to ControllerState.Disconnected — it never throws.
 *
 *   - Browser: BrowserControllerInit.init() (BrowserControllerInit.scala) is likewise
 *     infallible — navigator.getGamepads is always resolvable.
 *
 *   - JVM (the broken mirror): GlfwControllerJvm.scala:42-76 resolves ALL seven
 *     Panama downcall handles EAGERLY in the object initializer of
 *     GlfwControllerJvmInit, via SymbolLookup.loaderLookup() (line 43) + sym()
 *     (lines 45-46), which throws UnsatisfiedLinkError for every symbol that the
 *     application class loader has not already received through System.load.
 *     libglfw is only System.load-ed by WindowingOpsJvm.apply
 *     (WindowingOpsJvm.scala:1038-1045) when a DesktopApplication starts. In any
 *     process that has not (yet) done that — every headless test/CI process, and any
 *     game following the documented bootstrap order in Controllers.scala:41, which
 *     shows `GlfwControllerJvmInit.init()` BEFORE application wiring — the very first
 *     touch of GlfwControllerJvmInit throws UnsatisfiedLinkError("GLFW symbol not
 *     found: glfwJoystickPresent") straight out of the object initializer (Errors
 *     escape clinit unwrapped), and Java class-initialization poisoning makes every
 *     later touch throw NoClassDefFoundError. The seam then keeps its stub
 *     (GlfwControllerBackend.scala:52: `pollControllerImpl = _ => Disconnected`) and
 *     every poll on the primary desktop platform reports Disconnected forever —
 *     exactly ISS-763. No production or test code exercises GlfwControllerJvmInit
 *     (repo grep: zero call sites outside doc comments), so nothing ever proved the
 *     JVM path reaches GLFW.
 *
 * This suite pins the Native-parity contract and is expected to FAIL on the current
 * branch (behavioral RED: both tests die on the eager symbol resolution in
 * GlfwControllerJvmInit's initializer).
 */
package sge
package controllers

/** RED suite for ISS-763. See the file header for the failure rationale and the Native-vs-JVM structural comparison. */
class GlfwControllerJvmWiringIss763Suite extends munit.FunSuite {

  /** Runs the body and afterwards restores [[GlfwControllerBackend.pollControllerImpl]] to what it was before, so a (future-green) init() cannot leak the real GLFW impl into other suites sharing this
    * process-global seam.
    */
  private def withRestoredSeam[A](body: => A): A = {
    val before = GlfwControllerBackend.pollControllerImpl
    try body
    finally GlfwControllerBackend.pollControllerImpl = before
  }

  /** Calls [[GlfwControllerJvmInit.init]] and converts the LinkageError it currently dies with into a plain munit assertion failure. Without this, munit classifies UnsatisfiedLinkError /
    * NoClassDefFoundError as FATAL (scala.util.control.NonFatal) and aborts the whole suite after the first test, skipping the rest — each test must stay independently red. Observed today (RED):
    * `java.lang.UnsatisfiedLinkError: GLFW symbol not found: glfwJoystickPresent` at GlfwControllerJvmInit$.<clinit> (GlfwControllerJvm.scala:46/52) on first touch, NoClassDefFoundError on every
    * later touch (poisoned class).
    */
  private def initOrFail(): Unit =
    try GlfwControllerJvmInit.init()
    catch {
      case e: LinkageError =>
        fail(
          s"ISS-763: GlfwControllerJvmInit.init() must be infallible like GlfwControllerNativeInit.init() (GlfwControllerNative.scala:43-44) but threw $e — the eager Panama symbol resolution in its object initializer (GlfwControllerJvm.scala:42-76) fails whenever libglfw was not already System.load-ed (WindowingOpsJvm.scala:1038-1045), so the JVM polling seam keeps the Disconnected stub (GlfwControllerBackend.scala:52) and desktop gamepads stay dead"
        )
    }

  // ── (1) init() infallibility parity ──────────────────────────────────────
  // Native parity: GlfwControllerNativeInit.init() (GlfwControllerNative.scala:43-44)
  // can never fail — symbol availability is a link-time guarantee, and init() only
  // installs the polling function into the GlfwControllerBackend seam. The JVM mirror
  // must give the same guarantee: init() succeeds in a headless process (no
  // DesktopApplication, so WindowingOpsJvm has not System.load-ed libglfw) and
  // installs a non-stub polling impl; establishing/deferring symbol availability is
  // init()'s own job (the way WindowingOpsJvm.apply establishes it for windowing,
  // WindowingOpsJvm.scala:1038-1045), not a hidden precondition on the caller.
  //
  // RED NOW: the first touch of GlfwControllerJvmInit runs its object initializer,
  // which eagerly resolves glfwJoystickPresent et al. via SymbolLookup.loaderLookup()
  // (GlfwControllerJvm.scala:43-76) and throws
  // ExceptionInInitializerError(UnsatisfiedLinkError("GLFW symbol not found: ...")).
  test(
    "ISS-763(1): GlfwControllerJvmInit.init() succeeds headlessly and installs the GLFW polling impl (parity with GlfwControllerNativeInit.init)"
  ) {
    withRestoredSeam {
      val stub = GlfwControllerBackend.pollControllerImpl
      initOrFail() // ← RED: init() dies with UnsatisfiedLinkError today
      assert(
        GlfwControllerBackend.pollControllerImpl ne stub,
        "init() must replace the Disconnected stub (GlfwControllerBackend.scala:52) with the GLFW polling impl, like GlfwControllerNativeInit.init() does on Scala Native"
      )
    }
  }

  // ── (2) headless polling degrades to Disconnected, never throws ──────────
  // Native parity: with GLFW linked but not initialized, glfwJoystickPresent returns 0
  // (GLFW_NOT_INITIALIZED) and pollController degrades to Disconnected
  // (GlfwControllerNative.scala:48) — polling NEVER crashes the frame loop. The JVM
  // mirror must degrade the same way when libglfw is not available/initialized in the
  // process, instead of leaving a poisoned class behind (NoClassDefFoundError) or
  // throwing out of the per-frame auto-poll hook (Controllers.scala:89).
  //
  // RED NOW: GlfwControllerJvmInit.init() throws before any poll can happen
  // (UnsatisfiedLinkError on first touch in this JVM, NoClassDefFoundError if
  // test (1) already poisoned the class).
  test(
    "ISS-763(2): after init(), headless polling reports every slot Disconnected without throwing (Native GLFW_NOT_INITIALIZED parity)"
  ) {
    withRestoredSeam {
      initOrFail() // ← RED: init() dies with a LinkageError today (see file header)
      val backend = new GlfwControllerBackend
      var i       = 0
      while (i < backend.maxControllers) {
        val state = backend.pollController(i)
        assert(
          !state.connected,
          s"slot $i: with GLFW unavailable/uninitialized, the JVM backend must degrade to Disconnected like the Native backend (GlfwControllerNative.scala:48)"
        )
        i += 1
      }
    }
  }
}
