/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/objects/TiledMapTileMapObject.java
 * Original authors: Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all methods, fields, and constructor match 1:1
 * - Constructor body uses locally{} block for init (matches Java constructor body)
 * - setTextureRegion called with Nullable(textureRegion) due to SGE's TextureMapObject accepting Nullable
 * - 6 accessor methods (get/set for flipHorizontally, flipVertically, tile) all match Java
 * TODO: Java-style getters/setters — isFlipHorizontally/setFlipHorizontally, isFlipVertically/setFlipVertically, getTile/setTile
 */
package sge
package maps
package tiled
package objects

import sge.graphics.g2d.TextureRegion
import sge.maps.objects.TextureMapObject
import sge.utils.Nullable

/** A {@link MapObject} with a {@link TiledMapTile}. Can be both {@link StaticTiledMapTile} or {@link AnimatedTiledMapTile}. For compatibility reasons, this extends {@link TextureMapObject}. Use
  * {@link TiledMapTile#getTextureRegion()} instead of {@link #getTextureRegion()}.
  * @author
  *   Daniel Holderbaum
  */
class TiledMapTileMapObject(
  private var tile:             TiledMapTile,
  private var flipHorizontally: Boolean,
  private var flipVertically:   Boolean
) extends TextureMapObject() {

  locally {
    val textureRegion = new TextureRegion(tile.getTextureRegion)
    textureRegion.flip(flipHorizontally, flipVertically)
    setTextureRegion(Nullable(textureRegion))
  }

  def isFlipHorizontally: Boolean = flipHorizontally

  def setFlipHorizontally(flipHorizontally: Boolean): Unit =
    this.flipHorizontally = flipHorizontally

  def isFlipVertically: Boolean = flipVertically

  def setFlipVertically(flipVertically: Boolean): Unit =
    this.flipVertically = flipVertically

  def getTile: TiledMapTile = tile

  def setTile(tile: TiledMapTile): Unit =
    this.tile = tile
}
