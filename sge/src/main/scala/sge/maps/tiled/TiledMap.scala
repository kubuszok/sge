/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMap.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All methods (getTileSets, setOwnedResources, close) match Java 1:1
 *   - Java dispose() → close() (AutoCloseable)
 *   - Java Disposable → AutoCloseable; Array<Disposable> → DynamicArray[AutoCloseable]
 *   - ownedResources: Java null → Nullable[DynamicArray[AutoCloseable]]
 *   - Split package, braces, no-return conventions satisfied
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 44
 * Covenant-baseline-methods: TiledMap,close,ownedResources,setOwnedResources,tileSets
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/TiledMap.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled

import sge.utils.{ DynamicArray, Nullable }

/** @brief
  *   Represents a tiled map, adds the concept of tiles and tilesets.
  *
  * @see
  *   Map
  */
class TiledMap extends maps.Map {
  private var ownedResources: Nullable[DynamicArray[AutoCloseable]] = Nullable.empty

  /** @return collection of tilesets for this map. */
  val tileSets: TiledMapTileSets = TiledMapTileSets()

  /** Used by loaders to set resources when loading the map directly, without {@link AssetManager}. To be disposed in {@link #close()}.
    * @param resources
    */
  def setOwnedResources(resources: DynamicArray[AutoCloseable]): Unit =
    this.ownedResources = Nullable(resources)

  override def close(): Unit =
    ownedResources.foreach { resources =>
      resources.foreach(_.close())
    }
}
