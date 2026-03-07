/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/TiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 *   - All 4 interface methods match Java 1:1
 *   - Split package, braces, no-return conventions satisfied
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
