/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/PolygonMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getPolygon()/setPolygon() -> var polygon
 *   Convention: no-arg constructor made primary; other constructors delegate to it
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: PolygonMapObject,polygon,this
 * Covenant-source-reference: com/badlogic/gdx/maps/objects/PolygonMapObject.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package objects

import sge.math.Polygon

/** @brief Represents {@link Polygon} map objects */
class PolygonMapObject extends MapObject {

  var polygon: Polygon = Polygon(Array.empty[Float])

  /** @param vertices polygon defining vertices (at least 3) */
  def this(vertices: Array[Float]) = {
    this()
    polygon = Polygon(vertices)
  }

  /** @param polygon the polygon */
  def this(polygon: Polygon) = {
    this()
    this.polygon = polygon
  }
}
