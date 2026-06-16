// SGE — Desktop integration check: multi-window EGL (ISS-538)
//
// Pins TWO structural defects of the desktop multi-window EGL path, both
// living in GlOpsJvm (mirror bug in GlOpsNative):
//
//   1. eglTerminate tears down ALL windows. GlOpsJvm.destroyContext does
//        eglMakeCurrent(none) -> eglDestroySurface -> eglDestroyContext ->
//        eglTerminate(display)
//      but eglTerminate terminates the ENTIRE shared EGL display connection,
//      invalidating every OTHER window's context/surface on that display.
//      Closing ONE window kills GL for ALL windows. Correct: destroy only this
//      window's surface+context; eglTerminate only when the LAST context goes
//      (or at app shutdown).
//
//   2. sharedContext is ignored, so there is no cross-window GL resource
//      sharing. GlOpsJvm.createContext always passes EGL_NO_CONTEXT as the
//      eglCreateContext share argument, and its trait signature has no
//      sharedContext parameter at all. DesktopApplication threads a
//      `sharedContext` Long all the way into setupWindow() ->
//      createGlfwWindow(config, sharedContext) and then DROPS it: it never
//      reaches glOps.createContext (DesktopApplication.scala:290 calls
//      createContext with NO share arg). So textures/buffers created in one
//      window's context are invisible in another.
//
// Crash-safe, in-process, NO ApplicationListener: we drive the exact SGE
// platform seams the desktop backend uses (WindowingOpsJvm + GlOpsJvm — the
// same objects DesktopApplicationFactory wires) to create TWO GLFW windows
// (GLFW_CLIENT_API = GLFW_NO_API, hidden) and TWO EGL contexts on the shared
// ANGLE display, then exercise the two contracts. The harness never touches
// the monitor APIs (glfwGetMonitorName aborts headless, ISS-485) and never
// builds an FBO (ISS-572), and runs in its OWN subprocess
// (MultiWindowEglHarnessMain), so the only way each clause can FAIL is the
// contract under test.
//
// GL is reached via raw Panama downcall handles bound against the GLESv2
// shared library resolved through multiarch.core.NativeLibLoader — the same
// library + loader DesktopApplicationFactory uses for AngleGL32. We bind only
// the half-dozen entry points each clause needs (glGetError, glGetString,
// glGenTextures, glBindTexture, glTexImage2D, glIsTexture).

package sge.it.desktop.checks

import sge.it.desktop.CheckResult
import sge.platform.{ GlOpsJvm, WindowingOps, WindowingOpsJvm }

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle

/** Drives two hidden GLFW windows + two EGL contexts to pin the multi-window EGL contract (ISS-538). */
object MultiWindowEglCheck {

  // ─── GL constants we need ──────────────────────────────────────────────
  private val GL_NO_ERROR:        Int = 0x0000
  private val GL_VERSION:         Int = 0x1f02
  private val GL_TEXTURE_2D:      Int = 0x0de1
  private val GL_RGBA:            Int = 0x1908
  private val GL_UNSIGNED_BYTE:   Int = 0x1401
  private val GL_TRUE:            Int = 1

  private val I: ValueLayout.OfInt    = JAVA_INT
  private val P: AddressLayout        = ADDRESS

  /** Bound GLESv2 entry points. Resolved once against the same shared library + loader the engine uses. */
  private final class Gl(lookup: SymbolLookup) {
    private val linker: Linker = Linker.nativeLinker()
    private def h(name: String, desc: FunctionDescriptor): MethodHandle =
      linker.downcallHandle(
        lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GLESv2 symbol not found: $name")),
        desc
      )

    private val hGetError    = h("glGetError", FunctionDescriptor.of(I))
    private val hGetString   = h("glGetString", FunctionDescriptor.of(P, I))
    private val hGenTextures = h("glGenTextures", FunctionDescriptor.ofVoid(I, P))
    private val hBindTexture = h("glBindTexture", FunctionDescriptor.ofVoid(I, I))
    private val hTexImage2D  = h("glTexImage2D", FunctionDescriptor.ofVoid(I, I, I, I, I, I, I, I, P))
    private val hIsTexture   = h("glIsTexture", FunctionDescriptor.of(I, I))

    def getError(): Int = hGetError.invoke().asInstanceOf[Int]

    /** glGetString(GL_VERSION); returns the version string, or "" if NULL (no current context / invalid display). */
    def getVersion(): String = {
      val seg = hGetString.invoke(GL_VERSION).asInstanceOf[MemorySegment]
      if (seg == MemorySegment.NULL || seg.address() == 0L) ""
      else seg.reinterpret(Long.MaxValue).getString(0)
    }

