// SGE — Android visibility listener interface
//
// Allows immersive mode recovery when system UI visibility changes.
// Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidVisibilityListener
//   Convention: ops interface pattern; Activity/View passed as AnyRef
//   Audited: 2026-03-08

package sge
package platform
package android

/** Visibility listener for immersive mode recovery on Android.
  *
  * Registers a system UI visibility change listener on the Activity's DecorView, so that immersive mode is automatically re-applied when the user swipes to show the navigation/status bars.
  */
trait VisibilityListenerOps {

  /** Creates and registers the visibility change listener.
    * @param window
    *   the Android Window (as AnyRef) — used to get the DecorView
    * @param handler
    *   the Android Handler (as AnyRef) — used to post immersive mode calls
    * @param immersiveModeCallback
    *   a Runnable that re-applies immersive mode flags
    */
  def createListener(window: AnyRef, handler: AnyRef, immersiveModeCallback: Runnable): Unit
}
