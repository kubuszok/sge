/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Matrix3.java
 *                  com/badlogic/gdx/math/Matrix4.java
 * Original authors: badlogicgames@gmail.com, mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package math

/** A 3x3 <a href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> matrix; useful for 2D transforms.
  *
  * @author
  *   mzechner (original implementation)
  */
class Matrix3 {
  val values:      Array[Float] = Array.ofDim(9)
  private val tmp: Array[Float] = Array.ofDim(9)

  /** Sets this matrix to the identity matrix
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def idt(): Matrix3 = {
    val values = this.values
    values(Matrix3.M00) = 1
    values(Matrix3.M10) = 0
    values(Matrix3.M20) = 0
    values(Matrix3.M01) = 0
    values(Matrix3.M11) = 1
    values(Matrix3.M21) = 0
    values(Matrix3.M02) = 0
    values(Matrix3.M12) = 0
    values(Matrix3.M22) = 1
    this
  }

  /** Postmultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
    *
    * <pre> A.mul(B) results in A := AB </pre>
    *
    * @param m
    *   Matrix to multiply by.
    * @return
    *   This matrix for the purpose of chaining operations together.
    */
  def mul(m: Matrix3): Matrix3 = {
    val values = this.values

    val v00 = values(Matrix3.M00) * m.values(Matrix3.M00) + values(Matrix3.M01) * m.values(Matrix3.M10) + values(Matrix3.M02) * m.values(Matrix3.M20)
    val v01 = values(Matrix3.M00) * m.values(Matrix3.M01) + values(Matrix3.M01) * m.values(Matrix3.M11) + values(Matrix3.M02) * m.values(Matrix3.M21)
    val v02 = values(Matrix3.M00) * m.values(Matrix3.M02) + values(Matrix3.M01) * m.values(Matrix3.M12) + values(Matrix3.M02) * m.values(Matrix3.M22)

    val v10 = values(Matrix3.M10) * m.values(Matrix3.M00) + values(Matrix3.M11) * m.values(Matrix3.M10) + values(Matrix3.M12) * m.values(Matrix3.M20)
    val v11 = values(Matrix3.M10) * m.values(Matrix3.M01) + values(Matrix3.M11) * m.values(Matrix3.M11) + values(Matrix3.M12) * m.values(Matrix3.M21)
    val v12 = values(Matrix3.M10) * m.values(Matrix3.M02) + values(Matrix3.M11) * m.values(Matrix3.M12) + values(Matrix3.M12) * m.values(Matrix3.M22)

    val v20 = values(Matrix3.M20) * m.values(Matrix3.M00) + values(Matrix3.M21) * m.values(Matrix3.M10) + values(Matrix3.M22) * m.values(Matrix3.M20)
    val v21 = values(Matrix3.M20) * m.values(Matrix3.M01) + values(Matrix3.M21) * m.values(Matrix3.M11) + values(Matrix3.M22) * m.values(Matrix3.M21)
    val v22 = values(Matrix3.M20) * m.values(Matrix3.M02) + values(Matrix3.M21) * m.values(Matrix3.M12) + values(Matrix3.M22) * m.values(Matrix3.M22)

    values(Matrix3.M00) = v00
    values(Matrix3.M10) = v10
    values(Matrix3.M20) = v20
    values(Matrix3.M01) = v01
    values(Matrix3.M11) = v11
    values(Matrix3.M21) = v21
    values(Matrix3.M02) = v02
    values(Matrix3.M12) = v12
    values(Matrix3.M22) = v22

    this
  }

  /** Premultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
    *
    * <pre> A.mulLeft(B) results in A := BA </pre>
    *
    * @param m
    *   The other Matrix to multiply by
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def mulLeft(m: Matrix3): Matrix3 = {
    val values = this.values

    val v00 = m.values(Matrix3.M00) * values(Matrix3.M00) + m.values(Matrix3.M01) * values(Matrix3.M10) + m.values(Matrix3.M02) * values(Matrix3.M20)
    val v01 = m.values(Matrix3.M00) * values(Matrix3.M01) + m.values(Matrix3.M01) * values(Matrix3.M11) + m.values(Matrix3.M02) * values(Matrix3.M21)
    val v02 = m.values(Matrix3.M00) * values(Matrix3.M02) + m.values(Matrix3.M01) * values(Matrix3.M12) + m.values(Matrix3.M02) * values(Matrix3.M22)

    val v10 = m.values(Matrix3.M10) * values(Matrix3.M00) + m.values(Matrix3.M11) * values(Matrix3.M10) + m.values(Matrix3.M12) * values(Matrix3.M20)
    val v11 = m.values(Matrix3.M10) * values(Matrix3.M01) + m.values(Matrix3.M11) * values(Matrix3.M11) + m.values(Matrix3.M12) * values(Matrix3.M21)
    val v12 = m.values(Matrix3.M10) * values(Matrix3.M02) + m.values(Matrix3.M11) * values(Matrix3.M12) + m.values(Matrix3.M12) * values(Matrix3.M22)

    val v20 = m.values(Matrix3.M20) * values(Matrix3.M00) + m.values(Matrix3.M21) * values(Matrix3.M10) + m.values(Matrix3.M22) * values(Matrix3.M20)
    val v21 = m.values(Matrix3.M20) * values(Matrix3.M01) + m.values(Matrix3.M21) * values(Matrix3.M11) + m.values(Matrix3.M22) * values(Matrix3.M21)
    val v22 = m.values(Matrix3.M20) * values(Matrix3.M02) + m.values(Matrix3.M21) * values(Matrix3.M12) + m.values(Matrix3.M22) * values(Matrix3.M22)

    values(Matrix3.M00) = v00
    values(Matrix3.M10) = v10
    values(Matrix3.M20) = v20
    values(Matrix3.M01) = v01
    values(Matrix3.M11) = v11
    values(Matrix3.M21) = v21
    values(Matrix3.M02) = v02
    values(Matrix3.M12) = v12
    values(Matrix3.M22) = v22

    this
  }

  /** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
    * @param degrees
    *   the angle in degrees.
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def setToRotation(degrees: Float): Matrix3 =
    setToRotationRad(MathUtils.degreesToRadians * degrees)

  /** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
    * @param radians
    *   the angle in radians.
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def setToRotationRad(radians: Float): Matrix3 = {
    val cos    = Math.cos(radians).toFloat
    val sin    = Math.sin(radians).toFloat
    val values = this.values

    values(Matrix3.M00) = cos
    values(Matrix3.M10) = sin
    values(Matrix3.M20) = 0

    values(Matrix3.M01) = -sin
    values(Matrix3.M11) = cos
    values(Matrix3.M21) = 0

    values(Matrix3.M02) = 0
    values(Matrix3.M12) = 0
    values(Matrix3.M22) = 1

    this
  }

  def setToRotation(axis: Vector3, degrees: Float): Matrix3 =
    setToRotation(axis, MathUtils.cosDeg(degrees), MathUtils.sinDeg(degrees))

  def setToRotation(axis: Vector3, cos: Float, sin: Float): Matrix3 = {
    val values = this.values
    val oc     = 1.0f - cos
    values(Matrix3.M00) = oc * axis.x * axis.x + cos
    values(Matrix3.M01) = oc * axis.x * axis.y - axis.z * sin
    values(Matrix3.M02) = oc * axis.z * axis.x + axis.y * sin
    values(Matrix3.M10) = oc * axis.x * axis.y + axis.z * sin
    values(Matrix3.M11) = oc * axis.y * axis.y + cos
    values(Matrix3.M12) = oc * axis.y * axis.z - axis.x * sin
    values(Matrix3.M20) = oc * axis.z * axis.x - axis.y * sin
    values(Matrix3.M21) = oc * axis.y * axis.z + axis.x * sin
    values(Matrix3.M22) = oc * axis.z * axis.z + cos
    this
  }

  /** Sets this matrix to a translation matrix.
    * @param x
    *   the translation in x
    * @param y
    *   the translation in y
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def setToTranslation(x: Float, y: Float): Matrix3 = {
    val values = this.values

    values(Matrix3.M00) = 1
    values(Matrix3.M10) = 0
    values(Matrix3.M20) = 0

    values(Matrix3.M01) = 0
    values(Matrix3.M11) = 1
    values(Matrix3.M21) = 0

    values(Matrix3.M02) = x
    values(Matrix3.M12) = y
    values(Matrix3.M22) = 1

    this
  }

  /** Sets this matrix to a translation matrix.
    * @param translation
    *   The translation vector.
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def setToTranslation(translation: Vector2): Matrix3 = {
    val values = this.values

    values(Matrix3.M00) = 1
    values(Matrix3.M10) = 0
    values(Matrix3.M20) = 0

    values(Matrix3.M01) = 0
    values(Matrix3.M11) = 1
    values(Matrix3.M21) = 0

    values(Matrix3.M02) = translation.x
    values(Matrix3.M12) = translation.y
    values(Matrix3.M22) = 1

    this
  }

  /** Sets this matrix to a scaling matrix.
    *
    * @param scaleX
    *   the scale in x
    * @param scaleY
    *   the scale in y
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def setToScaling(scaleX: Float, scaleY: Float): Matrix3 = {
    val values = this.values
    values(Matrix3.M00) = scaleX
    values(Matrix3.M10) = 0
    values(Matrix3.M20) = 0
    values(Matrix3.M01) = 0
    values(Matrix3.M11) = scaleY
    values(Matrix3.M21) = 0
    values(Matrix3.M02) = 0
    values(Matrix3.M12) = 0
    values(Matrix3.M22) = 1
    this
  }

  /** Sets this matrix to a scaling matrix.
    * @param scale
    *   The scale vector.
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def setToScaling(scale: Vector2): Matrix3 = {
    val values = this.values
    values(Matrix3.M00) = scale.x
    values(Matrix3.M10) = 0
    values(Matrix3.M20) = 0
    values(Matrix3.M01) = 0
    values(Matrix3.M11) = scale.y
    values(Matrix3.M21) = 0
    values(Matrix3.M02) = 0
    values(Matrix3.M12) = 0
    values(Matrix3.M22) = 1
    this
  }

  override def toString: String = {
    val values = this.values
    s"[${values(Matrix3.M00)}|${values(Matrix3.M01)}|${values(Matrix3.M02)}]\n" +
      s"[${values(Matrix3.M10)}|${values(Matrix3.M11)}|${values(Matrix3.M12)}]\n" +
      s"[${values(Matrix3.M20)}|${values(Matrix3.M21)}|${values(Matrix3.M22)}]"
  }

  /** @return The determinant of this matrix */
  def det(): Float = {
    val values = this.values
    values(Matrix3.M00) * values(Matrix3.M11) * values(Matrix3.M22) + values(Matrix3.M01) * values(Matrix3.M12) * values(Matrix3.M20) + values(Matrix3.M02) * values(Matrix3.M10) * values(
      Matrix3.M21
    ) -
      values(Matrix3.M00) * values(Matrix3.M12) * values(Matrix3.M21) - values(Matrix3.M01) * values(Matrix3.M10) * values(Matrix3.M22) - values(Matrix3.M02) * values(Matrix3.M11) * values(
        Matrix3.M20
      )
  }

  /** Inverts this matrix given that the determinant is != 0.
    * @return
    *   This matrix for the purpose of chaining operations.
    * @throws RuntimeException
    *   if the matrix is singular (not invertible)
    */
  def inv(): Matrix3 = {
    val det = this.det()
    if (det == 0) throw new RuntimeException("Can't invert a singular matrix")

    val inv_det = 1.0f / det
    val values  = this.values

    val v00 = values(Matrix3.M11) * values(Matrix3.M22) - values(Matrix3.M21) * values(Matrix3.M12)
    val v10 = values(Matrix3.M20) * values(Matrix3.M12) - values(Matrix3.M10) * values(Matrix3.M22)
    val v20 = values(Matrix3.M10) * values(Matrix3.M21) - values(Matrix3.M20) * values(Matrix3.M11)
    val v01 = values(Matrix3.M21) * values(Matrix3.M02) - values(Matrix3.M01) * values(Matrix3.M22)
    val v11 = values(Matrix3.M00) * values(Matrix3.M22) - values(Matrix3.M20) * values(Matrix3.M02)
    val v21 = values(Matrix3.M20) * values(Matrix3.M01) - values(Matrix3.M00) * values(Matrix3.M21)
    val v02 = values(Matrix3.M01) * values(Matrix3.M12) - values(Matrix3.M11) * values(Matrix3.M02)
    val v12 = values(Matrix3.M10) * values(Matrix3.M02) - values(Matrix3.M00) * values(Matrix3.M12)
    val v22 = values(Matrix3.M00) * values(Matrix3.M11) - values(Matrix3.M10) * values(Matrix3.M01)

    values(Matrix3.M00) = inv_det * v00
    values(Matrix3.M10) = inv_det * v10
    values(Matrix3.M20) = inv_det * v20
    values(Matrix3.M01) = inv_det * v01
    values(Matrix3.M11) = inv_det * v11
    values(Matrix3.M21) = inv_det * v21
    values(Matrix3.M02) = inv_det * v02
    values(Matrix3.M12) = inv_det * v12
    values(Matrix3.M22) = inv_det * v22

    this
  }

  /** Copies the values from the provided matrix to this matrix.
    * @param mat
    *   The matrix to copy.
    * @return
    *   This matrix for the purposes of chaining.
    */
  def set(mat: Matrix3): Matrix3 = {
    System.arraycopy(mat.values, 0, values, 0, values.length)
    this
  }

