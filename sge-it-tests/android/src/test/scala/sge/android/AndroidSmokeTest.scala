// SGE — Android integration test: Smoke + subsystem checks via headless emulator
//
// Launches the smoke test APK on a headless Android emulator (AVD)
// with SwiftShader (CPU-based GL ES) and monitors logcat for runtime
// errors and structured subsystem check results. Catches:
// - ClassNotFoundException / NoClassDefFoundError from missing deps
// - UnsatisfiedLinkError from missing native libraries
// - NullPointerException from initialization order bugs
// - GL errors from wrong API usage
// - Any FATAL exception during app startup
//
// Also parses structured SGE-IT check results from logcat:
//   SGE-IT:<SUBSYSTEM>:<PASS|FAIL>:<message>
//
// Prerequisites:
//   1. Android SDK with emulator + system image:
//        just android-sdk-setup
//   2. Build the smoke APK:
//        sbt 'sge-android-smoke/androidSign'
//   3. AVD created and emulator running (or test starts one):
//        just android-emulator-start
//
// Run: sbt 'sge-it-android/test'  or  just test-android

package sge.android

import munit.FunSuite

import java.io.File
import java.nio.file.{ Files, Path, Paths }
import scala.collection.mutable
import scala.sys.process.{ Process, ProcessLogger }
import scala.concurrent.duration._

class AndroidSmokeTest extends FunSuite {

  // Generous timeout — emulator boot + APK install + 30 frames
  override val munitTimeout: Duration = 180.seconds

  private val PACKAGE  = "sge.smoke"
  private val ACTIVITY = "sge.smoke.SmokeActivity"
  private val TAG      = "SGE-SMOKE"
  private val AVD_NAME = "sge-test-avd"

  // ── ADB helpers ─────────────────────────────────────────────────────

  /** Finds the adb binary from ANDROID_HOME or local android-sdk/. */
  private def findAdb(): String = {
    val sdkRoot = sys.env.getOrElse(
      "ANDROID_HOME",
      sys.env.getOrElse(
        "ANDROID_SDK_ROOT", {
          val local = Paths.get(System.getProperty("user.dir"), "android-sdk")
          if (Files.isDirectory(local)) local.toString
          else fail("Android SDK not found. Set ANDROID_HOME or run 'just android-sdk-setup'.")
        }
      )
    )
    val adb = Paths.get(sdkRoot, "platform-tools", "adb")
    if (Files.exists(adb)) adb.toString
    else fail(s"adb not found at $adb. Install platform-tools.")
  }

  /** Finds the emulator binary. */
  private def findEmulator(): String = {
    val sdkRoot  = sys.env.getOrElse("ANDROID_HOME", sys.env.getOrElse("ANDROID_SDK_ROOT", Paths.get(System.getProperty("user.dir"), "android-sdk").toString))
    val emulator = Paths.get(sdkRoot, "emulator", "emulator")
    if (Files.exists(emulator)) emulator.toString
    else fail(s"emulator not found at $emulator. Install emulator package.")
  }

  /** Runs an adb command and returns (exitCode, stdout, stderr). */
  private def adb(adbPath: String, args: String*): (Int, String, String) = {
    val stdout = new StringBuilder
    val stderr = new StringBuilder
    val exit   = Process(Seq(adbPath) ++ args).!(
      ProcessLogger(
        line => stdout.append(line).append('\n'),
        line => stderr.append(line).append('\n')
      )
    )
    (exit, stdout.toString, stderr.toString)
  }

  /** Waits for a device to be ready (boot completed). */
  private def waitForDevice(adbPath: String, timeout: Duration): Unit = {
    val deadline = System.currentTimeMillis() + timeout.toMillis
    // Wait for device to appear
    Process(Seq(adbPath, "wait-for-device")).!

    // Wait for boot to complete
    var booted = false
    while (!booted && System.currentTimeMillis() < deadline) {
      val (exit, out, _) = adb(adbPath, "shell", "getprop", "sys.boot_completed")
      if (exit == 0 && out.trim == "1") booted = true
      else Thread.sleep(2000)
    }
    if (!booted) fail("Emulator did not boot within timeout")
  }

  /** Checks if an emulator is already running. */
  private def isEmulatorRunning(adbPath: String): Boolean = {
    val (exit, out, _) = adb(adbPath, "devices")
    exit == 0 && out.contains("emulator-")
  }

  // ── APK helpers ─────────────────────────────────────────────────────

  /** Finds the signed smoke test APK. */
  private def findApk(): Path = {
    val cwd        = Paths.get(System.getProperty("user.dir"))
    val candidates = Seq(
      cwd.resolve("sge-android-smoke/target/scala-3.8.2/android/app-debug.apk"),
      cwd.resolve("../../sge-android-smoke/target/scala-3.8.2/android/app-debug.apk").normalize
    )
    candidates
      .find(Files.exists(_))
      .getOrElse(
        fail(
          "Smoke APK not found. Run 'sbt sge-android-smoke/androidSign' first.\n" +
            s"Checked: ${candidates.mkString(", ")}"
        )
      )
  }

