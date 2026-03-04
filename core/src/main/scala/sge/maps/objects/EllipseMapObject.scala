/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/EllipseMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: `ellipse` field promoted from non-final to `val`
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getEllipse
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Ellipse

/** @brief Represents {@link Ellipse} map objects. */
class EllipseMapObject(x: Float, y: Float, width: Float, height: Float) extends MapObject {

  private val ellipse: Ellipse = new Ellipse(x, y, width, height)

  /** Creates an {@link Ellipse} object whose lower left corner is at (0, 0) with width=1 and height=1 */
  def this() = this(0.0f, 0.0f, 1.0f, 1.0f)

  /** @return ellipse shape */
  def getEllipse: Ellipse = ellipse
}
