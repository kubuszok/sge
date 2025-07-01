package sge
package utils

import sge.math.Vector2

abstract class Scaling {

  /** Returns the size of the source scaled to the target. Note the same Vector2 instance is always returned and should never be cached.
    */
  def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2
}

object Scaling {
  protected val temp = new Vector2()

  /** Scales the source to fit the target while keeping the same aspect ratio. This may cause the source to be smaller than the target in one direction.
    */
  val fit = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      val targetRatio = targetHeight / targetWidth
      val sourceRatio = sourceHeight / sourceWidth
      val scale       = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
      temp.x = sourceWidth * scale
      temp.y = sourceHeight * scale
      temp
    }
  }

  /** Scales the source to fit the target while keeping the same aspect ratio, but the source is not scaled at all if smaller in both directions. This may cause the source to be smaller than the
    * target in one or both directions.
    */
  val contain = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      val targetRatio = targetHeight / targetWidth
      val sourceRatio = sourceHeight / sourceWidth
      var scale       = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
      if (scale > 1) scale = 1
      temp.x = sourceWidth * scale
      temp.y = sourceHeight * scale
      temp
    }
  }

  /** Scales the source to fill the target while keeping the same aspect ratio. This may cause the source to be larger than the target in one direction.
    */
  val fill = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      val targetRatio = targetHeight / targetWidth
      val sourceRatio = sourceHeight / sourceWidth
      val scale       = if (targetRatio < sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
      temp.x = sourceWidth * scale
      temp.y = sourceHeight * scale
      temp
    }
  }

  /** Scales the source to fill the target in the x direction while keeping the same aspect ratio. This may cause the source to be smaller or larger than the target in the y direction.
    */
  val fillX = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      val scale = targetWidth / sourceWidth
      temp.x = sourceWidth * scale
      temp.y = sourceHeight * scale
      temp
    }
  }

  /** Scales the source to fill the target in the y direction while keeping the same aspect ratio. This may cause the source to be smaller or larger than the target in the x direction.
    */
  val fillY = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      val scale = targetHeight / sourceHeight
      temp.x = sourceWidth * scale
      temp.y = sourceHeight * scale
      temp
    }
  }

  /** Scales the source to fill the target. This may cause the source to not keep the same aspect ratio. */
  val stretch = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      temp.x = targetWidth
      temp.y = targetHeight
      temp
    }
  }

  /** Scales the source to fill the target in the x direction, without changing the y direction. This may cause the source to not keep the same aspect ratio.
    */
  val stretchX = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      temp.x = targetWidth
      temp.y = sourceHeight
      temp
    }
  }

  /** Scales the source to fill the target in the y direction, without changing the x direction. This may cause the source to not keep the same aspect ratio.
    */
  val stretchY = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      temp.x = sourceWidth
      temp.y = targetHeight
      temp
    }
  }

  /** The source is not scaled. */
  val none = new Scaling() {
    def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2 = {
      temp.x = sourceWidth
      temp.y = sourceHeight
      temp
    }
  }
}
