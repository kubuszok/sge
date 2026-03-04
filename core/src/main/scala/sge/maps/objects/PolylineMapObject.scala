/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/PolylineMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getPolyline/setPolyline
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Polyline

/** @brief Represents {@link Polyline} map objects */
class PolylineMapObject extends MapObject {

  private var polyline: Polyline = new Polyline(Array.empty[Float])

  /** @param vertices polyline defining vertices */
  def this(vertices: Array[Float]) = {
    this()
    polyline = new Polyline(vertices)
  }

  /** @param polyline the polyline */
  def this(polyline: Polyline) = {
    this()
    this.polyline = polyline
  }

  /** @return polyline shape */
  def getPolyline: Polyline = polyline

  /** @param polyline new object's polyline shape */
  def setPolyline(polyline: Polyline): Unit =
    this.polyline = polyline
}
