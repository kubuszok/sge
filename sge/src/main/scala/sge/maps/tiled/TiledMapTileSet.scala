/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapTileSet.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All 7 methods (getName, setName, getProperties, getTile, iterator, putTile, removeTile, size) match Java 1:1
 *   - Java IntMap<TiledMapTile> → scala.collection.mutable.HashMap[Int, TiledMapTile]
 *   - getTile returns Nullable[TiledMapTile] (Java returns null)
 *   - Extends Iterable[TiledMapTile] (Java implements Iterable)
 *   - Split package, braces, no-return conventions satisfied
 *   - Renames: getName/setName → var name, getProperties → val properties
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 63
 * Covenant-baseline-methods: TiledMapTileSet,getTile,iterator,name,properties,putTile,removeTile,size,tiles
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/TiledMapTileSet.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled

import scala.collection.mutable
import sge.maps.MapProperties
import sge.utils.Nullable

/** @brief Set of {@link TiledMapTile} instances used to compose a TiledMapLayer */
class TiledMapTileSet extends Iterable[TiledMapTile] {

  var name: String = ""

  private val tiles: mutable.HashMap[Int, TiledMapTile] = mutable.HashMap.empty

  val properties: MapProperties = MapProperties()

  /** Gets the {@link TiledMapTile} that has the given id.
    *
    * @param id
    *   the id of the {@link TiledMapTile} to retrieve.
    * @return
    *   tile matching id, null if it doesn't exist
    */
  def getTile(id: Int): Nullable[TiledMapTile] =
    Nullable.fromOption(tiles.get(id))

  /** @return iterator to tiles in this tileset */
  override def iterator: Iterator[TiledMapTile] = tiles.valuesIterator

  /** Adds or replaces tile with that id
    *
    * @param id
    *   the id of the {@link TiledMapTile} to add or replace.
    * @param tile
    *   the {@link TiledMapTile} to add or replace.
    */
  def putTile(id: Int, tile: TiledMapTile): Unit =
    tiles.put(id, tile)

  /** @param id tile's id to be removed */
  def removeTile(id: Int): Unit =
    tiles.remove(id)

  /** @return the size of this TiledMapTileSet. */
  override def size: Int = tiles.size
}
