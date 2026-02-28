/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Circle.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

/** A convenient 2D circle class.
  * @author
  *   mzechner (original implementation)
  */
class Circle() extends Shape2D {
  var x:      Float = 0f
  var y:      Float = 0f
  var radius: Float = 0f

  /** Constructs a new circle with the given X and Y coordinates and the given radius.
    *
    * @param x
    *   X coordinate of the center of the circle
    * @param y
    *   Y coordinate of the center of the circle
    * @param radius
    *   The radius of the circle
    */
  def this(x: Float, y: Float, radius: Float) = {
    this()
    this.x = x
    this.y = y
    this.radius = radius
  }

  /** Constructs a new circle using a given {@link Vector2} that contains the desired X and Y coordinates, and a given radius.
    *
    * @param position
    *   The position {@link Vector2} of the center of the circle
    * @param radius
    *   The radius
    */
  def this(position: Vector2, radius: Float) = {
    this()
    this.x = position.x
    this.y = position.y
    this.radius = radius
  }

  /** Copy constructor
    *
    * @param circle
    *   The circle to construct a copy of.
    */
  def this(circle: Circle) = {
    this()
    this.x = circle.x
    this.y = circle.y
    this.radius = circle.radius
  }

  /** Creates a new {@link Circle} in terms of its center and a point on its edge.
    *
    * @param center
    *   The center of the new circle
    * @param edge
    *   Any point on the edge of the given circle
    */
  def this(center: Vector2, edge: Vector2) = {
    this()
    this.x = center.x
    this.y = center.y
    val dx = center.x - edge.x
    val dy = center.y - edge.y
    this.radius = Math.sqrt(dx * dx + dy * dy).toFloat
  }

  /** Sets a new location and radius for this circle.
    *
    * @param x
    *   X coordinate of the center of the circle
    * @param y
    *   Y coordinate of the center of the circle
    * @param radius
    *   Circle radius
    */
  def set(x: Float, y: Float, radius: Float): Unit = {
    this.x = x
    this.y = y
    this.radius = radius
  }

  /** Sets a new location and radius for this circle.
    *
    * @param position
    *   Position {@link Vector2} for this circle.
    * @param radius
    *   Circle radius
    */
  def set(position: Vector2, radius: Float): Unit = {
    this.x = position.x
    this.y = position.y
    this.radius = radius
  }

  /** Sets a new location and radius for this circle, based upon another circle.
    *
    * @param circle
    *   The circle to copy the position and radius of.
    */
  def set(circle: Circle): Unit = {
    this.x = circle.x
    this.y = circle.y
    this.radius = circle.radius
  }

  /** Sets this {@link Circle} 's values in terms of its center and a point on its edge.
    *
    * @param center
    *   The new center of the circle
    * @param edge
    *   Any point on the edge of the given circle
    */
  def set(center: Vector2, edge: Vector2): Unit = {
    this.x = center.x
    this.y = center.y
    val dx = center.x - edge.x
    val dy = center.y - edge.y
    this.radius = Math.sqrt(dx * dx + dy * dy).toFloat
  }

  /** Sets the x and y-coordinates of circle center from vector
    * @param position
    *   The position vector
    */
  def setPosition(position: Vector2): Unit = {
    this.x = position.x
    this.y = position.y
  }

  /** Sets the x and y-coordinates of circle center
    * @param x
    *   The x-coordinate
    * @param y
    *   The y-coordinate
    */
  def setPosition(x: Float, y: Float): Unit = {
    this.x = x
    this.y = y
  }

  /** Sets the x-coordinate of circle center
    * @param x
    *   The x-coordinate
    */
  def setX(x: Float): Unit =
    this.x = x

  /** Sets the y-coordinate of circle center
    * @param y
    *   The y-coordinate
    */
  def setY(y: Float): Unit =
    this.y = y

  /** Sets the radius of circle
    * @param radius
    *   The radius
    */
  def setRadius(radius: Float): Unit =
    this.radius = radius

  /** Checks whether or not this circle contains a given point.
    *
    * @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    *
    * @return
    *   true if this circle contains the given point.
    */
  def contains(x: Float, y: Float): Boolean = {
    val dx = this.x - x
    val dy = this.y - y
    dx * dx + dy * dy <= radius * radius
  }

  /** Checks whether or not this circle contains a given point.
    *
    * @param point
    *   The {@link Vector2} that contains the point coordinates.
    *
    * @return
    *   true if this circle contains this point; false otherwise.
    */
  def contains(point: Vector2): Boolean = {
    val dx = x - point.x
    val dy = y - point.y
    dx * dx + dy * dy <= radius * radius
  }

  /** @param c
    *   the other {@link Circle}
    * @return
    *   whether this circle contains the other circle.
    */
  def contains(c: Circle): Boolean = {
    val radiusDiff = radius - c.radius
    if (radiusDiff < 0f) false // Can't contain bigger circle
    else {
      val dx        = x - c.x
      val dy        = y - c.y
      val dst       = dx * dx + dy * dy
      val radiusSum = radius + c.radius
      !(radiusDiff * radiusDiff < dst) && (dst < radiusSum * radiusSum)
    }
  }

  /** @param c
    *   the other {@link Circle}
    * @return
    *   whether this circle overlaps the other circle.
    */
  def overlaps(c: Circle): Boolean = {
    val dx        = x - c.x
    val dy        = y - c.y
    val distance  = dx * dx + dy * dy
    val radiusSum = radius + c.radius
    distance < radiusSum * radiusSum
  }

  /** Returns a {@link String} representation of this {@link Circle} of the form {@code x,y,radius}. */
  override def toString: String =
    s"$x,$y,$radius"

  /** @return The circumference of this circle (as 2 * {@link MathUtils#PI2}) * {@code radius} */
  def circumference(): Float =
    this.radius * MathUtils.PI2

  /** @return The area of this circle (as {@link MathUtils#PI} * radius * radius). */
  def area(): Float =
    this.radius * this.radius * MathUtils.PI

  override def equals(o: Any): Boolean = o match {
    case c: Circle => this.x == c.x && this.y == c.y && this.radius == c.radius
    case _ => false
  }

  override def hashCode(): Int = {
    val prime  = 41
    var result = 1
    result = prime * result + java.lang.Float.floatToRawIntBits(radius)
    result = prime * result + java.lang.Float.floatToRawIntBits(x)
    result = prime * result + java.lang.Float.floatToRawIntBits(y)
    result
  }
}