  // ── Test ────────────────────────────────────────────────────────────

  test("smoke APK launches on emulator without fatal errors") {
    val adbPath = findAdb()
    val apkPath = findApk()

    // Start emulator if not running
    val weStartedEmulator = if (!isEmulatorRunning(adbPath)) {
      val emulatorPath = findEmulator()
      System.err.println(s"Starting emulator $AVD_NAME ...")
      // Start in background — headless, SwiftShader for GL, no audio/boot-anim
      val emulatorProcess = Process(
        Seq(
          emulatorPath,
          "-avd",
          AVD_NAME,
          "-no-window",
          "-gpu",
          "swiftshader_indirect",
          "-no-snapshot",
          "-noaudio",
          "-no-boot-anim"
        )
      ).run(ProcessLogger(_ => (), _ => ()))
      waitForDevice(adbPath, 120.seconds)
      true
    } else {
      System.err.println("Emulator already running")
      false
    }

    try {
      // Clear logcat
      adb(adbPath, "logcat", "-c")

      // Install APK
      System.err.println(s"Installing $apkPath ...")
      val (installExit, installOut, installErr) = adb(adbPath, "install", "-r", apkPath.toString)
      assert(installExit == 0, s"APK install failed:\n$installOut\n$installErr")

      // Launch the smoke activity
      System.err.println("Launching SmokeActivity ...")
      val (launchExit, _, launchErr) = adb(adbPath, "shell", "am", "start", "-n", s"$PACKAGE/$ACTIVITY")
      assert(launchExit == 0, s"Activity launch failed:\n$launchErr")

      // Wait for the app to run (30 frames at ~60fps = ~0.5s, plus startup overhead)
      Thread.sleep(15000)

      // Capture logcat
      val (_, logcat, _) = adb(adbPath, "logcat", "-d", "-s", s"$TAG:*", "AndroidRuntime:*")
      System.err.println("=== Logcat output ===")
      System.err.println(logcat)

      // Check for success marker
      val passed = logcat.contains("SMOKE_TEST_PASSED")

      // Check for fatal errors
      val fatalLines = logcat.linesIterator
        .filter(line =>
          line.contains("FATAL") || line.contains("SMOKE_TEST_FAILED") ||
            line.contains("AndroidRuntime") && line.contains("E/")
        )
        .toSeq

      // Also check full logcat for crashes
      val (_, fullLogcat, _) = adb(adbPath, "logcat", "-d", "-s", "AndroidRuntime:E")
      val crashes            = fullLogcat.linesIterator.filter(_.contains("FATAL EXCEPTION")).toSeq

      if (crashes.nonEmpty) {
        // Get the full crash stack trace
        val (_, crashLog, _) = adb(adbPath, "logcat", "-d", "-s", "AndroidRuntime:*")
        fail(
          s"App crashed with ${crashes.size} FATAL exception(s):\n$crashLog"
        )
      }

      if (fatalLines.nonEmpty) {
        fail(
          s"Smoke test encountered ${fatalLines.size} error(s):\n${fatalLines.mkString("\n")}"
        )
      }

      assert(passed,
             s"SMOKE_TEST_PASSED marker not found in logcat. App may have crashed silently.\n" +
               s"Logcat:\n$logcat"
      )

      // Parse structured subsystem check results
      val checkPattern = """SGE-IT:(\w+):(PASS|FAIL):(.*)""".r
      val checkResults = logcat.linesIterator.flatMap { line =>
        checkPattern.findFirstMatchIn(line).map { m =>
          (m.group(1), m.group(2), m.group(3))
        }
      }.toSeq

      if (checkResults.nonEmpty) {
        System.err.println(s"=== Subsystem check results (${checkResults.size}) ===")
        checkResults.foreach { case (name, status, msg) =>
          System.err.println(s"  $name: $status — $msg")
        }

        val failedChecks = checkResults.filter(_._2 == "FAIL")
        if (failedChecks.nonEmpty) {
          val details = failedChecks.map { case (name, _, msg) => s"  $name: $msg" }.mkString("\n")
          fail(s"${failedChecks.size} subsystem check(s) failed:\n$details")
        }
      }

    } finally {
      // Clean up: uninstall APK
      adb(adbPath, "uninstall", PACKAGE)

      // If we started the emulator, kill it
      if (weStartedEmulator) {
        System.err.println("Shutting down emulator ...")
        adb(adbPath, "emu", "kill")
      }
    }
  }
}
