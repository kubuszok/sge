/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/PointMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getPoint() -> val point
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Vector2

/** @brief Represents 2D points on the map */
class PointMapObject(x: Float = 0f, y: Float = 0f) extends MapObject {

  val point: Vector2 = new Vector2(x, y)
}
