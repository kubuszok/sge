/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/tiles/StaticTiledMapTile.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all methods, fields, and constructors match 1:1
 * - Java null-initialized properties/objects replaced with Nullable.empty
 * - properties/objects use Nullable lazy-init pattern (matches Java null-check-then-create)
 * - 2 constructors: primary (TextureRegion) + copy constructor (StaticTiledMapTile)
 *   Renames: getId/setId → var id, getBlendMode/setBlendMode → var blendMode,
 *     getTextureRegion/setTextureRegion → var textureRegion,
 *     getOffsetX/setOffsetX → var offsetX, getOffsetY/setOffsetY → var offsetY,
 *     getProperties → def properties, getObjects → def objects
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 95
 * Covenant-baseline-methods: StaticTiledMapTile,_blendMode,_id,_objects,_offsetX,_offsetY,_properties,blendMode,blendMode_,id,id_,objects,offsetX,offsetX_,offsetY,offsetY_,properties,textureRegion,textureRegion_,this
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/tiles/StaticTiledMapTile.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package maps
package tiled
package tiles

import sge.graphics.g2d.TextureRegion
import sge.maps.{ MapObjects, MapProperties }
import sge.utils.Nullable

/** @brief Represents a non changing {@link TiledMapTile} (can be cached) */
class StaticTiledMapTile(private var _textureRegion: TextureRegion) extends TiledMapTile {

  private var _id: Int = 0

  private var _blendMode: TiledMapTile.BlendMode = TiledMapTile.BlendMode.ALPHA

  private var _properties: Nullable[MapProperties] = Nullable.empty

  private var _objects: Nullable[MapObjects] = Nullable.empty

  private var _offsetX: Float = 0f

  private var _offsetY: Float = 0f

  /** Copy constructor
    *
    * @param copy
    *   the StaticTiledMapTile to copy.
    */
  def this(copy: StaticTiledMapTile) = {
    this(copy._textureRegion)
    copy._properties.foreach { props =>
      properties.putAll(props)
    }
    this._objects = copy._objects
    this._id = copy._id
  }

  override def id: Int = _id

  override def id_=(id: Int): Unit =
    this._id = id

  override def blendMode: TiledMapTile.BlendMode = _blendMode

  override def blendMode_=(blendMode: TiledMapTile.BlendMode): Unit =
    this._blendMode = blendMode

  override def properties: MapProperties = {
    if (_properties.isEmpty) {
      _properties = Nullable(MapProperties())
    }
    _properties.getOrElse(MapProperties())
  }

  override def objects: MapObjects = {
    if (_objects.isEmpty) {
      _objects = Nullable(MapObjects())
    }
    _objects.getOrElse(MapObjects())
  }

  override def textureRegion: TextureRegion = _textureRegion

  override def textureRegion_=(textureRegion: TextureRegion): Unit =
    this._textureRegion = textureRegion

  override def offsetX: Float = _offsetX

  override def offsetX_=(offsetX: Float): Unit =
    this._offsetX = offsetX

  override def offsetY: Float = _offsetY

  override def offsetY_=(offsetY: Float): Unit =
    this._offsetY = offsetY
}
