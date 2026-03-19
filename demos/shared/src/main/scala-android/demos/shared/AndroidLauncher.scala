/*
 * SGE Demos — Android launcher Activity base class.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.shared

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.{Bundle, Handler, Looper}
import android.util.Log
import android.view.{MotionEvent, View}
import javax.microedition.khronos.egl.{EGLConfig => AndroidEGLConfig}
import javax.microedition.khronos.opengles.GL10

import sge.{AndroidApplication, AndroidGraphics, AndroidInput, ApplicationListener, Sge}
import sge.platform.android._

/** Abstract Activity that bootstraps an [[sge.AndroidApplication]] for a [[DemoScene]].
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
      val provider = AndroidPlatformProviderImpl
      val config   = provider.defaultConfig()
      config.useAccelerometer = false
      config.useCompass       = false

      val lifecycle = provider.createLifecycle(this).asInstanceOf[AndroidLifecycleImpl]

      val listener: Sge ?=> ApplicationListener = new SingleSceneApp(scene)
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

      // Wire touch events → AndroidInput
      glView.setOnTouchListener(new View.OnTouchListener {
        override def onTouch(v: View, event: MotionEvent): Boolean = {
          val input = app.input.asInstanceOf[AndroidInput]
          input.onTouchEvent(event)
          true
        }
      })

      // Wire the GL renderer bridge
      glView.setRenderer(new GLSurfaceView.Renderer {

        private var created = false

        override def onSurfaceCreated(gl: GL10, eglConfig: AndroidEGLConfig): Unit = {
          try {
            val versionStr  = gl.glGetString(GL10.GL_VERSION)
            val vendorStr   = gl.glGetString(GL10.GL_VENDOR)
            val rendererStr = gl.glGetString(GL10.GL_RENDERER)
            Log.i(TAG, s"GL surface created: $versionStr / $vendorStr / $rendererStr")

            // Initialize SGE graphics GL state and Sge context
            val graphics = app.graphics.asInstanceOf[AndroidGraphics]
            graphics.setupGL(versionStr, vendorStr, rendererStr)
            app.initializeSge()
            // Defer listener.create() to onSurfaceChanged so dimensions are available
            created = false
            Log.i(TAG, "SGE initialized")
          } catch {
            case e: Throwable =>
              Log.e(TAG, "onSurfaceCreated exception", e)
          }
        }

        override def onSurfaceChanged(gl: GL10, w: Int, h: Int): Unit = {
          try {
            Log.i(TAG, s"onSurfaceChanged ${w}x${h}")
            val graphics = app.graphics.asInstanceOf[AndroidGraphics]
            graphics._width = w
            graphics._height = h
            gl.glViewport(0, 0, w, h)
            if (!created) {
              app.listener.create()
              created = true
            }
            app.listener.resize(sge.Pixels(w), sge.Pixels(h))
          } catch {
            case e: Throwable =>
              Log.e(TAG, "onSurfaceChanged exception", e)
          }
        }

        private var frameCount = 0L

        override def onDrawFrame(gl: GL10): Unit = {
          frameCount += 1
          if (frameCount <= 3 || frameCount % 60 == 0) {
            Log.i(TAG, s"onDrawFrame #$frameCount")
          }
          try {
            val graphics = app.graphics.asInstanceOf[AndroidGraphics]
            graphics.updateFrameTiming(false)
            app.processInputEvents()
            app.executeRunnables()
            app.listener.render()
          } catch {
            case e: Throwable =>
              Log.e(TAG, "onDrawFrame exception", e)
          }
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
