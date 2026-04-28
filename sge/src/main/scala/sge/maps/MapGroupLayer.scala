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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: MapGroupLayer,i,invalidateRenderOffset,layers
 * Covenant-source-reference: com/badlogic/gdx/maps/MapGroupLayer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package maps

/** Map layer containing a set of MapLayers, objects and properties */
class MapGroupLayer extends MapLayer {

  /** @return the {@link MapLayers} owned by this group */
  val layers: MapLayers = MapLayers()

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
