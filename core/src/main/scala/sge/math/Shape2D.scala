package sge
package math

trait Shape2D {

  /** Returns whether the given point is contained within the shape. */
  def contains(point: Vector2): Boolean

  /** Returns whether a point with the given coordinates is contained within the shape. */
  def contains(x: Float, y: Float): Boolean
}
