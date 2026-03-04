/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/PolylineMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getPolyline()/setPolyline() -> var polyline
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Polyline

/** @brief Represents {@link Polyline} map objects */
class PolylineMapObject extends MapObject {

  var polyline: Polyline = Polyline(Array.empty[Float])

  /** @param vertices polyline defining vertices */
  def this(vertices: Array[Float]) = {
    this()
    polyline = Polyline(vertices)
  }

  /** @param polyline the polyline */
  def this(polyline: Polyline) = {
    this()
    this.polyline = polyline
  }
}
