package sge
package math

/** Encapsulates a general vector. Allows chaining operations by returning a reference to itself in all modification methods. See {@link Vector2} and {@link Vector3} for specific implementations.
  * @author
  *   Xoppa (original implementation)
  */
sealed trait Vector[T <: Vector[T]] { self: T =>

  /** @return The Euclidean length */
  final def length: Float = Math.sqrt(lengthSq).toFloat

  /** This method is faster than {@link Vector#len()} because it avoids calculating a square root. It is useful for comparisons, but not for getting exact lengths, as the return value is the square of
    * the actual length.
    * @return
    *   The squared Euclidean length
    */
  def lengthSq: Float

  /** Limits the length of this vector, based on the desired maximum length.
    * @param limit
    *   desired maximum length for this vector
    * @return
    *   this vector for chaining
    */
  final def limit(limit: Float): this.type = limitSq(limit * limit)

  /** Limits the length of this vector, based on the desired maximum length squared. <p /> This method is slightly faster than limit().
    * @param limitSq
    *   squared desired maximum length for this vector
    * @return
    *   this vector for chaining
    * @see
    *   #len2()
    */
  final def limitSq(limitSq: Float): this.type = {
    val lenSq = lengthSq
    if (lenSq > limitSq) scale(Math.sqrt(limitSq / lenSq).toFloat)
    else this
  }

  /** Sets the length of this vector. Does nothing if this vector is zero.
    * @param length
    *   desired length for this vector
    * @return
    *   this vector for chaining
    */
  final def setLength(length: Float): this.type = setLengthSq(length * length)

  /** Sets the length of this vector, based on the square of the desired length. Does nothing if this vector is zero. <p /> This method is slightly faster than setLength().
    * @param lengthSq
    *   desired square of the length for this vector
    * @return
    *   this vector for chaining
    * @see
    *   #len2()
    */
  final def setLengthSq(lengthSq: Float): this.type = {
    val oldLenSq = lengthSq
    if (oldLenSq == 0 || oldLenSq == lengthSq) this else scale(Math.sqrt(lengthSq / oldLenSq).toFloat)
  }

  /** Scales this vector by a scalar
    * @param scalar
    *   The scalar
    * @return
    *   This vector for chaining
    */
  def scale(scalar: Float): this.type

  def downScale(scalar: Float): this.type

  /** Clamps this vector's length to given min and max values
    * @param min
    *   Min length
    * @param max
    *   Max length
    * @return
    *   This vector for chaining
    */
  def clamp(min: Float, max: Float): this.type

  /** Normalizes this vector. Does nothing if it is zero.
    * @return
    *   This vector for chaining
    */
  final def normalize(): this.type = {
    val len = length
    if (len != 1) downScale(len) // originally it was 0, but it makes no sense
    else this
  }

  /** Sets this vector to the unit vector with a random direction
    * @return
    *   This vector for chaining
    */
  def setToRandomDirection(): this.type

  /** @return Whether this vector is a unit length vector */
  final def isUnit: Boolean = isUnit(0.000000001f)

  /** @return Whether this vector is a unit length vector within the given margin. */
  final def isUnit(margin: Float): Boolean = Math.abs(lengthSq - 1f) < margin

  /** @return Whether this vector is a zero vector */
  def isZero: Boolean

  /** @return Whether the length of this vector is smaller than the given margin */
  def isZero(margin: Float): Boolean

  /** Sets the components of this vector to 0
    * @return
    *   This vector for chaining
    */
  def setZero(): this.type

  // Methods moved from Vector.Ops

  /** @return a copy of this vector */
  def copy: T

  /** Sets this vector from the given vector
    * @param v
    *   The vector
    * @return
    *   This vector for chaining
    */
  def set(v: T): this.type

  /** Subtracts the given vector from this vector.
    * @param v
    *   The vector
    * @return
    *   This vector for chaining
    */
  def -(v: T): this.type

  /** Adds the given vector to this vector
    * @param v
    *   The vector
    * @return
    *   This vector for chaining
    */
  def +(v: T): this.type

  /** @param v
    *   The other vector
    * @return
    *   The dot product between this and the other vector
    */
  def dot(v: T): Float

  /** Scales this vector by another vector
    * @return
    *   This vector for chaining
    */
  def scale(v: T): this.type

  /** @param v
    *   The other vector
    * @return
    *   the distance between this and the other vector
    */
  final def distance(v: T): Float = Math.sqrt(distanceSq(v)).toFloat

  /** This method is faster than {@link Vector#dst(Vector)} because it avoids calculating a square root. It is useful for comparisons, but not for getting accurate distances, as the return value is
    * the square of the actual distance.
    * @param v
    *   The other vector
    * @return
    *   the squared distance between this and the other vector
    */
  def distanceSq(v: T): Float

