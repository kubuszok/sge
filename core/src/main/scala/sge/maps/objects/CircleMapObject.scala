/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/CircleMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Circle

/** @brief Represents {@link Circle} shaped map objects */
class CircleMapObject(x: Float, y: Float, radius: Float) extends MapObject {

  private val circle: Circle = new Circle(x, y, radius)

  /** Creates a circle map object at (0,0) with r=1.0 */
  def this() = this(0.0f, 0.0f, 1.0f)

  /** @return circle shape */
  def getCircle: Circle = circle
}
