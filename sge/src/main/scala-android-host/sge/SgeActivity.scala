// SGE — Android Activity shell
//
// The thin scala-android host shell that completes the canonical Android base.
// It builds the provider / config / lifecycle / AndroidApplication +
// GLSurfaceView + Renderer and forwards every Android framework callback
// (Activity lifecycle, GLSurfaceView.Renderer, key + touch + generic-motion
// input) to the android-type-free SgeAndroidDriver, which holds the actual
// orchestration logic (unit-tested on a plain JVM via SgeAndroidDriverRedSuite).
//
// This generalizes the manual wiring that every game previously hand-copied
// (see sge-test/android-smoke/.../SmokeActivity.scala, now refactored to
// extend this shell). Subclasses provide `platformProvider` (the android
// backend, e.g. `sge.platform.android.AndroidPlatformProviderImpl`, which lives
// in sge-jvm-platform-android) and `createListener`; they may override
// `createConfig` / `onFrameRendered`.
//
// LOCATION: this class lives in sge-core (not sge-jvm-platform-android) so the
// android module need NOT depend on sge. SgeActivity references sge-core types
// (Sge, ApplicationListener, AndroidApplication, SgeAndroidDriver,
// AndroidGraphics) — all local to this module — plus the android framework
// (on this module's JVM compile classpath as a Provided dependency, so
// `extends Activity` resolves; not needed at desktop runtime). Its only former
// coupling to sge-jvm-platform-android, the concrete `AndroidPlatformProviderImpl`,
// is now the abstract [[platformProvider]] supplied by the game. This makes the
// build graph one-directional (sge -> sge-jvm-platform-android -> api) instead
// of the former sge<->android mutual reference that sbt 2.0's eager task graph
// deadlocked on.
//
// Compiled only when the android framework jar is on the classpath.

package sge

import _root_.android.app.Activity
import _root_.android.opengl.GLSurfaceView
import _root_.android.os.{ Bundle, Handler, Looper }
import _root_.android.util.Log
import _root_.android.view.{ KeyEvent, MotionEvent }
import javax.microedition.khronos.egl.{ EGLConfig => AndroidEGLConfig }
import javax.microedition.khronos.opengles.GL10

import sge.platform.android._

/** Canonical Android host [[android.app.Activity]] for an SGE application.
  *
  * `SgeActivity` is the thin, android-typed shell over [[SgeAndroidDriver]]. It performs the one-time bootstrap in [[onCreate]] (provider, config, lifecycle, [[AndroidApplication]], GL surface view
  * and [[android.opengl.GLSurfaceView.Renderer]]) and then forwards Android framework callbacks — Activity lifecycle, renderer surface/frame events, and key/touch/generic-motion input — to the
  * driver. All orchestration logic lives in the driver (which is android-type-free and unit-tested on a plain JVM); this class only translates Android framework callbacks into the plain
  * `Int`/`Char`/`AnyRef` arguments the driver accepts.
  *
  * Subclasses must implement [[platformProvider]] (the android backend) and [[createListener]]. They may override [[createConfig]] to customize the [[AndroidConfigOps]] and [[onFrameRendered]] for a
  * post-render hook.
  */
abstract class SgeActivity extends Activity {

  private val TAG = "SGE"

  /** The application instance, available to subclasses after [[onCreate]]. */
  private var _application: AndroidApplication = scala.compiletime.uninitialized

  /** The driver pumping the application's lifecycle, available after [[onCreate]]. */
  private var _driver: SgeAndroidDriver = scala.compiletime.uninitialized

  /** The android backend provider for this application — e.g. `sge.platform.android.AndroidPlatformProviderImpl` from sge-jvm-platform-android. Supplied by the game (which has the android backend on
    * its classpath) so sge-core need not depend on that module.
    */
  protected def platformProvider: AndroidPlatformProvider

  /** The application built by this Activity. Available to subclasses after [[onCreate]] has run. */
  protected def application: AndroidApplication = _application

  /** The [[Sge]] context for this application. Available to subclasses once the GL surface has been created and [[AndroidApplication.initializeSge]] has run; `null` before that. */
  protected def sge: Sge = _application.sgeContext

  /** Create the application listener for this game. Called once, lazily, when the [[Sge]] context is materialized in [[AndroidApplication.initializeSge]]. */
  protected def createListener(using Sge): ApplicationListener