  /** Linearly interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is stored in this vector.
    * @param target
    *   The target vector
    * @param alpha
    *   The interpolation coefficient
    * @return
    *   This vector for chaining.
    */
  def lerp(target: T, alpha: Float): this.type

  /** Interpolates between this vector and the given target vector by alpha (within range [0,1]) using the given Interpolation method. the result is stored in this vector.
    * @param target
    *   The target vector
    * @param alpha
    *   The interpolation coefficient
    * @param interpolator
    *   An Interpolation object describing the used interpolation method
    * @return
    *   This vector for chaining.
    */
  def interpolate(target: T, alpha: Float, interpolator: Interpolation): this.type

  /** @return true if this vector is in line with the other vector (either in the same or the opposite direction) */
  def isOnLine(other: T)(using Epsilon): Boolean

  /** @return
    *   true if this vector is collinear with the other vector ({@link #isOnLine(Vector, float)} && {@link #hasSameDirection(Vector)} ).
    */
  final def isCollinear(other: T)(using Epsilon): Boolean =
    isOnLine(other) && dot(other) > 0f

  /** @return
    *   true if this vector is opposite collinear with the other vector ({@link #isOnLine(Vector, float)} && {@link #hasOppositeDirection(Vector)} ).
    */
  final def isCollinearOpposite(other: T)(using Epsilon): Boolean =
    isOnLine(other) && dot(other) < 0f

  /** @return
    *   Whether this vector is perpendicular with the other vector. True if the dot product is 0.
    * @param epsilon
    *   a positive small number close to zero
    */
  final def isPerpendicular(other: T)(using Epsilon): Boolean = MathUtils.isZero(dot(other), Epsilon())

  /** @return
    *   Whether this vector has similar direction compared to the other vector. True if the normalized dot product is > 0.
    */
  final def hasSameDirection(other: T): Boolean = dot(other) > 0

  /** @return
    *   Whether this vector has opposite direction compared to the other vector. True if the normalized dot product is < 0.
    */
  final def hasOppositeDirection(other: T): Boolean = dot(other) < 0

  /** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
    * @param other
    * @param epsilon
    * @return
    *   whether the vectors have fuzzy equality.
    */
  def epsilonEquals(other: T)(using Epsilon): Boolean

  /** First scale a supplied vector, then add it to this vector.
    * @param v
    *   addition vector
    * @param scalar
    *   for scaling the addition vector
    */
  def mulAdd(v: T, scalar: Float): this.type

  /** First scale a supplied vector, then add it to this vector.
    * @param v
    *   addition vector
    * @param mulVec
    *   vector by whose values the addition vector will be scaled
    */
  def mulAdd(v: T, mulVec: T): this.type
}

