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

import java.io.{ BufferedReader, File, InputStreamReader, PrintWriter }
import java.net.Socket
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

  /** Finds the adb binary from ANDROID_HOME or local sge-deps/android-sdk/. */
  private def findAdb(): String = {
    val sdkRoot = sys.env.getOrElse(
      "ANDROID_HOME",
      sys.env.getOrElse(
        "ANDROID_SDK_ROOT", {
          val local = Paths.get(System.getProperty("user.dir"), "sge-deps", "android-sdk")
          if (Files.isDirectory(local)) local.toString
          else fail("Android SDK not found. Set ANDROID_HOME or trigger any sbt 'androidXxx' task to auto-download it.")
        }
      )
    )
    val adb = Paths.get(sdkRoot, "platform-tools", "adb")
    if (Files.exists(adb)) adb.toString
    else fail(s"adb not found at $adb. Install platform-tools.")
  }

  /** Finds the emulator binary. */
  private def findEmulator(): String = {
    val sdkRoot = sys.env.getOrElse(
      "ANDROID_HOME",
      sys.env.getOrElse("ANDROID_SDK_ROOT", Paths.get(System.getProperty("user.dir"), "sge-deps", "android-sdk").toString)
    )
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

  // ── Emulator console helpers ───────────────────────────────────────

  /** Sends a command to the emulator console (telnet on localhost:5554). Returns true if the command was sent successfully. The emulator console may require authentication via a token file.
    */
  private def emulatorConsole(command: String): Boolean =
    try {
      val socket = new Socket("localhost", 5554)
      socket.setSoTimeout(5000)
      val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
      val writer = new PrintWriter(socket.getOutputStream, true)

      // Read the greeting (e.g., "Android Console: ...")
      var line = reader.readLine()
      while (line != null && !line.contains("OK"))
        line = reader.readLine()

      // Authenticate if needed — read token from ~/.emulator_console_auth_token
      val tokenFile = Paths.get(System.getProperty("user.home"), ".emulator_console_auth_token")
      if (Files.exists(tokenFile)) {
        val token = new String(Files.readAllBytes(tokenFile)).trim
        writer.println(s"auth $token")
        line = reader.readLine()
        while (line != null && !line.contains("OK"))
          line = reader.readLine()
      }

      // Send the sensor command
      writer.println(command)
      line = reader.readLine()
      val success = line != null && line.contains("OK")

      writer.println("quit")
      socket.close()
      success
    } catch {
      case e: Exception =>
        System.err.println(s"Emulator console error: ${e.getMessage}")
        false
    }

  // ── APK helpers ─────────────────────────────────────────────────────

  /** Finds the signed smoke test APK by walking the android-smoke module's target tree. The cross-version segment of the output path (e.g. `jvm-3` vs `scala-3.x`) varies by sbt/toolchain version, so
    * locate the APK by name rather than hard-coding the full path (which broke on sbt 2.0's `jvm-3` layout vs older layouts). On failure, list any `.apk` present for diagnosis.
    */
  private def findApk(): Path = {
    val cwd = Paths.get(System.getProperty("user.dir"))
    // sbt-2.0 uses an out-of-tree layout: androidSign writes the signed APK to
    // <root>/target/out/jvm/<scala>/sge-test-android-smoke/android/app-debug.apk
    // (NOT sge-test/android-smoke/target/...). Locate it by name under target.
    val targetDir = cwd.resolve("target")
    walkForFile(targetDir, "app-debug.apk").getOrElse {
      val present = listFilesEndingWith(targetDir, ".apk")
      fail(
        "Smoke APK 'app-debug.apk' not found. Run 'sbt sge-android-smoke/androidSign' first.\n" +
          s"Searched under: $targetDir (cwd=$cwd)\n" +
          (if (present.isEmpty) "No .apk files exist under that tree."
           else s".apk files present:\n  ${present.mkString("\n  ")}")
      )
    }
  }

  /** First regular file named `name` anywhere under `root`, or None. */
  private def walkForFile(root: Path, name: String): Option[Path] =
    if (!Files.isDirectory(root)) None
    else {
      val stream = Files.walk(root)
      try {
        val found = stream.filter(p => Files.isRegularFile(p) && p.getFileName.toString == name).findFirst()
        if (found.isPresent) Some(found.get) else None
      } finally stream.close()
    }

  /** All regular files under `root` whose name ends with `suffix` (diagnostic). */
  private def listFilesEndingWith(root: Path, suffix: String): Seq[Path] =
    if (!Files.isDirectory(root)) Seq.empty
    else {
      val stream  = Files.walk(root)
      val builder = Seq.newBuilder[Path]
      try {
        stream.filter(p => Files.isRegularFile(p) && p.getFileName.toString.endsWith(suffix)).forEach(p => builder += p)
        builder.result()
      } finally stream.close()
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

      // Phase 1: Let app start and run initial checks (frame 5)
      Thread.sleep(3000)

      // Phase 2: Inject touch event — adb sends tap to center of screen
      System.err.println("Sending adb input tap 320 240 ...")
      adb(adbPath, "shell", "input", "tap", "320", "240")

      // Phase 2b: Inject sensor values via emulator console
      System.err.println("Injecting accelerometer values via emulator console ...")
      val sensorInjected = emulatorConsole("sensor set acceleration 5:3:8")
      System.err.println(s"Sensor injection ${if (sensorInjected) "succeeded" else "failed (console may not be available)"}")

      Thread.sleep(2000)

      // Phase 3: Test lifecycle — send HOME key to trigger pause
      System.err.println("Sending HOME key to trigger pause ...")
      adb(adbPath, "shell", "input", "keyevent", "KEYCODE_HOME")
      Thread.sleep(2000)

      // Phase 4: Bring app back to trigger resume
      System.err.println("Re-launching activity to trigger resume ...")
      adb(adbPath, "shell", "am", "start", "-n", s"$PACKAGE/$ACTIVITY")
      Thread.sleep(8000)

      // Capture logcat — use broad filter to catch both Log.i (SGE-SMOKE tag)
      // and scribe/System.out output (SGE-IT structured results)
      val (_, logcat, _) = adb(adbPath, "logcat", "-d", "-s", s"$TAG:*", "System.out:*", "AndroidRuntime:*")
      System.err.println("=== Logcat output ===")
      System.err.println(logcat)

      // Parse structured subsystem check results emitted by SmokeListener as
      //   SGE-IT:<SUBSYSTEM>:<PASS|FAIL>:<message>
      // Parsed up-front because the tightened pass condition below depends on
      // the full set of checks having been reported.
      val checkPattern = """SGE-IT:(\w+):(PASS|FAIL):(.*)""".r
      val checkResults = logcat.linesIterator.flatMap { line =>
        checkPattern.findFirstMatchIn(line).map { m =>
          (m.group(1), m.group(2), m.group(3))
        }
      }.toSeq
      val reportedChecks = checkResults.map(_._1).toSet

      // The frame-phase subsystem checks SmokeListener emits on frame 5
      // (runSubsystemChecks + setupTouchTracking). These fire purely from the
      // render loop, so they are RELIABLY reported on the headless CI emulator.
      // Requiring the whole set proves the app reached and finished frame 5
      // without crashing mid-phase: a crash before frame 5 leaves some of these
      // missing (fail), and a crash after one frame likewise never gets here.
      //
      // The 3 POST-ADB checks (TOUCH_DISPATCH, LIFECYCLE, SENSOR_INJECT) emitted
      // by runPostAdbChecks are deliberately NOT required here — CI evidence
      // (run 28519409711) shows they do not emit on the headless `-no-window`
      // emulator: the test's adb interaction phase (input tap / HOME / relaunch /
      // sensor injection) does not reliably drive touch, pause/resume, or sensor
      // events on a windowless emulator, so runPostAdbChecks' preconditions are
      // never met and the checks never log. This is an emulator-interaction
      // limitation, not an app-code regression (ISS-518/519 fixed the app code
      // but cannot make the headless emulator deliver the interactions). Tracked
      // as ISS-694. See the post-adb exclusion in the FAIL-enforcement set below.
      val framePhaseChecks = Set(
        "BOOTSTRAP",
        "GL2D",
        "GL3D",
        "FILEIO",
        "JSON_XML",
        "AUDIO",
        "INPUT",
        "PREFERENCES",
        "CLIPBOARD",
        "DISPLAY",
        "FILEHANDLE_TYPES",
        "SENSORS",
        "TOUCH_SETUP"
      )
      val missingChecks     = framePhaseChecks -- reportedChecks
      val allChecksReported = missingChecks.isEmpty

      // Count sustained-render markers. SmokeActivity logs "SGE-SMOKE: Frame N"
      // every 10th frame, so several distinct markers prove the loop advanced
      // well past a single frame rather than crashing early.
      val frameMarkers = logcat.linesIterator.count(_.contains("SGE-SMOKE: Frame "))

      // Check for fatal errors (but filter known non-fatal AndroidRuntime lines)
      val fatalLines = logcat.linesIterator
        .filter(line =>
          line.contains("SMOKE_TEST_FAILED") ||
            (line.contains("FATAL") && !line.contains("SGE-IT:"))
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

      // Tightened pass condition: the app must have reported every FRAME-PHASE
      // subsystem check (so nothing crashed before or during frame 5) AND
      // rendered enough frames to prove the loop sustained itself. We deliberately
      // do NOT require SMOKE_TEST_PASSED: that marker only fires when ALL checks
      // pass, but JSON_XML / FILEHANDLE_TYPES legitimately fail on the headless CI
      // emulator (see below), so it never fires on CI. The
      // frame-phase check set + frame floor is a stronger, honest signal than the
      // old "SMOKE_TEST_PASSED || any single frame" condition.
      val MinFrameMarkers = 3
      assert(
        allChecksReported,
        s"App did not report the full subsystem-check cycle; missing: ${missingChecks.toSeq.sorted.mkString(", ")}. " +
          s"The app likely crashed or hung mid-run.\nLogcat:\n$logcat"
      )
      assert(
        frameMarkers >= MinFrameMarkers,
        s"Only $frameMarkers 'SGE-SMOKE: Frame ' marker(s) found (need >= $MinFrameMarkers); " +
          s"the render loop did not sustain.\nLogcat:\n$logcat"
      )

      if (checkResults.nonEmpty) {
        System.err.println(s"=== Subsystem check results (${checkResults.size}) ===")
        checkResults.foreach { case (name, status, msg) =>
          System.err.println(s"  $name: $status — $msg")
        }

        // ISS-701 — de-theater the excusals. The prior revision blanket-excused a
        // Set of six check NAMES: ANY FAIL, with ANY message, in those checks was
        // swallowed. A NEW regression inside an excused area (JSON_XML parsing the
        // wrong root, external file IO corrupting data, a sensor-read exception)
        // therefore showed GREEN. (CLIPBOARD was among the six but has since been
        // LIFTED — see below.) Each excused gap is
        // now pinned to the EXACT failure MARKER its SmokeListener check emits for the
        // KNOWN capability-gap reason. A FAIL whose message does NOT contain that
        // marker is a different/new cause and FAILS the test. This is the Android
        // analogue of the browser ISS-726 de-theater (BrowserBootstrapTest) and the
        // ISS-856 native-ops-availability pattern: an excusal pins the exact expected
        // reason, it does not blanket-allow any failure.
        //
        // Expiry/ratchet anchor: every excusal is anchored to the OPEN standing issue
        // ISS-694 (Android headless-emulator capability gaps). The excusals are lifted
        // when ISS-694 is resolved (runtime WRITE_EXTERNAL_STORAGE grant, a
        // windowed/GPU emulator for clipboard, the XML secure-processing SDK gap, and
        // reliable post-adb interaction emission). If an excused check ever starts
        // PASSing, that is surfaced (and, for the reliably-emitting frame-phase gaps,
        // ratcheted: the test fails so the stale excusal must be removed).
        //
        // Two phases with different emission guarantees on the headless `-no-window`
        // emulator:
        //   frame   — runSubsystemChecks emits these on frame 5, so they RELIABLY
        //             report as FAIL. An unexpected PASS ratchets (fails the test so
        //             the stale excusal is removed).
        //   post-adb— runPostAdbChecks' preconditions (adb touch tap / HOME
        //             pause-resume / emulator-console sensor injection) are not met on
        //             a windowless emulator (CI run 28519409711: 680 frames, all 13
        //             frame-phase checks reported, these 3 never emitted), so they
        //             usually do NOT report at all — accepted (they are not in
        //             framePhaseChecks). If one DOES emit a FAIL it must carry the
        //             known-gap marker (a new cause still fails); a PASS is only
        //             surfaced, since emission here is non-deterministic (ISS-694).
        //
        // Marker per gap (drawn from SmokeListener's own FAIL branches):
        //   value = (expected known-gap FAIL-message marker, isFramePhase)
        val excusedGaps: Map[String, (String, Boolean)] = Map(
          // JSON_XML: XML secure-processing feature is unavailable on the emulator SDK
          // image, so XmlReader.parse throws — "Exception: ...". A successful-but-wrong
          // parse would report "XML root name: ..." (a regression) and is NOT excused.
          "JSON_XML" -> ("Exception:", true),
          // FILEHANDLE_TYPES: external-storage write needs a runtime
          // WRITE_EXTERNAL_STORAGE grant the smoke APK does not request, so the write
          // throws — "Exception: ...". A write that succeeds but reads back wrong data
          // reports "External readback mismatch: ..." (a regression) and is NOT excused.
          "FILEHANDLE_TYPES" -> ("Exception:", true),
          // CLIPBOARD was LIFTED (ISS-701 bounce, ISS-694): the ratchet fired on the
          // ubuntu-x86_64 CI emulator (run 29646741414) where CLIPBOARD reported
          // "PASS:Clipboard write/read OK" in the frame phase — clipboard works in the
          // running Activity; the assumed gap was a -no-window artifact. It is now a
          // NORMAL required-pass check (kept in framePhaseChecks, no excusedGaps entry),
          // so a PASS is accepted and any future FAIL fails the test via unexpectedFails.
          // TOUCH_DISPATCH: only-FAIL branch when no adb tap is delivered.
          "TOUCH_DISPATCH" -> ("No touch event received", false),
          // LIFECYCLE: only-FAIL branch when pause/resume did not both fire.
          "LIFECYCLE" -> ("Lifecycle incomplete:", false),
          // SENSOR_INJECT: known FAIL is the unchanged-values branch; an "Exception:"
          // here would be a different cause and is NOT excused.
          "SENSOR_INJECT" -> ("Sensor values unchanged:", false)
        )

        // Regression guard: a FAIL is real (fails the test) unless it is an excused
        // gap AND its message carries the pinned known-gap marker. An excused check
        // failing for a NEW reason (wrong/missing marker) is treated as a real failure.
        val unexpectedFails = checkResults.collect {
          case (name, "FAIL", msg) if !excusedGaps.get(name).exists { case (marker, _) => msg.contains(marker) } =>
            (name, msg)
        }
        if (unexpectedFails.nonEmpty) {
          val details = unexpectedFails.map { case (name, msg) => s"  $name: $msg" }.mkString("\n")
          fail(
            s"${unexpectedFails.size} subsystem check(s) failed for a non-excused reason " +
              "(an excused gap must fail with its known ISS-694 marker; a new/different cause fails here):\n" +
              details
          )
        }

        // Lift signal / ratchet: an excused capability gap that unexpectedly PASSes
        // means the environment now provides the capability, so the excusal is stale.
        // Frame-phase gaps reliably emit, so a PASS ratchets (fails, forcing removal
        // from excusedGaps + an ISS-694 update). Post-adb emission is non-deterministic
        // on `-no-window`, so a PASS there is only surfaced, never ratcheted.
        val excusedPasses = checkResults.collect {
          case (name, "PASS", msg) if excusedGaps.contains(name) => (name, msg)
        }
        excusedPasses.foreach { case (name, msg) =>
          System.err.println(
            s"NOTE: excused capability gap $name now PASSes ($msg) — its ISS-694 excusal may be liftable."
          )
        }
        val ratchetablePasses = excusedPasses.filter { case (name, _) => excusedGaps(name)._2 }
        assert(
          ratchetablePasses.isEmpty,
          "Excused frame-phase capability gap(s) now PASS and must be un-excused " +
            "(remove from excusedGaps, update ISS-694): " + ratchetablePasses.map(_._1).mkString(", ")
        )
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
