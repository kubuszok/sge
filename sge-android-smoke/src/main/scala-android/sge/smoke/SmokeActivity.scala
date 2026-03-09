// SGE — Android smoke test Activity
//
// Minimal Activity that bootstraps an SGE application, renders a few
// frames, and exits. Used by the Android IT test to catch runtime
// crashes (ClassNotFoundException, NPE, GL errors, missing deps).
//
// Compiled only when android.jar is on the classpath.
//
// The Activity bypasses AndroidApplication's full lifecycle to keep
// the bootstrap minimal. It directly creates ops instances, builds
// an AndroidGraphics, and wires a GLSurfaceView.Renderer bridge.

package sge
package smoke

import _root_.android.app.Activity
import _root_.android.opengl.GLSurfaceView
import _root_.android.os.{Bundle, Handler, Looper}
import _root_.android.util.Log
import javax.microedition.khronos.egl.{EGLConfig => AndroidEGLConfig}
import javax.microedition.khronos.opengles.GL10

import sge.platform.android._

/** Smoke test Activity. Renders 30 frames then exits. */
class SmokeActivity extends Activity {

  private val TAG = "SGE-SMOKE"

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    try {
      Log.i(TAG, "Creating SGE smoke test...")

      val provider = AndroidPlatformProviderImpl
      val config   = provider.defaultConfig()
      config.useAccelerometer = false
      config.useCompass       = false
      config.disableAudio     = true

      // Create ops instances (exercises class loading + SDK bindings)
      val lifecycle = provider.createLifecycle(this).asInstanceOf[AndroidLifecycleImpl]
      val rs        = FillResolutionStrategy
      val glSurface = provider.createGLSurfaceView(this, config, rs)
      lifecycle.setGLSurfaceView(glSurface)

      val displayMetrics = provider.createDisplayMetrics(getWindowManager())
      val cursorOps      = provider.createCursor(this)

      // Create SGE Graphics (plain class, no Activity inheritance)
      val graphics = new AndroidGraphics(config, provider, displayMetrics, glSurface, cursorOps)

      Log.i(TAG, "SGE subsystems created, setting up GL surface...")

      // Wire the GL renderer bridge
      val surfaceView = glSurface.view.asInstanceOf[GLSurfaceView]
      surfaceView.setRenderer(new GLSurfaceView.Renderer {

        override def onSurfaceCreated(gl: GL10, eglConfig: AndroidEGLConfig): Unit = {
          val versionStr  = gl.glGetString(GL10.GL_VERSION)
          val vendorStr   = gl.glGetString(GL10.GL_VENDOR)
          val rendererStr = gl.glGetString(GL10.GL_RENDERER)
          Log.i(TAG, s"GL surface created: $versionStr / $vendorStr / $rendererStr")

          graphics.setupGL(versionStr, vendorStr, rendererStr)
          Log.i(TAG, "SGE graphics initialized")
        }

        override def onSurfaceChanged(gl: GL10, w: Int, h: Int): Unit = {
          graphics.width = w
          graphics.height = h
          Log.i(TAG, s"Surface changed: ${w}x${h}")
        }

        override def onDrawFrame(gl: GL10): Unit = {
          graphics.updateFrameTiming(false)
          val gl20  = graphics.getGL20()
          val frame = graphics.getFrameId()

          // Clear to green
          gl20.glClearColor(0f, 0.4f, 0f, 1f)
          gl20.glClear(sge.graphics.ClearMask.ColorBufferBit)

          if (frame % 10 == 0) Log.i(TAG, s"Frame $frame")
          if (frame >= 30) {
            Log.i(TAG, "SMOKE_TEST_PASSED")
            runOnUiThread(() => finish())
          }
        }
      })

      setContentView(surfaceView)
      Log.i(TAG, "Content view set, rendering started")

    } catch {
      case e: Throwable =>
        Log.e(TAG, "SMOKE_TEST_FAILED: " + e.getMessage, e)
        finish()
    }
  }
}
