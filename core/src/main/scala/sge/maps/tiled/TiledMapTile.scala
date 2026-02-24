/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapTile.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled

import sge.graphics.g2d.TextureRegion
import sge.maps.{ MapObjects, MapProperties }

/** @brief Generalises the concept of tile in a TiledMap */
trait TiledMapTile {

  def getId: Int

  def setId(id: Int): Unit

  /** @return the {@link BlendMode} to use for rendering the tile */
  def getBlendMode: TiledMapTile.BlendMode

  /** Sets the {@link BlendMode} to use for rendering the tile
    *
    * @param blendMode
    *   the blend mode to use for rendering the tile
    */
  def setBlendMode(blendMode: TiledMapTile.BlendMode): Unit

  /** @return texture region used to render the tile */
  def getTextureRegion: TextureRegion

  /** Sets the texture region used to render the tile */
  def setTextureRegion(textureRegion: TextureRegion): Unit

  /** @return the amount to offset the x position when rendering the tile */
  def getOffsetX: Float

  /** Set the amount to offset the x position when rendering the tile */
  def setOffsetX(offsetX: Float): Unit

  /** @return the amount to offset the y position when rendering the tile */
  def getOffsetY: Float

  /** Set the amount to offset the y position when rendering the tile */
  def setOffsetY(offsetY: Float): Unit

  /** @return tile's properties set */
  def getProperties: MapProperties

  /** @return collection of objects contained in the tile */
  def getObjects: MapObjects
}

object TiledMapTile {

  enum BlendMode {
    case NONE, ALPHA
  }
}
