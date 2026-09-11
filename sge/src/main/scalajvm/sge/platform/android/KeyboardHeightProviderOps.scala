// SGE — Keyboard height provider interface
//
// Lifecycle interface for keyboard height detection. Implementations use
// either PopupWindow (pre-API 30) or AndroidX WindowInsets (API 30+).
// Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.keyboardheight.KeyboardHeightProvider
//   Convention: ops interface pattern
//   Audited: 2026-03-08

package sge
package platform
package android

/** Provider for detecting soft keyboard height on Android.
  *
  * Implementations: `StandardKeyboardHeightProviderImpl` (PopupWindow-based, API 21-29) and `AndroidXKeyboardHeightProviderImpl` (WindowInsets-based, API 30+).
  */
trait KeyboardHeightProviderOps {

  /** Start listening for keyboard height changes. */
  def start(): Unit

  /** Stop listening and release resources. */
  def close(): Unit

  /** Register the observer to be notified of keyboard height changes. */
  def setKeyboardHeightObserver(observer: KeyboardHeightObserverOps): Unit

  /** Returns the cached keyboard height in landscape orientation, or 0 if unknown. */
  def getKeyboardLandscapeHeight(): Int

  /** Returns the cached keyboard height in portrait orientation, or 0 if unknown. */
  def getKeyboardPortraitHeight(): Int
}
