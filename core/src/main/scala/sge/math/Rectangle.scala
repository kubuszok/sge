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

  /** @return the x-coordinate of the bottom left corner */
  def getX(): Float = x

  /** Sets the x-coordinate of the bottom left corner
    * @param x
    *   The x-coordinate
    * @return
    *   this rectangle for chaining
    */
  def setX(x: Float): Rectangle = {
    this.x = x
    this
  }

  /** @return the y-coordinate of the bottom left corner */
  def getY(): Float = y

  /** Sets the y-coordinate of the bottom left corner
    * @param y
    *   The y-coordinate
    * @return
    *   this rectangle for chaining
    */
  def setY(y: Float): Rectangle = {
    this.y = y
    this
  }

  /** @return the width */
  def getWidth(): Float = width

  /** Sets the width of this rectangle
    * @param width
    *   The width
    * @return
    *   this rectangle for chaining
    */
  def setWidth(width: Float): Rectangle = {
    this.width = width
    this
  }

  /** @return the height */
  def getHeight(): Float = height

  /** Sets the height of this rectangle
    * @param height
    *   The height
    * @return
    *   this rectangle for chaining
    */
  def setHeight(height: Float): Rectangle = {
    this.height = height
    this
  }

  /** return the Vector2 with coordinates of this rectangle
    * @param position
    *   The Vector2
    */
  def getPosition(position: Vector2): Vector2 =
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
  def getSize(size: Vector2): Vector2 =
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
  def getAspectRatio(): Float =
    if (height == 0) Float.NaN else width / height

  /** Calculates the center of the rectangle. Results are located in the given Vector2
    * @param vector
    *   the Vector2 to use
    * @return
    *   the given vector with results stored inside
    */
  def getCenter(vector: Vector2): Vector2 = {
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
    val ratio = getAspectRatio()

    if (ratio > rect.getAspectRatio()) {
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
    val ratio = getAspectRatio()

    if (ratio < rect.getAspectRatio()) {
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

  override def equals(obj: Any): Boolean = {
    if (this == obj) return true
    if (obj == null) return false
    if (getClass != obj.getClass) return false
    val other = obj.asInstanceOf[Rectangle]
    if (java.lang.Float.floatToRawIntBits(height) != java.lang.Float.floatToRawIntBits(other.height)) return false
    if (java.lang.Float.floatToRawIntBits(width) != java.lang.Float.floatToRawIntBits(other.width)) return false
    if (java.lang.Float.floatToRawIntBits(x) != java.lang.Float.floatToRawIntBits(other.x)) return false
    if (java.lang.Float.floatToRawIntBits(y) != java.lang.Float.floatToRawIntBits(other.y)) return false
    true
  }
}

object Rectangle {

  /** Static temporary rectangle. Use with care! Use only when sure other code will not also use this. */
  val tmp = new Rectangle()

  /** Static temporary rectangle. Use with care! Use only when sure other code will not also use this. */
  val tmp2 = new Rectangle()
}
