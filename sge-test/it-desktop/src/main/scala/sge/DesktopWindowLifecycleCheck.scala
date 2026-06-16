// SGE — Desktop integration check: DesktopWindow / windowing lifecycle (ISS-551)
//
// Pins TWO of the three ISS-551 clauses as live, observable defects:
//
//   Clause 1 (no GLFW error callback installed). LibGDX installs a
//     GLFWErrorCallback at init so GLFW failures surface; SGE installs none —
//     WindowingOps has no setErrorCallback method and nothing calls
//     glfwSetErrorCallback, so GLFW errors are silently dropped. This check
//     drives the exact SGE init seam DesktopApplication uses
//     (WindowingOpsJvm().init()), then probes the live GLFW state with a RAW
//     glfwSetErrorCallback downcall: glfwSetErrorCallback RETURNS the
//     previously-installed callback. We pass NULL (the canonical "query the
//     current callback" trick) and assert the returned previous is NON-NULL —
//     i.e. SGE installed one at init — then restore it. Against current code
//     the previous is NULL → RED. This observes the GLFW effect without
//     depending on HOW SGE installs it.
//
//   Clause 3 (NPE if a window closes before its first update). DesktopWindow.
//     _listener is scala.compiletime.uninitialized until the first update()
//     materializes it (initializeListener sets _listenerInitialized = true).
//     But close() calls _listener.pause(); _listener.dispose() UNCONDITIONALLY
//     as its very first statements, so closing a window that was created but
//     never updated throws NullPointerException. This check constructs a
//     DesktopWindow the same way DesktopApplication.setupWindow does (real
//     hidden GLFW window, real WindowingOps + GlOps, create(handle) run so
//     graphics/input exist), then calls close() WITHOUT ever running update()/
//     render(). Against current code _listener.pause() NPEs → RED. The window
//     has no EGL context (eglContext == 0), so close() never touches glOps;
//     DesktopGraphics.close() is a no-op and DefaultDesktopInput.close() only
//     unregisters callbacks, so after the fix (guard on _listenerInitialized)
//     close() must complete cleanly.
//
// Crash-safe, in-process, NO ApplicationListener / no game loop. It lives in
// `package sge` so it can reach DesktopWindow's private[sge] constructor +
// create(), exactly as the desktop backend wires it. It never touches the
// monitor APIs (glfwGetMonitorName aborts headless, ISS-485) and never builds
// an FBO (ISS-572), and runs in its OWN subprocess
// (DesktopWindowLifecycleHarnessMain), so the only way each clause can FAIL is
// the contract under test.
//
// GLFW is reached for clause 1 via a raw Panama downcall handle against the
// loaderLookup that WindowingOpsJvm() populates (it System.load()s GLFW), the
// same library the desktop backend binds — exactly the convention
// MultiWindowEglCheck (ISS-538) uses for raw GL.

package sge

import sge.it.desktop.CheckResult
import sge.platform.{ GlOps, GlOpsJvm, WindowingOps, WindowingOpsJvm }
import scala.collection.mutable.ArrayBuffer

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*

/** Drives the SGE desktop windowing seams to pin two ISS-551 lifecycle defects (error callback + close-before-first-update). */
object DesktopWindowLifecycleCheck {

  private val P: AddressLayout = ADDRESS

  /** Runs both clauses and returns (clause1 = GLFW error callback installed, clause3 = no NPE on close-before-first-update). */
  def run(): (CheckResult, CheckResult) = {
    val windowing = WindowingOpsJvm()
    if (!windowing.init()) {
      val failed = CheckResult("desktopwindow_errorcallback", passed = false, "glfwInit() failed — cannot probe windowing lifecycle")
      (failed, CheckResult("desktopwindow_close_before_update", passed = false, failed.message))
    } else {
      val glOps = GlOpsJvm()
      try {
        val clause1 = errorCallbackClause()
        val clause3 = closeBeforeUpdateClause(windowing, glOps)
        (clause1, clause3)
      } finally {
        try windowing.terminate() catch { case _: Throwable => () }
      }
    }
  }

