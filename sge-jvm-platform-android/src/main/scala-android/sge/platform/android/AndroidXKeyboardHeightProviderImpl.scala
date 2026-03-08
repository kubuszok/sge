// SGE — AndroidX keyboard height provider (WindowInsets-based, API 30+)
//
// Uses WindowInsetsCompat to detect keyboard height via the IME inset type.
// More reliable than PopupWindow approach and supports display cutouts.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.keyboardheight.AndroidXKeyboardHeightProvider
//   Renames: AndroidXKeyboardHeightProvider → AndroidXKeyboardHeightProviderImpl
//   Convention: ops interface pattern; _root_.android.* imports; _root_.androidx.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.app.Activity
import _root_.android.content.res.Configuration
import _root_.android.view.View
import _root_.androidx.core.graphics.Insets
import _root_.androidx.core.view.{OnApplyWindowInsetsListener, ViewCompat, WindowInsetsCompat}

/** Keyboard height provider using AndroidX [[WindowInsetsCompat]].
  *
  * Registers an `OnApplyWindowInsetsListener` on the Activity's content view. When the IME inset type is visible, the bottom inset
  * gives the keyboard height. Works on API 30+ with full display cutout and gesture inset support.
  *
  * @param activity
  *   the host Activity
  */
class AndroidXKeyboardHeightProviderImpl(activity: Activity) extends KeyboardHeightProviderOps {

  private var view:     View                     = scala.compiletime.uninitialized
  private var observer: KeyboardHeightObserverOps = scala.compiletime.uninitialized

  override def start(): Unit = {
    view = activity.findViewById[View](_root_.android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener {
      override def onApplyWindowInsets(v: View, windowInsets: WindowInsetsCompat): WindowInsetsCompat = {
        if (observer == null) return windowInsets

        var bottomInset = 0
        var leftInset   = 0
        var rightInset  = 0

        val orientation = activity.getResources().getConfiguration().orientation
        val isVisible   = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

        if (isVisible) {
          val insetTypes = WindowInsetsCompat.Type.systemBars() |
            WindowInsetsCompat.Type.ime() |
            WindowInsetsCompat.Type.displayCutout() |
            WindowInsetsCompat.Type.mandatorySystemGestures()

          val insets = windowInsets.getInsets(insetTypes)

          if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            AndroidXKeyboardHeightProviderImpl.keyboardPortraitHeight = insets.bottom
          } else {
            AndroidXKeyboardHeightProviderImpl.keyboardLandscapeHeight = insets.bottom
          }

          bottomInset = insets.bottom
          leftInset = insets.left
          rightInset = insets.right
        }

        // Skip duplicate notifications
        if (isVisible == AndroidXKeyboardHeightProviderImpl.cachedVisible &&
          bottomInset == AndroidXKeyboardHeightProviderImpl.cachedBottomInset &&
          leftInset == AndroidXKeyboardHeightProviderImpl.cachedInsetLeft &&
          rightInset == AndroidXKeyboardHeightProviderImpl.cachedInsetRight &&
          orientation == AndroidXKeyboardHeightProviderImpl.cachedOrientation) return windowInsets

        AndroidXKeyboardHeightProviderImpl.cachedVisible = isVisible
        AndroidXKeyboardHeightProviderImpl.cachedBottomInset = bottomInset
        AndroidXKeyboardHeightProviderImpl.cachedInsetLeft = leftInset
        AndroidXKeyboardHeightProviderImpl.cachedInsetRight = rightInset
        AndroidXKeyboardHeightProviderImpl.cachedOrientation = orientation

        observer.onKeyboardHeightChanged(isVisible, bottomInset, leftInset, rightInset, orientation)

        windowInsets
      }
    })
  }

  override def close(): Unit = {
    if (view != null) ViewCompat.setOnApplyWindowInsetsListener(view, null)
    observer = scala.compiletime.uninitialized
  }

  override def setKeyboardHeightObserver(obs: KeyboardHeightObserverOps): Unit = {
    observer = obs
  }

  override def getKeyboardLandscapeHeight(): Int =
    AndroidXKeyboardHeightProviderImpl.keyboardLandscapeHeight

  override def getKeyboardPortraitHeight(): Int =
    AndroidXKeyboardHeightProviderImpl.keyboardPortraitHeight
}

object AndroidXKeyboardHeightProviderImpl {
  // Static cache shared across instances (matches LibGDX behavior)
  @volatile private var keyboardLandscapeHeight: Int = 0
  @volatile private var keyboardPortraitHeight:  Int = 0
  @volatile private var cachedVisible:       Boolean = false
  @volatile private var cachedInsetLeft:         Int = 0
  @volatile private var cachedInsetRight:        Int = 0
  @volatile private var cachedBottomInset:       Int = 0
  @volatile private var cachedOrientation:       Int = 0
}
