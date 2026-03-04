/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapTileLayer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods (getWidth/Height, getTileWidth/Height, getCell, setCell) match Java 1:1
 *   - Inner Cell class: 4 fields, 8 get/set methods, 4 rotation constants -- all match Java
 *   - Java null returns from getCell/getTile → Nullable[A]
 *   - Split package, braces, no-return conventions satisfied
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (4 pairs in Cell)
 */
package sge
package maps
package tiled

import sge.maps.MapLayer
import sge.utils.Nullable

/** @brief Layer for a TiledMap */
class TiledMapTileLayer(width: Int, height: Int, tileWidth: Int, tileHeight: Int) extends MapLayer {

  private val cells: Array[Array[Nullable[TiledMapTileLayer.Cell]]] =
    Array.fill(width)(Array.fill(height)(Nullable.empty))

  /** @return layer's width in tiles */
  def getWidth: Int = width

  /** @return layer's height in tiles */
  def getHeight: Int = height

  /** @return tiles' width in pixels */
  def getTileWidth: Int = tileWidth

  /** @return tiles' height in pixels */
  def getTileHeight: Int = tileHeight

  /** @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    * @return
    *   {@link Cell} at (x, y)
    */
  def getCell(x: Int, y: Int): Nullable[TiledMapTileLayer.Cell] =
    if (x < 0 || x >= width) Nullable.empty
    else if (y < 0 || y >= height) Nullable.empty
    else cells(x)(y)

  /** Sets the {@link Cell} at the given coordinates.
    *
    * @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    * @param cell
    *   the {@link Cell} to set at the given coordinates.
    */
  def setCell(x: Int, y: Int, cell: Nullable[TiledMapTileLayer.Cell]): Unit =
    if (x >= 0 && x < width && y >= 0 && y < height) {
      cells(x)(y) = cell
    }
}

object TiledMapTileLayer {

  /** @brief represents a cell in a TiledLayer: TiledMapTile, flip and rotation properties. */
  class Cell {

    private var tile: Nullable[TiledMapTile] = Nullable.empty

    private var flipHorizontally: Boolean = false

    private var flipVertically: Boolean = false

    private var rotation: Int = 0

    /** @return The tile currently assigned to this cell. */
    def getTile: Nullable[TiledMapTile] = tile

    /** Sets the tile to be used for this cell.
      *
      * @param tile
      *   the {@link TiledMapTile} to use for this cell.
      * @return
      *   this, for method chaining
      */
    def setTile(tile: Nullable[TiledMapTile]): Cell = {
      this.tile = tile
      this
    }

    /** @return Whether the tile should be flipped horizontally. */
    def getFlipHorizontally: Boolean = flipHorizontally

    /** Sets whether to flip the tile horizontally.
      *
      * @param flipHorizontally
      *   whether or not to flip the tile horizontally.
      * @return
      *   this, for method chaining
      */
    def setFlipHorizontally(flipHorizontally: Boolean): Cell = {
      this.flipHorizontally = flipHorizontally
      this
    }

    /** @return Whether the tile should be flipped vertically. */
    def getFlipVertically: Boolean = flipVertically

    /** Sets whether to flip the tile vertically.
      *
      * @param flipVertically
      *   whether or not this tile should be flipped vertically.
      * @return
      *   this, for method chaining
      */
    def setFlipVertically(flipVertically: Boolean): Cell = {
      this.flipVertically = flipVertically
      this
    }

    /** @return The rotation of this cell, in 90 degree increments. */
    def getRotation: Int = rotation

    /** Sets the rotation of this cell, in 90 degree increments.
      *
      * @param rotation
      *   the rotation in 90 degree increments (see ints below).
      * @return
      *   this, for method chaining
      */
    def setRotation(rotation: Int): Cell = {
      this.rotation = rotation
      this
    }
  }

  object Cell {
    final val ROTATE_0   = 0
    final val ROTATE_90  = 1
    final val ROTATE_180 = 2
    final val ROTATE_270 = 3
  }
}
