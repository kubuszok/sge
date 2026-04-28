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
 *   - Cell: private vars tile/flipHorizontally/flipVertically/rotation → public vars, removed 8 getter/setter methods
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 72
 * Covenant-baseline-methods: Cell,ROTATE_0,ROTATE_180,ROTATE_270,ROTATE_90,TiledMapTileLayer,cells,flipHorizontally,flipVertically,getCell,rotation,setCell,tile
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/TiledMapTileLayer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package maps
package tiled

import sge.maps.MapLayer
import sge.utils.Nullable

/** @brief Layer for a TiledMap */
class TiledMapTileLayer(val width: Int, val height: Int, val tileWidth: Int, val tileHeight: Int) extends MapLayer {

  private val cells: Array[Array[Nullable[TiledMapTileLayer.Cell]]] =
    Array.fill(width)(Array.fill(height)(Nullable.empty))

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
    var tile:             Nullable[TiledMapTile] = Nullable.empty
    var flipHorizontally: Boolean                = false
    var flipVertically:   Boolean                = false
    var rotation:         Int                    = 0
  }

  object Cell {
    final val ROTATE_0   = 0
    final val ROTATE_90  = 1
    final val ROTATE_180 = 2
    final val ROTATE_270 = 3
  }
}
