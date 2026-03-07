/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Scaling.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `abstract class` with anonymous inner classes -> Scala `trait` with SAM lambda implementations
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import sge.math.Vector2

trait Scaling {

  /** Returns the size of the source scaled to the target. Note the same Vector2 instance is always returned and should never be cached.
    */
  def apply(sourceWidth: Float, sourceHeight: Float, targetWidth: Float, targetHeight: Float): Vector2
}

object Scaling {
  private val temp = Vector2()

  /** Scales the source to fit the target while keeping the same aspect ratio. This may cause the source to be smaller than the target in one direction.
    */
  val fit: Scaling = (sourceWidth, sourceHeight, targetWidth, targetHeight) => {
    val targetRatio = targetHeight / targetWidth
    val sourceRatio = sourceHeight / sourceWidth
    val scale       = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
    temp.x = sourceWidth * scale
    temp.y = sourceHeight * scale
    temp
  }

  /** Scales the source to fit the target while keeping the same aspect ratio, but the source is not scaled at all if smaller in both directions. This may cause the source to be smaller than the
    * target in one or both directions.
    */
  val contain: Scaling = (sourceWidth, sourceHeight, targetWidth, targetHeight) => {
    val targetRatio = targetHeight / targetWidth
    val sourceRatio = sourceHeight / sourceWidth
    var scale       = if (targetRatio > sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
    if (scale > 1) scale = 1
    temp.x = sourceWidth * scale
    temp.y = sourceHeight * scale
    temp
  }

  /** Scales the source to fill the target while keeping the same aspect ratio. This may cause the source to be larger than the target in one direction.
    */
  val fill: Scaling = (sourceWidth, sourceHeight, targetWidth, targetHeight) => {
    val targetRatio = targetHeight / targetWidth
    val sourceRatio = sourceHeight / sourceWidth
    val scale       = if (targetRatio < sourceRatio) targetWidth / sourceWidth else targetHeight / sourceHeight
    temp.x = sourceWidth * scale
    temp.y = sourceHeight * scale
    temp
  }

  /** Scales the source to fill the target in the x direction while keeping the same aspect ratio. This may cause the source to be smaller or larger than the target in the y direction.
    */
  val fillX: Scaling = (sourceWidth, sourceHeight, targetWidth, _) => {
    val scale = targetWidth / sourceWidth
    temp.x = sourceWidth * scale
    temp.y = sourceHeight * scale
    temp
  }

  /** Scales the source to fill the target in the y direction while keeping the same aspect ratio. This may cause the source to be smaller or larger than the target in the x direction.
    */
  val fillY: Scaling = (sourceWidth, sourceHeight, _, targetHeight) => {
    val scale = targetHeight / sourceHeight
    temp.x = sourceWidth * scale
    temp.y = sourceHeight * scale
    temp
  }

  /** Scales the source to fill the target. This may cause the source to not keep the same aspect ratio. */
  val stretch: Scaling = (_, _, targetWidth, targetHeight) => {
    temp.x = targetWidth
    temp.y = targetHeight
    temp
  }

  /** Scales the source to fill the target in the x direction, without changing the y direction. This may cause the source to not keep the same aspect ratio.
    */
  val stretchX: Scaling = (_, sourceHeight, targetWidth, _) => {
    temp.x = targetWidth
    temp.y = sourceHeight
    temp
  }

  /** Scales the source to fill the target in the y direction, without changing the x direction. This may cause the source to not keep the same aspect ratio.
    */
  val stretchY: Scaling = (sourceWidth, _, _, targetHeight) => {
    temp.x = sourceWidth
    temp.y = targetHeight
    temp
  }

  /** The source is not scaled. */
  val none: Scaling = (sourceWidth, sourceHeight, _, _) => {
    temp.x = sourceWidth
    temp.y = sourceHeight
    temp
  }
}
