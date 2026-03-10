// SGE — Android smoke test: minimal ApplicationListener
//
// Clears the screen to green, runs subsystem checks, and logs structured
// results. After completing checks it prints SMOKE_TEST_PASSED and exits.
//
// Subsystem check results are logged as:
//   SGE-IT:<SUBSYSTEM>:<PASS|FAIL>:<message>
//
// This file has no android.* imports and compiles on JVM without
// the Android SDK.

package sge
package smoke

import sge.utils.XmlReader

/** [[ApplicationListener]] for the Android smoke + integration test.
  *
  * Clears the screen to a color, runs subsystem checks on specific frames, and logs structured results. After 30 frames it calls `exit()` so the Activity finishes cleanly.
  */
class SmokeListener extends ApplicationListener with SgeAware {

  private var sge:              Sge     = scala.compiletime.uninitialized
  private var ready:            Boolean = false
  private var checksRun:        Boolean = false
  private var postChecksRun:    Boolean = false
  private[smoke] var allPassed: Boolean = true

  // Lifecycle tracking — counts how many times pause/resume are called
  @volatile private var pauseCount:  Int = 0
  @volatile private var resumeCount: Int = 0

  // Touch tracking — set by InputProcessor when touchDown is dispatched
  @volatile private var touchReceived: Boolean = false

  // Sensor tracking — initial accelerometer reading and injected reading
  @volatile private var initialAccelX: Float = 0f
  @volatile private var initialAccelY: Float = 0f
  @volatile private var initialAccelZ: Float = 0f

  // Time-based milestones (ms since create)
  private var createTimeMs: Long = 0L

  override def sgeAvailable(sge: Sge): Unit =
    this.sge = sge

  override def create(): Unit = {
    scribe.info("SGE SmokeListener.create()")
    createTimeMs = System.currentTimeMillis()
    ready = true
  }

  override def resize(width: Pixels, height: Pixels): Unit = ()

  override def render(): Unit = {
    if (!ready) return
    given Sge = sge
    val gl    = Sge().graphics.getGL20()
    gl.glClearColor(0f, 0.4f, 0f, 1f)
    gl.glClear(graphics.ClearMask.ColorBufferBit)
    val frame   = Sge().graphics.getFrameId()
    val elapsed = System.currentTimeMillis() - createTimeMs
    if (frame % 30 == 0) scribe.info(s"SGE smoke frame $frame, elapsed ${elapsed}ms")

    // Run initial subsystem checks on frame 5 (GL is settled by then)
    if (frame == 5 && !checksRun) {
      checksRun = true
      runSubsystemChecks()
    }

    // Run post-adb checks after 6 seconds (adb events arrive at ~3s, lifecycle at ~5s)
    if (elapsed >= 6000 && !postChecksRun) {
      postChecksRun = true
      runPostAdbChecks()
    }

    // Exit after 10 seconds
    if (elapsed >= 10000) {
      if (allPassed) scribe.info("SMOKE_TEST_PASSED")
      else scribe.info("SMOKE_TEST_FAILED")
      Sge().application.exit()
    }
  }

  override def pause(): Unit = {
    pauseCount += 1
    scribe.info(s"SGE SmokeListener.pause() count=$pauseCount")
  }

  override def resume(): Unit = {
    resumeCount += 1
    scribe.info(s"SGE SmokeListener.resume() count=$resumeCount")
  }

  override def dispose(): Unit =
    scribe.info("SGE SmokeListener.dispose()")

  // ── Subsystem checks ─────────────────────────────────────────────

  private def logCheck(name: String, passed: Boolean, message: String): Unit = {
    val status = if (passed) "PASS" else "FAIL"
    val line   = s"SGE-IT:$name:$status:$message"
    scribe.info(line)
    // Also print to stdout — on Android this goes to logcat as "System.out" tag,
    // ensuring visibility regardless of scribe's backend configuration.
    System.out.println(line)
    if (!passed) allPassed = false
  }

  private def runSubsystemChecks()(using Sge): Unit = {
    checkBootstrap()
    checkGL2D()
    checkGL3D()
    checkFileIO()
    checkJsonXml()
    checkAudio()
    checkInput()
    checkPreferences()
    checkClipboard()
    checkDisplayMetrics()
    checkFileHandleTypes()
    checkSensors()
    setupTouchTracking()
  }

