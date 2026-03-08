// SGE — Fill resolution strategy
//
// Self-contained (JDK types only). Stretches the rendering surface to fill
// the entire available screen space. This is the default strategy.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy
//   Renames: implements → extends ResolutionStrategyOps
//   Convention: calcMeasures takes pixel sizes (not Android MeasureSpec)
//   Audited: 2026-03-08

package sge
package platform
package android

/** Stretches the rendering surface to fill the entire available screen space.
  *
  * This is the default resolution strategy if none is specified.
  */
object FillResolutionStrategy extends ResolutionStrategyOps {

  override def calcMeasures(availableWidth: Int, availableHeight: Int): (Int, Int) =
    (availableWidth, availableHeight)
}
