/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Shape2D.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

trait Shape2D {

  /** Returns whether the given point is contained within the shape. */
  def contains(point: Vector2): Boolean

  /** Returns whether a point with the given coordinates is contained within the shape. */
  def contains(x: Float, y: Float): Boolean
}
