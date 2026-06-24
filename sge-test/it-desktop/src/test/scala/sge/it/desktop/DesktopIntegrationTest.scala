// SGE — Desktop integration test
//
// Launches DesktopHarnessMain as a subprocess so that GLFW's glfwInit()
// runs on the OS main thread (required by macOS Cocoa/AppKit).
// Reads the JSON results file and asserts all subsystem checks passed.
//
// Prerequisites:
//   - Native libs ship via provider JARs (pnm-provider-sge-desktop,
//     pnm-provider-sge-freetype-desktop, pnm-provider-sge-physics-desktop),
//     resolved transitively from sge/sge-freetype/sge-physics and loaded at
//     runtime by multiarch.core.NativeLibLoader (classpath extraction) — no
//     java.library.path wiring required.
//   - A display server (or xvfb/virtual display) for the windowed harness.
//
// CI hard-fail switch:
//   When SGE_CI_REQUIRE_DISPLAY=1 every skip in this suite becomes a
//   hard assertion (FAIL, not skip), so the job can never silently degrade to
//   a vacuous green where every test assume-skips. See requireOrAssume.
//
// Run: sbt 'sge-it-desktop/test'  or  re-scale runner desktop-it

package sge.it.desktop

import munit.FunSuite

import java.io.File
import java.nio.file.Files
import scala.concurrent.duration._

class DesktopIntegrationTest extends FunSuite {

  // Desktop app needs time to init GLFW + ANGLE + render frames + exit
  override val munitTimeout: Duration = 60.seconds

  // ── CI hard-fail switch ─────────────────────────────────────────────
  // SGE_CI_REQUIRE_DISPLAY=1 (set in the CI job env) flips every skip in
  // this suite into a hard failure. A precondition the job is supposed to
  // guarantee (display present, native libs resolvable) must never silently
  // skip on CI — that is exactly the vacuous-green failure mode ISS-485
  // documents. Locally the var is unset, so developers without a display or
  // native libs still get the usual skips.
  private val requireDisplay: Boolean =
    System.getenv("SGE_CI_REQUIRE_DISPLAY") == "1"

  /** Like munit's `assume`, but hard-fails instead of skipping when SGE_CI_REQUIRE_DISPLAY=1. Use everywhere a precondition would otherwise skip the test, so CI cannot return to zero executed
    * assertions.
    */
  private def requireOrAssume(cond: Boolean, clue: => String): Unit =
    if (requireDisplay) assert(cond, s"SGE_CI_REQUIRE_DISPLAY=1 but precondition failed: $clue")
    else assume(cond, clue)

  /** Classpath for the harness subprocesses.
    *
    * Under sbt 2.0, forked tests are launched by `sbt.ForkMain` with only sbt's agent jars on the JVM `-cp`; the application/test classpath is loaded through a separate classloader, so
    * `System.getProperty("java.class.path")` no longer lists the harness main classes (under sbt 1.x it did). The build injects the real `Test/fullClasspath` via `-Dsge.it.classpath=…`; fall back to
    * `java.class.path` for any non-forked/non-sbt invocation.
    */
  private val harnessClasspath: String =
    Option(System.getProperty("sge.it.classpath")).filter(_.nonEmpty).getOrElse(System.getProperty("java.class.path"))

  // ── Headless test: runs on CI without a display server ──────────────
  // Validates that native libraries load and key FFI symbols exist.
  // This catches broken library paths, missing symbols, and linking issues.