  /** Create the [[AndroidConfigOps]] for this application. Override to customize (sensors, GL attributes, etc.); defaults to the provider's [[AndroidPlatformProvider.defaultConfig]]. */
  protected def createConfig(provider: AndroidPlatformProvider): AndroidConfigOps = provider.defaultConfig()

  /** Post-render hook, invoked at the end of every [[android.opengl.GLSurfaceView.Renderer.onDrawFrame]] after the driver has rendered the frame. Override for per-frame custom logic (e.g. exit
    * conditions). Defaults to a no-op.
    */
  protected def onFrameRendered(): Unit = ()

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    try {
      Log.i(TAG, "Creating SGE application...")

      val provider = platformProvider
      val config   = createConfig(provider)

      val lifecycle = provider.createLifecycle(this)
      // `createListener` is itself a `Sge ?=> ApplicationListener` context function;
      // AndroidApplication invokes it inside initializeSge() with its own given Sge,
      // so the listener must NOT be summoned here (no Sge context exists yet).
      val listener: Sge ?=> ApplicationListener = createListener
      _application = new AndroidApplication(listener, config, provider, lifecycle, this)
      _driver = new SgeAndroidDriver(_application)

      Log.i(TAG, "AndroidApplication created, initializing graphics...")

      val surfaceView = _application
        .initializeGraphicsAndInput(
          getWindowManager(),
          new Handler(Looper.getMainLooper())
        )
        .asInstanceOf[GLSurfaceView]

      surfaceView.setRenderer(
        new GLSurfaceView.Renderer {

          override def onSurfaceCreated(gl: GL10, eglConfig: AndroidEGLConfig): Unit = {
            val versionStr  = gl.glGetString(GL10.GL_VERSION)
            val vendorStr   = gl.glGetString(GL10.GL_VENDOR)
            val rendererStr = gl.glGetString(GL10.GL_RENDERER)
            Log.i(TAG, s"GL surface created: $versionStr / $vendorStr / $rendererStr")
            // Pure forwarder: the driver runs setupGL then initializeSge on the
            // typed _graphics (no asInstanceOf[AndroidGraphics] here).
            _driver.onSurfaceCreated(versionStr, vendorStr, rendererStr)
            Log.i(TAG, "SGE fully initialized with all subsystems")
          }

          override def onSurfaceChanged(gl: GL10, w: Int, h: Int): Unit = {
            gl.glViewport(0, 0, w, h)
            Log.i(TAG, s"Surface changed: ${w}x${h}")
            // The driver resizes the graphics back-buffer (cast-free) before
            // driving create()/resize() on the listener.
            _driver.onSurfaceChanged(w, h)
          }

          override def onDrawFrame(gl: GL10): Unit = {
            // The driver advances graphics frame timing and drives the frame pump
            // (gated so render never fires before create() — ISS-554).
            _driver.onDrawFrame()
            onFrameRendered()
          }
        }
      )

      setContentView(surfaceView)
      Log.i(TAG, "Content view set, rendering started")

    } catch {
      case e: Throwable =>
        Log.e(TAG, "SGE startup failed: " + e.getMessage, e)
        finish()
    }
  }

  // ── Activity lifecycle → driver ───────────────────────────────────────

  override def onResume(): Unit = {
    super.onResume()
    _driver.onResume()
  }

  override def onPause(): Unit = {
    _driver.onPause()
    super.onPause()
  }

  override def onDestroy(): Unit = {
    _driver.onDestroy()
    super.onDestroy()
  }

  // ── Input → driver ────────────────────────────────────────────────────

  override def onKeyDown(keyCode: Int, event: KeyEvent): Boolean = {
    _driver.keyDown(keyCode)
    val uc = event.getUnicodeChar
    if (uc != 0) _driver.keyTyped(uc.toChar)
    true
  }

  override def onKeyUp(keyCode: Int, event: KeyEvent): Boolean = {
    _driver.keyUp(keyCode)
    true
  }

  override def dispatchTouchEvent(event: MotionEvent): Boolean = {
    _driver.touchEvent(event)
    super.dispatchTouchEvent(event)
  }

  override def onGenericMotionEvent(event: MotionEvent): Boolean = {
    _driver.genericMotion(event)
    super.onGenericMotionEvent(event)
  }
}
