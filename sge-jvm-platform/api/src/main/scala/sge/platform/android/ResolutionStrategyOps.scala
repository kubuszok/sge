// SGE — Android resolution strategy interface
//
// Self-contained (JDK types only). Determines GLSurfaceView dimensions
// from available screen space. Implementations live in sge-jvm-platform-android.

package sge
package platform
package android

/** Computes the actual rendering surface dimensions from available screen space.
  *
  * Unlike LibGDX, this interface takes already-extracted pixel sizes (not Android MeasureSpec packed ints). The Android surface view is responsible for extracting sizes from MeasureSpec before
  * calling this.
  */
trait ResolutionStrategyOps {

  /** Computes the desired rendering surface dimensions.
    * @param availableWidth
    *   available width in pixels
    * @param availableHeight
    *   available height in pixels
    * @return
    *   (width, height) tuple for the rendering surface
    */
  def calcMeasures(availableWidth: Int, availableHeight: Int): (Int, Int)
}