  test("native libraries load in headless mode") {
    // Use Panama to load native libraries and verify key symbols exist.
    // JdkPanama is on the classpath via multiarch-panama-jdk dependency.
    val panama = multiarch.panama.JdkPanama
    import panama.*

    val arena = Arena.global()

    // Native libraries are resolved from the provider JARs on the classpath
    // (pnm-provider-sge-desktop / -freetype-desktop / -physics-desktop) via
    // multiarch.core.NativeLibLoader, which detects the host platform,
    // extracts the right native/<classifier>/<lib> resource, and returns the
    // extracted file path. This is the same loading path the engine uses at
    // runtime (see sge.platform.*Jvm and DesktopApplicationFactory) — we do
    // NOT reimplement OS detection or scan java.library.path.
    val coreLibPath = loadNative("sge_native_ops")
    requireOrAssume(coreLibPath.isRight, s"libsge_native_ops not resolvable via provider JARs: ${coreLibPath.left.toOption.getOrElse("")}")

    val core = SymbolLookup.libraryLookup(coreLibPath.toOption.get, arena)
    // Verify key symbols
    assert(core.findSymbol("sge_alloc_memory").isDefined, "sge_alloc_memory symbol missing")
    assert(core.findSymbol("sge_copy_bytes").isDefined, "sge_copy_bytes symbol missing")
    assert(core.findSymbol("sge_transform_v4m4").isDefined, "sge_transform_v4m4 symbol missing")

    // FreeType library
    val ftLibPath = loadNative("sge_freetype")
    assert(ftLibPath.isRight, s"libsge_freetype not resolvable via provider JARs: ${ftLibPath.left.toOption.getOrElse("")}")
    val ft = SymbolLookup.libraryLookup(ftLibPath.toOption.get, arena)
    assert(ft.findSymbol("sge_ft_init_freetype").isDefined, "sge_ft_init_freetype symbol missing")

    // Physics library
    val physLibPath = loadNative("sge_physics")
    assert(physLibPath.isRight, s"libsge_physics not resolvable via provider JARs: ${physLibPath.left.toOption.getOrElse("")}")
    val phys = SymbolLookup.libraryLookup(physLibPath.toOption.get, arena)
    // sge_phys_create_world is the world-construction entry point the engine
    // actually binds (sge.platform.PhysicsOpsPanama:47). The old assertion
    // used a non-existent "sge_physics_world_new" name, but it never ran
    // because the suite assume-skipped at the dead java.library.path.
    assert(phys.findSymbol("sge_phys_create_world").isDefined, "sge_phys_create_world symbol missing")

    // Quick FFI roundtrip: alloc + free to verify downcalls work
    val linker   = Linker.nativeLinker()
    val allocSym = core.findOrThrow("sge_alloc_memory")
    val allocFd  = FunctionDescriptor.of(ADDRESS, JAVA_INT)
    val allocMh  = linker.downcallHandle(allocSym, allocFd)
    val freeSym  = core.findOrThrow("sge_free_memory")
    val freeFd   = FunctionDescriptor.ofVoid(ADDRESS)
    val freeMh   = linker.downcallHandle(freeSym, freeFd)

    val ptr = allocMh.invoke(64: java.lang.Integer).asInstanceOf[java.lang.foreign.MemorySegment]
    assert(!ptr.equals(java.lang.foreign.MemorySegment.NULL), "alloc returned null")
    freeMh.invoke(ptr)
  }

  /** Resolve a native library via the multiarch-core loader (provider-JAR classpath extraction, the engine's real loading path). Returns Right with the extracted library Path, or Left with the
    * loader's diagnostic message when the library cannot be resolved on this platform.
    */
  private def loadNative(name: String): Either[String, java.nio.file.Path] =
    try Right(multiarch.core.NativeLibLoader.load(name))
    catch {
      case e: UnsatisfiedLinkError => Left(e.getMessage)
      case e: LinkageError         => Left(s"${e.getClass.getSimpleName}: ${e.getMessage}")
    }

  // ── Windowed test: requires a display server ────────────────────────

