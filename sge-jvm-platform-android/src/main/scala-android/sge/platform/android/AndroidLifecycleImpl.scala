// SGE — Android lifecycle implementation
//
// Wraps an Android Activity to provide lifecycle operations.
// Compiled against android.jar. Never loaded on Desktop JVM.

package sge
package platform
package android

import _root_.android.app.Activity
import _root_.android.content.res.Configuration
import _root_.android.os.{ Build, Debug, Handler, Looper }
import _root_.android.view.{ View, WindowManager }

/** Concrete AndroidLifecycleOps backed by an Android Activity.
  *
  * @param activity
  *   the host Activity
  * @param glSurfaceViewOps
  *   the GL surface view ops (nullable, set after view creation)
  */
class AndroidLifecycleImpl(
  private val activity: Activity,
  @volatile private var _glSurfaceView: GLSurfaceViewOps | Null = null
) extends AndroidLifecycleOps {

  private val handler = new Handler(Looper.getMainLooper())

  def setGLSurfaceView(view: GLSurfaceViewOps): Unit = {
    _glSurfaceView = view
  }

  override def runOnUiThread(runnable: Runnable): Unit =
    activity.runOnUiThread(runnable)

  override def useImmersiveMode(use: Boolean): Unit = {
    handler.post(() => {
      val window = activity.getWindow()
      if (window != null) {
        val decorView = window.getDecorView()
        if (use) {
          decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          )
        } else {
          decorView.setSystemUiVisibility(0)
        }
      }
    })
  }

  override def getAndroidVersion(): Int = Build.VERSION.SDK_INT

  override def getNativeHeapAllocatedSize(): Long = Debug.getNativeHeapAllocatedSize()

  override def finish(): Unit = activity.finish()

  override def hasHardwareKeyboard(): Boolean = {
    val config = activity.getResources().getConfiguration()
    config.keyboard != Configuration.KEYBOARD_NOKEYS && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
  }

  override def getGLSurfaceView(): AnyRef | Null = {
    val view = _glSurfaceView
    if (view != null) view.getView() else null
  }

  override def resumeGLSurfaceView(): Unit = {
    val view = _glSurfaceView
    if (view != null) view.onResume()
  }

  override def pauseGLSurfaceView(): Unit = {
    val view = _glSurfaceView
    if (view != null) view.onPause()
  }
}
