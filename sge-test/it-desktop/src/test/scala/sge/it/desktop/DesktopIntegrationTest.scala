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

    assertEquals(checks.size, 20, s"Expected 20 checks, got ${checks.size}")
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
