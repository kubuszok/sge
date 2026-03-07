/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/RectangleMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getRectangle() -> val rectangle
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Rectangle

/** @brief Represents a rectangle shaped map object */
class RectangleMapObject(x: Float = 0.0f, y: Float = 0.0f, width: Float = 1.0f, height: Float = 1.0f) extends MapObject {

  val rectangle: Rectangle = Rectangle(x, y, width, height)
}
