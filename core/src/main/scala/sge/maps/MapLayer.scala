/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapLayer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> SgeError.InvalidInput
 *   Convention: null parent field -> Nullable[MapLayer]; null checks -> .fold/.foreach
 *   Idiom: getOpacity/getCombinedTintColor use Nullable.fold instead of null-check branches
 *   Idiom: setParent validates via Nullable.foreach instead of == null check
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (getName/setName, getOpacity/setOpacity, isVisible/setVisible, etc.)
 *   Audited: 2026-03-03
 */
package sge
package maps

import sge.graphics.Color
import sge.utils.{ Nullable, SgeError }

/** Map layer containing a set of objects and properties */
class MapLayer {
  private var name:              String             = ""
  private var opacity:           Float              = 1.0f
  private val tintColor:         Color              = new Color(Color.WHITE)
  private val tempColor:         Color              = new Color(Color.WHITE)
  private var visible:           Boolean            = true
  private var offsetX:           Float              = 0f
  private var offsetY:           Float              = 0f
  private var renderOffsetX:     Float              = 0f
  private var renderOffsetY:     Float              = 0f
  private var parallaxX:         Float              = 1f
  private var parallaxY:         Float              = 1f
  private var renderOffsetDirty: Boolean            = true
  private var parent:            Nullable[MapLayer] = Nullable.empty
  private val objects:           MapObjects         = new MapObjects()
  private val properties:        MapProperties      = new MapProperties()

  /** @return layer's name */
  def getName: String = name

  /** @param name new name for the layer */
  def setName(name: String): Unit =
    this.name = name

  /** @return layer's opacity */
  def getOpacity: Float =
    parent.fold(opacity)(p => opacity * p.getOpacity)

  /** @param opacity new opacity for the layer */
  def setOpacity(opacity: Float): Unit =
    this.opacity = opacity

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

  /** @return layer's parallax scrolling factor for x-axis */
  def getParallaxX: Float = parallaxX

  def setParallaxX(parallaxX: Float): Unit =
    this.parallaxX = parallaxX

  /** @return layer's parallax scrolling factor for y-axis */
  def getParallaxY: Float = parallaxY

  def setParallaxY(parallaxY: Float): Unit =
    this.parallaxY = parallaxY

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

  /** @return collection of objects contained in the layer */
  def getObjects: MapObjects = objects

  /** @return whether the layer is visible or not */
  def isVisible: Boolean = visible

  /** @param visible toggles layer's visibility */
  def setVisible(visible: Boolean): Unit =
    this.visible = visible

  /** @return layer's set of properties */
  def getProperties: MapProperties = properties

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