  /** Copies the values from the provided affine matrix to this matrix. The last row is set to (0, 0, 1).
    * @param affine
    *   The affine matrix to copy.
    * @return
    *   This matrix for the purposes of chaining.
    */
  def set(affine: Affine2): Matrix3 = {
    val values = this.values

    values(Matrix3.M00) = affine.m00
    values(Matrix3.M10) = affine.m10
    values(Matrix3.M20) = 0
    values(Matrix3.M01) = affine.m01
    values(Matrix3.M11) = affine.m11
    values(Matrix3.M21) = 0
    values(Matrix3.M02) = affine.m02
    values(Matrix3.M12) = affine.m12
    values(Matrix3.M22) = 1

    this
  }

  /** Sets this 3x3 matrix to the top left 3x3 corner of the provided 4x4 matrix.
    * @param mat
    *   The matrix whose top left corner will be copied. This matrix will not be modified.
    * @return
    *   This matrix for the purpose of chaining operations.
    */
  def set(mat: Matrix4): Matrix3 = {
    val values = this.values
    values(Matrix3.M00) = mat.values(Matrix4.M00)
    values(Matrix3.M10) = mat.values(Matrix4.M10)
    values(Matrix3.M20) = mat.values(Matrix4.M20)
    values(Matrix3.M01) = mat.values(Matrix4.M01)
    values(Matrix3.M11) = mat.values(Matrix4.M11)
    values(Matrix3.M21) = mat.values(Matrix4.M21)
    values(Matrix3.M02) = mat.values(Matrix4.M02)
    values(Matrix3.M12) = mat.values(Matrix4.M12)
    values(Matrix3.M22) = mat.values(Matrix4.M22)
    this
  }

  /** Sets the matrix to the given matrix as a float array. The float array must have at least 9 elements; the first 9 will be copied.
    *
    * @param values
    *   The matrix, in float form, that is to be copied. Remember that this matrix is in <a href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> order.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(values: Array[Float]): Matrix3 = {
    System.arraycopy(values, 0, this.values, 0, this.values.length)
    this
  }

  /** Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
    * @param vector
    *   The translation vector.
    * @return
    *   This matrix for the purpose of chaining.
    */
  def trn(vector: Vector2): Matrix3 = {
    values(Matrix3.M02) += vector.x
    values(Matrix3.M12) += vector.y
    this
  }

  /** Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
    * @param x
    *   The x-component of the translation vector.
    * @param y
    *   The y-component of the translation vector.
    * @return
    *   This matrix for the purpose of chaining.
    */
  def trn(x: Float, y: Float): Matrix3 = {
    values(Matrix3.M02) += x
    values(Matrix3.M12) += y
    this
  }

  /** Adds a translational component to the matrix in the 3rd column. The other columns are untouched.
    * @param vector
    *   The translation vector. (The z-component of the vector is ignored because this is a 3x3 matrix)
    * @return
    *   This matrix for the purpose of chaining.
    */
  def trn(vector: Vector3): Matrix3 = {
    values(Matrix3.M02) += vector.x
    values(Matrix3.M12) += vector.y
    this
  }

  /** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param x
    *   The x-component of the translation vector.
    * @param y
    *   The y-component of the translation vector.
    * @return
    *   This matrix for the purpose of chaining.
    */
  def translate(x: Float, y: Float): Matrix3 = {
    val tmp = this.tmp
    tmp(Matrix3.M00) = 1
    tmp(Matrix3.M10) = 0
    // tmp(Matrix3.M20) = 0

    tmp(Matrix3.M01) = 0
    tmp(Matrix3.M11) = 1
    // tmp(Matrix3.M21) = 0

    tmp(Matrix3.M02) = x
    tmp(Matrix3.M12) = y
    // tmp(Matrix3.M22) = 1
    Matrix3.mul(values, tmp)
    this
  }

  /** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param translation
    *   The translation vector.
    * @return
    *   This matrix for the purpose of chaining.
    */
  def translate(translation: Vector2): Matrix3 = {
    val tmp = this.tmp
    tmp(Matrix3.M00) = 1
    tmp(Matrix3.M10) = 0
    // tmp(Matrix3.M20) = 0

    tmp(Matrix3.M01) = 0
    tmp(Matrix3.M11) = 1
    // tmp(Matrix3.M21) = 0

    tmp(Matrix3.M02) = translation.x
    tmp(Matrix3.M12) = translation.y
    // tmp(Matrix3.M22) = 1
    Matrix3.mul(values, tmp)
    this
  }

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param degrees
    *   The angle in degrees
    * @return
    *   This matrix for the purpose of chaining.
    */
  def rotate(degrees: Float): Matrix3 =
    rotateRad(MathUtils.degreesToRadians * degrees)

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param radians
    *   The angle in radians
    * @return
    *   This matrix for the purpose of chaining.
    */
  def rotateRad(radians: Float): Matrix3 =
    if (radians == 0) this
    else {
      val cos = Math.cos(radians).toFloat
      val sin = Math.sin(radians).toFloat

      val tmp = this.tmp
      tmp(Matrix3.M00) = cos
      tmp(Matrix3.M10) = sin
      // tmp(Matrix3.M20) = 0

      tmp(Matrix3.M01) = -sin
      tmp(Matrix3.M11) = cos
      // tmp(Matrix3.M21) = 0

      tmp(Matrix3.M02) = 0
      tmp(Matrix3.M12) = 0
      // tmp(Matrix3.M22) = 1

      Matrix3.mul(values, tmp)
      this
    }

  /** Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param scaleX
    *   The scale in the x-axis.
    * @param scaleY
    *   The scale in the y-axis.
    * @return
    *   This matrix for the purpose of chaining.
    */
  def scale(scaleX: Float, scaleY: Float): Matrix3 = {
    val tmp = this.tmp
    tmp(Matrix3.M00) = scaleX
    tmp(Matrix3.M10) = 0
    // tmp(Matrix3.M20) = 0

    tmp(Matrix3.M01) = 0
    tmp(Matrix3.M11) = scaleY
    // tmp(Matrix3.M21) = 0

    tmp(Matrix3.M02) = 0
    tmp(Matrix3.M12) = 0
    // tmp(Matrix3.M22) = 1

    Matrix3.mul(values, tmp)
    this
  }

  /** Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param scale
    *   The vector to scale the matrix by.
    * @return
    *   This matrix for the purpose of chaining.
    */
  def scale(scale: Vector2): Matrix3 = {
    val tmp = this.tmp
    tmp(Matrix3.M00) = scale.x
    tmp(Matrix3.M10) = 0
    // tmp(Matrix3.M20) = 0

    tmp(Matrix3.M01) = 0
    tmp(Matrix3.M11) = scale.y
    // tmp(Matrix3.M21) = 0

    tmp(Matrix3.M02) = 0
    tmp(Matrix3.M12) = 0
    // tmp(Matrix3.M22) = 1

    Matrix3.mul(values, tmp)
    this
  }

  /** Get the values in this matrix.
    * @return
    *   The float values that make up this matrix in column-major order.
    */
  def getValues: Array[Float] =
    values

  def getTranslation(position: Vector2): Vector2 = {
    position.x = values(Matrix3.M02)
    position.y = values(Matrix3.M12)
    position
  }

  /** @param scale
    *   The vector which will receive the (non-negative) scale components on each axis.
    * @return
    *   The provided vector for chaining.
    */
  def getScale(scale: Vector2): Vector2 = {
    val values = this.values
    scale.x = Math.sqrt(values(Matrix3.M00) * values(Matrix3.M00) + values(Matrix3.M01) * values(Matrix3.M01)).toFloat
    scale.y = Math.sqrt(values(Matrix3.M10) * values(Matrix3.M10) + values(Matrix3.M11) * values(Matrix3.M11)).toFloat
    scale
  }

  def getRotation: Float =
    MathUtils.radiansToDegrees * Math.atan2(values(Matrix3.M10), values(Matrix3.M00)).toFloat

  def getRotationRad: Float =
    Math.atan2(values(Matrix3.M10), values(Matrix3.M00)).toFloat

  /** Scale the matrix in the both the x and y components by the scalar value.
    * @param scale
    *   The single value that will be used to scale both the x and y components.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def scl(scale: Float): Matrix3 = {
    values(Matrix3.M00) *= scale
    values(Matrix3.M11) *= scale
    this
  }

  /** Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
    * @param scale
    *   The {@link Vector3} to use to scale this matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def scl(scale: Vector2): Matrix3 = {
    values(Matrix3.M00) *= scale.x
    values(Matrix3.M11) *= scale.y
    this
  }

  /** Scale this matrix using the x and y components of the vector but leave the rest of the matrix alone.
    * @param scale
    *   The {@link Vector3} to use to scale this matrix. The z component will be ignored.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def scl(scale: Vector3): Matrix3 = {
    values(Matrix3.M00) *= scale.x
    values(Matrix3.M11) *= scale.y
    this
  }

  /** Transposes the current matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def transpose(): Matrix3 = {
    // Where MXY you do not have to change MXX
    val values = this.values
    val v01    = values(Matrix3.M10)
    val v02    = values(Matrix3.M20)
    val v10    = values(Matrix3.M01)
    val v12    = values(Matrix3.M21)
    val v20    = values(Matrix3.M02)
    val v21    = values(Matrix3.M12)
    values(Matrix3.M01) = v01
    values(Matrix3.M02) = v02
    values(Matrix3.M10) = v10
    values(Matrix3.M12) = v12
    values(Matrix3.M20) = v20
    values(Matrix3.M21) = v21
    this
  }
}

object Matrix3 {
  val M00 = 0
  val M01 = 3
  val M02 = 6
  val M10 = 1
  val M11 = 4
  val M12 = 7
  val M20 = 2
  val M21 = 5
  val M22 = 8

  /** Multiplies matrix a with matrix b in the following manner:
    *
    * <pre> mul(A, B) => A := AB </pre>
    *
    * @param mata
    *   The float array representing the first matrix. Must have at least 9 elements.
    * @param matb
    *   The float array representing the second matrix. Must have at least 9 elements.
    */
  private def mul(mata: Array[Float], matb: Array[Float]): Unit = {
    val v00 = mata(M00) * matb(M00) + mata(M01) * matb(M10) + mata(M02) * matb(M20)
    val v01 = mata(M00) * matb(M01) + mata(M01) * matb(M11) + mata(M02) * matb(M21)
    val v02 = mata(M00) * matb(M02) + mata(M01) * matb(M12) + mata(M02) * matb(M22)

    val v10 = mata(M10) * matb(M00) + mata(M11) * matb(M10) + mata(M12) * matb(M20)
    val v11 = mata(M10) * matb(M01) + mata(M11) * matb(M11) + mata(M12) * matb(M21)
    val v12 = mata(M10) * matb(M02) + mata(M11) * matb(M12) + mata(M12) * matb(M22)

    val v20 = mata(M20) * matb(M00) + mata(M21) * matb(M10) + mata(M22) * matb(M20)
    val v21 = mata(M20) * matb(M01) + mata(M21) * matb(M11) + mata(M22) * matb(M21)
    val v22 = mata(M20) * matb(M02) + mata(M21) * matb(M12) + mata(M22) * matb(M22)

    mata(M00) = v00
    mata(M10) = v10
    mata(M20) = v20
    mata(M01) = v01
    mata(M11) = v11
    mata(M21) = v21
    mata(M02) = v02
    mata(M12) = v12
    mata(M22) = v22
  }
}

