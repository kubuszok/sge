/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/PointMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Vector2

/** @brief Represents 2D points on the map */
class PointMapObject(x: Float, y: Float) extends MapObject {

  private val point: Vector2 = new Vector2(x, y)

  /** creates a 2D point map object at (0, 0) */
  def this() = this(0f, 0f)

  /** @return 2D point on the map as {@link Vector2} */
  def getPoint: Vector2 = point
}
