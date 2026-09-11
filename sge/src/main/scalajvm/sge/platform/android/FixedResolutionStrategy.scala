// SGE — Fixed resolution strategy
//
// Self-contained (JDK types only). Always returns the specified dimensions
// regardless of available screen space. The surface is centered on screen.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.surfaceview.FixedResolutionStrategy
//   Renames: implements → extends ResolutionStrategyOps
//   Convention: final case class instead of Java class; calcMeasures takes pixel sizes
//   Audited: 2026-03-08

package sge
package platform
package android

/** Always returns the specified fixed dimensions regardless of available screen space.
  *
  * @param width
  *   fixed rendering width in pixels
  * @param height
  *   fixed rendering height in pixels
  */
final case class FixedResolutionStrategy(width: Int, height: Int) extends ResolutionStrategyOps {

  override def calcMeasures(availableWidth: Int, availableHeight: Int): (Int, Int) =
    (width, height)
}