/** Encapsulates a <a href="http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column major</a> 4 by 4 matrix. Like the {@link Vector3} class it allows the chaining of methods by
  * returning a reference to itself. For example:
  *
  * <pre> Matrix4 mat = new Matrix4().trn(position).mul(camera.combined); </pre>
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
class Matrix4 {
  val values: Array[Float] = Array.ofDim(16)

  // Initialize as identity matrix
  values(Matrix4.M00) = 1f
  values(Matrix4.M11) = 1f
  values(Matrix4.M22) = 1f
  values(Matrix4.M33) = 1f

  /** Constructs a matrix from the given matrix.
    * @param matrix
    *   The matrix to copy. (This matrix is not modified)
    */
  def this(matrix: Matrix4) = {
    this()
    set(matrix)
  }

  /** Constructs a matrix from the given float array. The array must have at least 16 elements; the first 16 will be copied.
    * @param values
    *   The float array to copy. Remember that this matrix is in <a href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order. (The float array is not modified)
    */
  def this(values: Array[Float]) = {
    this()
    set(values)
  }

  /** Constructs a rotation matrix from the given {@link Quaternion} .
    * @param quaternion
    *   The quaternion to be copied. (The quaternion is not modified)
    */
  def this(quaternion: Quaternion) = {
    this()
    set(quaternion)
  }

  /** Construct a matrix from the given translation, rotation and scale.
    * @param position
    *   The translation
    * @param rotation
    *   The rotation, must be normalized
    * @param scale
    *   The scale
    */
  def this(position: Vector3, rotation: Quaternion, scale: Vector3) = {
    this()
    set(position, rotation, scale)
  }

  /** Sets the matrix to the given matrix.
    * @param matrix
    *   The matrix that is to be copied. (The given matrix is not modified)
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(matrix: Matrix4): Matrix4 =
    set(matrix.values)

  /** Sets the matrix to the given matrix as a float array. The float array must have at least 16 elements; the first 16 will be copied.
    *
    * @param values
    *   The matrix, in float form, that is to be copied. Remember that this matrix is in <a href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(values: Array[Float]): Matrix4 = {
    System.arraycopy(values, 0, this.values, 0, this.values.length)
    this
  }

  /** Sets the matrix to a rotation matrix representing the quaternion.
    * @param quaternion
    *   The quaternion that is to be used to set this matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(quaternion: Quaternion): Matrix4 =
    set(quaternion.x, quaternion.y, quaternion.z, quaternion.w)

  /** Sets the matrix to a rotation matrix representing the quaternion.
    *
    * @param quaternionX
    *   The X component of the quaternion that is to be used to set this matrix.
    * @param quaternionY
    *   The Y component of the quaternion that is to be used to set this matrix.
    * @param quaternionZ
    *   The Z component of the quaternion that is to be used to set this matrix.
    * @param quaternionW
    *   The W component of the quaternion that is to be used to set this matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(quaternionX: Float, quaternionY: Float, quaternionZ: Float, quaternionW: Float): Matrix4 =
    set(0f, 0f, 0f, quaternionX, quaternionY, quaternionZ, quaternionW)

  /** Set this matrix to the specified translation and rotation.
    * @param position
    *   The translation
    * @param orientation
    *   The rotation, must be normalized
    * @return
    *   This matrix for chaining
    */
  def set(position: Vector3, orientation: Quaternion): Matrix4 =
    set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w)

  /** Sets the matrix to a rotation matrix representing the translation and quaternion.
    * @param translationX
    *   The X component of the translation that is to be used to set this matrix.
    * @param translationY
    *   The Y component of the translation that is to be used to set this matrix.
    * @param translationZ
    *   The Z component of the translation that is to be used to set this matrix.
    * @param quaternionX
    *   The X component of the quaternion that is to be used to set this matrix.
    * @param quaternionY
    *   The Y component of the quaternion that is to be used to set this matrix.
    * @param quaternionZ
    *   The Z component of the quaternion that is to be used to set this matrix.
    * @param quaternionW
    *   The W component of the quaternion that is to be used to set this matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(translationX: Float, translationY: Float, translationZ: Float, quaternionX: Float, quaternionY: Float, quaternionZ: Float, quaternionW: Float): Matrix4 = {
    val xs = quaternionX * 2f
    val ys = quaternionY * 2f
    val zs = quaternionZ * 2f
    val wx = quaternionW * xs
    val wy = quaternionW * ys
    val wz = quaternionW * zs
    val xx = quaternionX * xs
    val xy = quaternionX * ys
    val xz = quaternionX * zs
    val yy = quaternionY * ys
    val yz = quaternionY * zs
    val zz = quaternionZ * zs

    values(Matrix4.M00) = 1f - (yy + zz)
    values(Matrix4.M01) = xy - wz
    values(Matrix4.M02) = xz + wy
    values(Matrix4.M03) = translationX

    values(Matrix4.M10) = xy + wz
    values(Matrix4.M11) = 1f - (xx + zz)
    values(Matrix4.M12) = yz - wx
    values(Matrix4.M13) = translationY

    values(Matrix4.M20) = xz - wy
    values(Matrix4.M21) = yz + wx
    values(Matrix4.M22) = 1f - (xx + yy)
    values(Matrix4.M23) = translationZ

    values(Matrix4.M30) = 0f
    values(Matrix4.M31) = 0f
    values(Matrix4.M32) = 0f
    values(Matrix4.M33) = 1f
    this
  }

  /** Set this matrix to the specified translation, rotation and scale.
    * @param position
    *   The translation
    * @param orientation
    *   The rotation, must be normalized
    * @param scale
    *   The scale
    * @return
    *   This matrix for chaining
    */
  def set(position: Vector3, orientation: Quaternion, scale: Vector3): Matrix4 =
    set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w, scale.x, scale.y, scale.z)

  /** Sets the matrix to a rotation matrix representing the translation and quaternion.
    * @param translationX
    *   The X component of the translation that is to be used to set this matrix.
    * @param translationY
    *   The Y component of the translation that is to be used to set this matrix.
    * @param translationZ
    *   The Z component of the translation that is to be used to set this matrix.
    * @param quaternionX
    *   The X component of the quaternion that is to be used to set this matrix.
    * @param quaternionY
    *   The Y component of the quaternion that is to be used to set this matrix.
    * @param quaternionZ
    *   The Z component of the quaternion that is to be used to set this matrix.
    * @param quaternionW
    *   The W component of the quaternion that is to be used to set this matrix.
    * @param scaleX
    *   The X component of the scaling that is to be used to set this matrix.
    * @param scaleY
    *   The Y component of the scaling that is to be used to set this matrix.
    * @param scaleZ
    *   The Z component of the scaling that is to be used to set this matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def set(
    translationX: Float,
    translationY: Float,
    translationZ: Float,
    quaternionX:  Float,
    quaternionY:  Float,
    quaternionZ:  Float,
    quaternionW:  Float,
    scaleX:       Float,
    scaleY:       Float,
    scaleZ:       Float
  ): Matrix4 = {
    val xs = quaternionX * 2f
    val ys = quaternionY * 2f
    val zs = quaternionZ * 2f
    val wx = quaternionW * xs
    val wy = quaternionW * ys
    val wz = quaternionW * zs
    val xx = quaternionX * xs
    val xy = quaternionX * ys
    val xz = quaternionX * zs
    val yy = quaternionY * ys
    val yz = quaternionY * zs
    val zz = quaternionZ * zs

    values(Matrix4.M00) = scaleX * (1.0f - (yy + zz))
    values(Matrix4.M01) = scaleY * (xy - wz)
    values(Matrix4.M02) = scaleZ * (xz + wy)
    values(Matrix4.M03) = translationX

    values(Matrix4.M10) = scaleX * (xy + wz)
    values(Matrix4.M11) = scaleY * (1.0f - (xx + zz))
    values(Matrix4.M12) = scaleZ * (yz - wx)
    values(Matrix4.M13) = translationY

    values(Matrix4.M20) = scaleX * (xz - wy)
    values(Matrix4.M21) = scaleY * (yz + wx)
    values(Matrix4.M22) = scaleZ * (1.0f - (xx + yy))
    values(Matrix4.M23) = translationZ

    values(Matrix4.M30) = 0f
    values(Matrix4.M31) = 0f
    values(Matrix4.M32) = 0f
    values(Matrix4.M33) = 1f
    this
  }

  /** Sets the four columns of the matrix which correspond to the x-, y- and z-axis of the vector space this matrix creates as well as the 4th column representing the translation of any point that is
    * multiplied by this matrix.
    * @param xAxis
    *   The x-axis.
    * @param yAxis
    *   The y-axis.
    * @param zAxis
    *   The z-axis.
    * @param pos
    *   The translation vector.
    */
  def set(xAxis: Vector3, yAxis: Vector3, zAxis: Vector3, pos: Vector3): Matrix4 = {
    values(Matrix4.M00) = xAxis.x
    values(Matrix4.M01) = xAxis.y
    values(Matrix4.M02) = xAxis.z
    values(Matrix4.M10) = yAxis.x
    values(Matrix4.M11) = yAxis.y
    values(Matrix4.M12) = yAxis.z
    values(Matrix4.M20) = zAxis.x
    values(Matrix4.M21) = zAxis.y
    values(Matrix4.M22) = zAxis.z
    values(Matrix4.M03) = pos.x
    values(Matrix4.M13) = pos.y
    values(Matrix4.M23) = pos.z
    values(Matrix4.M30) = 0
    values(Matrix4.M31) = 0
    values(Matrix4.M32) = 0
    values(Matrix4.M33) = 1
    this
  }

  /** @return a copy of this matrix */
  def cpy(): Matrix4 =
    new Matrix4(this)

  /** Adds a translational component to the matrix in the 4th column. The other columns are untouched.
    * @param vector
    *   The translation vector to add to the current matrix. (This vector is not modified)
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def trn(vector: Vector3): Matrix4 = {
    values(Matrix4.M03) = values(Matrix4.M03) + vector.x
    values(Matrix4.M13) = values(Matrix4.M13) + vector.y
    values(Matrix4.M23) = values(Matrix4.M23) + vector.z
    this
  }

  /** Adds a translational component to the matrix in the 4th column. The other columns are untouched.
    * @param x
    *   The x-component of the translation vector.
    * @param y
    *   The y-component of the translation vector.
    * @param z
    *   The z-component of the translation vector.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def trn(x: Float, y: Float, z: Float): Matrix4 = {
    values(Matrix4.M03) = values(Matrix4.M03) + x
    values(Matrix4.M13) = values(Matrix4.M13) + y
    values(Matrix4.M23) = values(Matrix4.M23) + z
    this
  }

  /** @return the backing float array */
  def getValues(): Array[Float] =
    values

  /** Postmultiplies this matrix with the given matrix, storing the result in this matrix. For example:
    *
    * <pre> A.mul(B) results in A := AB. </pre>
    *
    * @param matrix
    *   The other matrix to multiply by.
    * @return
    *   This matrix for the purpose of chaining operations together.
    */
  def mul(matrix: Matrix4): Matrix4 = {
    Matrix4.mul(values, matrix.values)
    this
  }

  /** Premultiplies this matrix with the given matrix, storing the result in this matrix. For example:
    *
    * <pre> A.mulLeft(B) results in A := BA. </pre>
    *
    * @param matrix
    *   The other matrix to multiply by.
    * @return
    *   This matrix for the purpose of chaining operations together.
    */
  def mulLeft(matrix: Matrix4): Matrix4 = {
    Matrix4.tmpMat.set(matrix)
    Matrix4.mul(Matrix4.tmpMat.values, values)
    this
  }

  /** Transposes the matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def tra(): Matrix4 = {
    val m01 = values(Matrix4.M01)
    val m02 = values(Matrix4.M02)
    val m03 = values(Matrix4.M03)
    val m12 = values(Matrix4.M12)
    val m13 = values(Matrix4.M13)
    val m23 = values(Matrix4.M23)
    values(Matrix4.M01) = values(Matrix4.M10)
    values(Matrix4.M02) = values(Matrix4.M20)
    values(Matrix4.M03) = values(Matrix4.M30)
    values(Matrix4.M10) = m01
    values(Matrix4.M12) = values(Matrix4.M21)
    values(Matrix4.M13) = values(Matrix4.M31)
    values(Matrix4.M20) = m02
    values(Matrix4.M21) = m12
    values(Matrix4.M23) = values(Matrix4.M32)
    values(Matrix4.M30) = m03
    values(Matrix4.M31) = m13
    values(Matrix4.M32) = m23
    this
  }

  /** Sets the matrix to an identity matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def idt(): Matrix4 = {
    values(Matrix4.M00) = 1f
    values(Matrix4.M01) = 0f
    values(Matrix4.M02) = 0f
    values(Matrix4.M03) = 0f
    values(Matrix4.M10) = 0f
    values(Matrix4.M11) = 1f
    values(Matrix4.M12) = 0f
    values(Matrix4.M13) = 0f
    values(Matrix4.M20) = 0f
    values(Matrix4.M21) = 0f
    values(Matrix4.M22) = 1f
    values(Matrix4.M23) = 0f
    values(Matrix4.M30) = 0f
    values(Matrix4.M31) = 0f
    values(Matrix4.M32) = 0f
    values(Matrix4.M33) = 1f
    this
  }

  /** Inverts the matrix. Stores the result in this matrix.
    * @return
    *   This matrix for the purpose of chaining methods together.
    * @throws RuntimeException
    *   if the matrix is singular (not invertible)
    */
  def inv(): Matrix4 = {
    val l_det = det()
    if (l_det == 0) throw new RuntimeException("non-invertible matrix")
    val inv_det = 1.0f / l_det
    val values  = this.values

    // Extract matrix elements for readability
    val m00 = values(Matrix4.M00); val m01 = values(Matrix4.M01); val m02 = values(Matrix4.M02); val m03 = values(Matrix4.M03)
    val m10 = values(Matrix4.M10); val m11 = values(Matrix4.M11); val m12 = values(Matrix4.M12); val m13 = values(Matrix4.M13)
    val m20 = values(Matrix4.M20); val m21 = values(Matrix4.M21); val m22 = values(Matrix4.M22); val m23 = values(Matrix4.M23)
    val m30 = values(Matrix4.M30); val m31 = values(Matrix4.M31); val m32 = values(Matrix4.M32); val m33 = values(Matrix4.M33)

    // Cofactors of the adjugate matrix (each is a 3x3 subdeterminant with 6 terms)
    val v00 = m12 * m23 * m31 - m13 * m22 * m31 + m13 * m21 * m32 - m11 * m23 * m32 - m12 * m21 * m33 + m11 * m22 * m33
    val v01 = m03 * m22 * m31 - m02 * m23 * m31 - m03 * m21 * m32 + m01 * m23 * m32 + m02 * m21 * m33 - m01 * m22 * m33
    val v02 = m02 * m13 * m31 - m03 * m12 * m31 + m03 * m11 * m32 - m01 * m13 * m32 - m02 * m11 * m33 + m01 * m12 * m33
    val v03 = m03 * m12 * m21 - m02 * m13 * m21 - m03 * m11 * m22 + m01 * m13 * m22 + m02 * m11 * m23 - m01 * m12 * m23
    val v10 = m13 * m22 * m30 - m12 * m23 * m30 - m13 * m20 * m32 + m10 * m23 * m32 + m12 * m20 * m33 - m10 * m22 * m33
    val v11 = m02 * m23 * m30 - m03 * m22 * m30 + m03 * m20 * m32 - m00 * m23 * m32 - m02 * m20 * m33 + m00 * m22 * m33
    val v12 = m03 * m12 * m30 - m02 * m13 * m30 - m03 * m10 * m32 + m00 * m13 * m32 + m02 * m10 * m33 - m00 * m12 * m33
    val v13 = m02 * m13 * m20 - m03 * m12 * m20 + m03 * m10 * m22 - m00 * m13 * m22 - m02 * m10 * m23 + m00 * m12 * m23
    val v20 = m11 * m23 * m30 - m13 * m21 * m30 + m13 * m20 * m31 - m10 * m23 * m31 - m11 * m20 * m33 + m10 * m21 * m33
    val v21 = m03 * m21 * m30 - m01 * m23 * m30 - m03 * m20 * m31 + m00 * m23 * m31 + m01 * m20 * m33 - m00 * m21 * m33
    val v22 = m01 * m13 * m30 - m03 * m11 * m30 + m03 * m10 * m31 - m00 * m13 * m31 - m01 * m10 * m33 + m00 * m11 * m33
    val v23 = m03 * m11 * m20 - m01 * m13 * m20 - m03 * m10 * m21 + m00 * m13 * m21 + m01 * m10 * m23 - m00 * m11 * m23
    val v30 = m12 * m21 * m30 - m11 * m22 * m30 - m12 * m20 * m31 + m10 * m22 * m31 + m11 * m20 * m32 - m10 * m21 * m32
    val v31 = m01 * m22 * m30 - m02 * m21 * m30 + m02 * m20 * m31 - m00 * m22 * m31 - m01 * m20 * m32 + m00 * m21 * m32
    val v32 = m02 * m11 * m30 - m01 * m12 * m30 - m02 * m10 * m31 + m00 * m12 * m31 + m01 * m10 * m32 - m00 * m11 * m32
    val v33 = m01 * m12 * m20 - m02 * m11 * m20 + m02 * m10 * m21 - m00 * m12 * m21 - m01 * m10 * m22 + m00 * m11 * m22

    values(Matrix4.M00) = inv_det * v00
    values(Matrix4.M10) = inv_det * v10
    values(Matrix4.M20) = inv_det * v20
    values(Matrix4.M30) = inv_det * v30
    values(Matrix4.M01) = inv_det * v01
    values(Matrix4.M11) = inv_det * v11
    values(Matrix4.M21) = inv_det * v21
    values(Matrix4.M31) = inv_det * v31
    values(Matrix4.M02) = inv_det * v02
    values(Matrix4.M12) = inv_det * v12
    values(Matrix4.M22) = inv_det * v22
    values(Matrix4.M32) = inv_det * v32
    values(Matrix4.M03) = inv_det * v03
    values(Matrix4.M13) = inv_det * v13
    values(Matrix4.M23) = inv_det * v23
    values(Matrix4.M33) = inv_det * v33
    this
  }

  /** @return The determinant of this matrix */
  def det(): Float =
    values(Matrix4.M30) * values(Matrix4.M21) * values(Matrix4.M12) * values(Matrix4.M03) - values(Matrix4.M20) * values(Matrix4.M31) * values(Matrix4.M12) * values(Matrix4.M03)
      - values(Matrix4.M30) * values(Matrix4.M11) * values(Matrix4.M22) * values(Matrix4.M03) + values(Matrix4.M10) * values(Matrix4.M31) * values(Matrix4.M22) * values(Matrix4.M03)
      + values(Matrix4.M20) * values(Matrix4.M11) * values(Matrix4.M32) * values(Matrix4.M03) - values(Matrix4.M10) * values(Matrix4.M21) * values(Matrix4.M32) * values(Matrix4.M03)
      - values(Matrix4.M30) * values(Matrix4.M21) * values(Matrix4.M02) * values(Matrix4.M13) + values(Matrix4.M20) * values(Matrix4.M31) * values(Matrix4.M02) * values(Matrix4.M13)
      + values(Matrix4.M30) * values(Matrix4.M01) * values(Matrix4.M22) * values(Matrix4.M13) - values(Matrix4.M00) * values(Matrix4.M31) * values(Matrix4.M22) * values(Matrix4.M13)
      - values(Matrix4.M20) * values(Matrix4.M01) * values(Matrix4.M32) * values(Matrix4.M13) + values(Matrix4.M00) * values(Matrix4.M21) * values(Matrix4.M32) * values(Matrix4.M13)
      + values(Matrix4.M30) * values(Matrix4.M11) * values(Matrix4.M02) * values(Matrix4.M23) - values(Matrix4.M10) * values(Matrix4.M31) * values(Matrix4.M02) * values(Matrix4.M23)
      - values(Matrix4.M30) * values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M23) + values(Matrix4.M00) * values(Matrix4.M31) * values(Matrix4.M12) * values(Matrix4.M23)
      + values(Matrix4.M10) * values(Matrix4.M01) * values(Matrix4.M32) * values(Matrix4.M23) - values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M32) * values(Matrix4.M23)
      - values(Matrix4.M20) * values(Matrix4.M11) * values(Matrix4.M02) * values(Matrix4.M33) + values(Matrix4.M10) * values(Matrix4.M21) * values(Matrix4.M02) * values(Matrix4.M33)
      + values(Matrix4.M20) * values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M33) - values(Matrix4.M00) * values(Matrix4.M21) * values(Matrix4.M12) * values(Matrix4.M33)
      - values(Matrix4.M10) * values(Matrix4.M01) * values(Matrix4.M22) * values(Matrix4.M33) + values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M22) * values(Matrix4.M33)

  /** @return The determinant of the 3x3 upper left matrix */
  def det3x3(): Float =
    values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M22) + values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M20) + values(Matrix4.M02) * values(Matrix4.M10) * values(Matrix4.M21)
      - values(Matrix4.M00) * values(Matrix4.M12) * values(Matrix4.M21) - values(Matrix4.M01) * values(Matrix4.M10) * values(Matrix4.M22) - values(Matrix4.M02) * values(Matrix4.M11) * values(
        Matrix4.M20
      )

  /** Sets the matrix to a projection matrix with a near- and far plane, a field of view in degrees and an aspect ratio. Note that the field of view specified is the angle in degrees for the height,
    * the field of view for the width will be calculated according to the aspect ratio.
    * @param near
    *   The near plane
    * @param far
    *   The far plane
    * @param fovy
    *   The field of view of the height in degrees
    * @param aspectRatio
    *   The "width over height" aspect ratio
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToProjection(near: Float, far: Float, fovy: Float, aspectRatio: Float): Matrix4 = {
    idt()
    val l_fd = (1.0f / Math.tan((fovy * (Math.PI / 180)) / 2.0f)).toFloat
    val l_a1 = (far + near) / (near - far)
    val l_a2 = (2 * far * near) / (near - far)
    values(Matrix4.M00) = l_fd / aspectRatio
    values(Matrix4.M10) = 0
    values(Matrix4.M20) = 0
    values(Matrix4.M30) = 0
    values(Matrix4.M01) = 0
    values(Matrix4.M11) = l_fd
    values(Matrix4.M21) = 0
    values(Matrix4.M31) = 0
    values(Matrix4.M02) = 0
    values(Matrix4.M12) = 0
    values(Matrix4.M22) = l_a1
    values(Matrix4.M32) = -1
    values(Matrix4.M03) = 0
    values(Matrix4.M13) = 0
    values(Matrix4.M23) = l_a2
    values(Matrix4.M33) = 0
    this
  }

  /** Sets the matrix to a projection matrix with a near/far plane, and left, bottom, right and top specifying the points on the near plane that are mapped to the lower left and upper right corners of
    * the viewport. This allows to create projection matrix with off-center vanishing point.
    * @param left
    * @param right
    * @param bottom
    * @param top
    * @param near
    *   The near plane
    * @param far
    *   The far plane
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToProjection(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 = {
    val x    = 2.0f * near / (right - left)
    val y    = 2.0f * near / (top - bottom)
    val a    = (right + left) / (right - left)
    val b    = (top + bottom) / (top - bottom)
    val l_a1 = (far + near) / (near - far)
    val l_a2 = (2 * far * near) / (near - far)
    values(Matrix4.M00) = x
    values(Matrix4.M10) = 0
    values(Matrix4.M20) = 0
    values(Matrix4.M30) = 0
    values(Matrix4.M01) = 0
    values(Matrix4.M11) = y
    values(Matrix4.M21) = 0
    values(Matrix4.M31) = 0
    values(Matrix4.M02) = a
    values(Matrix4.M12) = b
    values(Matrix4.M22) = l_a1
    values(Matrix4.M32) = -1
    values(Matrix4.M03) = 0
    values(Matrix4.M13) = 0
    values(Matrix4.M23) = l_a2
    values(Matrix4.M33) = 0
    this
  }

  /** Sets this matrix to an orthographic projection matrix with the origin at (x,y) extending by width and height. The near plane is set to 0, the far plane is set to 1.
    * @param x
    *   The x-coordinate of the origin
    * @param y
    *   The y-coordinate of the origin
    * @param width
    *   The width
    * @param height
    *   The height
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToOrtho2D(x: Float, y: Float, width: Float, height: Float): Matrix4 = {
    setToOrtho(x, x + width, y, y + height, 0, 1)
    this
  }

  /** Sets this matrix to an orthographic projection matrix with the origin at (x,y) extending by width and height, having a near and far plane.
    * @param x
    *   The x-coordinate of the origin
    * @param y
    *   The y-coordinate of the origin
    * @param width
    *   The width
    * @param height
    *   The height
    * @param near
    *   The near plane
    * @param far
    *   The far plane
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToOrtho2D(x: Float, y: Float, width: Float, height: Float, near: Float, far: Float): Matrix4 = {
    setToOrtho(x, x + width, y, y + height, near, far)
    this
  }

  /** Sets the matrix to an orthographic projection like glOrtho (http://www.opengl.org/sdk/docs/man/xhtml/glOrtho.xml) following the OpenGL equivalent
    * @param left
    *   The left clipping plane
    * @param right
    *   The right clipping plane
    * @param bottom
    *   The bottom clipping plane
    * @param top
    *   The top clipping plane
    * @param near
    *   The near clipping plane
    * @param far
    *   The far clipping plane
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4 = {
    val x_orth = 2 / (right - left)
    val y_orth = 2 / (top - bottom)
    val z_orth = -2 / (far - near)

    val tx = -(right + left) / (right - left)
    val ty = -(top + bottom) / (top - bottom)
    val tz = -(far + near) / (far - near)

    values(Matrix4.M00) = x_orth
    values(Matrix4.M10) = 0
    values(Matrix4.M20) = 0
    values(Matrix4.M30) = 0
    values(Matrix4.M01) = 0
    values(Matrix4.M11) = y_orth
    values(Matrix4.M21) = 0
    values(Matrix4.M31) = 0
    values(Matrix4.M02) = 0
    values(Matrix4.M12) = 0
    values(Matrix4.M22) = z_orth
    values(Matrix4.M32) = 0
    values(Matrix4.M03) = tx
    values(Matrix4.M13) = ty
    values(Matrix4.M23) = tz
    values(Matrix4.M33) = 1
    this
  }

  /** Sets the 4th column to the translation vector.
    * @param vector
    *   The translation vector
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setTranslation(vector: Vector3): Matrix4 = {
    values(Matrix4.M03) = vector.x
    values(Matrix4.M13) = vector.y
    values(Matrix4.M23) = vector.z
    this
  }

  /** Sets the 4th column to the translation vector.
    * @param x
    *   The X coordinate of the translation vector
    * @param y
    *   The Y coordinate of the translation vector
    * @param z
    *   The Z coordinate of the translation vector
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setTranslation(x: Float, y: Float, z: Float): Matrix4 = {
    values(Matrix4.M03) = x
    values(Matrix4.M13) = y
    values(Matrix4.M23) = z
    this
  }

  /** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the translation vector.
    * @param vector
    *   The translation vector
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToTranslation(vector: Vector3): Matrix4 = {
    idt()
    values(Matrix4.M03) = vector.x
    values(Matrix4.M13) = vector.y
    values(Matrix4.M23) = vector.z
    this
  }

  /** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the translation vector.
    * @param x
    *   The x-component of the translation vector.
    * @param y
    *   The y-component of the translation vector.
    * @param z
    *   The z-component of the translation vector.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToTranslation(x: Float, y: Float, z: Float): Matrix4 = {
    idt()
    values(Matrix4.M03) = x
    values(Matrix4.M13) = y
    values(Matrix4.M23) = z
    this
  }

  /** Sets this matrix to a translation and scaling matrix by first overwriting it with an identity and then setting the translation vector in the 4th column and the scaling vector in the diagonal.
    * @param translation
    *   The translation vector
    * @param scaling
    *   The scaling vector
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToTranslationAndScaling(translation: Vector3, scaling: Vector3): Matrix4 = {
    idt()
    values(Matrix4.M03) = translation.x
    values(Matrix4.M13) = translation.y
    values(Matrix4.M23) = translation.z
    values(Matrix4.M00) = scaling.x
    values(Matrix4.M11) = scaling.y
    values(Matrix4.M22) = scaling.z
    this
  }

  /** Sets this matrix to a translation and scaling matrix by first overwriting it with an identity and then setting the translation vector in the 4th column and the scaling vector in the diagonal.
    * @param translationX
    *   The x-component of the translation vector
    * @param translationY
    *   The y-component of the translation vector
    * @param translationZ
    *   The z-component of the translation vector
    * @param scalingX
    *   The x-component of the scaling vector
    * @param scalingY
    *   The x-component of the scaling vector
    * @param scalingZ
    *   The x-component of the scaling vector
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToTranslationAndScaling(translationX: Float, translationY: Float, translationZ: Float, scalingX: Float, scalingY: Float, scalingZ: Float): Matrix4 = {
    idt()
    values(Matrix4.M03) = translationX
    values(Matrix4.M13) = translationY
    values(Matrix4.M23) = translationZ
    values(Matrix4.M00) = scalingX
    values(Matrix4.M11) = scalingY
    values(Matrix4.M22) = scalingZ
    this
  }

  /** Sets the matrix to a rotation matrix around the given axis.
    * @param axis
    *   The axis
    * @param degrees
    *   The angle in degrees
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToRotation(axis: Vector3, degrees: Float): Matrix4 =
    if (degrees == 0) {
      idt()
      this
    } else {
      set(Matrix4.quat.set(axis, degrees))
    }

  /** Sets the matrix to a rotation matrix around the given axis.
    * @param axis
    *   The axis
    * @param radians
    *   The angle in radians
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToRotationRad(axis: Vector3, radians: Float): Matrix4 =
    if (radians == 0) {
      idt()
      this
    } else {
      set(Matrix4.quat.setFromAxisRad(axis, radians))
    }

  /** Sets the matrix to a rotation matrix around the given axis.
    * @param axisX
    *   The x-component of the axis
    * @param axisY
    *   The y-component of the axis
    * @param axisZ
    *   The z-component of the axis
    * @param degrees
    *   The angle in degrees
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToRotation(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): Matrix4 =
    if (degrees == 0) {
      idt()
      this
    } else {
      set(Matrix4.quat.setFromAxis(axisX, axisY, axisZ, degrees))
    }

  /** Sets the matrix to a rotation matrix around the given axis.
    * @param axisX
    *   The x-component of the axis
    * @param axisY
    *   The y-component of the axis
    * @param axisZ
    *   The z-component of the axis
    * @param radians
    *   The angle in radians
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToRotationRad(axisX: Float, axisY: Float, axisZ: Float, radians: Float): Matrix4 =
    if (radians == 0) {
      idt()
      this
    } else {
      set(Matrix4.quat.setFromAxisRad(axisX, axisY, axisZ, radians))
    }

  /** Set the matrix to a rotation matrix between two vectors.
    * @param v1
    *   The base vector
    * @param v2
    *   The target vector
    * @return
    *   This matrix for the purpose of chaining methods together
    */
  def setToRotation(v1: Vector3, v2: Vector3): Matrix4 =
    set(Matrix4.quat.setFromCross(v1, v2))

  /** Set the matrix to a rotation matrix between two vectors.
    * @param x1
    *   The base vectors x value
    * @param y1
    *   The base vectors y value
    * @param z1
    *   The base vectors z value
    * @param x2
    *   The target vector x value
    * @param y2
    *   The target vector y value
    * @param z2
    *   The target vector z value
    * @return
    *   This matrix for the purpose of chaining methods together
    */
  def setToRotation(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Matrix4 =
    set(Matrix4.quat.setFromCross(x1, y1, z1, x2, y2, z2))

  /** Sets this matrix to a rotation matrix from the given euler angles.
    * @param yaw
    *   the yaw in degrees
    * @param pitch
    *   the pitch in degrees
    * @param roll
    *   the roll in degrees
    * @return
    *   This matrix
    */
  def setFromEulerAngles(yaw: Float, pitch: Float, roll: Float): Matrix4 = {
    Matrix4.quat.setEulerAngles(yaw, pitch, roll)
    set(Matrix4.quat)
  }

  /** Sets this matrix to a rotation matrix from the given euler angles.
    * @param yaw
    *   the yaw in radians
    * @param pitch
    *   the pitch in radians
    * @param roll
    *   the roll in radians
    * @return
    *   This matrix
    */
  def setFromEulerAnglesRad(yaw: Float, pitch: Float, roll: Float): Matrix4 = {
    Matrix4.quat.setEulerAnglesRad(yaw, pitch, roll)
    set(Matrix4.quat)
  }

  /** Sets this matrix to a scaling matrix
    * @param vector
    *   The scaling vector
    * @return
    *   This matrix for chaining.
    */
  def setToScaling(vector: Vector3): Matrix4 = {
    idt()
    values(Matrix4.M00) = vector.x
    values(Matrix4.M11) = vector.y
    values(Matrix4.M22) = vector.z
    this
  }

  /** Sets this matrix to a scaling matrix
    * @param x
    *   The x-component of the scaling vector
    * @param y
    *   The y-component of the scaling vector
    * @param z
    *   The z-component of the scaling vector
    * @return
    *   This matrix for chaining.
    */
  def setToScaling(x: Float, y: Float, z: Float): Matrix4 = {
    idt()
    values(Matrix4.M00) = x
    values(Matrix4.M11) = y
    values(Matrix4.M22) = z
    this
  }

  /** Sets the matrix to a look at matrix with a direction and an up vector. Multiply with a translation matrix to get a camera model view matrix.
    * @param direction
    *   The direction vector
    * @param up
    *   The up vector
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def setToLookAt(direction: Vector3, up: Vector3): Matrix4 = {
    Matrix4.l_vez.set(direction).nor()
    Matrix4.l_vex.set(direction).crs(up).nor()
    Matrix4.l_vey.set(Matrix4.l_vex).crs(Matrix4.l_vez).nor()
    idt()
    values(Matrix4.M00) = Matrix4.l_vex.x
    values(Matrix4.M01) = Matrix4.l_vex.y
    values(Matrix4.M02) = Matrix4.l_vex.z
    values(Matrix4.M10) = Matrix4.l_vey.x
    values(Matrix4.M11) = Matrix4.l_vey.y
    values(Matrix4.M12) = Matrix4.l_vey.z
    values(Matrix4.M20) = -Matrix4.l_vez.x
    values(Matrix4.M21) = -Matrix4.l_vez.y
    values(Matrix4.M22) = -Matrix4.l_vez.z
    this
  }

  /** Sets this matrix to a look at matrix with the given position, target and up vector.
    * @param position
    *   the position
    * @param target
    *   the target
    * @param up
    *   the up vector
    * @return
    *   This matrix
    */
  def setToLookAt(position: Vector3, target: Vector3, up: Vector3): Matrix4 = {
    Matrix4.tmpVec.set(target).sub(position)
    setToLookAt(Matrix4.tmpVec, up)
    mul(Matrix4.tmpMat.setToTranslation(-position.x, -position.y, -position.z))
    this
  }

  def setToWorld(position: Vector3, forward: Vector3, up: Vector3): Matrix4 = {
    Matrix4.tmpForward.set(forward).nor()
    Matrix4.right.set(Matrix4.tmpForward).crs(up).nor()
    Matrix4.tmpUp.set(Matrix4.right).crs(Matrix4.tmpForward).nor()
    set(Matrix4.right, Matrix4.tmpUp, Matrix4.tmpForward.scl(-1), position)
    this
  }

  /** Linearly interpolates between this matrix and the given matrix mixing by alpha
    * @param matrix
    *   the matrix
    * @param alpha
    *   the alpha value in the range [0,1]
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def lerp(matrix: Matrix4, alpha: Float): Matrix4 = {
    for (i <- 0 until 16) values(i) = values(i) * (1 - alpha) + matrix.values(i) * alpha
    this
  }

  /** Averages the given transform with this one and stores the result in this matrix. Translations and scales are lerped while rotations are slerped.
    * @param other
    *   The other transform
    * @param w
    *   Weight of this transform; weight of the other transform is (1 - w)
    * @return
    *   This matrix for chaining
    */
  def avg(other: Matrix4, w: Float): Matrix4 = {
    getScale(Matrix4.tmpVec)
    other.getScale(Matrix4.tmpForward)

    getRotation(Matrix4.quat)
    other.getRotation(Matrix4.quat2)

    getTranslation(Matrix4.tmpUp)
    other.getTranslation(Matrix4.right)

    setToScaling(Matrix4.tmpVec.scl(w).add(Matrix4.tmpForward.scl(1 - w)))
    rotate(Matrix4.quat.slerp(Matrix4.quat2, 1 - w))
    setTranslation(Matrix4.tmpUp.scl(w).add(Matrix4.right.scl(1 - w)))
    this
  }

  /** Averages the given transforms and stores the result in this matrix. Translations and scales are lerped while rotations are slerped. Does not destroy the data contained in t.
    * @param t
    *   List of transforms
    * @return
    *   This matrix for chaining
    */
  def avg(t: Array[Matrix4]): Matrix4 = {
    val w = 1.0f / t.length

    Matrix4.tmpVec.set(t(0).getScale(Matrix4.tmpUp).scl(w))
    Matrix4.quat.set(t(0).getRotation(Matrix4.quat2).exp(w))
    Matrix4.tmpForward.set(t(0).getTranslation(Matrix4.tmpUp).scl(w))

    for (i <- 1 until t.length) {
      Matrix4.tmpVec.add(t(i).getScale(Matrix4.tmpUp).scl(w))
      Matrix4.quat.mul(t(i).getRotation(Matrix4.quat2).exp(w))
      Matrix4.tmpForward.add(t(i).getTranslation(Matrix4.tmpUp).scl(w))
    }
    Matrix4.quat.nor()

    setToScaling(Matrix4.tmpVec)
    rotate(Matrix4.quat)
    setTranslation(Matrix4.tmpForward)
    this
  }

  /** Averages the given transforms with the given weights and stores the result in this matrix. Translations and scales are lerped while rotations are slerped. Does not destroy the data contained in
    * t or w; Sum of w_i must be equal to 1, or unexpected results will occur.
    * @param t
    *   List of transforms
    * @param w
    *   List of weights
    * @return
    *   This matrix for chaining
    */
  def avg(t: Array[Matrix4], w: Array[Float]): Matrix4 = {
    Matrix4.tmpVec.set(t(0).getScale(Matrix4.tmpUp).scl(w(0)))
    Matrix4.quat.set(t(0).getRotation(Matrix4.quat2).exp(w(0)))
    Matrix4.tmpForward.set(t(0).getTranslation(Matrix4.tmpUp).scl(w(0)))

    for (i <- 1 until t.length) {
      Matrix4.tmpVec.add(t(i).getScale(Matrix4.tmpUp).scl(w(i)))
      Matrix4.quat.mul(t(i).getRotation(Matrix4.quat2).exp(w(i)))
      Matrix4.tmpForward.add(t(i).getTranslation(Matrix4.tmpUp).scl(w(i)))
    }
    Matrix4.quat.nor()

    setToScaling(Matrix4.tmpVec)
    rotate(Matrix4.quat)
    setTranslation(Matrix4.tmpForward)
    this
  }

  /** Sets this matrix to the given 3x3 matrix. The third column of this matrix is set to (0,0,1,0).
    * @param mat
    *   the matrix
    */
  def set(mat: Matrix3): Matrix4 = {
    values(0) = mat.values(0)
    values(1) = mat.values(1)
    values(2) = mat.values(2)
    values(3) = 0
    values(4) = mat.values(3)
    values(5) = mat.values(4)
    values(6) = mat.values(5)
    values(7) = 0
    values(8) = 0
    values(9) = 0
    values(10) = 1
    values(11) = 0
    values(12) = mat.values(6)
    values(13) = mat.values(7)
    values(14) = 0
    values(15) = mat.values(8)
    this
  }

  /** Sets this matrix to the given affine matrix. The values are mapped as follows:
    *
    * <pre> [ M00 M01 0 M02 ] [ M10 M11 0 M12 ] [ 0 0 1 0 ] [ 0 0 0 1 ] </pre>
    *
    * @param affine
    *   the affine matrix
    * @return
    *   This matrix for chaining
    */
  def set(affine: Affine2): Matrix4 = {
    values(Matrix4.M00) = affine.m00
    values(Matrix4.M10) = affine.m10
    values(Matrix4.M20) = 0
    values(Matrix4.M30) = 0
    values(Matrix4.M01) = affine.m01
    values(Matrix4.M11) = affine.m11
    values(Matrix4.M21) = 0
    values(Matrix4.M31) = 0
    values(Matrix4.M02) = 0
    values(Matrix4.M12) = 0
    values(Matrix4.M22) = 1
    values(Matrix4.M32) = 0
    values(Matrix4.M03) = affine.m02
    values(Matrix4.M13) = affine.m12
    values(Matrix4.M23) = 0
    values(Matrix4.M33) = 1
    this
  }

  /** Assumes that this matrix is a 2D affine transformation, copying only the relevant components. The values are mapped as follows:
    *
    * <pre> [ M00 M01 _ M02 ] [ M10 M11 _ M12 ] [ _ _ _ _ ] [ _ _ _ _ ] </pre>
    *
    * @param affine
    *   the source matrix
    * @return
    *   This matrix for chaining
    */
  def setAsAffine(affine: Affine2): Matrix4 = {
    values(Matrix4.M00) = affine.m00
    values(Matrix4.M10) = affine.m10
    values(Matrix4.M01) = affine.m01
    values(Matrix4.M11) = affine.m11
    values(Matrix4.M03) = affine.m02
    values(Matrix4.M13) = affine.m12
    this
  }

  /** Assumes that both matrices are 2D affine transformations, copying only the relevant components. The copied values are:
    *
    * <pre> [ M00 M01 _ M03 ] [ M10 M11 _ M13 ] [ _ _ _ _ ] [ _ _ _ _ ] </pre>
    *
    * @param mat
    *   the source matrix
    * @return
    *   This matrix for chaining
    */
  def setAsAffine(mat: Matrix4): Matrix4 = {
    values(Matrix4.M00) = mat.values(Matrix4.M00)
    values(Matrix4.M10) = mat.values(Matrix4.M10)
    values(Matrix4.M01) = mat.values(Matrix4.M01)
    values(Matrix4.M11) = mat.values(Matrix4.M11)
    values(Matrix4.M03) = mat.values(Matrix4.M03)
    values(Matrix4.M13) = mat.values(Matrix4.M13)
    this
  }

  def scl(scale: Vector3): Matrix4 = {
    values(Matrix4.M00) *= scale.x
    values(Matrix4.M11) *= scale.y
    values(Matrix4.M22) *= scale.z
    values(Matrix4.M01) *= scale.x
    values(Matrix4.M12) *= scale.y
    values(Matrix4.M20) *= scale.x
    values(Matrix4.M21) *= scale.y
    values(Matrix4.M22) *= scale.z
    values(Matrix4.M30) *= scale.x
    values(Matrix4.M31) *= scale.y
    values(Matrix4.M32) *= scale.z
    this
  }

  def scl(x: Float, y: Float, z: Float): Matrix4 = {
    values(Matrix4.M00) *= x
    values(Matrix4.M11) *= y
    values(Matrix4.M22) *= z
    values(Matrix4.M01) *= x
    values(Matrix4.M12) *= y
    values(Matrix4.M20) *= x
    values(Matrix4.M21) *= y
    values(Matrix4.M22) *= z
    values(Matrix4.M30) *= x
    values(Matrix4.M31) *= y
    values(Matrix4.M32) *= z
    this
  }

  def scl(scale: Float): Matrix4 = {
    values(Matrix4.M00) *= scale
    values(Matrix4.M11) *= scale
    values(Matrix4.M22) *= scale
    values(Matrix4.M01) *= scale
    values(Matrix4.M12) *= scale
    values(Matrix4.M20) *= scale
    values(Matrix4.M21) *= scale
    values(Matrix4.M22) *= scale
    values(Matrix4.M30) *= scale
    values(Matrix4.M31) *= scale
    values(Matrix4.M32) *= scale
    this
  }

  def getTranslation(position: Vector3): Vector3 = {
    position.x = values(Matrix4.M03)
    position.y = values(Matrix4.M13)
    position.z = values(Matrix4.M23)
    position
  }

  /** Gets the rotation of this matrix.
    * @param rotation
    *   The {@link Quaternion} to receive the rotation
    * @param normalizeAxes
    *   True to normalize the axes, necessary when the matrix might also include scaling.
    * @return
    *   The provided {@link Quaternion} for chaining.
    */
  def getRotation(rotation: Quaternion, normalizeAxes: Boolean): Quaternion =
    rotation.setFromMatrix(normalizeAxes, this)

  /** Gets the rotation of this matrix.
    * @param rotation
    *   The {@link Quaternion} to receive the rotation
    * @return
    *   The provided {@link Quaternion} for chaining.
    */
  def getRotation(rotation: Quaternion): Quaternion =
    rotation.setFromMatrix(this)

  /** @return the squared scale factor on the X axis */
  def getScaleXSquared(): Float =
    values(Matrix4.M00) * values(Matrix4.M00) + values(Matrix4.M01) * values(Matrix4.M01) + values(Matrix4.M02) * values(Matrix4.M02)

  /** @return the squared scale factor on the Y axis */
  def getScaleYSquared(): Float =
    values(Matrix4.M10) * values(Matrix4.M10) + values(Matrix4.M11) * values(Matrix4.M11) + values(Matrix4.M12) * values(Matrix4.M12)

  /** @return the squared scale factor on the Z axis */
  def getScaleZSquared(): Float =
    values(Matrix4.M20) * values(Matrix4.M20) + values(Matrix4.M21) * values(Matrix4.M21) + values(Matrix4.M22) * values(Matrix4.M22)

  /** @return the scale factor on the X axis (non-negative) */
  def getScaleX(): Float =
    if (MathUtils.isZero(values(Matrix4.M01)) && MathUtils.isZero(values(Matrix4.M02))) Math.abs(values(Matrix4.M00))
    else Math.sqrt(getScaleXSquared()).toFloat

  /** @return the scale factor on the Y axis (non-negative) */
  def getScaleY(): Float =
    if (MathUtils.isZero(values(Matrix4.M10)) && MathUtils.isZero(values(Matrix4.M12))) Math.abs(values(Matrix4.M11))
    else Math.sqrt(getScaleYSquared()).toFloat

  /** @return the scale factor on the X axis (non-negative) */
  def getScaleZ(): Float =
    if (MathUtils.isZero(values(Matrix4.M20)) && MathUtils.isZero(values(Matrix4.M21))) Math.abs(values(Matrix4.M22))
    else Math.sqrt(getScaleZSquared()).toFloat

  /** @param scale
    *   The vector which will receive the (non-negative) scale components on each axis.
    * @return
    *   The provided vector for chaining.
    */
  def getScale(scale: Vector3): Vector3 =
    scale.set(getScaleX(), getScaleY(), getScaleZ())

  /** removes the translational part and transposes the matrix. */
  def toNormalMatrix(): Matrix4 = {
    values(Matrix4.M03) = 0
    values(Matrix4.M13) = 0
    values(Matrix4.M23) = 0
    inv().tra()
  }

  override def toString: String =
    s"[${values(Matrix4.M00)}|${values(Matrix4.M01)}|${values(Matrix4.M02)}|${values(Matrix4.M03)}]\n" +
      s"[${values(Matrix4.M10)}|${values(Matrix4.M11)}|${values(Matrix4.M12)}|${values(Matrix4.M13)}]\n" +
      s"[${values(Matrix4.M20)}|${values(Matrix4.M21)}|${values(Matrix4.M22)}|${values(Matrix4.M23)}]\n" +
      s"[${values(Matrix4.M30)}|${values(Matrix4.M31)}|${values(Matrix4.M32)}|${values(Matrix4.M33)}]\n"

  /** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' glTranslate/glRotate/glScale
    * @param translation
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def translate(translation: Vector3): Matrix4 =
    translate(translation.x, translation.y, translation.z)

  /** Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param x
    *   Translation in the x-axis.
    * @param y
    *   Translation in the y-axis.
    * @param z
    *   Translation in the z-axis.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def translate(x: Float, y: Float, z: Float): Matrix4 = {
    values(Matrix4.M03) += values(Matrix4.M00) * x + values(Matrix4.M01) * y + values(Matrix4.M02) * z
    values(Matrix4.M13) += values(Matrix4.M10) * x + values(Matrix4.M11) * y + values(Matrix4.M12) * z
    values(Matrix4.M23) += values(Matrix4.M20) * x + values(Matrix4.M21) * y + values(Matrix4.M22) * z
    values(Matrix4.M33) += values(Matrix4.M30) * x + values(Matrix4.M31) * y + values(Matrix4.M32) * z
    this
  }

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param axis
    *   The vector axis to rotate around.
    * @param degrees
    *   The angle in degrees.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def rotate(axis: Vector3, degrees: Float): Matrix4 =
    if (degrees == 0) this
    else {
      Matrix4.quat.set(axis, degrees)
      rotate(Matrix4.quat)
    }

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param axis
    *   The vector axis to rotate around.
    * @param radians
    *   The angle in radians.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def rotateRad(axis: Vector3, radians: Float): Matrix4 =
    if (radians == 0) this
    else {
      Matrix4.quat.setFromAxisRad(axis, radians)
      rotate(Matrix4.quat)
    }

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale
    * @param axisX
    *   The x-axis component of the vector to rotate around.
    * @param axisY
    *   The y-axis component of the vector to rotate around.
    * @param axisZ
    *   The z-axis component of the vector to rotate around.
    * @param degrees
    *   The angle in degrees
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): Matrix4 =
    if (degrees == 0) this
    else {
      Matrix4.quat.setFromAxis(axisX, axisY, axisZ, degrees)
      rotate(Matrix4.quat)
    }

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale
    * @param axisX
    *   The x-axis component of the vector to rotate around.
    * @param axisY
    *   The y-axis component of the vector to rotate around.
    * @param axisZ
    *   The z-axis component of the vector to rotate around.
    * @param radians
    *   The angle in radians
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def rotateRad(axisX: Float, axisY: Float, axisZ: Float, radians: Float): Matrix4 =
    if (radians == 0) this
    else {
      Matrix4.quat.setFromAxisRad(axisX, axisY, axisZ, radians)
      rotate(Matrix4.quat)
    }

  /** Postmultiplies this matrix with a (counter-clockwise) rotation matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param rotation
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def rotate(rotation: Quaternion): Matrix4 = {
    val x  = rotation.x
    val y  = rotation.y
    val z  = rotation.z
    val w  = rotation.w
    val xx = x * x
    val xy = x * y
    val xz = x * z
    val xw = x * w
    val yy = y * y
    val yz = y * z
    val yw = y * w
    val zz = z * z
    val zw = z * w
    // Set matrix from quaternion
    values(Matrix4.M00) = 1 - 2 * (yy + zz)
    values(Matrix4.M01) = 2 * (xy - zw)
    values(Matrix4.M02) = 2 * (xz + yw)
    values(Matrix4.M03) = 0
    values(Matrix4.M10) = 2 * (xy + zw)
    values(Matrix4.M11) = 1 - 2 * (xx + zz)
    values(Matrix4.M12) = 2 * (yz - xw)
    values(Matrix4.M13) = 0
    values(Matrix4.M20) = 2 * (xz - yw)
    values(Matrix4.M21) = 2 * (yz + xw)
    values(Matrix4.M22) = 1 - 2 * (xx + yy)
    values(Matrix4.M23) = 0
    values(Matrix4.M30) = 0
    values(Matrix4.M31) = 0
    values(Matrix4.M32) = 0
    values(Matrix4.M33) = 1
    this
  }

  /** Postmultiplies this matrix by the rotation between two vectors.
    * @param v1
    *   The base vector
    * @param v2
    *   The target vector
    * @return
    *   This matrix for the purpose of chaining methods together
    */
  def rotate(v1: Vector3, v2: Vector3): Matrix4 =
    rotate(Matrix4.quat.setFromCross(v1, v2))

  /** Post-multiplies this matrix by a rotation toward a direction.
    * @param direction
    *   direction to rotate toward
    * @param up
    *   up vector
    * @return
    *   This matrix for chaining
    */
  def rotateTowardDirection(direction: Vector3, up: Vector3): Matrix4 = {
    Matrix4.l_vez.set(direction).nor()
    Matrix4.l_vex.set(direction).crs(up).nor()
    Matrix4.l_vey.set(Matrix4.l_vex).crs(Matrix4.l_vez).nor()
    values(Matrix4.M00) = Matrix4.l_vex.x
    values(Matrix4.M01) = Matrix4.l_vex.y
    values(Matrix4.M02) = Matrix4.l_vex.z
    values(Matrix4.M10) = Matrix4.l_vey.x
    values(Matrix4.M11) = Matrix4.l_vey.y
    values(Matrix4.M12) = Matrix4.l_vey.z
    values(Matrix4.M20) = -Matrix4.l_vez.x
    values(Matrix4.M21) = -Matrix4.l_vez.y
    values(Matrix4.M22) = -Matrix4.l_vez.z
    this
  }

  /** Post-multiplies this matrix by a rotation toward a target.
    * @param target
    *   the target to rotate to
    * @param up
    *   the up vector
    * @return
    *   This matrix for chaining
    */
  def rotateTowardTarget(target: Vector3, up: Vector3): Matrix4 = {
    Matrix4.tmpVec.set(target.x - values(Matrix4.M03), target.y - values(Matrix4.M13), target.z - values(Matrix4.M23))
    rotateTowardDirection(Matrix4.tmpVec, up)
  }

  /** Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
    * @param scaleX
    *   The scale in the x-axis.
    * @param scaleY
    *   The scale in the y-axis.
    * @param scaleZ
    *   The scale in the z-axis.
    * @return
    *   This matrix for the purpose of chaining methods together.
    */
  def scale(scaleX: Float, scaleY: Float, scaleZ: Float): Matrix4 = {
    values(Matrix4.M00) *= scaleX
    values(Matrix4.M01) *= scaleY
    values(Matrix4.M02) *= scaleZ
    values(Matrix4.M10) *= scaleX
    values(Matrix4.M11) *= scaleY
    values(Matrix4.M12) *= scaleZ
    values(Matrix4.M20) *= scaleX
    values(Matrix4.M21) *= scaleY
    values(Matrix4.M22) *= scaleZ
    values(Matrix4.M30) *= scaleX
    values(Matrix4.M31) *= scaleY
    values(Matrix4.M32) *= scaleZ
    this
  }

  /** Copies the 4x3 upper-left sub-matrix into float array. The destination array is supposed to be a column major matrix.
    * @param dst
    *   the destination matrix
    */
  def extract4x3Matrix(dst: Array[Float]): Unit = {
    dst(0) = values(Matrix4.M00)
    dst(1) = values(Matrix4.M10)
    dst(2) = values(Matrix4.M20)
    dst(3) = values(Matrix4.M01)
    dst(4) = values(Matrix4.M11)
    dst(5) = values(Matrix4.M21)
    dst(6) = values(Matrix4.M02)
    dst(7) = values(Matrix4.M12)
    dst(8) = values(Matrix4.M22)
    dst(9) = values(Matrix4.M03)
    dst(10) = values(Matrix4.M13)
    dst(11) = values(Matrix4.M23)
  }

  /** @return True if this matrix has any rotation or scaling, false otherwise */
  def hasRotationOrScaling(): Boolean =
    !(MathUtils.isEqual(values(Matrix4.M00), 1) && MathUtils.isEqual(values(Matrix4.M11), 1) && MathUtils.isEqual(values(Matrix4.M22), 1)
      && MathUtils.isZero(values(Matrix4.M01)) && MathUtils.isZero(values(Matrix4.M02)) && MathUtils.isZero(values(Matrix4.M10)) && MathUtils.isZero(values(Matrix4.M12))
      && MathUtils.isZero(values(Matrix4.M20)) && MathUtils.isZero(values(Matrix4.M21)))
}

