/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapGroupLayer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: (none)
 *   Convention: for loop -> while loop (matches Java iteration pattern)
 *   Idiom: layers.size() -> layers.size (Scala property access)
 *   Audited: 2026-03-03
 */
package sge
package maps

/** Map layer containing a set of MapLayers, objects and properties */
class MapGroupLayer extends MapLayer {

  private val layers: MapLayers = new MapLayers()

  /** @return the {@link MapLayers} owned by this group */
  def getLayers: MapLayers = layers

  override def invalidateRenderOffset(): Unit = {
    super.invalidateRenderOffset()
    var i = 0
    while (i < layers.size) {
      val child = layers.get(i)
      child.invalidateRenderOffset()
      i += 1
    }
  }
}
