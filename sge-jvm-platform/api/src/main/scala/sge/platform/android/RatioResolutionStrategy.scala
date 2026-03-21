// SGE — Ratio resolution strategy
//
// Self-contained (JDK types only). Maintains a given aspect ratio and
// stretches the rendering surface to the maximum available screen size.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.surfaceview.RatioResolutionStrategy
//   Renames: implements → extends ResolutionStrategyOps
//   Convention: final case class instead of Java class; calcMeasures takes pixel sizes
//   Audited: 2026-03-08

package sge
package platform
package android

/** Maintains a given aspect ratio and stretches to fill as much of the available space as possible.
  *
  * @param ratio
  *   desired width-to-height aspect ratio
  */
final case class RatioResolutionStrategy(ratio: Float) extends ResolutionStrategyOps {

  override def calcMeasures(availableWidth: Int, availableHeight: Int): (Int, Int) = {
    val realRatio = availableWidth.toFloat / availableHeight
    if (realRatio < ratio) {
      val width  = availableWidth
      val height = Math.round(width / ratio)
      (width, height)
    } else {
      val height = availableHeight
      val width  = Math.round(height * ratio)
      (width, height)
    }
  }
}

object RatioResolutionStrategy {

  /** Creates a ratio strategy from explicit width and height values. */
  def apply(width: Float, height: Float): RatioResolutionStrategy =
    new RatioResolutionStrategy(width / height)
}