object Matrix4 {
  val M00 = 0
  val M01 = 4
  val M02 = 8
  val M03 = 12
  val M10 = 1
  val M11 = 5
  val M12 = 9
  val M13 = 13
  val M20 = 2
  val M21 = 6
  val M22 = 10
  val M23 = 14
  val M30 = 3
  val M31 = 7
  val M32 = 11
  val M33 = 15

  // Static fields that were moved from the class
  private val quat       = new Quaternion()
  private val quat2      = new Quaternion()
  private val l_vez      = new Vector3()
  private val l_vex      = new Vector3()
  private val l_vey      = new Vector3()
  private val tmpVec     = new Vector3()
  private val tmpMat     = new Matrix4()
  private val right      = new Vector3()
  private val tmpForward = new Vector3()
  private val tmpUp      = new Vector3()
  // @off
  /*JNI
  #include <memory.h>
  #include <stdio.h>
  #include <string.h>

  #define M00 0
  #define M01 4
  #define M02 8
  #define M03 12
  #define M10 1
  #define M11 5
  #define M12 9
  #define M13 13
  #define M20 2
  #define M21 6
  #define M22 10
  #define M23 14
  #define M30 3
  #define M31 7
  #define M32 11
  #define M33 15

  static inline void matrix4_mul(float* mata, float* matb) {
    float tmp[16];
    tmp[M00] = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
    tmp[M01] = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
    tmp[M02] = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
    tmp[M03] = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
    tmp[M10] = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
    tmp[M11] = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
    tmp[M12] = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
    tmp[M13] = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
    tmp[M20] = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
    tmp[M21] = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
    tmp[M22] = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
    tmp[M23] = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
    tmp[M30] = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
    tmp[M31] = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
    tmp[M32] = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
    tmp[M33] = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];
    memcpy(mata, tmp, sizeof(float) *  16);
  }

  static inline void matrix4_mulVec(float* mat, float* vec) {
    float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03];
    float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13];
    float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23];
    vec[0] = x;
    vec[1] = y;
    vec[2] = z;
  }

  static inline void matrix4_proj(float* mat, float* vec) {
    float inv_w = 1.0f / (vec[0] * mat[M30] + vec[1] * mat[M31] + vec[2] * mat[M32] + mat[M33]);
    float x = (vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02] + mat[M03]) * inv_w;
    float y = (vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12] + mat[M13]) * inv_w;
    float z = (vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22] + mat[M23]) * inv_w;
    vec[0] = x;
    vec[1] = y;
    vec[2] = z;
  }

  static inline void matrix4_rot(float* mat, float* vec) {
    float x = vec[0] * mat[M00] + vec[1] * mat[M01] + vec[2] * mat[M02];
    float y = vec[0] * mat[M10] + vec[1] * mat[M11] + vec[2] * mat[M12];
    float z = vec[0] * mat[M20] + vec[1] * mat[M21] + vec[2] * mat[M22];
    vec[0] = x;
    vec[1] = y;
    vec[2] = z;
  }
   */

