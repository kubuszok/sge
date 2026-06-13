// SGE — Android smoke test Activity
//
// Activity that bootstraps a full SGE application using SgeActivity (the
// canonical Android host shell) and SmokeListener. SgeActivity creates all
// subsystems (graphics, audio, files, input, net) and drives the lifecycle /
// frame / input wiring via SgeAndroidDriver; this Activity only supplies the
// listener + config and adds the smoke-specific 30-frame exit logging.
//
// Results are logged via both scribe (SmokeListener) and android.util.Log
// (this Activity) so they're visible in logcat regardless of the scribe backend.
//
// Compiled only when android.jar is on the classpath.

package sge
package smoke

import _root_.android.util.Log

import sge.platform.android._

/** Smoke test Activity. Extends [[sge.SgeActivity]], so the full Sge context with all subsystems is created and pumped by the shared host shell + [[sge.SgeAndroidDriver]]. Supplies a
  * [[SmokeListener]] (which runs the 6 subsystem checks) and exits after 30 frames, logging SMOKE_TEST_PASSED / SMOKE_TEST_FAILED for the integration test to detect.
  */
class SmokeActivity extends SgeActivity {

  private val TAG = "SGE-SMOKE"

  override protected def createListener(using Sge): ApplicationListener = new SmokeListener()

  override protected def createConfig(provider: AndroidPlatformProvider): AndroidConfigOps = {
    val config = provider.defaultConfig()
    config.useAccelerometer = true
    config.useGyroscope = true
    config.useCompass = false
    // Audio enabled — subsystem checks verify audio accessibility
    config
  }

  override protected def onFrameRendered(): Unit = {
    // Per-frame liveness marker (every 10 frames). The android-it test treats
    // "SGE-SMOKE: Frame " as a liveness fallback when some emulator subsystem
    // checks fail, so this logging must be preserved.
    val frame = application.graphics.frameId
    if (frame % 10 == 0) Log.i(TAG, s"Frame $frame")

    // Echo the pass/fail marker via Log.i so logcat captures it reliably.
    // SmokeListener.exit() (after 30 frames) sets app.running = false.
    if (!application.running) {
      if (application.listener.asInstanceOf[SmokeListener].allPassed) Log.i(TAG, "SMOKE_TEST_PASSED")
      else Log.i(TAG, "SMOKE_TEST_FAILED")
      application.listener.dispose()
    }
  }
}
