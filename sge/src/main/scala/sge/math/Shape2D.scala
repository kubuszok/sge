/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Shape2D.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: contains(Vector2), contains(x,y)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: Shape2D,contains
 * Covenant-source-reference: com/badlogic/gdx/math/Shape2D.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math

trait Shape2D {

  /** Returns whether the given point is contained within the shape. */
  def contains(point: Vector2): Boolean

  /** Returns whether a point with the given coordinates is contained within the shape. */
  def contains(x: Float, y: Float): Boolean
}
