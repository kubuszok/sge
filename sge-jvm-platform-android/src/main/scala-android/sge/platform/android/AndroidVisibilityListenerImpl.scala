// SGE — Android visibility listener implementation
//
// Registers OnSystemUiVisibilityChangeListener on DecorView to
// re-apply immersive mode when system UI is shown.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidVisibilityListener
//   Convention: ops implementation; compiled against android.jar
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.os.Handler
import _root_.android.view.{ View, Window }

/** Concrete implementation of [[VisibilityListenerOps]] using Android's OnSystemUiVisibilityChangeListener.
  *
  * Note: OnSystemUiVisibilityChangeListener is deprecated in API 30+, but still functional and widely used.
  */
@SuppressWarnings(Array("deprecation"))
object AndroidVisibilityListenerImpl extends VisibilityListenerOps {

  override def createListener(window: AnyRef, handler: AnyRef, immersiveModeCallback: Runnable): Unit = {
    try {
      val androidWindow  = window.asInstanceOf[Window]
      val androidHandler = handler.asInstanceOf[Handler]
      val rootView       = androidWindow.getDecorView()
      rootView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener {
        override def onSystemUiVisibilityChange(visibility: Int): Unit = {
          androidHandler.post(immersiveModeCallback)
        }
      })
    } catch {
      case _: Throwable => () // Graceful degradation on older APIs or unsupported environments
    }
  }
}
