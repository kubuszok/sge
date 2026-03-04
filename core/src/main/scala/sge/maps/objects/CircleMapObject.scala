/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/CircleMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getCircle() -> val circle
 *   Convention: `circle` field promoted from non-final to `val`
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Circle

/** @brief Represents {@link Circle} shaped map objects */
class CircleMapObject(x: Float = 0.0f, y: Float = 0.0f, radius: Float = 1.0f) extends MapObject {

  val circle: Circle = Circle(x, y, radius)
}
