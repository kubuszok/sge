/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapLayer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError.InvalidInput; getName/setName → var name,
 *     isVisible/setVisible → var visible, getObjects → val objects, getProperties → val properties,
 *     getParallaxX/setParallaxX → var parallaxX, getParallaxY/setParallaxY → var parallaxY
 *   Convention: null parent field -> Nullable[MapLayer]; null checks -> .fold/.foreach
 *   Idiom: getOpacity/getCombinedTintColor use Nullable.fold instead of null-check branches
 *   Idiom: setParent validates via Nullable.foreach instead of == null check
 *   Audited: 2026-03-03
 */
package sge
package maps

import sge.graphics.Color
import sge.utils.{ Nullable, SgeError }

/** Map layer containing a set of objects and properties */
class MapLayer {
  var name:                      String             = ""
  private var _opacity:          Float              = 1.0f
  private val tintColor:         Color              = Color(Color.WHITE)
  private val tempColor:         Color              = Color(Color.WHITE)
  var visible:                   Boolean            = true
  private var offsetX:           Float              = 0f
  private var offsetY:           Float              = 0f
  private var renderOffsetX:     Float              = 0f
  private var renderOffsetY:     Float              = 0f
  var parallaxX:                 Float              = 1f
  var parallaxY:                 Float              = 1f
  private var renderOffsetDirty: Boolean            = true
  private var parent:            Nullable[MapLayer] = Nullable.empty
  val objects:                   MapObjects         = MapObjects()
  val properties:                MapProperties      = MapProperties()

  /** @return layer's opacity (combined with parent opacity) */
  def getOpacity: Float =
    parent.fold(_opacity)(p => _opacity * p.getOpacity)

  /** @param opacity new opacity for the layer */
  def setOpacity(opacity: Float): Unit =
    this._opacity = opacity

  /** Returns a temporary color that is the combination of this layer's tint color and its parent's tint color. The returned color is reused internally, so it should not be held onto or modified.
    * @return
    *   layer's tint color combined with the parent's tint color
    */
  def getCombinedTintColor: Color =
    parent.fold(tempColor.set(tintColor))(p => tempColor.set(tintColor).mul(p.getCombinedTintColor))

  /** @return layer's tint color */
  def getTintColor: Color = tintColor

  /** @param tintColor new tint color for the layer */
  def setTintColor(tintColor: Color): Unit =
    this.tintColor.set(tintColor)

  /** @return layer's x offset */
  def getOffsetX: Float = offsetX

  /** @param offsetX new x offset for the layer */
  def setOffsetX(offsetX: Float): Unit = {
    this.offsetX = offsetX
    invalidateRenderOffset()
  }

  /** @return layer's y offset */
  def getOffsetY: Float = offsetY

  /** @param offsetY new y offset for the layer */
  def setOffsetY(offsetY: Float): Unit = {
    this.offsetY = offsetY
    invalidateRenderOffset()
  }

  // parallaxX and parallaxY are public vars

  /** @return the layer's x render offset, this takes into consideration all parent layers' offsets */
  def getRenderOffsetX: Float = {
    if (renderOffsetDirty) calculateRenderOffsets()
    renderOffsetX
  }

  /** @return the layer's y render offset, this takes into consideration all parent layers' offsets */
  def getRenderOffsetY: Float = {
    if (renderOffsetDirty) calculateRenderOffsets()
    renderOffsetY
  }

  /** set the renderOffsetDirty state to true, when this layer or any parents' offset has changed * */
  def invalidateRenderOffset(): Unit =
    renderOffsetDirty = true

  /** @return the layer's parent {@link MapLayer}, or null if the layer does not have a parent * */
  def getParent: Nullable[MapLayer] = parent

  /** @param parent the layer's new parent {@MapLayer}, internal use only * */
  def setParent(parent: Nullable[MapLayer]): Unit = {
    parent.foreach { p =>
      if (p eq this) throw SgeError.InvalidInput("Can't set self as the parent")
    }
    this.parent = parent
  }

  // objects, visible, and properties are public vars/vals

  protected def calculateRenderOffsets(): Unit = {
    parent.fold {
      renderOffsetX = offsetX
      renderOffsetY = offsetY
    } { p =>
      p.calculateRenderOffsets()
      renderOffsetX = p.getRenderOffsetX + offsetX
      renderOffsetY = p.getRenderOffsetY + offsetY
    }
    renderOffsetDirty = false
  }
}
