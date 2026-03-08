// SGE — Standard keyboard height provider (PopupWindow-based, API 21-29)
//
// Uses a zero-width PopupWindow anchored to the Activity's content view.
// A GlobalLayoutListener detects when the keyboard pushes the popup's
// visible frame upward, allowing the keyboard height to be calculated.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.keyboardheight.StandardKeyboardHeightProvider
//   Renames: StandardKeyboardHeightProvider → StandardKeyboardHeightProviderImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.app.Activity
import _root_.android.content.Context
import _root_.android.content.res.Configuration
import _root_.android.graphics.{Point, Rect}
import _root_.android.graphics.drawable.ColorDrawable
import _root_.android.view.{Gravity, View, ViewGroup, ViewTreeObserver}
import _root_.android.view.WindowManager.LayoutParams
import _root_.android.widget.{LinearLayout, PopupWindow}

/** Keyboard height provider using a [[PopupWindow]] trick.
  *
  * A zero-width popup is shown at the bottom of the screen. When the soft keyboard opens, the popup's visible display frame
  * shrinks, and the difference gives the keyboard height. Works on API 21-29 (pre-WindowInsets).
  *
  * Note: Floating keyboards will always report as "opened" because they reduce the visible frame.
  *
  * @param activity
  *   the host Activity
  */
@SuppressWarnings(Array("deprecation"))
class StandardKeyboardHeightProviderImpl(activity: Activity) extends PopupWindow(activity) with KeyboardHeightProviderOps {

  private var observer: KeyboardHeightObserverOps = scala.compiletime.uninitialized

  private val popupView: View = {
    val inflater     = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[_root_.android.view.LayoutInflater]
    val linearLayout = new LinearLayout(inflater.getContext())
    val layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    linearLayout.setLayoutParams(layoutParams)
    linearLayout
  }

  // Configure the popup
  setContentView(popupView)
  setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE | LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
  setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED)
  setWidth(0)
  setHeight(ViewGroup.LayoutParams.MATCH_PARENT)

  popupView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener {
    override def onGlobalLayout(): Unit = {
      handleOnGlobalLayout()
    }
  })

  override def start(): Unit = {
    val parentView = activity.findViewById[View](_root_.android.R.id.content)
    if (!isShowing() && parentView.getWindowToken() != null) {
      setBackgroundDrawable(new ColorDrawable(0))
      showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
    }
  }

  override def close(): Unit = {
    observer = scala.compiletime.uninitialized
    dismiss()
  }

  override def setKeyboardHeightObserver(obs: KeyboardHeightObserverOps): Unit = {
    observer = obs
  }

  override def getKeyboardLandscapeHeight(): Int =
    StandardKeyboardHeightProviderImpl.keyboardLandscapeHeight

  override def getKeyboardPortraitHeight(): Int =
    StandardKeyboardHeightProviderImpl.keyboardPortraitHeight

  private def getScreenOrientation(): Int =
    activity.getResources().getConfiguration().orientation

  private def handleOnGlobalLayout(): Unit = {
    val screenSize = new Point()
    activity.getWindowManager().getDefaultDisplay().getSize(screenSize)

    val rect = new Rect()
    popupView.getWindowVisibleDisplayFrame(rect)

    val orientation    = getScreenOrientation()
    val keyboardHeight = screenSize.y - rect.bottom
    val leftInset      = rect.left
    val rightInset     = Math.abs(screenSize.x - rect.right)

    if (keyboardHeight > 0) {
      if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        StandardKeyboardHeightProviderImpl.keyboardPortraitHeight = keyboardHeight
      } else {
        StandardKeyboardHeightProviderImpl.keyboardLandscapeHeight = keyboardHeight
      }
    }

    val isVisible = keyboardHeight > 0 ||
      (StandardKeyboardHeightProviderImpl.keyboardLandscapeHeight == 0 &&
        StandardKeyboardHeightProviderImpl.keyboardPortraitHeight == 0)

    // Skip duplicate notifications
    if (isVisible == StandardKeyboardHeightProviderImpl.cachedVisible &&
      keyboardHeight == StandardKeyboardHeightProviderImpl.cachedBottomInset &&
      leftInset == StandardKeyboardHeightProviderImpl.cachedInsetLeft &&
      rightInset == StandardKeyboardHeightProviderImpl.cachedInsetRight &&
      orientation == StandardKeyboardHeightProviderImpl.cachedOrientation) return

    StandardKeyboardHeightProviderImpl.cachedVisible = isVisible
    StandardKeyboardHeightProviderImpl.cachedBottomInset = keyboardHeight
    StandardKeyboardHeightProviderImpl.cachedInsetLeft = leftInset
    StandardKeyboardHeightProviderImpl.cachedInsetRight = rightInset
    StandardKeyboardHeightProviderImpl.cachedOrientation = orientation

    if (observer != null) {
      observer.onKeyboardHeightChanged(isVisible, keyboardHeight, leftInset, rightInset, orientation)
    }
  }
}

object StandardKeyboardHeightProviderImpl {
  // Static cache shared across instances (matches LibGDX behavior)
  @volatile private var keyboardLandscapeHeight: Int = 0
  @volatile private var keyboardPortraitHeight:  Int = 0
  @volatile private var cachedVisible:       Boolean = false
  @volatile private var cachedInsetLeft:         Int = 0
  @volatile private var cachedInsetRight:        Int = 0
  @volatile private var cachedBottomInset:       Int = 0
  @volatile private var cachedOrientation:       Int = 0
}
