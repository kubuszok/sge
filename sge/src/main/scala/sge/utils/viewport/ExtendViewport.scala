/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/ExtendViewport.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * --- AUDIT (2026-03-03) ---
 * API-complete: YES — all 4 constructors and all getters/setters ported
 * Behavioural parity: YES — update logic (fit + extend) is identical
 * Conventions: OK — no return, no null, split packages, braces on multiline defs
 * Notes:
 *   - _backing var pattern used for constructor params that need mutation
 *   - Missing Javadoc on primary constructor (5-param) matches Java omission
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera
import sge.utils.Scaling

/** A viewport that keeps the world aspect ratio by both scaling and extending the world. By default, the world is first scaled to fit within the viewport using {@link Scaling#fit} , then the shorter
  * dimension is lengthened to fill the viewport. Other scaling, such as {@link Scaling#contain} , may lengthen the world in both directions. A maximum size can be specified to limit how much the
  * world is extended and black bars (letterboxing) are used for any remaining space.
  * @author
  *   Nathan Sweet
  */
class ExtendViewport(minWorldWidth: Float, minWorldHeight: Float, maxWorldWidth: Float, maxWorldHeight: Float, camera: Camera)(using Sge) extends Viewport {
  var minWidth:  Float   = minWorldWidth
  var minHeight: Float   = minWorldHeight
  var maxWidth:  Float   = maxWorldWidth
  var maxHeight: Float   = maxWorldHeight
  var scaling:   Scaling = Scaling.fit

  this.camera = camera

  /** Creates a new viewport using a new {@link OrthographicCamera} with no maximum world size. */
  def this(minWorldWidth: Float, minWorldHeight: Float)(using Sge) = {
    this(minWorldWidth, minWorldHeight, 0, 0, OrthographicCamera())
  }

  /** Creates a new viewport with no maximum world size. */
  def this(minWorldWidth: Float, minWorldHeight: Float, camera: Camera)(using Sge) = {
    this(minWorldWidth, minWorldHeight, 0, 0, camera)
  }

  /** Creates a new viewport using a new {@link OrthographicCamera} and a maximum world size.
    * @see
    *   ExtendViewport#ExtendViewport(float, float, float, float, Camera)
    */
  def this(minWorldWidth: Float, minWorldHeight: Float, maxWorldWidth: Float, maxWorldHeight: Float)(using Sge) = {
    this(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, OrthographicCamera())
  }

  override def update(screenWidth: Pixels, screenHeight: Pixels, centerCamera: Boolean): Unit = {
    val sw = screenWidth.toInt
    val sh = screenHeight.toInt
    // Fit min size to the screen.
    var worldWidth  = minWidth
    var worldHeight = minHeight
    val scaled      = scaling.apply(worldWidth, worldHeight, sw.toFloat, sh.toFloat)

    // Extend, possibly in both directions depending on the scaling.
    var viewportWidth  = Math.round(scaled.x)
    var viewportHeight = Math.round(scaled.y)
    if (viewportWidth < sw) {
      val toViewportSpace = viewportHeight / worldHeight
      val toWorldSpace    = worldHeight / viewportHeight
      var lengthen        = (sw - viewportWidth) * toWorldSpace
      if (maxWidth > 0) lengthen = Math.min(lengthen, maxWidth - minWidth)
      worldWidth += lengthen
      viewportWidth += Math.round(lengthen * toViewportSpace)
    }
    if (viewportHeight < sh) {
      val toViewportSpace = viewportWidth / worldWidth
      val toWorldSpace    = worldWidth / viewportWidth
      var lengthen        = (sh - viewportHeight) * toWorldSpace
      if (maxHeight > 0) lengthen = Math.min(lengthen, maxHeight - minHeight)
      worldHeight += lengthen
      viewportHeight += Math.round(lengthen * toViewportSpace)
    }

    setWorldSize(worldWidth, worldHeight)

    // Center.
    setScreenBounds(Pixels((sw - viewportWidth) / 2), Pixels((sh - viewportHeight) / 2), Pixels(viewportWidth), Pixels(viewportHeight))

    apply(centerCamera)
  }

}
