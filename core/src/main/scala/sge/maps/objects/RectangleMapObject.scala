/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/RectangleMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Rectangle

/** @brief Represents a rectangle shaped map object */
class RectangleMapObject(x: Float, y: Float, width: Float, height: Float) extends MapObject {

  private val rectangle: Rectangle = new Rectangle(x, y, width, height)

  /** Creates a rectangle object which lower left corner is at (0, 0) with width=1 and height=1 */
  def this() = this(0.0f, 0.0f, 1.0f, 1.0f)

  /** @return rectangle shape */
  def getRectangle: Rectangle = rectangle
}