final case class Vector2(var x: Float = 0, var y: Float = 0) extends Vector[Vector2] {

  def set(x: Float, y: Float): this.type = {
    this.x = x
    this.y = y
    this
  }

  override def lengthSq: Float = x * x + y * y

  override def clamp(min: Float, max: Float): this.type = {
    val lenSq = lengthSq
    if (lenSq == 0f) this
    else {
      val max2 = max * max
      if (lenSq > max2) scale(Math.sqrt(max2 / lenSq).toFloat)
      else {
        val min2 = min * min
        if (lenSq < min2) scale(Math.sqrt(min2 / lenSq).toFloat)
        else this
      }
    }
  }

  override def scale(scalar: Float): this.type = {
    x *= scalar
    y *= scalar
    this
  }

  override def downScale(scalar: Float): this.type = {
    x /= scalar
    y /= scalar
    this
  }

  override def setToRandomDirection(): this.type = {
    val theta = MathUtils.random(0f, MathUtils.PI2)
    set(MathUtils.cos(theta), MathUtils.sin(theta))
  }

  /** @return Whether this vector is a zero vector */
  override def isZero: Boolean = x == 0 && y == 0

  /** @return Whether the length of this vector is smaller than the given margin */
  override def isZero(margin: Float): Boolean = lengthSq < margin * margin

  override def setZero(): this.type = set(0, 0)

  def -(x: Float, y: Float): this.type = {
    this.x -= x
    this.y -= y
    this
  }

  def +(x: Float, y: Float): this.type = {
    this.x += x
    this.y += y
    this
  }

  /** Left-multiplies this vector by the given matrix
    * @param mat
    *   the matrix
    * @return
    *   this vector
    */
  def *(mat: Matrix3): this.type = {
    x = this.x * mat.values(0) + this.y * mat.values(3) + mat.values(6)
    y = this.x * mat.values(1) + this.y * mat.values(4) + mat.values(7)
    this
  }

  def dot(x: Float, y: Float): Float = this.x * x + this.y * y

  /** Calculates the 2D cross product between this and the given vector.
    * @param v
    *   the other vector
    * @return
    *   the cross product
    */
  def cross(v: Vector2): Float = x * v.y - y * v.x

  /** Calculates the 2D cross product between this and the given vector.
    * @param x
    *   the x-coordinate of the other vector
    * @param y
    *   the y-coordinate of the other vector
    * @return
    *   the cross product
    */
  def cross(x: Float, y: Float): Float = this.x * y - this.y * x

  /** @return
    *   the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis (typically counter-clockwise) and in the [0, 360) range.
    */
  def angleDeg(): Float = {
    var angle: Float = (Math.atan2(y, x) * MathUtils.radiansToDegrees).toFloat
    if (angle < 0) angle += 360
    angle
  }

  /** @return
    *   the angle in degrees of this vector (point) relative to the given vector. Angles are towards the positive y-axis (typically counter-clockwise.) in the [0, 360) range
    */
  def angleDeg(reference: Vector2): Float = {
    var angle: Float = (Math.atan2(reference.cross(this), reference.dot(this)) * MathUtils.radiansToDegrees).toFloat
    if (angle < 0) angle += 360
    angle
  }

  /** @return
    *   the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis (typically counter-clockwise) and in the [0, 360) range.
    */
  def angleDeg(x: Float, y: Float): Float = {
    var angle: Float = (Math.atan2(y, x) * MathUtils.radiansToDegrees).toFloat
    if (angle < 0) angle += 360
    angle
  }

  /** @return
    *   the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis. (typically counter-clockwise)
    */
  def angleRad(): Float =
    Math.atan2(y, x).toFloat

  /** @return
    *   the angle in radians of this vector (point) relative to the given vector. Angles are towards the positive y-axis. (typically counter-clockwise.)
    */
  def angleRad(reference: Vector2): Float =
    Math.atan2(reference.cross(this), reference.dot(this)).toFloat

  /** @return
    *   the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis. (typically counter-clockwise)
    */
  def angleRad(x: Float, y: Float): Float =
    Math.atan2(y, x).toFloat

  /** Sets the angle of the vector in degrees relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
    * @param degrees
    *   The angle in degrees to set.
    */
  def setAngleDeg(degrees: Float): this.type =
    setAngleRad(degrees * MathUtils.degreesToRadians)

  /** Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
    * @param radians
    *   The angle in radians to set.
    */
  def setAngleRad(radians: Float): this.type = {
    this.set(length, 0f)
    this.rotateRad(radians)
    this
  }

  /** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
    * @param degrees
    *   the angle in degrees
    */
  def rotateDeg(degrees: Float): this.type =
    rotateRad(degrees * MathUtils.degreesToRadians)

  /** Rotates the Vector2 by the given angle, counter-clockwise assuming the y-axis points up.
    * @param radians
    *   the angle in radians
    */
  def rotateRad(radians: Float): this.type = {
    val cos = Math.cos(radians).toFloat
    val sin = Math.sin(radians).toFloat

    val newX = this.x * cos - this.y * sin
    val newY = this.x * sin + this.y * cos

    this.x = newX
    this.y = newY

    this
  }

  /** Rotates the Vector2 by 90 degrees in the specified direction, where >= 0 is counter-clockwise and < 0 is clockwise. */
  def rotate90(dir: Int): this.type = {
    val x = this.x
    if (dir >= 0) {
      this.x = -y
      y = x
    } else {
      this.x = y
      y = -x
    }
    this
  }

  // Implementation of Vector methods

  override def copy: Vector2 = Vector2(x, y)

  override def set(v: Vector2): this.type = set(v.x, v.y)

  override def -(v: Vector2): this.type = this.-(v.x, v.y)

  override def +(v: Vector2): this.type = this.+(v.x, v.y)

  override def dot(v: Vector2): Float = dot(v.x, v.y)

  override def scale(v: Vector2): this.type = {
    x *= v.x
    y *= v.y
    this
  }

  override def distanceSq(v: Vector2): Float = {
    val dx = x - v.x
    val dy = y - v.y
    dx * dx + dy * dy
  }

  override def lerp(target: Vector2, alpha: Float): this.type = {
    val invAlpha = 1.0f - alpha
    x = (x * invAlpha) + (target.x * alpha)
    y = (y * invAlpha) + (target.y * alpha)
    this
  }

  override def interpolate(target: Vector2, alpha: Float, interpolator: Interpolation): this.type =
    lerp(target, interpolator.apply(0f, 1f, alpha))

  override def isOnLine(other: Vector2)(using Epsilon): Boolean = MathUtils.isZero(x * other.y - y * other.x, Epsilon())

  override def epsilonEquals(other: Vector2)(using Epsilon): Boolean =
    if Math.abs(x - other.x) > Epsilon() then false
    else if Math.abs(y - other.y) > Epsilon() then false
    else true

  def epsilonEquals(x: Float, y: Float)(using Epsilon): Boolean =
    if Math.abs(this.x - x) > Epsilon() then false
    else if Math.abs(this.y - y) > Epsilon() then false
    else true

  override def mulAdd(v: Vector2, scalar: Float): this.type = {
    x += v.x * scalar
    y += v.y * scalar
    this
  }

  override def mulAdd(v: Vector2, mulVec: Vector2): this.type = {
    x += v.x * mulVec.x
    y += v.y * mulVec.y
    this
  }

  /** Rotates the Vector2 by the given angle around reference vector, counter-clockwise assuming the y-axis points up.
    * @param degrees
    *   the angle in degrees
    * @param reference
    *   center Vector2
    */
  def rotateAroundDeg(reference: Vector2, degrees: Float): this.type =
    this.-(reference).rotateDeg(degrees).+(reference)

  /** Rotates the Vector2 by the given angle around reference vector, counter-clockwise assuming the y-axis points up.
    * @param radians
    *   the angle in radians
    * @param reference
    *   center Vector2
    */
  def rotateAroundRad(reference: Vector2, radians: Float): this.type =
    this.-(reference).rotateRad(radians).+(reference)
}