  /** Multiplies the vectors with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} . The vectors array is assumed to hold
    * 3-component vectors. Offset specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter specifies the number of vectors stored in the vectors
    * array. The stride parameter specifies the number of floats between subsequent vectors and must be >= 3. This is the same as {@link Vector3#mul(Matrix4)} applied to multiple vectors.
    * @param mat
    *   the matrix
    * @param vecs
    *   the vectors
    * @param offset
    *   the offset into the vectors array
    * @param numVecs
    *   the number of vectors
    * @param stride
    *   the stride between vectors in floats
    */
  def mulVec(mat: Array[Float], vecs: Array[Float], offset: Int, numVecs: Int, stride: Int): Unit = {
    var vecOffset = offset
    for (_ <- 0 until numVecs) {
      val vec = Array(vecs(vecOffset), vecs(vecOffset + 1), vecs(vecOffset + 2))
      mulVec(mat, vec)
      vecs(vecOffset) = vec(0)
      vecs(vecOffset + 1) = vec(1)
      vecs(vecOffset + 2) = vec(2)
      vecOffset += stride
    }
  }

  /** Multiplies the vectors with the given matrix, , performing a division by w. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} . The vectors
    * array is assumed to hold 3-component vectors. Offset specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter specifies the number of
    * vectors stored in the vectors array. The stride parameter specifies the number of floats between subsequent vectors and must be >= 3. This is the same as {@link Vector3#prj(Matrix4)} applied to
    * multiple vectors.
    * @param mat
    *   the matrix
    * @param vecs
    *   the vectors
    * @param offset
    *   the offset into the vectors array
    * @param numVecs
    *   the number of vectors
    * @param stride
    *   the stride between vectors in floats
    */
  def prj(mat: Array[Float], vecs: Array[Float], offset: Int, numVecs: Int, stride: Int): Unit = {
    var vecOffset = offset
    for (_ <- 0 until numVecs) {
      val vec = Array(vecs(vecOffset), vecs(vecOffset + 1), vecs(vecOffset + 2))
      prj(mat, vec)
      vecs(vecOffset) = vec(0)
      vecs(vecOffset + 1) = vec(1)
      vecs(vecOffset + 2) = vec(2)
      vecOffset += stride
    }
  }

