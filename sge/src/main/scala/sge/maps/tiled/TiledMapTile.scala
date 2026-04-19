/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapTile.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All 12 interface methods match Java 1:1
 *   - BlendMode Java enum → Scala 3 enum in companion object
 *   - Split package, braces, no-return conventions satisfied
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 68
 * Covenant-baseline-methods: BlendMode,TiledMapTile,blendMode,blendMode_,id,id_,objects,offsetX,offsetX_,offsetY,offsetY_,properties,textureRegion,textureRegion_
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/TiledMapTile.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled

import sge.graphics.g2d.TextureRegion
import sge.maps.{ MapObjects, MapProperties }

/** @brief Generalises the concept of tile in a TiledMap */
trait TiledMapTile {

  def id: Int

  def id_=(id: Int): Unit

  /** @return the {@link BlendMode} to use for rendering the tile */
  def blendMode: TiledMapTile.BlendMode

  /** Sets the {@link BlendMode} to use for rendering the tile
    *
    * @param blendMode
    *   the blend mode to use for rendering the tile
    */
  def blendMode_=(blendMode: TiledMapTile.BlendMode): Unit

  /** @return texture region used to render the tile */
  def textureRegion: TextureRegion

  /** Sets the texture region used to render the tile */
  def textureRegion_=(textureRegion: TextureRegion): Unit

  /** @return the amount to offset the x position when rendering the tile */
  def offsetX: Float

  /** Set the amount to offset the x position when rendering the tile */
  def offsetX_=(offsetX: Float): Unit

  /** @return the amount to offset the y position when rendering the tile */
  def offsetY: Float

  /** Set the amount to offset the y position when rendering the tile */
  def offsetY_=(offsetY: Float): Unit

  /** @return tile's properties set */
  def properties: MapProperties

  /** @return collection of objects contained in the tile */
  def objects: MapObjects
}

object TiledMapTile {

  enum BlendMode {
    case NONE, ALPHA
  }
}
