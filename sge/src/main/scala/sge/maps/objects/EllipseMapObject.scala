/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/EllipseMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getEllipse() -> val ellipse
 *   Convention: `ellipse` field promoted from non-final to `val`
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: EllipseMapObject,ellipse
 * Covenant-source-reference: com/badlogic/gdx/maps/objects/EllipseMapObject.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 72917675ed4718b7997044e26dc6f94544150391
 */
package sge
package maps
package objects

import sge.math.Ellipse

/** @brief Represents {@link Ellipse} map objects. */
class EllipseMapObject(x: Float = 0.0f, y: Float = 0.0f, width: Float = 1.0f, height: Float = 1.0f) extends MapObject {

  val ellipse: Ellipse = Ellipse(x, y, width, height)
}