  /** Multiplies the vectors with the top most 3x3 sub-matrix of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} . The
    * vectors array is assumed to hold 3-component vectors. Offset specifies the offset into the array where the x-component of the first vector is located. The numVecs parameter specifies the number
    * of vectors stored in the vectors array. The stride parameter specifies the number of floats between subsequent vectors and must be >= 3. This is the same as {@link Vector3#rot(Matrix4)} applied
    * to multiple vectors.
    * @param mat
    *   the matrix
    * @param vecs
    *   the vectors
    * @param offset
    *   the offset into the vectors array
    * @param numVecs
    *   the number of vectors
    * @param stride
    *   the stride between vectors in floats
    */
  def rot(mat: Array[Float], vecs: Array[Float], offset: Int, numVecs: Int, stride: Int): Unit = {
    var vecOffset = offset
    for (_ <- 0 until numVecs) {
      val vec = Array(vecs(vecOffset), vecs(vecOffset + 1), vecs(vecOffset + 2))
      rot(mat, vec)
      vecs(vecOffset) = vec(0)
      vecs(vecOffset + 1) = vec(1)
      vecs(vecOffset + 2) = vec(2)
      vecOffset += stride
    }
  }
  // @on

  /** Multiplies the matrix mata with matrix matb, storing the result in mata. The arrays are assumed to hold 4x4 column major matrices as you can get from {@link Matrix4#values} . This is the same as
    * {@link Matrix4#mul(Matrix4)} .
    *
    * @param mata
    *   the first matrix.
    * @param matb
    *   the second matrix.
    */
  def mul(mata: Array[Float], matb: Array[Float]): Unit = {
    val m00 = mata(Matrix4.M00) * matb(Matrix4.M00) + mata(Matrix4.M01) * matb(Matrix4.M10) + mata(Matrix4.M02) * matb(Matrix4.M20) + mata(Matrix4.M03) * matb(Matrix4.M30)
    val m01 = mata(Matrix4.M00) * matb(Matrix4.M01) + mata(Matrix4.M01) * matb(Matrix4.M11) + mata(Matrix4.M02) * matb(Matrix4.M21) + mata(Matrix4.M03) * matb(Matrix4.M31)
    val m02 = mata(Matrix4.M00) * matb(Matrix4.M02) + mata(Matrix4.M01) * matb(Matrix4.M12) + mata(Matrix4.M02) * matb(Matrix4.M22) + mata(Matrix4.M03) * matb(Matrix4.M32)
    val m03 = mata(Matrix4.M00) * matb(Matrix4.M03) + mata(Matrix4.M01) * matb(Matrix4.M13) + mata(Matrix4.M02) * matb(Matrix4.M23) + mata(Matrix4.M03) * matb(Matrix4.M33)
    val m10 = mata(Matrix4.M10) * matb(Matrix4.M00) + mata(Matrix4.M11) * matb(Matrix4.M10) + mata(Matrix4.M12) * matb(Matrix4.M20) + mata(Matrix4.M13) * matb(Matrix4.M30)
    val m11 = mata(Matrix4.M10) * matb(Matrix4.M01) + mata(Matrix4.M11) * matb(Matrix4.M11) + mata(Matrix4.M12) * matb(Matrix4.M21) + mata(Matrix4.M13) * matb(Matrix4.M31)
    val m12 = mata(Matrix4.M10) * matb(Matrix4.M02) + mata(Matrix4.M11) * matb(Matrix4.M12) + mata(Matrix4.M12) * matb(Matrix4.M22) + mata(Matrix4.M13) * matb(Matrix4.M32)
    val m13 = mata(Matrix4.M10) * matb(Matrix4.M03) + mata(Matrix4.M11) * matb(Matrix4.M13) + mata(Matrix4.M12) * matb(Matrix4.M23) + mata(Matrix4.M13) * matb(Matrix4.M33)
    val m20 = mata(Matrix4.M20) * matb(Matrix4.M00) + mata(Matrix4.M21) * matb(Matrix4.M10) + mata(Matrix4.M22) * matb(Matrix4.M20) + mata(Matrix4.M23) * matb(Matrix4.M30)
    val m21 = mata(Matrix4.M20) * matb(Matrix4.M01) + mata(Matrix4.M21) * matb(Matrix4.M11) + mata(Matrix4.M22) * matb(Matrix4.M21) + mata(Matrix4.M23) * matb(Matrix4.M31)
    val m22 = mata(Matrix4.M20) * matb(Matrix4.M02) + mata(Matrix4.M21) * matb(Matrix4.M12) + mata(Matrix4.M22) * matb(Matrix4.M22) + mata(Matrix4.M23) * matb(Matrix4.M32)
    val m23 = mata(Matrix4.M20) * matb(Matrix4.M03) + mata(Matrix4.M21) * matb(Matrix4.M13) + mata(Matrix4.M22) * matb(Matrix4.M23) + mata(Matrix4.M23) * matb(Matrix4.M33)
    val m30 = mata(Matrix4.M30) * matb(Matrix4.M00) + mata(Matrix4.M31) * matb(Matrix4.M10) + mata(Matrix4.M32) * matb(Matrix4.M20) + mata(Matrix4.M33) * matb(Matrix4.M30)
    val m31 = mata(Matrix4.M30) * matb(Matrix4.M01) + mata(Matrix4.M31) * matb(Matrix4.M11) + mata(Matrix4.M32) * matb(Matrix4.M21) + mata(Matrix4.M33) * matb(Matrix4.M31)
    val m32 = mata(Matrix4.M30) * matb(Matrix4.M02) + mata(Matrix4.M31) * matb(Matrix4.M12) + mata(Matrix4.M32) * matb(Matrix4.M22) + mata(Matrix4.M33) * matb(Matrix4.M32)
    val m33 = mata(Matrix4.M30) * matb(Matrix4.M03) + mata(Matrix4.M31) * matb(Matrix4.M13) + mata(Matrix4.M32) * matb(Matrix4.M23) + mata(Matrix4.M33) * matb(Matrix4.M33)
    mata(Matrix4.M00) = m00
    mata(Matrix4.M10) = m10
    mata(Matrix4.M20) = m20
    mata(Matrix4.M30) = m30
    mata(Matrix4.M01) = m01
    mata(Matrix4.M11) = m11
    mata(Matrix4.M21) = m21
    mata(Matrix4.M31) = m31
    mata(Matrix4.M02) = m02
    mata(Matrix4.M12) = m12
    mata(Matrix4.M22) = m22
    mata(Matrix4.M32) = m32
    mata(Matrix4.M03) = m03
    mata(Matrix4.M13) = m13
    mata(Matrix4.M23) = m23
    mata(Matrix4.M33) = m33
  }