  test("desktop harness runs all subsystem checks") {
    // Skip on headless CI environments (no display server for GLFW windowing).
    // Under SGE_CI_REQUIRE_DISPLAY=1 this becomes a hard failure instead — CI
    // sets that var and provides a virtual display (xvfb), so a missing display
    // there is a real failure, not a reason to skip.
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    requireOrAssume(!isHeadless, "No display server available — skipping desktop harness test")

    val resultsFile = File.createTempFile("sge-it-desktop-", ".json")
    resultsFile.deleteOnExit()

    // Build subprocess command using same JVM and classpath as this test
    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = harnessClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    // On macOS, GLFW requires the main thread (thread 0) for Cocoa init
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add("--enable-native-access=ALL-UNNAMED")
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.it.desktop.DesktopHarnessMain")
    cmd.add(resultsFile.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO() // show stdout/stderr in test output
    val process  = pb.start()
    val exitCode = process.waitFor()

    assertEquals(exitCode, 0, s"Harness process exited with code $exitCode")

    // Read and parse results
    val json = new String(Files.readAllBytes(resultsFile.toPath))
    assert(json.nonEmpty, "Results file is empty")
    System.err.println("=== Desktop IT Results ===")
    System.err.println(json)

    // Parse check results (simple JSON parsing)
    val checks = parseChecks(json)
    assert(checks.nonEmpty, "No checks found in results")

    // The harness must have executed its full check list — never a subset.
    assert(checks.size >= 23, s"Expected at least 23 checks, got ${checks.size}")

    // The `fbo` check exercised a genuine product bug (ISS-572): the
    // GLFrameBuffer.textureAttachments createRef-backed DynamicArray[GLTexture]
    // threw a ClassCastException at GLFrameBuffer.foreach (GLFrameBuffer.scala:302)
    // — "[Ljava.lang.Object; cannot be cast to [Lsge.graphics.GLTexture;" — from
    // the lowlevel.* MkArray.withResolved castArray fallback. That lls/createRef
    // bug is fixed in lls 0.2.0, so the check now passes and is no longer
    // excluded: the harness must run its full check list with every check green.
    val fboResult = checks.find(_.name == "fbo")
    assert(fboResult.isDefined, "fbo check did not execute — harness did not run all checks")

    val failed = checks.filterNot(_.passed)
    if (failed.nonEmpty) {
      val details = failed.map(c => s"  ${c.name}: ${c.message}").mkString("\n")
      fail(s"${failed.size} subsystem check(s) failed:\n$details")
    }
  }

  // ── ISS-537: supportsExtension reflects the live GL context ─────────
  // DesktopGraphics.supportsExtension(name) must consult the actual ANGLE GL
  // context, not the GLFW context. The GLFW window is created with
  // GLFW_NO_API (DesktopApplication.scala), so glfwExtensionSupported()
  // returns false for EVERY name — even extensions the ANGLE ES 3.0 context
  // genuinely exposes. The desktop backend must instead enumerate via
  // glGetStringi(GL_EXTENSIONS, i) for i in 0 until glGetInteger(GL_NUM_EXTENSIONS).
  //
  // This runs in a dedicated subprocess (SupportsExtensionHarnessMain) that
  // touches ONLY the GL extension APIs — never the monitor APIs — so the only
  // way it can FAIL is the supportsExtension contract itself. The harness
  // independently enumerates the live extension set, picks a genuinely-present
  // extension, and asserts supportsExtension returns true for it (and false
  // for an absent name). Against the buggy DesktopGraphics this is the red:
  // supportsExtension returns false for a present extension.

  test("supportsExtension reflects the live GL context (ISS-537)") {
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    requireOrAssume(!isHeadless, "No display server available — skipping supportsExtension test")

    val resultsFile = File.createTempFile("sge-it-supportsext-", ".txt")
    resultsFile.deleteOnExit()

    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = harnessClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add("--enable-native-access=ALL-UNNAMED")
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.it.desktop.SupportsExtensionHarnessMain")
    cmd.add(resultsFile.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO()
    val process  = pb.start()
    val exitCode = process.waitFor()

    assertEquals(exitCode, 0, s"supportsExtension harness process exited with code $exitCode")

    val result = new String(Files.readAllBytes(resultsFile.toPath)).trim
    assert(result.nonEmpty, "supportsExtension result file is empty")
    System.err.println(s"=== ISS-537 supportsExtension result ===\n$result")

    // The harness writes "PASS:msg" or "FAIL:msg". Against the buggy code it
    // writes FAIL because supportsExtension('<present>') returned false.
    assert(
      result.startsWith("PASS:"),
      s"ISS-537: supportsExtension does not reflect the live GL context: $result"
    )
  }

  // ── ISS-540: glGetActiveUniformBlockName must not overflow the caller buffer ──
  // The BUFFER overload
  //   GL30.glGetActiveUniformBlockName(program, idx, length, uniformBlockName)
  // passes a GLsizei `bufSize` to native GL — the max bytes GL may write into
  // `uniformBlockName`. AngleGL30 / AngleGL30Native HARDCODE this to 1024 instead
  // of deriving it from uniformBlockName.remaining(), so a caller buffer smaller
  // than the block name is overrun → native heap corruption. The correct value is
  // remaining() (glGetDebugMessageLog already does this).
  //
  // This runs in a dedicated subprocess (UniformBlockNameHarnessMain) that touches
  // ONLY shader/program/UBO APIs — never the monitor APIs (glfwGetMonitorName
  // aborts headless) and never an FBO (ISS-572 ClassCastException) — so the only
  // way it can FAIL is the overflow itself. The harness compiles + links a real
  // ES3 program with a 26-char uniform block, allocates a LARGE (2048) direct
  // buffer filled with a 0xAA sentinel, grants only remaining()==8, calls the
  // buffer overload, and asserts no byte past index 8 was clobbered. Against the
  // buggy bufSize=1024, GL writes the full 26-char name → sentinel overwritten →
  // the red.

  test("glGetActiveUniformBlockName respects caller buffer remaining() (ISS-540)") {
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    requireOrAssume(!isHeadless, "No display server available — skipping uniformBlockName overflow test")

    val resultsFile = File.createTempFile("sge-it-uniformblockname-", ".txt")
    resultsFile.deleteOnExit()

    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = harnessClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add("--enable-native-access=ALL-UNNAMED")
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.it.desktop.UniformBlockNameHarnessMain")
    cmd.add(resultsFile.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO()
    val process  = pb.start()
    val exitCode = process.waitFor()

    assertEquals(exitCode, 0, s"uniformBlockName harness process exited with code $exitCode")

    val result = new String(Files.readAllBytes(resultsFile.toPath)).trim
    assert(result.nonEmpty, "uniformBlockName result file is empty")
    System.err.println(s"=== ISS-540 uniformBlockName overflow result ===\n$result")

    // The harness writes "PASS:msg" or "FAIL:msg". Against the buggy code it
    // writes FAIL because glGetActiveUniformBlockName wrote past remaining().
    assert(
      result.startsWith("PASS:"),
      s"ISS-540: glGetActiveUniformBlockName overflowed the caller buffer (hardcoded bufSize 1024): $result"
    )
  }

  // ── ISS-538: multi-window EGL must isolate contexts and share resources ──
  // TWO structural defects in the desktop multi-window EGL path (GlOpsJvm,
  // mirror bug in GlOpsNative):
  //   (1) GlOpsJvm.destroyContext calls eglTerminate(display) — but the EGL
  //       display is SHARED across all windows, so tearing down ONE window's
  //       context terminates the display and invalidates EVERY other window's
  //       context. Closing one window kills GL for all of them.
  //   (2) GlOpsJvm.createContext always passes EGL_NO_CONTEXT as the
  //       eglCreateContext share argument (and has no sharedContext param), so
  //       textures/buffers created in one window are invisible in another.
  //       DesktopApplication threads a `sharedContext` into setupWindow ->
  //       createGlfwWindow but drops it before glOps.createContext.
  //
  // This runs in a DEDICATED subprocess (MultiWindowEglHarnessMain) that
  // creates TWO hidden GLFW windows + TWO EGL contexts on the shared display
  // and exercises both contracts via raw GLESv2 calls. It never touches the
  // monitor APIs (glfwGetMonitorName aborts headless, ISS-485) and never
  // builds an FBO (ISS-572), so the only way each clause can FAIL is the
  // contract under test. The harness writes one "PASS:msg"/"FAIL:msg" line per
  // clause to two separate files; both must PASS.

  test("multi-window EGL: destroying one window keeps others valid, contexts share resources (ISS-538)") {
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    requireOrAssume(!isHeadless, "No display server available — skipping multi-window EGL test")

    val clause1File = File.createTempFile("sge-it-multiwindowegl-terminate-", ".txt")
    val clause2File = File.createTempFile("sge-it-multiwindowegl-sharing-", ".txt")
    clause1File.deleteOnExit()
    clause2File.deleteOnExit()

    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = harnessClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add("--enable-native-access=ALL-UNNAMED")
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.it.desktop.MultiWindowEglHarnessMain")
    cmd.add(clause1File.getAbsolutePath)
    cmd.add(clause2File.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO()
    val process  = pb.start()
    val exitCode = process.waitFor()

    assertEquals(exitCode, 0, s"multi-window EGL harness process exited with code $exitCode")

    val clause1 = new String(Files.readAllBytes(clause1File.toPath)).trim
    val clause2 = new String(Files.readAllBytes(clause2File.toPath)).trim
    assert(clause1.nonEmpty, "multi-window EGL clause-1 result file is empty")
    assert(clause2.nonEmpty, "multi-window EGL clause-2 result file is empty")
    System.err.println(
      s"=== ISS-538 multi-window EGL results ===\nclause1 (eglTerminate isolation): $clause1\nclause2 (cross-window sharing): $clause2"
    )

    // Clause 1: destroyContext(ctx1) must NOT terminate the shared display.
    // Against the bug, eglTerminate(display) invalidates ctx2 → FAIL.
    assert(
      clause1.startsWith("PASS:"),
      s"ISS-538 clause 1: destroying one window's EGL context terminated the SHARED display and invalidated " +
        s"another window's context (GlOpsJvm.destroyContext calls eglTerminate per-window): $clause1"
    )

    // Clause 2: a texture from ctx1 must be recognized in ctx2 (shared namespace).
    // Against the bug, eglCreateContext uses EGL_NO_CONTEXT → no sharing → FAIL.
    assert(
      clause2.startsWith("PASS:"),
      s"ISS-538 clause 2: two windows' EGL contexts do not share GL resources (GlOpsJvm.createContext passes " +
        s"EGL_NO_CONTEXT and drops the sharedContext DesktopApplication threads into setupWindow): $clause2"
    )
  }

  // ── ISS-539: gl31Available / gl32Available must reflect the real GL version ──
  // DesktopApplication (scaladesktop/sge/DesktopApplication.scala:308-309)
  // creates ONE AngleGL32 instance and installs it as gl20 AND gl30 AND gl31 AND
  // gl32 (all Nullable-present), unconditionally:
  //     val gl32 = glFactory()
  //     window.graphics.initGL(gl32, Nullable(gl32), Nullable(gl32), Nullable(gl32))
  // So DesktopGraphics.gl31Available (= _gl31.isDefined) and gl32Available
  // (= _gl32.isDefined) are ALWAYS true, even though the desktop EGL context is
  // requested as ES 3.0 (GlOpsJvm asks EGL_CONTEXT_MAJOR_VERSION=3, MINOR=0) and
  // the window reports GL=3.0. Code guarded by `if (gl32Available)` then dispatches
  // ES 3.1/3.2 entry points on a 3.0 context → GL errors.
  //
  // This runs in a DEDICATED subprocess (GlVersionGateHarnessMain) that touches
  // ONLY the GL version-query APIs — never the monitor APIs (glfwGetMonitorName
  // aborts headless, ISS-485) and never an FBO (ISS-572 ClassCastException) — so
  // the only way it can FAIL is the version-gate contract itself. The harness
  // queries the live context's major.minor via
  // glGetIntegerv(GL_MAJOR_VERSION/GL_MINOR_VERSION) and asserts, policy-agnostically:
  //   gl31Available == (major > 3 || (major == 3 && minor >= 1))
  //   gl32Available == (major > 3 || (major == 3 && minor >= 2))
  // On the real ES 3.0 ANGLE context (major=3, minor=0) both expected values are
  // false but the current code returns true for both → the red.
  //
  // NOTE: the install site (DesktopApplication:308-309) lives in scaladesktop,
  // shared by JVM + Native, so the bug is identical on Native; this IT proves it
  // on JVM only.

  test("gl31Available/gl32Available reflect the live GL version, not unconditional truth (ISS-539)") {
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    requireOrAssume(!isHeadless, "No display server available — skipping GL version-gate test")

    val resultsFile = File.createTempFile("sge-it-glversiongate-", ".txt")
    resultsFile.deleteOnExit()

    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = harnessClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add("--enable-native-access=ALL-UNNAMED")
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.it.desktop.GlVersionGateHarnessMain")
    cmd.add(resultsFile.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO()
    val process  = pb.start()
    val exitCode = process.waitFor()

    assertEquals(exitCode, 0, s"GL version-gate harness process exited with code $exitCode")

    val result = new String(Files.readAllBytes(resultsFile.toPath)).trim
    assert(result.nonEmpty, "GL version-gate result file is empty")
    System.err.println(s"=== ISS-539 GL version-gate result ===\n$result")

    // The harness writes "PASS:msg" or "FAIL:msg". Against the buggy code it
    // writes FAIL because gl31Available/gl32Available are unconditionally true on
    // an ES 3.0 context.
    assert(
      result.startsWith("PASS:"),
      s"ISS-539: gl31Available/gl32Available do not reflect the live GL version: $result"
    )
  }

  // ── ISS-551: desktop window / windowing lifecycle ──────────────────────
  // THREE clauses in the SGE-original desktop window code; this IT pins two of
  // them as live, observable defects (the third, close() leaking the maximize +
  // refresh callbacks, is fixed by inspection — see the issue):
  //
  //   Clause 1 (no GLFW error callback installed). LibGDX installs a
  //     GLFWErrorCallback at init so GLFW failures surface; SGE installs none —
  //     WindowingOps has no setErrorCallback method and nothing calls
  //     glfwSetErrorCallback. The harness drives the SGE init seam
  //     (WindowingOpsJvm().init()) then probes the live GLFW state with a RAW
  //     glfwSetErrorCallback downcall, which returns the previously-installed
  //     callback. Against the bug that previous is NULL → RED.
  //
  //   Clause 3 (NPE if a window closes before its first update). DesktopWindow.
  //     close() calls _listener.pause()/dispose() unconditionally, but _listener
  //     is scala.compiletime.uninitialized until the first update(). The harness
  //     builds a DesktopWindow the same way DesktopApplication.setupWindow does
  //     and calls close() WITHOUT any update() → _listener.pause() NPEs → RED.
  //
  // This runs in a DEDICATED subprocess (DesktopWindowLifecycleHarnessMain) that
  // touches only the windowing-lifecycle seams — never the monitor APIs
  // (glfwGetMonitorName aborts headless, ISS-485) and never an FBO (ISS-572) —
  // so the only way each clause can FAIL is the contract under test. The harness
  // writes one "PASS:msg"/"FAIL:msg" line per clause to two files; both must PASS.
  //
  // NOTE: the bug sites (DesktopWindow.close ~360-374 and the absent
  // setErrorCallback in WindowingOps) live in scaladesktop / shared platform
  // code; this IT proves the two clauses on JVM only.

  test("desktop window lifecycle: GLFW error callback installed at init; close() before first update does not NPE (ISS-551)") {
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    requireOrAssume(!isHeadless, "No display server available — skipping desktop window lifecycle test")

    val clause1File = File.createTempFile("sge-it-desktopwindow-errorcallback-", ".txt")
    val clause3File = File.createTempFile("sge-it-desktopwindow-closebeforeupdate-", ".txt")
    clause1File.deleteOnExit()
    clause3File.deleteOnExit()

    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = harnessClasspath

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add("--enable-native-access=ALL-UNNAMED")
    cmd.add("-cp")
    cmd.add(cp)
    cmd.add("sge.it.desktop.DesktopWindowLifecycleHarnessMain")
    cmd.add(clause1File.getAbsolutePath)
    cmd.add(clause3File.getAbsolutePath)

    val pb = new ProcessBuilder(cmd)
    pb.inheritIO()
    val process  = pb.start()
    val exitCode = process.waitFor()

    assertEquals(exitCode, 0, s"desktop window lifecycle harness process exited with code $exitCode")

    val clause1 = new String(Files.readAllBytes(clause1File.toPath)).trim
    val clause3 = new String(Files.readAllBytes(clause3File.toPath)).trim
    assert(clause1.nonEmpty, "desktop window lifecycle clause-1 result file is empty")
    assert(clause3.nonEmpty, "desktop window lifecycle clause-3 result file is empty")
    System.err.println(
      s"=== ISS-551 desktop window lifecycle results ===\nclause1 (error callback installed): $clause1\nclause3 (close before first update): $clause3"
    )

    // Evaluate BOTH clauses before failing so a one-clause fix can't flip the
    // suite green: the test fails until clause 1 (error callback installed) AND
    // clause 3 (no NPE on close-before-first-update) both PASS.
    //   Clause 1: against the bug, glfwSetErrorCallback returns NULL as the
    //     previous callback → FAIL.
    //   Clause 3: against the bug, _listener.pause() on the uninitialized
    //     _listener → NullPointerException → FAIL.
    val failures = scala.collection.mutable.ListBuffer.empty[String]
    if (!clause1.startsWith("PASS:")) {
      failures += s"ISS-551 clause 1: SGE installs no GLFW error callback at init (WindowingOps has no setErrorCallback and " +
        s"nothing calls glfwSetErrorCallback) so GLFW errors are silently dropped: $clause1"
    }
    if (!clause3.startsWith("PASS:")) {
      failures += s"ISS-551 clause 3: DesktopWindow.close() NPEs on a window closed before its first update (it calls " +
        s"_listener.pause()/dispose() before _listener is initialized): $clause3"
    }
    if (failures.nonEmpty) {
      fail(s"${failures.size} ISS-551 clause(s) failed:\n${failures.mkString("\n")}")
    }
  }

  /** Minimal JSON parsing for check results. */
  private def parseChecks(json: String): Seq[CheckResult] = {
    // Match {"name":"...","passed":...,"message":"..."}
    val pattern = """"name"\s*:\s*"([^"]+)"\s*,\s*"passed"\s*:\s*(true|false)\s*,\s*"message"\s*:\s*"([^"]*)"""".r
    pattern
      .findAllMatchIn(json)
      .map { m =>
        CheckResult(m.group(1), m.group(2) == "true", m.group(3))
      }
      .toSeq
  }
}