    def genTexture(): Int = {
      val arena = Arena.ofConfined()
      try {
        val out = arena.allocate(I)
        hGenTextures.invoke(1, out)
        out.get(I, 0)
      } finally arena.close()
    }

    def bindTexture(id: Int): Unit =
      hBindTexture.invoke(GL_TEXTURE_2D, id)

    /** Allocate a 1x1 RGBA texture for the currently bound texture object. */
    def texImage1x1(): Unit = {
      val arena = Arena.ofConfined()
      try {
        val pixels = arena.allocate(4L) // 1px * RGBA
        pixels.set(JAVA_BYTE, 0L, 0xff.toByte)
        pixels.set(JAVA_BYTE, 1L, 0x00.toByte)
        pixels.set(JAVA_BYTE, 2L, 0x00.toByte)
        pixels.set(JAVA_BYTE, 3L, 0xff.toByte)
        hTexImage2D.invoke(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
      } finally arena.close()
    }

    def isTexture(id: Int): Boolean = {
      val res: Int = hIsTexture.invoke(id).asInstanceOf[Int]
      res == GL_TRUE
    }
  }

  /** Runs both clauses and returns (clause1 = eglTerminate isolation, clause2 = cross-window sharing). */
  def run(): (CheckResult, CheckResult) = {
    val windowing = WindowingOpsJvm()
    if (!windowing.init()) {
      val failed = CheckResult("multiwindowegl", passed = false, "glfwInit() failed — cannot create windows")
      return (failed, failed.copy(message = failed.message))
    }

    val glOps  = GlOpsJvm()
    val glesv2 = multiarch.core.NativeLibLoader.load("GLESv2")
    val gl     = new Gl(SymbolLookup.libraryLookup(glesv2, Arena.global()))

    // Create two hidden, no-client-API GLFW windows. ANGLE owns the GL context
    // via EGL (GLFW_NO_API), exactly as DesktopApplication.createGlfwWindow does.
    def newGlfwWindow(title: String): Long = {
      windowing.defaultWindowHints()
      windowing.setWindowHint(WindowingOps.GLFW_CLIENT_API, WindowingOps.GLFW_NO_API)
      windowing.setWindowHint(WindowingOps.GLFW_VISIBLE, WindowingOps.GLFW_FALSE)
      windowing.createWindow(64, 64, title)
    }

    var win1: Long = 0L
    var win2: Long = 0L
    var ctx1: Long = 0L
    var ctx2: Long = 0L
    try {
      win1 = newGlfwWindow("SGE ISS-538 win1")
      win2 = newGlfwWindow("SGE ISS-538 win2")
      if (win1 == 0L || win2 == 0L) {
        val failed = CheckResult("multiwindowegl", passed = false, s"glfwCreateWindow failed (win1=$win1, win2=$win2)")
        return (failed, failed)
      }

      // Two EGL contexts on the shared ANGLE display — produced by the exact
      // production code path under test (GlOpsJvm.createContext).
      val native1 = windowing.getNativeWindowHandle(win1)
      val native2 = windowing.getNativeWindowHandle(win2)
      ctx1 = glOps.createContext(native1, 8, 8, 8, 8, 16, 0, 0)
      ctx2 = glOps.createContext(native2, 8, 8, 8, 8, 16, 0, 0)
      if (ctx1 == 0L || ctx2 == 0L) {
        val failed = CheckResult("multiwindowegl", passed = false, s"createContext failed (ctx1=$ctx1, ctx2=$ctx2)")
        return (failed, failed)
      }

      // ─── Clause 2 FIRST (sharing) — run while BOTH displays/contexts are
      // still alive, before clause 1 destroys ctx1. ──────────────────────────
      val clause2 = sharingClause(glOps, gl, ctx1, ctx2)

      // ─── Clause 1 (eglTerminate isolation) ────────────────────────────────
      val clause1 = terminateClause(glOps, gl, ctx1, ctx2)
      // ctx1 has been destroyed by terminateClause; null it so cleanup skips it.
      ctx1 = 0L

      (clause1, clause2)
    } finally {
      // Best-effort cleanup. Against the bug, destroyContext(ctx1) already
      // terminated the display, so destroying ctx2 may be a no-op; swallow.
      if (ctx2 != 0L) try glOps.destroyContext(ctx2) catch { case _: Throwable => () }
      if (ctx1 != 0L) try glOps.destroyContext(ctx1) catch { case _: Throwable => () }
      if (win2 != 0L) try windowing.destroyWindow(win2) catch { case _: Throwable => () }
      if (win1 != 0L) try windowing.destroyWindow(win1) catch { case _: Throwable => () }
      try windowing.terminate() catch { case _: Throwable => () }
    }
  }