  /** Multiplies the vector with the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} . The vector array is assumed to hold a
    * 3-component vector, with x being the first element, y being the second and z being the last component. The result is stored in the vector array. This is the same as {@link Vector3#mul(Matrix4)}
    * .
    * @param mat
    *   the matrix
    * @param vec
    *   the vector.
    */
  def mulVec(mat: Array[Float], vec: Array[Float]): Unit = {
    val x = vec(0) * mat(Matrix4.M00) + vec(1) * mat(Matrix4.M01) + vec(2) * mat(Matrix4.M02) + mat(Matrix4.M03)
    val y = vec(0) * mat(Matrix4.M10) + vec(1) * mat(Matrix4.M11) + vec(2) * mat(Matrix4.M12) + mat(Matrix4.M13)
    val z = vec(0) * mat(Matrix4.M20) + vec(1) * mat(Matrix4.M21) + vec(2) * mat(Matrix4.M22) + mat(Matrix4.M23)
    vec(0) = x
    vec(1) = y
    vec(2) = z
  }

  /** Multiplies the vector with the given matrix, performing a division by w. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} . The vector
    * array is assumed to hold a 3-component vector, with x being the first element, y being the second and z being the last component. The result is stored in the vector array. This is the same as
    * {@link Vector3#prj(Matrix4)} .
    * @param mat
    *   the matrix
    * @param vec
    *   the vector.
    */
  def prj(mat: Array[Float], vec: Array[Float]): Unit = {
    val inv_w = 1.0f / (vec(0) * mat(Matrix4.M30) + vec(1) * mat(Matrix4.M31) + vec(2) * mat(Matrix4.M32) + mat(Matrix4.M33))
    val x     = (vec(0) * mat(Matrix4.M00) + vec(1) * mat(Matrix4.M01) + vec(2) * mat(Matrix4.M02) + mat(Matrix4.M03)) * inv_w
    val y     = (vec(0) * mat(Matrix4.M10) + vec(1) * mat(Matrix4.M11) + vec(2) * mat(Matrix4.M12) + mat(Matrix4.M13)) * inv_w
    val z     = (vec(0) * mat(Matrix4.M20) + vec(1) * mat(Matrix4.M21) + vec(2) * mat(Matrix4.M22) + mat(Matrix4.M23)) * inv_w
    vec(0) = x
    vec(1) = y
    vec(2) = z
  }

