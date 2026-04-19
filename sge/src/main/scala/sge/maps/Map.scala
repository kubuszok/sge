/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/Map.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Disposable -> AutoCloseable; dispose() -> close()
 *   Convention: split package (sge / maps)
 *   Renames: getLayers → val layers, getProperties → val properties
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: Map,close,layers,properties
 * Covenant-source-reference: com/badlogic/gdx/maps/Map.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps

/** A generic level map implementation. </p>
  *
  * A map has {@link MapProperties} which describe general attributes. Availability of properties depends on the type of map, e.g. what format is was loaded from etc. </p>
  *
  * A map has {@link MapLayers}. Map layers are ordered and indexed. A {@link MapLayer} contains {@link MapObjects} which represent things within the layer. Different types of {@link MapObject} are
  * available, e.g. {@link CircleMapObject}, {@link TextureMapObject}, and so on. </p>
  *
  * A map can be rendered by a {@link MapRenderer}. A MapRenderer implementation may chose to only render specific MapObject or MapLayer types. </p>
  *
  * There are more specialized implementations of Map for specific use cases. e.g. the {@link TiledMap} class and its associated classes add functionality specifically for tile maps on top of the
  * basic map functionality. </p>
  *
  * Maps must be closed through a call to {@link #close()} when no longer used.
  */
class Map extends AutoCloseable {
  val layers:     MapLayers     = MapLayers()
  val properties: MapProperties = MapProperties()

  /** Disposes all resources like {@link Texture} instances that the map may own. */
  override def close(): Unit = {}
}
