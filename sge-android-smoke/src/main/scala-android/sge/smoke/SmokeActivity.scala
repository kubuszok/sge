// SGE — Android smoke test Activity
//
// Activity that bootstraps a full SGE application using AndroidApplication
// and SmokeListener. Creates all subsystems (graphics, audio, files, input,
// net), runs 6 subsystem checks, and exits after 30 frames.
//
// Results are logged via both scribe (SmokeListener) and android.util.Log
// (this Activity) so they're visible in logcat regardless of the scribe backend.
//
// Compiled only when android.jar is on the classpath.

package sge
package smoke

import _root_.android.app.Activity
import _root_.android.opengl.GLSurfaceView
import _root_.android.os.{ Bundle, Handler, Looper }
import _root_.android.util.Log
import javax.microedition.khronos.egl.{ EGLConfig => AndroidEGLConfig }
import javax.microedition.khronos.opengles.GL10

import sge.platform.android._

/** Smoke test Activity. Creates a full Sge context with all subsystems, runs SmokeListener's 6 subsystem checks, then exits after 30 frames.
  */
class SmokeActivity extends Activity {

  private val TAG = "SGE-SMOKE"

  private var app: AndroidApplication = scala.compiletime.uninitialized

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    try {
      Log.i(TAG, "Creating SGE smoke test...")

      val provider = AndroidPlatformProviderImpl
      val config   = provider.defaultConfig()
      config.useAccelerometer = true
      config.useGyroscope = true
      config.useCompass = false
      // Audio enabled — subsystem checks verify audio accessibility

      val lifecycle = provider.createLifecycle(this).asInstanceOf[AndroidLifecycleImpl]
      app = new AndroidApplication(new SmokeListener(), config, provider, lifecycle, this)

      Log.i(TAG, "AndroidApplication created, initializing graphics...")

      val surfaceView = app
        .initializeGraphicsAndInput(
          getWindowManager(),
          new Handler(Looper.getMainLooper())
        )
        .asInstanceOf[GLSurfaceView]

      surfaceView.setRenderer(
        new GLSurfaceView.Renderer {

          private var created = false

          override def onSurfaceCreated(gl: GL10, eglConfig: AndroidEGLConfig): Unit = {
            val versionStr  = gl.glGetString(GL10.GL_VERSION)
            val vendorStr   = gl.glGetString(GL10.GL_VENDOR)
            val rendererStr = gl.glGetString(GL10.GL_RENDERER)
            Log.i(TAG, s"GL surface created: $versionStr / $vendorStr / $rendererStr")

            val graphics = app.graphics.asInstanceOf[AndroidGraphics]
            graphics.setupGL(versionStr, vendorStr, rendererStr)
            app.initializeSge()
            // Defer listener.create() to onSurfaceChanged so dimensions are available
            created = false
            Log.i(TAG, "SGE fully initialized with all subsystems")
          }

          override def onSurfaceChanged(gl: GL10, w: Int, h: Int): Unit = {
            val graphics = app.graphics.asInstanceOf[AndroidGraphics]
            graphics._width = w
            graphics._height = h
            gl.glViewport(0, 0, w, h)
            Log.i(TAG, s"Surface changed: ${w}x${h}")
            if (!created) {
              app.listener.create()
              created = true
            }
            app.listener.resize(Pixels(w), Pixels(h))
          }

          override def onDrawFrame(gl: GL10): Unit = {
            val graphics = app.graphics.asInstanceOf[AndroidGraphics]
            graphics.updateFrameTiming(false)
            app.processInputEvents()
            app.executeRunnables()

            val frame = graphics.frameId
            if (frame % 10 == 0) Log.i(TAG, s"Frame $frame")

            app.listener.render()

            // Echo the pass/fail marker via Log.i so logcat captures it reliably
            if (!app.running) {
              if (app.listener.asInstanceOf[SmokeListener].allPassed) Log.i(TAG, "SMOKE_TEST_PASSED")
              else Log.i(TAG, "SMOKE_TEST_FAILED")
              app.listener.dispose()
            }
          }
        }
      )

      setContentView(surfaceView)
      Log.i(TAG, "Content view set, rendering started")

    } catch {
      case e: Throwable =>
        Log.e(TAG, "SMOKE_TEST_FAILED: " + e.getMessage, e)
        finish()
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    if (app != null) { // scalafix:ok
      app.onResume()
      app.listener.resume()
    }
  }

  override def onPause(): Unit = {
    if (app != null) { // scalafix:ok
      app.listener.pause()
      app.onPause()
    }
    super.onPause()
  }

  override def onDestroy(): Unit = {
    if (app != null) app.onDestroy() // scalafix:ok
    super.onDestroy()
  }
}