  /** Clause 1: destroying window 1's context must NOT invalidate window 2's context. */
  private def terminateClause(glOps: GlOpsJvm, gl: Gl, ctx1: Long, ctx2: Long): CheckResult = {
    // Sanity: ctx2 is current + usable BEFORE we touch ctx1.
    glOps.makeCurrent(ctx2)
    val versionBefore = gl.getVersion()
    val _             = gl.getError() // clear any pending error
    if (versionBefore.isEmpty) {
      return CheckResult(
        "multiwindowegl_terminate",
        passed = false,
        "precondition failed: ctx2 had no GL_VERSION even before ctx1 was destroyed — cannot isolate the bug"
      )
    }

    // The action under test: tear down window 1's context.
    glOps.destroyContext(ctx1)

    // Window 2's context MUST still be valid: make it current, do a GL call.
    glOps.makeCurrent(ctx2)
    val versionAfter = gl.getVersion()
    val errAfter     = gl.getError()

    if (versionAfter.nonEmpty && errAfter == GL_NO_ERROR) {
      CheckResult(
        "multiwindowegl_terminate",
        passed = true,
        s"ctx2 still valid after destroyContext(ctx1): GL_VERSION='$versionAfter', glGetError=0x${errAfter.toHexString}"
      )
    } else {
      CheckResult(
        "multiwindowegl_terminate",
        passed = false,
        s"destroyContext(ctx1) invalidated ctx2 (the shared EGL display was terminated): after makeCurrent(ctx2) " +
          s"GL_VERSION='$versionAfter' (was '$versionBefore'), glGetError=0x${errAfter.toHexString}. " +
          "GlOpsJvm.destroyContext calls eglTerminate(display), tearing down the ENTIRE shared display and every " +
          "other window's context on it (ISS-538 clause 1)."
      )
    }
  }

  /** Clause 2: a texture created in window 1's context must be recognized in window 2's context (shared namespace).
    *
    * Pre-fix the two contexts are created with eglCreateContext(..., EGL_NO_CONTEXT, ...) — no sharing — so the id is NOT a texture in ctx2 → RED. The fix threads window 1's context into createContext
    * and passes it as the eglCreateContext share argument (the `sharedContext` DesktopApplication already plumbs into setupWindow but currently drops before glOps.createContext); with sharing, the same
    * two-window setup makes the id valid in ctx2 and this assertion flips to green.
    */
  private def sharingClause(glOps: GlOpsJvm, gl: Gl, ctx1: Long, ctx2: Long): CheckResult = {
    // Create + allocate a real texture object in window 1's context.
    glOps.makeCurrent(ctx1)
    val texId = gl.genTexture()
    gl.bindTexture(texId)
    gl.texImage1x1()
    val errGen = gl.getError()
    if (texId == 0 || errGen != GL_NO_ERROR) {
      return CheckResult(
        "multiwindowegl_sharing",
        passed = false,
        s"precondition failed: could not create a texture in ctx1 (texId=$texId, glGetError=0x${errGen.toHexString})"
      )
    }

    // Sanity: it is a texture in its OWN context.
    val validInCtx1 = gl.isTexture(texId)

    // Switch to window 2's context and query the SAME id.
    glOps.makeCurrent(ctx2)
    val validInCtx2 = gl.isTexture(texId)
    val errQuery    = gl.getError()

    if (validInCtx2 && errQuery == GL_NO_ERROR) {
      CheckResult(
        "multiwindowegl_sharing",
        passed = true,
        s"texture id $texId created in ctx1 is recognized in ctx2 — cross-window GL resource sharing works " +
          s"(validInCtx1=$validInCtx1, glGetError=0x${errQuery.toHexString})"
      )
    } else {
      CheckResult(
        "multiwindowegl_sharing",
        passed = false,
        s"texture id $texId created in ctx1 is NOT recognized in ctx2 (glIsTexture=$validInCtx2, " +
          s"validInCtx1=$validInCtx1, glGetError=0x${errQuery.toHexString}): the two EGL contexts do not share a " +
          "resource namespace. GlOpsJvm.createContext passes EGL_NO_CONTEXT as the eglCreateContext share arg and " +
          "has no sharedContext parameter, so the sharedContext DesktopApplication threads into setupWindow is " +
          "dropped before reaching eglCreateContext (ISS-538 clause 2). Once createContext threads + passes the " +
          "first window's context as the share context, this same setup makes the texture shared and flips green."
      )
    }
  }
}
