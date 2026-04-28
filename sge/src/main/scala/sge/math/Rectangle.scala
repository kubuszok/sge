/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Rectangle.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: removed redundant getX/setX, getY/setY, getWidth/setWidth, getHeight/setHeight (fields are public vars)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: constructors, set(2), x/y/width/height (public vars),
 * getPosition, setPosition(2), setSize(2), getSize, contains(3), overlaps, merge(4), getAspectRatio,
 * getCenter, setCenter(2), fitOutside, fitInside, toString, fromString, area, perimeter,
 * hashCode, equals. Static: tmp, tmp2.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 445
 * Covenant-baseline-methods: Rectangle,area,aspectRatio,center,contains,equals,fitInside,fitOutside,fromString,hashCode,height,maxX,maxY,merge,minX,minY,overlaps,perimeter,position,prime,ratio,result,s0,s1,s2,set,setCenter,setPosition,setSize,size,this,tmp,tmp2,toString,width,x,xmax,xmin,y,ymax,ymin
 * Covenant-source-reference: com/badlogic/gdx/math/Rectangle.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 5950a23bec6b46030cb6bbf0ecfe54854bf9ba39
 */
package sge
package math

/** Encapsulates a 2D rectangle defined by its corner point in the bottom left and its extents in x (width) and y (height).
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
class Rectangle() extends Shape2D {

  var x:      Float = 0f
  var y:      Float = 0f
  var width:  Float = 0f
  var height: Float = 0f

  /** Constructs a new rectangle with the given corner point in the bottom left and dimensions.
    * @param x
    *   The corner point x-coordinate
    * @param y
    *   The corner point y-coordinate
    * @param width
    *   The width
    * @param height
    *   The height
    */
  def this(x: Float, y: Float, width: Float, height: Float) = {
    this()
    this.x = x
    this.y = y
    this.width = width
    this.height = height
  }

  /** Constructs a rectangle based on the given rectangle
    * @param rect
    *   The rectangle
    */
  def this(rect: Rectangle) = {
    this()
    x = rect.x
    y = rect.y
    width = rect.width
    height = rect.height
  }

  /** @param x
    *   bottom-left x coordinate
    * @param y
    *   bottom-left y coordinate
    * @param width
    *   width
    * @param height
    *   height
    * @return
    *   this rectangle for chaining
    */
  def set(x: Float, y: Float, width: Float, height: Float): Rectangle = {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
    this
  }

  /** return the Vector2 with coordinates of this rectangle
    * @param position
    *   The Vector2
    */
  def position(position: Vector2): Vector2 =
    position.set(x, y)

  /** Sets the x and y-coordinates of the bottom left corner from vector
    * @param position
    *   The position vector
    * @return
    *   this rectangle for chaining
    */
  def setPosition(position: Vector2): Rectangle = {
    this.x = position.x
    this.y = position.y
    this
  }

  /** Sets the x and y-coordinates of the bottom left corner
    * @param x
    *   The x-coordinate
    * @param y
    *   The y-coordinate
    * @return
    *   this rectangle for chaining
    */
  def setPosition(x: Float, y: Float): Rectangle = {
    this.x = x
    this.y = y
    this
  }

  /** Sets the width and height of this rectangle
    * @param width
    *   The width
    * @param height
    *   The height
    * @return
    *   this rectangle for chaining
    */
  def setSize(width: Float, height: Float): Rectangle = {
    this.width = width
    this.height = height
    this
  }

  /** Sets the squared size of this rectangle
    * @param sizeXY
    *   The size
    * @return
    *   this rectangle for chaining
    */
  def setSize(sizeXY: Float): Rectangle = {
    this.width = sizeXY
    this.height = sizeXY
    this
  }

  /** @return
    *   the Vector2 with size of this rectangle
    * @param size
    *   The Vector2
    */
  def size(size: Vector2): Vector2 =
    size.set(width, height)

  /** @param x
    *   point x coordinate
    * @param y
    *   point y coordinate
    * @return
    *   whether the point is contained in the rectangle
    */
  def contains(x: Float, y: Float): Boolean =
    this.x <= x && this.x + this.width >= x && this.y <= y && this.y + this.height >= y

  /** @param point
    *   The coordinates vector
    * @return
    *   whether the point is contained in the rectangle
    */
  def contains(point: Vector2): Boolean =
    contains(point.x, point.y)

  /** @param circle
    *   the circle
    * @return
    *   whether the circle is contained in the rectangle
    */
  def contains(circle: Circle): Boolean =
    (circle.x - circle.radius >= x) && (circle.x + circle.radius <= x + width) && (circle.y - circle.radius >= y) &&
      (circle.y + circle.radius <= y + height)

  /** @param rectangle
    *   the other {@link Rectangle} .
    * @return
    *   whether the other rectangle is contained in this rectangle.
    */
  def contains(rectangle: Rectangle): Boolean = {
    val xmin = rectangle.x
    val xmax = xmin + rectangle.width
    val ymin = rectangle.y
    val ymax = ymin + rectangle.height

    ((xmin > x && xmin < x + width) && (xmax > x && xmax < x + width)) &&
    ((ymin > y && ymin < y + height) && (ymax > y && ymax < y + height))
  }

  /** @param r
    *   the other {@link Rectangle}
    * @return
    *   whether this rectangle overlaps the other rectangle.
    */
  def overlaps(r: Rectangle): Boolean =
    x < r.x + r.width && x + width > r.x && y < r.y + r.height && y + height > r.y

  /** Sets the values of the given rectangle to this rectangle.
    * @param rect
    *   the other rectangle
    * @return
    *   this rectangle for chaining
    */
  def set(rect: Rectangle): Rectangle = {
    this.x = rect.x
    this.y = rect.y
    this.width = rect.width
    this.height = rect.height
    this
  }

  /** Merges this rectangle with the other rectangle. The rectangle should not have negative width or negative height.
    * @param rect
    *   the other rectangle
    * @return
    *   this rectangle for chaining
    */
  def merge(rect: Rectangle): Rectangle = {
    val minX = Math.min(x, rect.x)
    val maxX = Math.max(x + width, rect.x + rect.width)
    this.x = minX
    this.width = maxX - minX

    val minY = Math.min(y, rect.y)
    val maxY = Math.max(y + height, rect.y + rect.height)
    this.y = minY
    this.height = maxY - minY

    this
  }

  /** Merges this rectangle with a point. The rectangle should not have negative width or negative height.
    * @param x
    *   the x coordinate of the point
    * @param y
    *   the y coordinate of the point
    * @return
    *   this rectangle for chaining
    */
  def merge(x: Float, y: Float): Rectangle = {
    val minX = Math.min(this.x, x)
    val maxX = Math.max(this.x + width, x)
    this.x = minX
    this.width = maxX - minX

    val minY = Math.min(this.y, y)
    val maxY = Math.max(this.y + height, y)
    this.y = minY
    this.height = maxY - minY

    this
  }

  /** Merges this rectangle with a point. The rectangle should not have negative width or negative height.
    * @param vec
    *   the vector describing the point
    * @return
    *   this rectangle for chaining
    */
  def merge(vec: Vector2): Rectangle =
    merge(vec.x, vec.y)

  /** Merges this rectangle with a list of points. The rectangle should not have negative width or negative height.
    * @param vecs
    *   the vectors describing the points
    * @return
    *   this rectangle for chaining
    */
  def merge(vecs: Array[Vector2]): Rectangle = {
    var minX = x
    var maxX = x + width
    var minY = y
    var maxY = y + height
    for (i <- vecs.indices) {
      val v = vecs(i)
      minX = Math.min(minX, v.x)
      maxX = Math.max(maxX, v.x)
      minY = Math.min(minY, v.y)
      maxY = Math.max(maxY, v.y)
    }
    x = minX
    width = maxX - minX
    y = minY
    height = maxY - minY
    this
  }

  /** Calculates the aspect ratio ( width / height ) of this rectangle
    * @return
    *   the aspect ratio of this rectangle. Returns Float.NaN if height is 0 to avoid ArithmeticException
    */
  def aspectRatio: Float =
    if (height == 0) Float.NaN else width / height

  /** Calculates the center of the rectangle. Results are located in the given Vector2
    * @param vector
    *   the Vector2 to use
    * @return
    *   the given vector with results stored inside
    */
  def center(vector: Vector2): Vector2 = {
    vector.x = x + width / 2
    vector.y = y + height / 2
    vector
  }

  /** Moves this rectangle so that its center point is located at a given position
    * @param x
    *   the position's x
    * @param y
    *   the position's y
    * @return
    *   this for chaining
    */
  def setCenter(x: Float, y: Float): Rectangle = {
    setPosition(x - width / 2, y - height / 2)
    this
  }

  /** Moves this rectangle so that its center point is located at a given position
    * @param position
    *   the position
    * @return
    *   this for chaining
    */
  def setCenter(position: Vector2): Rectangle = {
    setPosition(position.x - width / 2, position.y - height / 2)
    this
  }

  /** Fits this rectangle around another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the other rectangle (e.g. Having a camera translate and scale to show a
    * given area)
    * @param rect
    *   the other rectangle to fit this rectangle around
    * @return
    *   this rectangle for chaining
    * @see
    *   Scaling
    */
  def fitOutside(rect: Rectangle): Rectangle = {
    val ratio = aspectRatio

    if (ratio > rect.aspectRatio) {
      // Wider than tall
      setSize(rect.height * ratio, rect.height)
    } else {
      // Taller than wide
      setSize(rect.width, rect.width / ratio)
    }

    setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2)
    this
  }

  /** Fits this rectangle into another rectangle while maintaining aspect ratio. This scales and centers the rectangle to the other rectangle (e.g. Scaling a texture within a arbitrary cell without
    * squeezing)
    * @param rect
    *   the other rectangle to fit this rectangle inside
    * @return
    *   this rectangle for chaining
    * @see
    *   Scaling
    */
  def fitInside(rect: Rectangle): Rectangle = {
    val ratio = aspectRatio

    if (ratio < rect.aspectRatio) {
      // Taller than wide
      setSize(rect.height * ratio, rect.height)
    } else {
      // Wider than tall
      setSize(rect.width, rect.width / ratio)
    }

    setPosition((rect.x + rect.width / 2) - width / 2, (rect.y + rect.height / 2) - height / 2)
    this
  }

  /** Converts this {@code Rectangle} to a string in the format {@code [x,y,width,height]} .
    * @return
    *   a string representation of this object.
    */
  override def toString: String =
    "[" + x + "," + y + "," + width + "," + height + "]"

  /** Sets this {@code Rectangle} to the value represented by the specified string according to the format of {@link #toString()}.
    * @param v
    *   the string.
    * @return
    *   this rectangle for chaining
    */
  def fromString(v: String): Rectangle = {
    val s0 = v.indexOf(',', 1)
    val s1 = v.indexOf(',', s0 + 1)
    val s2 = v.indexOf(',', s1 + 1)
    if (s0 != -1 && s1 != -1 && s2 != -1 && v.charAt(0) == '[' && v.charAt(v.length - 1) == ']') {
      try {
        val x      = java.lang.Float.parseFloat(v.substring(1, s0))
        val y      = java.lang.Float.parseFloat(v.substring(s0 + 1, s1))
        val width  = java.lang.Float.parseFloat(v.substring(s1 + 1, s2))
        val height = java.lang.Float.parseFloat(v.substring(s2 + 1, v.length - 1))
        this.set(x, y, width, height)
      } catch {
        case _: NumberFormatException =>
          throw utils.SgeError.MathError(s"Malformed Rectangle: $v")
      }
    } else throw utils.SgeError.MathError(s"Malformed Rectangle: $v")
  }

  def area(): Float =
    this.width * this.height

  def perimeter(): Float =
    2 * (this.width + this.height)

  override def hashCode(): Int = {
    val prime  = 31
    var result = 1
    result = prime * result + java.lang.Float.floatToRawIntBits(height)
    result = prime * result + java.lang.Float.floatToRawIntBits(width)
    result = prime * result + java.lang.Float.floatToRawIntBits(x)
    result = prime * result + java.lang.Float.floatToRawIntBits(y)
    result
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: Rectangle =>
      (this eq other) ||
      (java.lang.Float.floatToRawIntBits(height) == java.lang.Float.floatToRawIntBits(other.height) &&
        java.lang.Float.floatToRawIntBits(width) == java.lang.Float.floatToRawIntBits(other.width) &&
        java.lang.Float.floatToRawIntBits(x) == java.lang.Float.floatToRawIntBits(other.x) &&
        java.lang.Float.floatToRawIntBits(y) == java.lang.Float.floatToRawIntBits(other.y))
    case _ => false
  }
}

object Rectangle {

  /** Static temporary rectangle. Use with care! Use only when sure other code will not also use this. */
  val tmp = Rectangle()

  /** Static temporary rectangle. Use with care! Use only when sure other code will not also use this. */
  val tmp2 = Rectangle()
}
