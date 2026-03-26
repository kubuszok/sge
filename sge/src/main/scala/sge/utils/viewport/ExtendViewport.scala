/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/ExtendViewport.java
 * Original author: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: getX/setX → public var; Scaling is a SAM trait
 *   Idiom: split packages, Pixels opaque type
 *   Audited: 2026-03-10
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera

/** A viewport that keeps the world aspect ratio by both scaling and extending the world. By default, the world is first scaled to fit within the viewport using {@link Scaling#fit}, then the shorter
  * dimension is lengthened to fill the viewport. Other scaling, such as {@link Scaling#contain}, may lengthen the world in both directions. A maximum size can be specified to limit how much the world
  * is extended and black bars (letterboxing) are used for any remaining space.
  * @author
  *   Nathan Sweet
  */
class ExtendViewport(
  var minWorldWidth:  WorldUnits,
  var minWorldHeight: WorldUnits,
  var maxWorldWidth:  WorldUnits,
  var maxWorldHeight: WorldUnits,
  camera:             Camera
)(using Sge)
    extends Viewport {

  var currentScaling: Scaling = Scaling.fit

  this.camera = camera

  /** Creates a new viewport using a new {@link OrthographicCamera} with no maximum world size. */
  def this(minWorldWidth: WorldUnits, minWorldHeight: WorldUnits)(using Sge) =
    this(minWorldWidth, minWorldHeight, WorldUnits.zero, WorldUnits.zero, OrthographicCamera())

  /** Creates a new viewport with no maximum world size. */
  def this(minWorldWidth: WorldUnits, minWorldHeight: WorldUnits, camera: Camera)(using Sge) =
    this(minWorldWidth, minWorldHeight, WorldUnits.zero, WorldUnits.zero, camera)

  /** Creates a new viewport using a new {@link OrthographicCamera} and a maximum world size. */
  def this(minWorldWidth: WorldUnits, minWorldHeight: WorldUnits, maxWorldWidth: WorldUnits, maxWorldHeight: WorldUnits)(using Sge) =
    this(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, OrthographicCamera())

  override def update(screenWidth: Pixels, screenHeight: Pixels, centerCamera: Boolean): Unit = {
    val sw = screenWidth.toInt
    val sh = screenHeight.toInt

    // Fit min size to the screen.
    var ww     = minWorldWidth.toFloat
    var wh     = minWorldHeight.toFloat
    val scaled = currentScaling.apply(ww, wh, sw.toFloat, sh.toFloat)

    // Extend, possibly in both directions depending on the scaling.
    var viewportWidth  = Math.round(scaled.x)
    var viewportHeight = Math.round(scaled.y)
    if (viewportWidth < sw) {
      val toViewportSpace = viewportHeight.toFloat / wh
      val toWorldSpace    = wh / viewportHeight.toFloat
      var lengthen        = (sw - viewportWidth) * toWorldSpace
      if (maxWorldWidth > WorldUnits.zero) lengthen = Math.min(lengthen, maxWorldWidth.toFloat - minWorldWidth.toFloat)
      ww += lengthen
      viewportWidth += Math.round(lengthen * toViewportSpace)
    }
    if (viewportHeight < sh) {
      val toViewportSpace = viewportWidth.toFloat / ww
      val toWorldSpace    = ww / viewportWidth.toFloat
      var lengthen        = (sh - viewportHeight) * toWorldSpace
      if (maxWorldHeight > WorldUnits.zero) lengthen = Math.min(lengthen, maxWorldHeight.toFloat - minWorldHeight.toFloat)
      wh += lengthen
      viewportHeight += Math.round(lengthen * toViewportSpace)
    }

    setWorldSize(WorldUnits(ww), WorldUnits(wh))

    // Center.
    setScreenBounds(Pixels((sw - viewportWidth) / 2), Pixels((sh - viewportHeight) / 2), Pixels(viewportWidth), Pixels(viewportHeight))

    apply(centerCamera)
  }
}