final case class Vector3(var x: Float = 0, var y: Float = 0, var z: Float = 0) extends Vector[Vector3] {

  def set(x: Float, y: Float, z: Float): this.type = {
    this.x = x
    this.y = y
    this.z = z
    this
  }

  def set(values: Array[Float]): this.type = set(values(0), values(1), values(2))

  def set(vector: Vector2, z: Float): this.type = set(vector.x, vector.y, z)

  def setFromSpherical(azimuthalAngle: Float, polarAngle: Float): this.type = {
    val cosPolar = MathUtils.cos(polarAngle)
    val sinPolar = MathUtils.sin(polarAngle)
    val cosAzim  = MathUtils.cos(azimuthalAngle)
    val sinAzim  = MathUtils.sin(azimuthalAngle)
    set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar)
  }

  override def lengthSq: Float = x * x + y * y + z * z

  override def scale(scalar: Float): this.type = {
    x *= scalar
    y *= scalar
    z *= scalar
    this
  }

  override def downScale(scalar: Float): this.type = {
    x /= scalar
    y /= scalar
    z /= scalar
    this
  }

  override def clamp(min: Float, max: Float): this.type = {
    val lenSq = lengthSq
    if (lenSq == 0f) this
    else {
      val max2 = max * max
      if (lenSq > max2) scale(Math.sqrt(max2 / lenSq).toFloat)
      else {
        val min2 = min * min
        if (lenSq < min2) scale(Math.sqrt(min2 / lenSq).toFloat)
        else this
      }
    }
  }

  override def setToRandomDirection(): this.type = {
    val u     = MathUtils.random()
    val v     = MathUtils.random()
    val theta = MathUtils.PI2 * u // azimuthal angle
    val phi   = Math.acos(2f * v - 1f).toFloat // polar angle
    setFromSpherical(theta, phi)
  }

  /** @return Whether this vector is a zero vector */
  override def isZero: Boolean = x == 0 && y == 0 && z == 0

  /** @return Whether the length of this vector is smaller than the given margin */
  override def isZero(margin: Float): Boolean = lengthSq < margin * margin

  override def setZero(): this.type = set(0, 0, 0)

  def -(x: Float, y: Float, z: Float): this.type = {
    this.x -= x
    this.y -= y
    this.z -= z
    this
  }

  def +(x: Float, y: Float, z: Float): this.type = {
    this.x += x
    this.y += y
    this.z += z
    this
  }

  def +(value: Float): this.type = {
    x += value
    y += value
    z += value
    this
  }

  def -(value: Float): this.type = {
    x -= value
    y -= value
    z -= value
    this
  }

  def scale(vx: Float, vy: Float, vz: Float): this.type = {
    x *= vx
    y *= vy
    z *= vz
    this
  }

  def dot(x: Float, y: Float, z: Float): Float = this.x * x + this.y * y + this.z * z

  def cross(vector: Vector3): this.type =
    set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)

  def cross(x: Float, y: Float, z: Float): this.type =
    set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x)

  def distance(x: Float, y: Float, z: Float): Float = {
    val a = x - this.x
    val b = y - this.y
    val c = z - this.z
    Math.sqrt(a * a + b * b + c * c).toFloat
  }

  def distanceSq(x: Float, y: Float, z: Float): Float = {
    val a = x - this.x
    val b = y - this.y
    val c = z - this.z
    a * a + b * b + c * c
  }

  def angleDeg(): Float = {
    var angle = (Math.atan2(y, x) * MathUtils.radiansToDegrees).toFloat
    if (angle < 0) angle += 360
    angle
  }

  def angleRad(): Float = Math.atan2(y, x).toFloat

  def setAngleDeg(degrees: Float): this.type = setAngleRad(degrees * MathUtils.degreesToRadians)

  def setAngleRad(radians: Float): this.type = {
    set(length, 0f, 0f)
    rotateRad(radians, 0f, 0f, 1f)
  }

  def rotateDeg(degrees: Float, axisX: Float, axisY: Float, axisZ: Float): this.type =
    rotateRad(degrees * MathUtils.degreesToRadians, axisX, axisY, axisZ)

  def rotateRad(radians: Float, axisX: Float, axisY: Float, axisZ: Float): this.type =
    // This would need Matrix3 implementation - placeholder for now
    this

  def sphericalLerp(target: Vector3, alpha: Float): this.type = {
    val dotProduct = dot(target.x, target.y, target.z)
    // If the inputs are too close for comfort, simply linearly interpolate.
    if (dotProduct > 0.9995 || dotProduct < -0.9995) {
      x += alpha * (target.x - x)
      y += alpha * (target.y - y)
      z += alpha * (target.z - z)
      return this
    }

    // theta0 = angle between input vectors
    val theta0 = Math.acos(dotProduct).toFloat
    // theta = angle between this vector and result
    val theta = theta0 * alpha

    val st = Math.sin(theta).toFloat
    val tx = target.x - x * dotProduct
    val ty = target.y - y * dotProduct
    val tz = target.z - z * dotProduct
    val l2 = tx * tx + ty * ty + tz * tz
    val dl = st * (if (l2 < 0.0001f) 1f else 1f / Math.sqrt(l2).toFloat)

    scale(Math.cos(theta).toFloat)
    x += tx * dl
    y += ty * dl
    z += tz * dl
    normalize()
  }

  override def toString: String = s"($x,$y,$z)"

  override def hashCode(): Int = {
    val prime  = 31
    var result = 1
    result = prime * result + java.lang.Float.floatToIntBits(x)
    result = prime * result + java.lang.Float.floatToIntBits(y)
    result = prime * result + java.lang.Float.floatToIntBits(z)
    result
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: Vector3 =>
      java.lang.Float.floatToIntBits(x) == java.lang.Float.floatToIntBits(other.x) &&
      java.lang.Float.floatToIntBits(y) == java.lang.Float.floatToIntBits(other.y) &&
      java.lang.Float.floatToIntBits(z) == java.lang.Float.floatToIntBits(other.z)
    case _ => false
  }

  def epsilonEquals(x: Float, y: Float, z: Float, epsilon: Float): Boolean =
    if (Math.abs(x - this.x) > epsilon) false
    else if (Math.abs(y - this.y) > epsilon) false
    else if (Math.abs(z - this.z) > epsilon) false
    else true

  @annotation.targetName("epsilonEqualsImplicit")
  def epsilonEquals(x: Float, y: Float, z: Float)(using Epsilon): Boolean =
    epsilonEquals(x, y, z, Epsilon())

  // Implementation of Vector methods

  override def copy: Vector3 = Vector3(x, y, z)

  override def set(v: Vector3): this.type = set(v.x, v.y, v.z)

  override def -(v: Vector3): this.type = this.-(v.x, v.y, v.z)

  override def +(v: Vector3): this.type = this.+(v.x, v.y, v.z)

  override def dot(v: Vector3): Float = dot(v.x, v.y, v.z)

  override def scale(v: Vector3): this.type = {
    x *= v.x
    y *= v.y
    z *= v.z
    this
  }

  override def distanceSq(v: Vector3): Float = {
    val dx = x - v.x
    val dy = y - v.y
    val dz = z - v.z
    dx * dx + dy * dy + dz * dz
  }

  override def lerp(target: Vector3, alpha: Float): this.type = {
    x += alpha * (target.x - x)
    y += alpha * (target.y - y)
    z += alpha * (target.z - z)
    this
  }

  override def interpolate(target: Vector3, alpha: Float, interpolator: Interpolation): this.type =
    lerp(target, interpolator.apply(0f, 1f, alpha))

  override def isOnLine(other: Vector3)(using Epsilon): Boolean = {
    val crossX = y * other.z - z * other.y
    val crossY = z * other.x - x * other.z
    val crossZ = x * other.y - y * other.x
    (crossX * crossX + crossY * crossY + crossZ * crossZ) <= (Epsilon() * Epsilon())
  }

  override def epsilonEquals(other: Vector3)(using Epsilon): Boolean =
    if (Math.abs(x - other.x) > Epsilon()) false
    else if (Math.abs(y - other.y) > Epsilon()) false
    else if (Math.abs(z - other.z) > Epsilon()) false
    else true

  override def mulAdd(v: Vector3, scalar: Float): this.type = {
    x += v.x * scalar
    y += v.y * scalar
    z += v.z * scalar
    this
  }

  override def mulAdd(v: Vector3, mulVec: Vector3): this.type = {
    x += v.x * mulVec.x
    y += v.y * mulVec.y
    z += v.z * mulVec.z
    this
  }

  def rotateAroundDeg(axis: Vector3, degrees: Float): this.type =
    rotateAroundRad(axis, degrees * MathUtils.degreesToRadians)

  def rotateAroundRad(axis: Vector3, radians: Float): this.type =
    // This would need proper quaternion/matrix implementation
    // Placeholder for now
    this

  // Alias methods for compatibility with existing code
  def nor():                                    this.type = normalize()
  def crs(vector: Vector3):                     this.type = cross(vector)
  def crs(x:      Float, y:  Float, z:  Float): this.type = cross(x, y, z)
  def scl(scalar: Float):                       this.type = scale(scalar)
  def scl(vx:     Float, vy: Float, vz: Float): this.type = scale(vx, vy, vz)
  def sub(v:      Vector3):                     this.type = this.-(v)
  def sub(x:      Float, y:  Float, z:  Float): this.type = this.-(x, y, z)
  def add(v:      Vector3):                     this.type = this.+(v)
  def add(x:      Float, y:  Float, z:  Float): this.type = this.+(x, y, z)

  // Matrix multiplication methods
  def mul(matrix: Matrix4): this.type = {
    val newX = x * matrix.values(Matrix4.M00) + y * matrix.values(Matrix4.M01) + z * matrix.values(Matrix4.M02) + matrix.values(Matrix4.M03)
    val newY = x * matrix.values(Matrix4.M10) + y * matrix.values(Matrix4.M11) + z * matrix.values(Matrix4.M12) + matrix.values(Matrix4.M13)
    val newZ = x * matrix.values(Matrix4.M20) + y * matrix.values(Matrix4.M21) + z * matrix.values(Matrix4.M22) + matrix.values(Matrix4.M23)
    set(newX, newY, newZ)
  }
}

