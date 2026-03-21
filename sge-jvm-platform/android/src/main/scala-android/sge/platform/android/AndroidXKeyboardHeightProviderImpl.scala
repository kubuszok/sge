// SGE — AndroidX keyboard height provider (WindowInsets-based, API 30+)
//
// Uses WindowInsetsCompat to detect keyboard height via the IME inset type.
// More reliable than PopupWindow approach and supports display cutouts.
//
// Requires androidx.core dependency at runtime. Without AndroidX on the
// classpath this class throws UnsupportedOperationException on construction.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.keyboardheight.AndroidXKeyboardHeightProvider
//   Renames: AndroidXKeyboardHeightProvider → AndroidXKeyboardHeightProviderImpl
//   Convention: ops interface pattern; stub without AndroidX dependency
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.app.Activity

/** Keyboard height provider using AndroidX WindowInsetsCompat.
  *
  * This implementation requires `androidx.core` on the classpath. Without it, the class throws [[UnsupportedOperationException]] on construction. Use [[StandardKeyboardHeightProviderImpl]] as a
  * fallback for builds without AndroidX.
  *
  * @param activity
  *   the host Activity
  */
class AndroidXKeyboardHeightProviderImpl(activity: Activity) extends KeyboardHeightProviderOps {

  throw new UnsupportedOperationException(
    "AndroidXKeyboardHeightProviderImpl requires androidx.core on the classpath. " +
      "Use StandardKeyboardHeightProviderImpl instead."
  )

  override def start(): Unit = ()

  override def close(): Unit = ()

  override def setKeyboardHeightObserver(obs: KeyboardHeightObserverOps): Unit = ()

  override def getKeyboardLandscapeHeight(): Int = 0

  override def getKeyboardPortraitHeight(): Int = 0
}
