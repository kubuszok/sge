/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/objects/TiledMapTileMapObject.java
 * Original authors: Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
