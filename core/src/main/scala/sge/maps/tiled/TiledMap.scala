/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMap.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

/** @brief
  *   Represents a tiled map, adds the concept of tiles and tilesets.
  *
  * @see
  *   Map
  */
class TiledMap extends maps.Map {
  private val tilesets:       TiledMapTileSets                     = new TiledMapTileSets()
  private var ownedResources: Nullable[ArrayBuffer[AutoCloseable]] = Nullable.empty

  /** @return collection of tilesets for this map. */
  def getTileSets: TiledMapTileSets = tilesets

  /** Used by loaders to set resources when loading the map directly, without {@link AssetManager}. To be disposed in {@link #close()}.
    * @param resources
    */
  def setOwnedResources(resources: ArrayBuffer[AutoCloseable]): Unit =
    this.ownedResources = Nullable(resources)

  override def close(): Unit =
    ownedResources.foreach { resources =>
      resources.foreach(_.close())
    }
}
