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
 * - getProperties/getObjects use Nullable lazy-init pattern (matches Java null-check-then-create)
 * - 2 constructors: primary (TextureRegion) + copy constructor (StaticTiledMapTile)
 */
package sge
package maps
package tiled
package tiles

import sge.graphics.g2d.TextureRegion
import sge.maps.{ MapObjects, MapProperties }
import sge.utils.Nullable

/** @brief Represents a non changing {@link TiledMapTile} (can be cached) */
class StaticTiledMapTile(private var textureRegion: TextureRegion) extends TiledMapTile {

  private var id: Int = 0

  private var blendMode: TiledMapTile.BlendMode = TiledMapTile.BlendMode.ALPHA

  private var _properties: Nullable[MapProperties] = Nullable.empty

  private var _objects: Nullable[MapObjects] = Nullable.empty

  private var offsetX: Float = 0f

  private var offsetY: Float = 0f

  /** Copy constructor
    *
    * @param copy
    *   the StaticTiledMapTile to copy.
    */
  def this(copy: StaticTiledMapTile) = {
    this(copy.textureRegion)
    copy._properties.foreach { props =>
      getProperties.putAll(props)
    }
    this._objects = copy._objects
    this.id = copy.id
  }

  override def getId: Int = id

  override def setId(id: Int): Unit =
    this.id = id

  override def getBlendMode: TiledMapTile.BlendMode = blendMode

  override def setBlendMode(blendMode: TiledMapTile.BlendMode): Unit =
    this.blendMode = blendMode

  override def getProperties: MapProperties = {
    if (_properties.isEmpty) {
      _properties = Nullable(MapProperties())
    }
    _properties.getOrElse(MapProperties())
  }

  override def getObjects: MapObjects = {
    if (_objects.isEmpty) {
      _objects = Nullable(MapObjects())
    }
    _objects.getOrElse(MapObjects())
  }

  override def getTextureRegion: TextureRegion = textureRegion

  override def setTextureRegion(textureRegion: TextureRegion): Unit =
    this.textureRegion = textureRegion

  override def getOffsetX: Float = offsetX

  override def setOffsetX(offsetX: Float): Unit =
    this.offsetX = offsetX

  override def getOffsetY: Float = offsetY

  override def setOffsetY(offsetY: Float): Unit =
    this.offsetY = offsetY
}
