/*
 * SGE Demos — Android launcher Activity base class.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package shared

import _root_.android.app.Activity
import _root_.android.opengl.GLSurfaceView
import _root_.android.os.{Bundle, Handler, Looper}
import _root_.android.util.Log
import javax.microedition.khronos.egl.{EGLConfig => AndroidEGLConfig}
import javax.microedition.khronos.opengles.GL10

import _root_.sge.{AndroidApplication, Sge}
import _root_.sge.platform.android._

/** Abstract Activity that bootstraps an [[_root_.sge.AndroidApplication]] for a [[DemoScene]].
  *
  * Subclasses override [[scene]] to provide the demo to run. The Activity handles all
  * Android lifecycle wiring: creates the ops provider, AndroidApplication, GL surface,
  * and renderer bridge.
  *
  * Compiled only when android.jar is on the classpath (`scala-android/` source root).
  */
abstract class AndroidLauncherActivity extends Activity {

  private val TAG = "SGE-DEMO"

  /** The demo scene to run. Override in each demo's Activity. */
  def scene: DemoScene

  private var app: AndroidApplication = scala.compiletime.uninitialized

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    try {
      val listener = new SingleSceneApp(scene)
      val provider = AndroidPlatformProviderImpl
      val config   = provider.defaultConfig()
      config.useAccelerometer = false
      config.useCompass       = false

      val lifecycle = provider.createLifecycle(this).asInstanceOf[AndroidLifecycleImpl]

      app = new AndroidApplication(
        listener,
        config,
        provider,
        lifecycle,
        this
      )

      // initializeGraphicsAndInput creates the GL surface, wires lifecycle, and returns the view
      val glView = app.initializeGraphicsAndInput(
        getWindowManager(),
        new Handler(Looper.getMainLooper())
      ).asInstanceOf[GLSurfaceView]

      // Wire the GL renderer bridge
      glView.setRenderer(new GLSurfaceView.Renderer {

        override def onSurfaceCreated(gl: GL10, eglConfig: AndroidEGLConfig): Unit = {
          val versionStr  = gl.glGetString(GL10.GL_VERSION)
          val vendorStr   = gl.glGetString(GL10.GL_VENDOR)
          val rendererStr = gl.glGetString(GL10.GL_RENDERER)
          Log.i(TAG, s"GL surface created: $versionStr / $vendorStr / $rendererStr")

          // Initialize SGE graphics GL state
          val graphics = app.getGraphics().asInstanceOf[AndroidGraphics]
          graphics.setupGL(versionStr, vendorStr, rendererStr)

          // Build Sge context and notify listener
          app.initializeSge()
          listener.create()
          Log.i(TAG, "SGE initialized")
        }

        override def onSurfaceChanged(gl: GL10, w: Int, h: Int): Unit = {
          val graphics = app.getGraphics().asInstanceOf[AndroidGraphics]
          graphics.width = w
          graphics.height = h
          listener.resize(_root_.sge.Pixels(w), _root_.sge.Pixels(h))
        }

        override def onDrawFrame(gl: GL10): Unit = {
          val graphics = app.getGraphics().asInstanceOf[AndroidGraphics]
          graphics.updateFrameTiming(false)
          app.processInputEvents()
          app.executeRunnables()
          listener.render()
        }
      })

      setContentView(glView)
      Log.i(TAG, "Content view set, rendering started")

    } catch {
      case e: Throwable =>
        Log.e(TAG, "Failed to initialize SGE: " + e.getMessage, e)
        finish()
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    if (app != null) app.onResume() // scalafix:ok
  }

  override def onPause(): Unit = {
    if (app != null) app.onPause() // scalafix:ok
    super.onPause()
  }

  override def onDestroy(): Unit = {
    if (app != null) app.onDestroy() // scalafix:ok
    super.onDestroy()
  }
}
