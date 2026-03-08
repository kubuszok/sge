// SGE — Keyboard height observer interface
//
// Callback notified when keyboard visibility, height, or insets change.
// Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.keyboardheight.KeyboardHeightObserver
//   Convention: ops interface pattern
//   Audited: 2026-03-08

package sge
package platform
package android

/** Observer notified when the soft keyboard height changes.
  *
  * Use with [[KeyboardHeightProviderOps]] implementations to detect keyboard visibility.
  */
trait KeyboardHeightObserverOps {

  /** Called when the keyboard height has changed.
    *
    * Note: `opened` is a best-effort measure. On Android SDK 21-30, floating keyboards will always report as "opened".
    *
    * @param opened
    *   true if the keyboard appears to be visible
    * @param height
    *   the height of the keyboard in pixels
    * @param leftInset
    *   the left inset to consider (display cutout / system bars)
    * @param rightInset
    *   the right inset to consider
    * @param orientation
    *   the device orientation (1 = portrait, 2 = landscape, matching Android Configuration constants)
    */
  def onKeyboardHeightChanged(opened: Boolean, height: Int, leftInset: Int, rightInset: Int, orientation: Int): Unit
}

object KeyboardHeightObserverOps {

  /** Orientation constant matching android.content.res.Configuration.ORIENTATION_PORTRAIT. */
  final val ORIENTATION_PORTRAIT = 1

  /** Orientation constant matching android.content.res.Configuration.ORIENTATION_LANDSCAPE. */
  final val ORIENTATION_LANDSCAPE = 2
}
