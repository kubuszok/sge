// SGE — Desktop integration test
//
// Launches DesktopHarnessMain as a subprocess so that GLFW's glfwInit()
// runs on the OS main thread (required by macOS Cocoa/AppKit).
// Reads the JSON results file and asserts all subsystem checks passed.
//
// Prerequisites:
//   - Rust native lib: just rust-build
//   - ANGLE (libGLESv2, libEGL) and GLFW (libglfw) in java.library.path
//   - miniaudio is bundled with the Rust native lib
//
// Run: sbt 'sge-it-desktop/test'  or  just it-desktop

package sge.it.desktop

import munit.FunSuite

import java.io.File
import java.nio.file.Files
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class DesktopIntegrationTest extends FunSuite {

  // Desktop app needs time to init GLFW + ANGLE + render frames + exit
  override val munitTimeout: Duration = 60.seconds

  // ── Headless test: runs on CI without a display server ──────────────
  // Validates that native libraries load and key FFI symbols exist.
  // This catches broken library paths, missing symbols, and linking issues.

  test("native libraries load in headless mode") {
    val libPath = System.getProperty("java.library.path", "")
    assume(libPath.nonEmpty, "java.library.path is not set — skipping")

    // Use Panama to load native libraries and verify key symbols exist.
    // JdkPanama is on the classpath via sge-jvm-platform-jdk.
    val panama = sge.platform.JdkPanama
    import panama.*

    val arena = Arena.global()

    // Core native ops library (buffer ops, ETC1, GLFW, miniaudio)
    val coreLib = findLibrary("sge_native_ops", libPath)
    assume(coreLib.isDefined, s"libsge_native_ops not on java.library.path — skipping (path: $libPath)")
    val core = SymbolLookup.libraryLookup(java.nio.file.Path.of(coreLib.get), arena)
    // Verify key symbols
    assert(core.findSymbol("sge_alloc_memory").isDefined, "sge_alloc_memory symbol missing")
    assert(core.findSymbol("sge_copy_bytes").isDefined, "sge_copy_bytes symbol missing")
    assert(core.findSymbol("sge_transform_v4m4").isDefined, "sge_transform_v4m4 symbol missing")

    // FreeType library
    val ftLib = findLibrary("sge_freetype", libPath)
    assert(ftLib.isDefined, s"libsge_freetype not found on java.library.path: $libPath")
    val ft = SymbolLookup.libraryLookup(java.nio.file.Path.of(ftLib.get), arena)
    assert(ft.findSymbol("sge_ft_init_freetype").isDefined, "sge_ft_init_freetype symbol missing")

    // Physics library
    val physLib = findLibrary("sge_physics", libPath)
    assert(physLib.isDefined, s"libsge_physics not found on java.library.path: $libPath")
    val phys = SymbolLookup.libraryLookup(java.nio.file.Path.of(physLib.get), arena)
    assert(phys.findSymbol("sge_physics_world_new").isDefined, "sge_physics_world_new symbol missing")

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

  /** Find a native library file on java.library.path. */
  private def findLibrary(name: String, libPath: String): Option[String] = {
    val libName = System.mapLibraryName(name)
    libPath.split(java.io.File.pathSeparator).iterator.map(dir => java.nio.file.Path.of(dir, libName)).find(java.nio.file.Files.exists(_)).map(_.toString)
  }

  // ── Windowed test: requires a display server ────────────────────────

  test("desktop harness runs all subsystem checks") {
    // Skip on headless CI environments (no display server for GLFW windowing)
    val isHeadless = java.awt.GraphicsEnvironment.isHeadless ||
      (System.getenv("DISPLAY") == null && System.getenv("WAYLAND_DISPLAY") == null &&
        !System.getProperty("os.name", "").toLowerCase.contains("mac") &&
        !System.getProperty("os.name", "").toLowerCase.contains("win"))
    assume(!isHeadless, "No display server available — skipping desktop harness test")

    val resultsFile = File.createTempFile("sge-it-desktop-", ".json")
    resultsFile.deleteOnExit()

    // Build subprocess command using same JVM and classpath as this test
    val javaHome = System.getProperty("java.home")
    val javaBin  = s"$javaHome/bin/java"
    val cp       = System.getProperty("java.class.path")
    val libPath  = System.getProperty("java.library.path", "")

    val cmd = new java.util.ArrayList[String]()
    cmd.add(javaBin)
    // On macOS, GLFW requires the main thread (thread 0) for Cocoa init
    if (System.getProperty("os.name", "").toLowerCase.contains("mac")) {
      cmd.add("-XstartOnFirstThread")
    }
    cmd.add(s"-Djava.library.path=$libPath")
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

    val failed = checks.filter(!_.passed)
    if (failed.nonEmpty) {
      val details = failed.map(c => s"  ${c.name}: ${c.message}").mkString("\n")
      fail(s"${failed.size} subsystem check(s) failed:\n$details")
    }

    assert(checks.size >= 23, s"Expected at least 23 checks, got ${checks.size}")
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
