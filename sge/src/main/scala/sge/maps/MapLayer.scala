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
 *     getParallaxX/setParallaxX → var parallaxX, getParallaxY/setParallaxY → var parallaxY,
 *     getOpacity/setOpacity → def opacity/opacity_=,
 *     getCombinedTintColor → def combinedTintColor,
 *     getTintColor/setTintColor → def tintColor/tintColor_=,
 *     getOffsetX/setOffsetX → def offsetX/offsetX_=,
 *     getOffsetY/setOffsetY → def offsetY/offsetY_=,
 *     getRenderOffsetX → def renderOffsetX, getRenderOffsetY → def renderOffsetY,
 *     getParent/setParent → def parent/parent_=
 *   Convention: null parent field -> Nullable[MapLayer]; null checks -> .fold/.foreach
 *   Idiom: opacity/combinedTintColor use Nullable.fold instead of null-check branches
 *   Idiom: parent_= validates via Nullable.foreach instead of == null check
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 131
 * Covenant-baseline-methods: MapLayer,_offsetX,_offsetY,_opacity,_parent,_renderOffsetX,_renderOffsetY,_tintColor,calculateRenderOffsets,combinedTintColor,invalidateRenderOffset,name,objects,offsetX,offsetX_,offsetY,offsetY_,opacity,opacity_,parallaxX,parallaxY,parent,parent_,properties,renderOffsetDirty,renderOffsetX,renderOffsetY,tempColor,tintColor,tintColor_,visible
 * Covenant-source-reference: com/badlogic/gdx/maps/MapLayer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 0dc27743a43739f14f7220b4ec7dcc3ada3c8b61
 */
package sge
package maps

import sge.graphics.Color
import sge.utils.{ Nullable, SgeError }

/** Map layer containing a set of objects and properties */
class MapLayer {
  var name:                      String             = ""
  private var _opacity:          Float              = 1.0f
  private val _tintColor:        Color              = Color(Color.WHITE)
  private val tempColor:         Color              = Color(Color.WHITE)
  var visible:                   Boolean            = true
  private var _offsetX:          Float              = 0f
  private var _offsetY:          Float              = 0f
  private var _renderOffsetX:    Float              = 0f
  private var _renderOffsetY:    Float              = 0f
  var parallaxX:                 Float              = 1f
  var parallaxY:                 Float              = 1f
  private var renderOffsetDirty: Boolean            = true
  private var _parent:           Nullable[MapLayer] = Nullable.empty
  val objects:                   MapObjects         = MapObjects()
  val properties:                MapProperties      = MapProperties()

  /** @return layer's opacity (combined with parent opacity) */
  def opacity: Float =
    _parent.fold(_opacity)(p => _opacity * p.opacity)

  /** @param opacity new opacity for the layer */
  def opacity_=(opacity: Float): Unit =
    this._opacity = opacity

  /** Returns a temporary color that is the combination of this layer's tint color and its parent's tint color. The returned color is reused internally, so it should not be held onto or modified.
    * @return
    *   layer's tint color combined with the parent's tint color
    */
  def combinedTintColor: Color =
    _parent.fold(tempColor.set(_tintColor))(p => tempColor.set(_tintColor).mul(p.combinedTintColor))

  /** @return layer's tint color */
  def tintColor: Color = _tintColor

  /** @param tintColor new tint color for the layer */
  def tintColor_=(tintColor: Color): Unit =
    this._tintColor.set(tintColor)

  /** @return layer's x offset */
  def offsetX: Float = _offsetX

  /** @param offsetX new x offset for the layer */
  def offsetX_=(offsetX: Float): Unit = {
    this._offsetX = offsetX
    invalidateRenderOffset()
  }

  /** @return layer's y offset */
  def offsetY: Float = _offsetY

  /** @param offsetY new y offset for the layer */
  def offsetY_=(offsetY: Float): Unit = {
    this._offsetY = offsetY
    invalidateRenderOffset()
  }

  // parallaxX and parallaxY are public vars

  /** @return the layer's x render offset, this takes into consideration all parent layers' offsets */
  def renderOffsetX: Float = {
    if (renderOffsetDirty) calculateRenderOffsets()
    _renderOffsetX
  }

  /** @return the layer's y render offset, this takes into consideration all parent layers' offsets */
  def renderOffsetY: Float = {
    if (renderOffsetDirty) calculateRenderOffsets()
    _renderOffsetY
  }

  /** set the renderOffsetDirty state to true, when this layer or any parents' offset has changed * */
  def invalidateRenderOffset(): Unit =
    renderOffsetDirty = true

  /** @return the layer's parent {@link MapLayer}, or null if the layer does not have a parent * */
  def parent: Nullable[MapLayer] = _parent

  /** @param parent the layer's new parent {@MapLayer}, internal use only * */
  def parent_=(parent: Nullable[MapLayer]): Unit = {
    parent.foreach { p =>
      if (p eq this) throw SgeError.InvalidInput("Can't set self as the parent")
    }
    this._parent = parent
  }

  // objects, visible, and properties are public vars/vals

  protected def calculateRenderOffsets(): Unit = {
    _parent.fold {
      _renderOffsetX = _offsetX
      _renderOffsetY = _offsetY
    } { p =>
      p.calculateRenderOffsets()
      _renderOffsetX = p.renderOffsetX + _offsetX
      _renderOffsetY = p.renderOffsetY + _offsetY
    }
    renderOffsetDirty = false
  }
}
