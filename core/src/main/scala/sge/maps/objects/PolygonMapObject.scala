/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/PolygonMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Polygon

/** @brief Represents {@link Polygon} map objects */
class PolygonMapObject extends MapObject {

  private var polygon: Polygon = new Polygon(Array.empty[Float])

  /** @param vertices polygon defining vertices (at least 3) */
  def this(vertices: Array[Float]) = {
    this()
    polygon = new Polygon(vertices)
  }

  /** @param polygon the polygon */
  def this(polygon: Polygon) = {
    this()
    this.polygon = polygon
  }

  /** @return polygon shape */
  def getPolygon: Polygon = polygon

  /** @param polygon new object's polygon shape */
  def setPolygon(polygon: Polygon): Unit =
    this.polygon = polygon
}
