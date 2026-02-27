/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapTileSets.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled

import scala.util.boundary
import scala.util.boundary.break
import sge.utils.{ DynamicArray, Nullable }

/** @brief Collection of {@link TiledMapTileSet} */
class TiledMapTileSets extends Iterable[TiledMapTileSet] {

  private val tilesets: DynamicArray[TiledMapTileSet] = DynamicArray[TiledMapTileSet]()

  /** @param index
    *   index to get the desired {@link TiledMapTileSet} at.
    * @return
    *   tileset at index
    */
  def getTileSet(index: Int): TiledMapTileSet =
    tilesets(index)

  /** @param name
    *   Name of the {@link TiledMapTileSet} to retrieve.
    * @return
    *   tileset with matching name, null if it doesn't exist
    */
  def getTileSet(name: String): Nullable[TiledMapTileSet] = boundary {
    var i = 0
    val n = tilesets.size
    while (i < n) {
      val tileset = tilesets(i)
      if (name == tileset.getName) {
        break(Nullable(tileset))
      }
      i += 1
    }
    Nullable.empty
  }

  /** @param tileset set to be added to the collection */
  def addTileSet(tileset: TiledMapTileSet): Unit =
    tilesets.add(tileset)

  /** Removes tileset at index
    *
    * @param index
    *   index at which to remove a tileset.
    */
  def removeTileSet(index: Int): Unit =
    tilesets.removeIndex(index)

  /** @param tileset set to be removed */
  def removeTileSet(tileset: TiledMapTileSet): Unit =
    tilesets.removeValue(tileset)

  /** @param id
    *   id of the {@link TiledMapTile} to get.
    * @return
    *   tile with matching id, null if it doesn't exist
    */
  def getTile(id: Int): Nullable[TiledMapTile] = boundary {
    // The purpose of backward iteration here is to maintain backwards compatibility
    // with maps created with earlier versions of a shared tileset. The assumption
    // is that the tilesets are in order of ascending firstgid, and by backward
    // iterating precedence for conflicts is given to later tilesets in the list,
    // which are likely to be the earlier version of any given gid.
    // See TiledMapModifiedExternalTilesetTest for example of this issue.
    var i = tilesets.size - 1
    while (i >= 0) {
      val tileset = tilesets(i)
      val tile    = tileset.getTile(id)
      if (tile.isDefined) {
        break(tile)
      }
      i -= 1
    }
    Nullable.empty
  }

  /** @return iterator to tilesets */
  override def iterator: Iterator[TiledMapTileSet] = tilesets.iterator
}