  /** Clause 1: after the SGE init seam (WindowingOpsJvm().init()), GLFW must already carry an installed error callback.
    *
    * glfwSetErrorCallback returns the PREVIOUS callback. We pass NULL to read the current one, assert the returned previous is non-NULL (SGE installed one at init), then restore it. Against current code
    * SGE installs none, so the previous is NULL → RED.
    */
  private def errorCallbackClause(): CheckResult = {
    // GLFW is already loaded into this process by WindowingOpsJvm() (System.load),
    // so loaderLookup resolves glfwSetErrorCallback — the same lookup the JVM
    // backend itself uses (WindowingOpsJvm.apply).
    val lookup = SymbolLookup.loaderLookup()
    val sym    = lookup.find("glfwSetErrorCallback")
    if (!sym.isPresent) {
      CheckResult(
        "desktopwindow_errorcallback",
        passed = false,
        "glfwSetErrorCallback symbol not found in the loaded GLFW library — cannot probe error-callback installation"
      )
    } else {
      val linker = Linker.nativeLinker()
      // GLFWerrorfun glfwSetErrorCallback(GLFWerrorfun callback) — pointer in, previous pointer out.
      val handle = linker.downcallHandle(sym.get(), FunctionDescriptor.of(P, P))

      // Raw GLFW interop boundary (matches the harness convention): pass NULL to
      // query the currently-installed callback; GLFW returns the previous pointer.
      val previous = handle.invoke(MemorySegment.NULL).asInstanceOf[MemorySegment]

      // Leave GLFW as we found it: re-install whatever was there (NULL if nothing).
      val _ = handle.invoke(previous).asInstanceOf[MemorySegment]

      val installed = previous != MemorySegment.NULL && previous.address() != 0L
      if (installed) {
        CheckResult(
          "desktopwindow_errorcallback",
          passed = true,
          s"GLFW error callback was installed at SGE init: glfwSetErrorCallback returned a non-NULL previous callback (0x${java.lang.Long.toHexString(previous.address())})"
        )
      } else {
        CheckResult(
          "desktopwindow_errorcallback",
          passed = false,
          "no GLFW error callback installed after SGE init: glfwSetErrorCallback returned NULL as the previous callback. " +
            "LibGDX installs a GLFWErrorCallback at init so GLFW failures surface; SGE's WindowingOps has no setErrorCallback " +
            "method and nothing calls glfwSetErrorCallback, so GLFW errors are silently dropped (ISS-551 clause 1)."
        )
      }
    }
  }

  /** Clause 3: closing a window that was created but never updated must not throw.
    *
    * Builds a DesktopWindow exactly as DesktopApplication.setupWindow does (real hidden GLFW window, create(handle) so graphics/input exist) but runs NO update()/render(), then calls close(). Against
    * current code close() calls _listener.pause() on the still-uninitialized _listener → NullPointerException → RED. The window has no EGL context so close() never touches glOps.
    */
  private def closeBeforeUpdateClause(windowing: WindowingOps, glOps: GlOps): CheckResult = {
    // Create a hidden, no-client-API GLFW window the same way
    // DesktopApplication.createGlfwWindow does (ANGLE owns GL via EGL).
    windowing.defaultWindowHints()
    windowing.setWindowHint(WindowingOps.GLFW_CLIENT_API, WindowingOps.GLFW_NO_API)
    windowing.setWindowHint(WindowingOps.GLFW_VISIBLE, WindowingOps.GLFW_FALSE)
    val handle = windowing.createWindow(64, 64, "SGE ISS-551 close-before-update")
    if (handle == 0L) {
      CheckResult("desktopwindow_close_before_update", passed = false, "glfwCreateWindow failed — cannot build a window to close")
    } else {
      val config = new DesktopApplicationConfig()
      config.disableAudio = true
      config.vSyncEnabled = false

      val window =
        new DesktopWindow(
          listenerFactory = LifecycleProbeApp.listenerFactory,
          lifecycleListeners = ArrayBuffer.empty[LifecycleListener],
          config = config,
          application = new LifecycleProbeApp(windowing),
          windowing = windowing,
          glOps = glOps
        )

      var closeCompleted = false
      try {
        // Run create() exactly as setupWindow does: this materializes _graphics
        // and _input (so the post-fix close() can dispose them safely) and
        // registers the GLFW callbacks — but NEVER update(), so _listener stays
        // uninitialized (_listenerInitialized == false).
        window.create(handle)

        // The action under test: close BEFORE the first update.
        window.close()
        closeCompleted = true

        CheckResult(
          "desktopwindow_close_before_update",
          passed = true,
          "DesktopWindow.close() completed without throwing on a window that was created but never updated"
        )
      } catch {
        case npe: NullPointerException =>
          CheckResult(
            "desktopwindow_close_before_update",
            passed = false,
            "DesktopWindow.close() threw NullPointerException on a window created but never updated: " +
              s"${npe.getMessage}. close() calls _listener.pause()/dispose() unconditionally, but _listener is " +
              "scala.compiletime.uninitialized until the first update() — close() must guard on _listenerInitialized (ISS-551 clause 3)."
          )
        case t: Throwable =>
          CheckResult(
            "desktopwindow_close_before_update",
            passed = false,
            s"DesktopWindow.close() threw ${t.getClass.getName} on a window created but never updated: ${t.getMessage}"
          )
      } finally {
        // On the green path close() already destroyed the native window; only the
        // red path (close aborted before destroyWindow) leaves it to clean up.
        if (!closeCompleted) {
          try windowing.destroyWindow(handle) catch { case _: Throwable => () }
        }
      }
    }
  }
}
