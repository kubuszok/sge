// SGE — Android display metrics operations interface
//
// Self-contained (JDK types only). Provides display density, PPI, safe area
// insets, and display mode information. Implemented in sge-jvm-platform-android
// using android.util.DisplayMetrics and android.view.DisplayCutout.

package sge
package platform
package android

/** Display metrics operations for Android. Uses only JDK types. */
trait DisplayMetricsOps {

  /** Pixels per inch (horizontal). */
  def ppiX: Float

  /** Pixels per inch (vertical). */
  def ppiY: Float

  /** Pixels per centimeter (horizontal). ppiX / 2.54 */
  def ppcX: Float

  /** Pixels per centimeter (vertical). ppiY / 2.54 */
  def ppcY: Float

  /** Logical density of the display. */
  def density: Float

  /** Safe area insets from display cutouts. */
  def safeInsetLeft:   Int
  def safeInsetTop:    Int
  def safeInsetRight:  Int
  def safeInsetBottom: Int

  /** Refreshes metrics from the current display state.
    * @param windowManager
    *   the Android WindowManager (as AnyRef)
    */
  def updateMetrics(windowManager: AnyRef): Unit

  /** Refreshes safe area insets from the current window state.
    * @param window
    *   the Android Window (as AnyRef), or null if unavailable
    */
  def updateSafeInsets(window: AnyRef): Unit

  /** Returns the current display mode as (width, height, refreshRate, bitsPerPixel).
    * @param context
    *   the Android Context (as AnyRef)
    * @param bitsPerPixel
    *   total bits from config (r+g+b+a)
    */
  def displayMode(context: AnyRef, bitsPerPixel: Int): (Int, Int, Int, Int)
}
