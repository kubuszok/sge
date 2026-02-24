/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/Map.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
  private val layers:     MapLayers     = new MapLayers()
  private val properties: MapProperties = new MapProperties()

  /** @return the map's layers */
  def getLayers: MapLayers = layers

  /** @return the map's properties */
  def getProperties: MapProperties = properties

  /** Disposes all resources like {@link Texture} instances that the map may own. */
  override def close(): Unit = {}
}