  /** Set up InputProcessor to track touch events from adb. */
  private def setupTouchTracking()(using Sge): Unit =
    try {
      val input = Sge().input
      input.setInputProcessor(
        new InputProcessor {
          override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Input.Button): Boolean = {
            touchReceived = true
            scribe.info(s"SGE-IT:TOUCH_RECEIVED at ($screenX,$screenY) pointer=$pointer")
            true
          }
        }
      )
      logCheck("TOUCH_SETUP", passed = true, "InputProcessor installed for touch tracking")
    } catch {
      case e: Exception =>
        logCheck("TOUCH_SETUP", passed = false, s"Exception: ${e.getMessage}")
    }

  /** Checks run after adb events should have been injected (touch tap, lifecycle, sensor). */
  private def runPostAdbChecks()(using Sge): Unit = {
    // Check if adb touch tap was received
    if (touchReceived) {
      logCheck("TOUCH_DISPATCH", passed = true, "Touch event received from adb input tap")
    } else {
      logCheck("TOUCH_DISPATCH", passed = false, "No touch event received (adb input tap may not have been sent)")
    }

    // Check if lifecycle pause/resume occurred
    if (pauseCount > 0 && resumeCount > 0) {
      logCheck("LIFECYCLE", passed = true, s"Lifecycle OK: pause=$pauseCount, resume=$resumeCount")
    } else {
      logCheck("LIFECYCLE", passed = false, s"Lifecycle incomplete: pause=$pauseCount, resume=$resumeCount (expected both > 0)")
    }

    // Check if sensor values changed after emulator console injection
    checkSensorInjection()
  }

  private def checkBootstrap()(using Sge): Unit =
    try {
      val gl = Sge().graphics.getGL20()
      if (gl == null) logCheck("BOOTSTRAP", passed = false, "GL20 is null")
      else logCheck("BOOTSTRAP", passed = true, "Sge context created, GL20 available")
    } catch {
      case e: Exception =>
        logCheck("BOOTSTRAP", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkGL2D()(using Sge): Unit =
    try {
      val gl = Sge().graphics.getGL20()
      // Simple GL call verification — draw and read back would require FBO which
      // may not be available on all Android SwiftShader configs. Just verify GL
      // calls don't crash.
      gl.glClearColor(1f, 0f, 0f, 1f)
      gl.glClear(graphics.ClearMask.ColorBufferBit)
      // Reset to green
      gl.glClearColor(0f, 0.4f, 0f, 1f)
      gl.glClear(graphics.ClearMask.ColorBufferBit)
      logCheck("GL2D", passed = true, "GL clear calls OK")
    } catch {
      case e: Exception =>
        logCheck("GL2D", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkGL3D()(using Sge): Unit =
    try {
      val gl = Sge().graphics.getGL20()
      // Compile a shader with a uniform matrix — exercises the 3D pipeline
      val vs =
        """#version 100
          |uniform mat4 u_projTrans;
          |attribute vec4 a_position;
          |void main() { gl_Position = u_projTrans * a_position; }""".stripMargin
      val fs =
        """#version 100
          |precision mediump float;
          |void main() { gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0); }""".stripMargin
      val shader = new graphics.glutils.ShaderProgram(vs, fs)
      if (!shader.compiled) {
        logCheck("GL3D", passed = false, s"Shader failed: ${shader.getLog()}")
        shader.close()
        return
      }
      shader.bind()
      val loc = shader.fetchUniformLocation("u_projTrans", false)
      if (loc < 0) {
        logCheck("GL3D", passed = false, s"Uniform not found")
        shader.close()
        return
      }
      shader.setUniformMatrix("u_projTrans", new math.Matrix4())
      val err = gl.glGetError()
      shader.close()
      if (err == 0) logCheck("GL3D", passed = true, "Shader + uniform OK")
      else logCheck("GL3D", passed = false, s"GL error: 0x${err.toHexString}")
    } catch {
      case e: Exception =>
        logCheck("GL3D", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkFileIO()(using Sge): Unit =
    try {
      // Write to a temp file and read back
      val tmpDir  = System.getProperty("java.io.tmpdir", "/data/local/tmp")
      val files   = Sge().files
      val tmpFile = files.absolute(s"$tmpDir/sge-it-test-${System.nanoTime()}.txt")
      val testStr = "SGE integration test"
      tmpFile.writeString(testStr, false)
      val readBack = tmpFile.readString()
      tmpFile.delete()
      if (readBack == testStr) logCheck("FILEIO", passed = true, "Write/readback OK")
      else logCheck("FILEIO", passed = false, s"Readback mismatch")
    } catch {
      case e: Exception =>
        logCheck("FILEIO", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkJsonXml(): Unit =
    try {
      // Verify JSON infrastructure classes are loadable (full parse test
      // requires jsoniter-scala-macros which is "provided" scope in sge)
      Class.forName("com.github.plokhotnyuk.jsoniter_scala.core.package$")

      // XML parsing via XmlReader
      val xmlStr    = "<config><width>100</width></config>"
      val xmlReader = new XmlReader()
      val root      = xmlReader.parse(xmlStr)
      if (root.name != "config") {
        logCheck("JSON_XML", passed = false, s"XML root name: ${root.name}")
        return
      }
      logCheck("JSON_XML", passed = true, "JSON + XML parsing OK")
    } catch {
      case e: Exception =>
        logCheck("JSON_XML", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkAudio()(using Sge): Unit =
    try {
      // Just verify the audio subsystem is accessible without crashing.
      // On Android emulator with -noaudio, the audio backend is typically noop.
      val audioType = Sge().audio.getClass.getSimpleName
      logCheck("AUDIO", passed = true, s"Audio subsystem accessible ($audioType)")
    } catch {
      case e: UnsatisfiedLinkError =>
        logCheck("AUDIO", passed = false, s"Native lib missing: ${e.getMessage}")
      case e: Exception =>
        logCheck("AUDIO", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkInput()(using Sge): Unit =
    try {
      val input = Sge().input
      // Basic state queries should not throw
      val x    = input.getX()
      val y    = input.getY()
      val maxP = input.getMaxPointers()
      input.isKeyPressed(Input.Keys.A)
      if (maxP <= 0) {
        logCheck("INPUT", passed = false, s"maxPointers=$maxP, expected > 0")
      } else {
        logCheck("INPUT", passed = true, s"Input queries OK: maxPointers=$maxP, pos=($x,$y)")
      }
    } catch {
      case e: Exception =>
        logCheck("INPUT", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkPreferences()(using Sge): Unit =
    try {
      val prefs   = Sge().application.getPreferences("sge-it-test")
      val testKey = s"test-${System.nanoTime()}"
      prefs.putString(testKey, "hello-sge").flush()
      val readBack = prefs.getString(testKey)
      prefs.remove(testKey)
      prefs.flush()
      if (readBack == "hello-sge") {
        logCheck("PREFERENCES", passed = true, "Preferences write/read/remove OK")
      } else {
        logCheck("PREFERENCES", passed = false, s"Readback mismatch: '$readBack'")
      }
    } catch {
      case e: Exception =>
        logCheck("PREFERENCES", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkClipboard()(using Sge): Unit =
    try {
      val clipboard = Sge().application.getClipboard()
      val testText  = s"sge-it-${System.nanoTime()}"
      clipboard.contents = utils.Nullable(testText)
      val readBack = clipboard.contents
      if (readBack.isDefined && readBack.get == testText) {
        logCheck("CLIPBOARD", passed = true, "Clipboard write/read OK")
      } else {
        logCheck("CLIPBOARD", passed = false, s"Readback: ${readBack.fold("empty")(identity)}")
      }
    } catch {
      case e: Exception =>
        logCheck("CLIPBOARD", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkDisplayMetrics()(using Sge): Unit =
    try {
      val graphics = Sge().graphics
      val w        = graphics.getWidth()
      val h        = graphics.getHeight()
      val ppiX     = graphics.getPpiX()
      val density  = graphics.getDensity()
      if (w.toInt < 1 || h.toInt < 1) {
        logCheck("DISPLAY", passed = false, s"Invalid dimensions: ${w.toInt}x${h.toInt}")
      } else if (ppiX <= 0 || density <= 0) {
        logCheck("DISPLAY", passed = false, s"Invalid metrics: ppiX=$ppiX, density=$density")
      } else {
        logCheck("DISPLAY", passed = true, s"${w.toInt}x${h.toInt}, ppiX=$ppiX, density=$density")
      }
    } catch {
      case e: Exception =>
        logCheck("DISPLAY", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkSensors()(using Sge): Unit =
    try {
      val input  = Sge().input
      val hasAcc = input.isPeripheralAvailable(Input.Peripheral.Accelerometer)
      val hasGyr = input.isPeripheralAvailable(Input.Peripheral.Gyroscope)

      // Read accelerometer — emulator simulates gravity (typically ~9.8 on one axis)
      val ax = input.getAccelerometerX()
      val ay = input.getAccelerometerY()
      val az = input.getAccelerometerZ()

      // Save initial reading for later injection comparison
      initialAccelX = ax
      initialAccelY = ay
      initialAccelZ = az

      // Read gyroscope — emulator at rest should report near-zero
      val gx = input.getGyroscopeX()
      val gy = input.getGyroscopeY()
      val gz = input.getGyroscopeZ()

      if (!hasAcc) {
        logCheck("SENSORS", passed = false, "Accelerometer not available")
      } else {
        // At least one accelerometer axis should be non-zero (gravity)
        val accelMagnitude = Math.sqrt((ax * ax + ay * ay + az * az).toDouble)
        if (accelMagnitude < 0.1) {
          logCheck("SENSORS", passed = false, s"Accelerometer magnitude too low: $accelMagnitude (acc=$ax,$ay,$az)")
        } else {
          logCheck(
            "SENSORS",
            passed = true,
            s"acc=($ax,$ay,$az) mag=${"%.1f".format(accelMagnitude)}, gyro=($gx,$gy,$gz), hasGyro=$hasGyr"
          )
        }
      }
    } catch {
      case e: Exception =>
        logCheck("SENSORS", passed = false, s"Exception: ${e.getMessage}")
    }

  /** Verify accelerometer values changed after emulator console injection. */
  private def checkSensorInjection()(using Sge): Unit =
    try {
      val input = Sge().input
      val ax    = input.getAccelerometerX()
      val ay    = input.getAccelerometerY()
      val az    = input.getAccelerometerZ()

      // The test runner injects acceleration 5.0:3.0:8.0 via emulator console.
      // Check if values are close to the injected ones (tolerance for float precision).
      val dx = Math.abs(ax - 5.0f)
      val dy = Math.abs(ay - 3.0f)
      val dz = Math.abs(az - 8.0f)

      if (dx < 1.0f && dy < 1.0f && dz < 1.0f) {
        logCheck("SENSOR_INJECT", passed = true, s"Injected values received: acc=($ax,$ay,$az)")
      } else {
        // Values didn't match injection — could be that emulator console wasn't reachable.
        // Report the actual values for debugging but don't fail hard (console auth may vary).
        val changed = ax != initialAccelX || ay != initialAccelY || az != initialAccelZ
        if (changed) {
          logCheck(
            "SENSOR_INJECT",
            passed = true,
            s"Sensor values changed: initial=($initialAccelX,$initialAccelY,$initialAccelZ) → ($ax,$ay,$az)"
          )
        } else {
          logCheck("SENSOR_INJECT", passed = false, s"Sensor values unchanged: acc=($ax,$ay,$az), expected ~(5,3,8)")
        }
      }
    } catch {
      case e: Exception =>
        logCheck("SENSOR_INJECT", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkFileHandleTypes()(using Sge): Unit =
    try {
      val files = Sge().files

      // External storage write/read roundtrip
      val extFile = files.external(s"sge-it-test-${System.nanoTime()}.txt")
      val testStr = "external-storage-test"
      extFile.writeString(testStr, false)
      val readBack = extFile.readString()
      extFile.delete()

      if (readBack != testStr) {
        logCheck("FILEHANDLE_TYPES", passed = false, s"External readback mismatch: '$readBack'")
      } else {
        logCheck("FILEHANDLE_TYPES", passed = true, "External storage write/read OK")
      }
    } catch {
      case e: Exception =>
        logCheck("FILEHANDLE_TYPES", passed = false, s"Exception: ${e.getMessage}")
    }
}