  /** Multiplies the vector with the top most 3x3 sub-matrix of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} . The vector
    * array is assumed to hold a 3-component vector, with x being the first element, y being the second and z being the last component. The result is stored in the vector array. This is the same as
    * {@link Vector3#rot(Matrix4)} .
    * @param mat
    *   the matrix
    * @param vec
    *   the vector.
    */
  def rot(mat: Array[Float], vec: Array[Float]): Unit = {
    val x = vec(0) * mat(Matrix4.M00) + vec(1) * mat(Matrix4.M01) + vec(2) * mat(Matrix4.M02)
    val y = vec(0) * mat(Matrix4.M10) + vec(1) * mat(Matrix4.M11) + vec(2) * mat(Matrix4.M12)
    val z = vec(0) * mat(Matrix4.M20) + vec(1) * mat(Matrix4.M21) + vec(2) * mat(Matrix4.M22)
    vec(0) = x
    vec(1) = y
    vec(2) = z
  }

  /** Computes the inverse of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} .
    * @param values
    *   the matrix values.
    * @return
    *   false in case the inverse could not be calculated, true otherwise.
    */
  def inv(values: Array[Float]): Boolean = {
    val l_det = det(values)
    if (l_det == 0) false
    else {
      val m00 = values(Matrix4.M12) * values(Matrix4.M23) * values(Matrix4.M31) - values(Matrix4.M13) * values(Matrix4.M22) * values(Matrix4.M31)
        + values(Matrix4.M13) * values(Matrix4.M21) * values(Matrix4.M32) - values(Matrix4.M11) * values(Matrix4.M23) * values(Matrix4.M32)
        - values(Matrix4.M12) * values(Matrix4.M21) * values(Matrix4.M33) + values(Matrix4.M11) * values(Matrix4.M22) * values(Matrix4.M33)
      val m01 = values(Matrix4.M03) * values(Matrix4.M22) * values(Matrix4.M31) - values(Matrix4.M02) * values(Matrix4.M23) * values(Matrix4.M31)
        - values(Matrix4.M03) * values(Matrix4.M21) * values(Matrix4.M32) + values(Matrix4.M01) * values(Matrix4.M23) * values(Matrix4.M32)
        + values(Matrix4.M02) * values(Matrix4.M21) * values(Matrix4.M33) - values(Matrix4.M01) * values(Matrix4.M22) * values(Matrix4.M33)
      val m02 = values(Matrix4.M02) * values(Matrix4.M13) * values(Matrix4.M31) - values(Matrix4.M03) * values(Matrix4.M12) * values(Matrix4.M31)
        + values(Matrix4.M03) * values(Matrix4.M11) * values(Matrix4.M32) - values(Matrix4.M01) * values(Matrix4.M13) * values(Matrix4.M32)
        - values(Matrix4.M02) * values(Matrix4.M11) * values(Matrix4.M33) + values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M33)
      val m03 = values(Matrix4.M03) * values(Matrix4.M12) * values(Matrix4.M21) - values(Matrix4.M02) * values(Matrix4.M13) * values(Matrix4.M21)
        - values(Matrix4.M03) * values(Matrix4.M11) * values(Matrix4.M22) + values(Matrix4.M01) * values(Matrix4.M13) * values(Matrix4.M22)
        + values(Matrix4.M02) * values(Matrix4.M11) * values(Matrix4.M23) - values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M23)
      val m10 = values(Matrix4.M13) * values(Matrix4.M22) * values(Matrix4.M30) - values(Matrix4.M12) * values(Matrix4.M23) * values(Matrix4.M30)
        - values(Matrix4.M13) * values(Matrix4.M20) * values(Matrix4.M32) + values(Matrix4.M10) * values(Matrix4.M23) * values(Matrix4.M32)
        + values(Matrix4.M12) * values(Matrix4.M20) * values(Matrix4.M33) - values(Matrix4.M10) * values(Matrix4.M22) * values(Matrix4.M33)
      val m11 = values(Matrix4.M02) * values(Matrix4.M23) * values(Matrix4.M30) - values(Matrix4.M03) * values(Matrix4.M22) * values(Matrix4.M30)
        + values(Matrix4.M03) * values(Matrix4.M20) * values(Matrix4.M32) - values(Matrix4.M00) * values(Matrix4.M23) * values(Matrix4.M32)
        - values(Matrix4.M02) * values(Matrix4.M20) * values(Matrix4.M33) + values(Matrix4.M00) * values(Matrix4.M22) * values(Matrix4.M33)
      val m12 = values(Matrix4.M03) * values(Matrix4.M12) * values(Matrix4.M30) - values(Matrix4.M02) * values(Matrix4.M13) * values(Matrix4.M30)
        - values(Matrix4.M03) * values(Matrix4.M10) * values(Matrix4.M32) + values(Matrix4.M00) * values(Matrix4.M13) * values(Matrix4.M32)
        + values(Matrix4.M02) * values(Matrix4.M10) * values(Matrix4.M33) - values(Matrix4.M00) * values(Matrix4.M12) * values(Matrix4.M33)
      val m13 = values(Matrix4.M02) * values(Matrix4.M13) * values(Matrix4.M20) - values(Matrix4.M03) * values(Matrix4.M12) * values(Matrix4.M20)
        + values(Matrix4.M03) * values(Matrix4.M10) * values(Matrix4.M22) - values(Matrix4.M00) * values(Matrix4.M13) * values(Matrix4.M22)
        - values(Matrix4.M02) * values(Matrix4.M10) * values(Matrix4.M23) + values(Matrix4.M00) * values(Matrix4.M12) * values(Matrix4.M23)
      val m20 = values(Matrix4.M11) * values(Matrix4.M23) * values(Matrix4.M30) - values(Matrix4.M13) * values(Matrix4.M21) * values(Matrix4.M30)
        + values(Matrix4.M13) * values(Matrix4.M20) * values(Matrix4.M31) - values(Matrix4.M10) * values(Matrix4.M23) * values(Matrix4.M31)
        - values(Matrix4.M11) * values(Matrix4.M20) * values(Matrix4.M33) + values(Matrix4.M10) * values(Matrix4.M21) * values(Matrix4.M33)
      val m21 = values(Matrix4.M03) * values(Matrix4.M21) * values(Matrix4.M30) - values(Matrix4.M01) * values(Matrix4.M23) * values(Matrix4.M30)
        - values(Matrix4.M03) * values(Matrix4.M20) * values(Matrix4.M31) + values(Matrix4.M00) * values(Matrix4.M23) * values(Matrix4.M31)
        + values(Matrix4.M01) * values(Matrix4.M20) * values(Matrix4.M33) - values(Matrix4.M00) * values(Matrix4.M21) * values(Matrix4.M33)
      val m22 = values(Matrix4.M01) * values(Matrix4.M13) * values(Matrix4.M30) - values(Matrix4.M03) * values(Matrix4.M11) * values(Matrix4.M30)
        + values(Matrix4.M03) * values(Matrix4.M10) * values(Matrix4.M31) - values(Matrix4.M00) * values(Matrix4.M13) * values(Matrix4.M31)
        - values(Matrix4.M01) * values(Matrix4.M10) * values(Matrix4.M33) + values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M33)
      val m23 = values(Matrix4.M03) * values(Matrix4.M11) * values(Matrix4.M20) - values(Matrix4.M01) * values(Matrix4.M13) * values(Matrix4.M20)
        - values(Matrix4.M03) * values(Matrix4.M10) * values(Matrix4.M21) + values(Matrix4.M00) * values(Matrix4.M13) * values(Matrix4.M21)
        + values(Matrix4.M01) * values(Matrix4.M10) * values(Matrix4.M23) - values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M23)
      val m30 = values(Matrix4.M12) * values(Matrix4.M21) * values(Matrix4.M30) - values(Matrix4.M11) * values(Matrix4.M22) * values(Matrix4.M30)
        - values(Matrix4.M12) * values(Matrix4.M20) * values(Matrix4.M31) + values(Matrix4.M10) * values(Matrix4.M22) * values(Matrix4.M31)
        + values(Matrix4.M11) * values(Matrix4.M20) * values(Matrix4.M32) - values(Matrix4.M10) * values(Matrix4.M21) * values(Matrix4.M32)
      val m31 = values(Matrix4.M01) * values(Matrix4.M22) * values(Matrix4.M30) - values(Matrix4.M02) * values(Matrix4.M21) * values(Matrix4.M30)
        + values(Matrix4.M02) * values(Matrix4.M20) * values(Matrix4.M31) - values(Matrix4.M00) * values(Matrix4.M22) * values(Matrix4.M31)
        - values(Matrix4.M01) * values(Matrix4.M20) * values(Matrix4.M32) + values(Matrix4.M00) * values(Matrix4.M21) * values(Matrix4.M32)
      val m32 = values(Matrix4.M02) * values(Matrix4.M11) * values(Matrix4.M30) - values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M30)
        - values(Matrix4.M02) * values(Matrix4.M10) * values(Matrix4.M31) + values(Matrix4.M00) * values(Matrix4.M12) * values(Matrix4.M31)
        + values(Matrix4.M01) * values(Matrix4.M10) * values(Matrix4.M32) - values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M32)
      val m33 = values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M20) - values(Matrix4.M02) * values(Matrix4.M11) * values(Matrix4.M20)
        + values(Matrix4.M02) * values(Matrix4.M10) * values(Matrix4.M21) - values(Matrix4.M00) * values(Matrix4.M12) * values(Matrix4.M21)
        - values(Matrix4.M01) * values(Matrix4.M10) * values(Matrix4.M22) + values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M22)
      val inv_det = 1.0f / l_det
      values(Matrix4.M00) = m00 * inv_det
      values(Matrix4.M10) = m10 * inv_det
      values(Matrix4.M20) = m20 * inv_det
      values(Matrix4.M30) = m30 * inv_det
      values(Matrix4.M01) = m01 * inv_det
      values(Matrix4.M11) = m11 * inv_det
      values(Matrix4.M21) = m21 * inv_det
      values(Matrix4.M31) = m31 * inv_det
      values(Matrix4.M02) = m02 * inv_det
      values(Matrix4.M12) = m12 * inv_det
      values(Matrix4.M22) = m22 * inv_det
      values(Matrix4.M32) = m32 * inv_det
      values(Matrix4.M03) = m03 * inv_det
      values(Matrix4.M13) = m13 * inv_det
      values(Matrix4.M23) = m23 * inv_det
      values(Matrix4.M33) = m33 * inv_det
      true
    }
  }

  /** Computes the determinante of the given matrix. The matrix array is assumed to hold a 4x4 column major matrix as you can get from {@link Matrix4#values} .
    * @param values
    *   the matrix values.
    * @return
    *   the determinante.
    */
  def det(values: Array[Float]): Float =
    values(Matrix4.M30) * values(Matrix4.M21) * values(Matrix4.M12) * values(Matrix4.M03) - values(Matrix4.M20) * values(Matrix4.M31) * values(Matrix4.M12) * values(Matrix4.M03)
      - values(Matrix4.M30) * values(Matrix4.M11) * values(Matrix4.M22) * values(Matrix4.M03) + values(Matrix4.M10) * values(Matrix4.M31) * values(Matrix4.M22) * values(Matrix4.M03)
      + values(Matrix4.M20) * values(Matrix4.M11) * values(Matrix4.M32) * values(Matrix4.M03) - values(Matrix4.M10) * values(Matrix4.M21) * values(Matrix4.M32) * values(Matrix4.M03)
      - values(Matrix4.M30) * values(Matrix4.M21) * values(Matrix4.M02) * values(Matrix4.M13) + values(Matrix4.M20) * values(Matrix4.M31) * values(Matrix4.M02) * values(Matrix4.M13)
      + values(Matrix4.M30) * values(Matrix4.M01) * values(Matrix4.M22) * values(Matrix4.M13) - values(Matrix4.M00) * values(Matrix4.M31) * values(Matrix4.M22) * values(Matrix4.M13)
      - values(Matrix4.M20) * values(Matrix4.M01) * values(Matrix4.M32) * values(Matrix4.M13) + values(Matrix4.M00) * values(Matrix4.M21) * values(Matrix4.M32) * values(Matrix4.M13)
      + values(Matrix4.M30) * values(Matrix4.M11) * values(Matrix4.M02) * values(Matrix4.M23) - values(Matrix4.M10) * values(Matrix4.M31) * values(Matrix4.M02) * values(Matrix4.M23)
      - values(Matrix4.M30) * values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M23) + values(Matrix4.M00) * values(Matrix4.M31) * values(Matrix4.M12) * values(Matrix4.M23)
      + values(Matrix4.M10) * values(Matrix4.M01) * values(Matrix4.M32) * values(Matrix4.M23) - values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M32) * values(Matrix4.M23)
      - values(Matrix4.M20) * values(Matrix4.M11) * values(Matrix4.M02) * values(Matrix4.M33) + values(Matrix4.M10) * values(Matrix4.M21) * values(Matrix4.M02) * values(Matrix4.M33)
      + values(Matrix4.M20) * values(Matrix4.M01) * values(Matrix4.M12) * values(Matrix4.M33) - values(Matrix4.M00) * values(Matrix4.M21) * values(Matrix4.M12) * values(Matrix4.M33)
      - values(Matrix4.M10) * values(Matrix4.M01) * values(Matrix4.M22) * values(Matrix4.M33) + values(Matrix4.M00) * values(Matrix4.M11) * values(Matrix4.M22) * values(Matrix4.M33)
}