object Vector3 {
  val X:    Vector3 = Vector3(1, 0, 0)
  val Y:    Vector3 = Vector3(0, 1, 0)
  val Z:    Vector3 = Vector3(0, 0, 1)
  val Zero: Vector3 = Vector3(0, 0, 0)

  def length(x: Float, y: Float, z: Float): Float = Math.sqrt(x * x + y * y + z * z).toFloat

  def lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z

  def distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float = {
    val a = x2 - x1
    val b = y2 - y1
    val c = z2 - z1
    Math.sqrt(a * a + b * b + c * c).toFloat
  }

  def distanceSq(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float = {
    val a = x2 - x1
    val b = y2 - y1
    val c = z2 - z1
    a * a + b * b + c * c
  }

  def dot(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float =
    x1 * x2 + y1 * y2 + z1 * z2
}

/** Encapsulates a 4D vector. Allows chaining operations by returning a reference to itself in all modification methods.
  * @author
  *   badlogicgames@gmail.com (original implementation)
  * @author
  *   Antz (original implementation)
  */
final case class Vector4(var x: Float = 0, var y: Float = 0, var z: Float = 0, var w: Float = 0) extends Vector[Vector4] {

  /** Sets the vector to the given components
    * @param x
    *   The x-component
    * @param y
    *   The y-component
    * @param z
    *   The z-component
    * @param w
    *   The w-component
    * @return
    *   this vector for chaining
    */
  def set(x: Float, y: Float, z: Float, w: Float): this.type = {
    this.x = x
    this.y = y
    this.z = z
    this.w = w
    this
  }

  override def set(vector: Vector4): this.type =
    this.set(vector.x, vector.y, vector.z, vector.w)

  /** Sets the components from the array. The array must have at least 4 elements
    * @param values
    *   The array
    * @return
    *   this vector for chaining
    */
  def set(values: Array[Float]): this.type =
    this.set(values(0), values(1), values(2), values(3))

  /** Sets the components to the given Vector2, z-component and w-component
    * @param vector
    *   The vector2 holding the x- and y-components
    * @param z
    *   The z-component
    * @param w
    *   The w-component
    * @return
    *   This vector for chaining
    */
  def set(vector: Vector2, z: Float, w: Float): this.type =
    this.set(vector.x, vector.y, z, w)

  /** Sets the components of the given vector3 and w-component
    * @param vector
    *   The vector
    * @param w
    *   The w-component
    * @return
    *   This vector for chaining
    */
  def set(vector: Vector3, w: Float): this.type =
    this.set(vector.x, vector.y, vector.z, w)

  override def lengthSq: Float = x * x + y * y + z * z + w * w

  override def scale(scalar: Float): this.type = {
    x *= scalar
    y *= scalar
    z *= scalar
    w *= scalar
    this
  }

  override def downScale(scalar: Float): this.type = {
    x /= scalar
    y /= scalar
    z /= scalar
    w /= scalar
    this
  }

  override def clamp(min: Float, max: Float): this.type = {
    val lenSq = lengthSq
    if (lenSq == 0f) this
    else {
      val max2 = max * max
      if (lenSq > max2) scale(Math.sqrt(max2 / lenSq).toFloat)
      else {
        val min2 = min * min
        if (lenSq < min2) scale(Math.sqrt(min2 / lenSq).toFloat)
        else this
      }
    }
  }

  override def setToRandomDirection(): this.type = {
    // The algorithm here is #19 at
    // https://extremelearning.com.au/how-to-generate-uniformly-random-points-on-n-spheres-and-n-balls/ .
    // It is the only recommended way to randomly generate a point on the surface of the unit 4D hypersphere.

    // From the documentation of Random.nextGaussian(), but using float math.
    var v1, v2, s, multiplier = 0f

    // First pair using while loop instead of do-while
    v1 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
    v2 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
    s = v1 * v1 + v2 * v2
    while (s >= 1 || s == 0) {
      v1 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
      v2 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
      s = v1 * v1 + v2 * v2
    }
    multiplier = Math.sqrt(-2 * Math.log(s) / s).toFloat
    x = v1 * multiplier
    y = v2 * multiplier

    // Each run of the Marsaglia polar method produces two normal-distributed variates.
    // Second pair using while loop instead of do-while
    v1 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
    v2 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
    s = v1 * v1 + v2 * v2
    while (s >= 1 || s == 0) {
      v1 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
      v2 = (MathUtils.random() - 0.5f) * 2 // between -1.0 and 1.0
      s = v1 * v1 + v2 * v2
    }
    multiplier = Math.sqrt(-2 * Math.log(s) / s).toFloat
    z = v1 * multiplier
    w = v2 * multiplier
    // Once we normalize four normal-distributed floats, we have a point on the unit hypersphere's surface.
    this.normalize()
  }

  /** @return Whether this vector is a zero vector */
  override def isZero: Boolean = x == 0 && y == 0 && z == 0 && w == 0

  /** @return Whether the length of this vector is smaller than the given margin */
  override def isZero(margin: Float): Boolean = lengthSq < margin * margin

  override def setZero(): this.type = set(0, 0, 0, 0)

  override def copy: Vector4 = Vector4(x, y, z, w)

  override def +(vector: Vector4): this.type =
    this.+(vector.x, vector.y, vector.z, vector.w)

  /** Adds the given components to this vector
    * @param x
    *   Added to the x-component
    * @param y
    *   Added to the y-component
    * @param z
    *   Added to the z-component
    * @param w
    *   Added to the w-component
    * @return
    *   This vector for chaining.
    */
  def +(x: Float, y: Float, z: Float, w: Float): this.type = {
    this.x += x
    this.y += y
    this.z += z
    this.w += w
    this
  }

  /** Adds the given value to all four components of the vector.
    * @param values
    *   The value
    * @return
    *   This vector for chaining
    */
  def +(values: Float): this.type = {
    this.x += values
    this.y += values
    this.z += values
    this.w += values
    this
  }

  override def -(vector: Vector4): this.type =
    this.-(vector.x, vector.y, vector.z, vector.w)

  /** Subtracts the given components from this vector.
    * @param x
    *   Subtracted from the x-component
    * @param y
    *   Subtracted from the y-component
    * @param z
    *   Subtracted from the z-component
    * @param w
    *   Subtracted from the w-component
    * @return
    *   This vector for chaining
    */
  def -(x: Float, y: Float, z: Float, w: Float): this.type = {
    this.x -= x
    this.y -= y
    this.z -= z
    this.w -= w
    this
  }

  /** Subtracts the given value from all components of this vector
    * @param value
    *   The value
    * @return
    *   This vector for chaining
    */
  def -(value: Float): this.type = {
    this.x -= value
    this.y -= value
    this.z -= value
    this.w -= value
    this
  }

  /** Scales this vector by the given values
    * @param vx
    *   Multiplied with the X value
    * @param vy
    *   Multiplied with the Y value
    * @param vz
    *   Multiplied with the Z value
    * @param vw
    *   Multiplied with the W value
    * @return
    *   This vector for chaining
    */
  def scale(vx: Float, vy: Float, vz: Float, vw: Float): this.type = {
    x *= vx
    y *= vy
    z *= vz
    w *= vw
    this
  }

  override def scale(v: Vector4): this.type = {
    x *= v.x
    y *= v.y
    z *= v.z
    w *= v.w
    this
  }

  override def dot(v: Vector4): Float = dot(v.x, v.y, v.z, v.w)

  def dot(x: Float, y: Float, z: Float, w: Float): Float =
    this.x * x + this.y * y + this.z * z + this.w * w

  override def distanceSq(v: Vector4): Float = {
    val dx = x - v.x
    val dy = y - v.y
    val dz = z - v.z
    val dw = w - v.w
    dx * dx + dy * dy + dz * dz + dw * dw
  }

  def distanceSq(x: Float, y: Float, z: Float, w: Float): Float = {
    val a = x - this.x
    val b = y - this.y
    val c = z - this.z
    val d = w - this.w
    a * a + b * b + c * c + d * d
  }

  def distance(x: Float, y: Float, z: Float, w: Float): Float =
    Math.sqrt(distanceSq(x, y, z, w)).toFloat

  override def lerp(target: Vector4, alpha: Float): this.type = {
    x += alpha * (target.x - x)
    y += alpha * (target.y - y)
    z += alpha * (target.z - z)
    w += alpha * (target.w - w)
    this
  }

  override def interpolate(target: Vector4, alpha: Float, interpolator: Interpolation): this.type =
    lerp(target, interpolator.apply(0f, 1f, alpha))

  override def isOnLine(other: Vector4)(using Epsilon): Boolean = {
    // For 4D vectors, we need to check if the cross products of all 2D projections are zero
    // This is a simplified implementation - a full 4D collinearity test is more complex
    val epsilon = Epsilon()
    val len1Sq  = lengthSq
    val len2Sq  = other.lengthSq
    if (len1Sq == 0f || len2Sq == 0f) return true

    val dotProd       = dot(other)
    val expectedDotSq = len1Sq * len2Sq
    Math.abs(dotProd * dotProd - expectedDotSq) <= epsilon * epsilon * expectedDotSq
  }

  override def epsilonEquals(other: Vector4)(using Epsilon): Boolean = {
    val epsilon = Epsilon()
    if (Math.abs(x - other.x) > epsilon) false
    else if (Math.abs(y - other.y) > epsilon) false
    else if (Math.abs(z - other.z) > epsilon) false
    else if (Math.abs(w - other.w) > epsilon) false
    else true
  }

  def epsilonEquals(x: Float, y: Float, z: Float, w: Float)(using Epsilon): Boolean = {
    val epsilon = Epsilon()
    if (Math.abs(this.x - x) > epsilon) false
    else if (Math.abs(this.y - y) > epsilon) false
    else if (Math.abs(this.z - z) > epsilon) false
    else if (Math.abs(this.w - w) > epsilon) false
    else true
  }

  override def mulAdd(v: Vector4, scalar: Float): this.type = {
    x += v.x * scalar
    y += v.y * scalar
    z += v.z * scalar
    w += v.w * scalar
    this
  }

  override def mulAdd(v: Vector4, mulVec: Vector4): this.type = {
    x += v.x * mulVec.x
    y += v.y * mulVec.y
    z += v.z * mulVec.z
    w += v.w * mulVec.w
    this
  }

  /** Returns true if this vector and the vector parameter have identical components.
    * @param vector
    *   The other vector
    * @return
    *   Whether this and the other vector are equal with exact precision
    */
  def idt(vector: Vector4): Boolean =
    x == vector.x && y == vector.y && z == vector.z && w == vector.w

  override def toString: String = s"($x,$y,$z,$w)"

  override def hashCode(): Int = {
    val prime  = 31
    var result = 1
    result = prime * result + java.lang.Float.floatToIntBits(x)
    result = prime * result + java.lang.Float.floatToIntBits(y)
    result = prime * result + java.lang.Float.floatToIntBits(z)
    result = prime * result + java.lang.Float.floatToIntBits(w)
    result
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: Vector4 =>
      java.lang.Float.floatToIntBits(x) == java.lang.Float.floatToIntBits(other.x) &&
      java.lang.Float.floatToIntBits(y) == java.lang.Float.floatToIntBits(other.y) &&
      java.lang.Float.floatToIntBits(z) == java.lang.Float.floatToIntBits(other.z) &&
      java.lang.Float.floatToIntBits(w) == java.lang.Float.floatToIntBits(other.w)
    case _ => false
  }
}

object Vector4 {
  val X:    Vector4 = Vector4(1, 0, 0, 0)
  val Y:    Vector4 = Vector4(0, 1, 0, 0)
  val Z:    Vector4 = Vector4(0, 0, 1, 0)
  val W:    Vector4 = Vector4(0, 0, 0, 1)
  val Zero: Vector4 = Vector4(0, 0, 0, 0)

  def length(x: Float, y: Float, z: Float, w: Float): Float = Math.sqrt(x * x + y * y + z * z + w * w).toFloat

  def lengthSq(x: Float, y: Float, z: Float, w: Float): Float = x * x + y * y + z * z + w * w

  def distance(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float = {
    val a = x2 - x1
    val b = y2 - y1
    val c = z2 - z1
    val d = w2 - w1
    Math.sqrt(a * a + b * b + c * c + d * d).toFloat
  }

  def distanceSq(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float = {
    val a = x2 - x1
    val b = y2 - y1
    val c = z2 - z1
    val d = w2 - w1
    a * a + b * b + c * c + d * d
  }

  def dot(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float =
    x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2
}
