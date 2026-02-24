/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled

import sge.maps.{ MapLayer, MapObject, MapRenderer }

trait TiledMapRenderer extends MapRenderer {
  def renderObjects(layer: MapLayer): Unit

  def renderObject(obj: MapObject): Unit

  def renderTileLayer(layer: TiledMapTileLayer): Unit

  def renderImageLayer(layer: TiledMapImageLayer): Unit
}
