package sge
package math

/** A convenient 2D ellipse class, based on the circle class
  * @author
  *   tonyp7 (original implementation)
  */
class Ellipse() extends Shape2D {

  var x:      Float = 0f
  var y:      Float = 0f
  var width:  Float = 0f
  var height: Float = 0f

  /** Copy constructor
    * @param ellipse
    *   Ellipse to construct a copy of.
    */
  def this(ellipse: Ellipse) = {
    this()
    this.x = ellipse.x
    this.y = ellipse.y
    this.width = ellipse.width
    this.height = ellipse.height
  }

  /** Constructs a new ellipse
    * @param x
    *   X coordinate of the center of the ellipse
    * @param y
    *   Y coordinate of the center of the ellipse
    * @param width
    *   the width of the ellipse
    * @param height
    *   the height of the ellipse
    */
  def this(x: Float, y: Float, width: Float, height: Float) = {
    this()
    this.x = x
    this.y = y
    this.width = width
    this.height = height
  }

  /** Constructs a new ellipse
    * @param position
    *   Position vector of the center of the ellipse
    * @param width
    *   the width of the ellipse
    * @param height
    *   the height of the ellipse
    */
  def this(position: Vector2, width: Float, height: Float) = {
    this()
    this.x = position.x
    this.y = position.y
    this.width = width
    this.height = height
  }

  /** Constructs a new ellipse
    * @param position
    *   Position vector of the center of the ellipse
    * @param size
    *   Size vector
    */
  def this(position: Vector2, size: Vector2) = {
    this()
    this.x = position.x
    this.y = position.y
    this.width = size.x
    this.height = size.y
  }

  /** Constructs a new {@link Ellipse} from the position and radius of a {@link Circle} (since circles are special cases of ellipses).
    * @param circle
    *   The circle to take the values of
    */
  def this(circle: Circle) = {
    this()
    this.x = circle.x
    this.y = circle.y
    this.width = circle.radius * 2f
    this.height = circle.radius * 2f
  }

  /** Checks whether or not this ellipse contains the given point.
    * @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    * @return
    *   true if this ellipse contains the given point; false otherwise.
    */
  def contains(x: Float, y: Float): Boolean = {
    val dx = x - this.x
    val dy = y - this.y
    (dx * dx) / (width * 0.5f * width * 0.5f) + (dy * dy) / (height * 0.5f * height * 0.5f) <= 1.0f
  }

  /** Checks whether or not this ellipse contains the given point.
    * @param point
    *   Position vector
    * @return
    *   true if this ellipse contains the given point; false otherwise.
    */
  def contains(point: Vector2): Boolean =
    contains(point.x, point.y)

  /** Sets a new position and size for this ellipse.
    * @param x
    *   X coordinate of the center of the ellipse
    * @param y
    *   Y coordinate of the center of the ellipse
    * @param width
    *   the width of the ellipse
    * @param height
    *   the height of the ellipse
    */
  def set(x: Float, y: Float, width: Float, height: Float): Unit = {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
  }

  /** Sets a new position and size for this ellipse based upon another ellipse.
    * @param ellipse
    *   The ellipse to copy the position and size of.
    */
  def set(ellipse: Ellipse): Unit = {
    x = ellipse.x
    y = ellipse.y
    width = ellipse.width
    height = ellipse.height
  }

  def set(circle: Circle): Unit = {
    this.x = circle.x
    this.y = circle.y
    this.width = circle.radius * 2f
    this.height = circle.radius * 2f
  }

  def set(position: Vector2, size: Vector2): Unit = {
    this.x = position.x
    this.y = position.y
    this.width = size.x
    this.height = size.y
  }

  /** Sets the x and y-coordinates of ellipse center from a {@link Vector2} .
    * @param position
    *   The position vector of the center of the ellipse
    * @return
    *   this ellipse for chaining
    */
  def setPosition(position: Vector2): Ellipse = {
    this.x = position.x
    this.y = position.y
    this
  }

  /** Sets the x and y-coordinates of ellipse center
    * @param x
    *   The x-coordinate of the center of the ellipse
    * @param y
    *   The y-coordinate of the center of the ellipse
    * @return
    *   this ellipse for chaining
    */
  def setPosition(x: Float, y: Float): Ellipse = {
    this.x = x
    this.y = y
    this
  }

  /** Sets the width and height of this ellipse
    * @param width
    *   The width
    * @param height
    *   The height
    * @return
    *   this ellipse for chaining
    */
  def setSize(width: Float, height: Float): Ellipse = {
    this.width = width
    this.height = height
    this
  }

  /** @return The area of this {@link Ellipse} as {@link MathUtils#PI} * {@link Ellipse#width} * {@link Ellipse#height} */
  def area(): Float =
    MathUtils.PI * (this.width * this.height) / 4

  /** Approximates the circumference of this {@link Ellipse} . Oddly enough, the circumference of an ellipse is actually difficult to compute exactly.
    * @return
    *   The Ramanujan approximation to the circumference of an ellipse if one dimension is at least three times longer than the other, else the simpler approximation
    */
  def circumference(): Float = {
    val a = this.width / 2
    val b = this.height / 2
    if (a * 3 > b || b * 3 > a) {
      // If one dimension is three times as long as the other...
      (MathUtils.PI * ((3 * (a + b)) - Math.sqrt((3 * a + b) * (a + 3 * b)))).toFloat
    } else {
      // We can use the simpler approximation, then
      (MathUtils.PI2 * Math.sqrt((a * a + b * b) / 2)).toFloat
    }
  }

  /** Returns a {@link String} representation of this {@link Ellipse} of the form {@code [x,y,width,height]}. */
  override def toString: String =
    "[" + x + "," + y + "," + width + "," + height + "]"

  override def equals(o: Any): Boolean = {
    if (o == this) return true
    if (o == null || o.getClass != this.getClass) return false
    val e = o.asInstanceOf[Ellipse]
    this.x == e.x && this.y == e.y && this.width == e.width && this.height == e.height
  }

  override def hashCode(): Int = {
    val prime  = 53
    var result = 1
    result = prime * result + java.lang.Float.floatToRawIntBits(this.height)
    result = prime * result + java.lang.Float.floatToRawIntBits(this.width)
    result = prime * result + java.lang.Float.floatToRawIntBits(this.x)
    result = prime * result + java.lang.Float.floatToRawIntBits(this.y)
    result
  }
}
