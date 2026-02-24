/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Camera.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.graphics.g2d.Batch
import sge.math.Frustum
import sge.math.Matrix4
import sge.math.Quaternion
import sge.math.Vector3
import sge.math.collision.Ray
import sge.utils.Nullable
import sge.Sge

/** Base class for OrthographicCamera and PerspectiveCamera.
  * @author
  *   mzechner
  */
abstract class Camera(implicit sge: Sge) {

  /** the position of the camera * */
  val position: Vector3 = new Vector3()

  /** the unit length direction vector of the camera * */
  val direction: Vector3 = new Vector3(0, 0, -1)

  /** the unit length up vector of the camera * */
  val up: Vector3 = new Vector3(0, 1, 0)

  /** the projection matrix * */
  val projection: Matrix4 = new Matrix4()

  /** the view matrix * */
  val view: Matrix4 = new Matrix4()

  /** the combined projection and view matrix * */
  val combined: Matrix4 = new Matrix4()

  /** the inverse combined projection and view matrix * */
  val invProjectionView: Matrix4 = new Matrix4()

  /** the near clipping plane distance, has to be positive * */
  var near: Float = 1

  /** the far clipping plane distance, has to be positive * */
  var far: Float = 100

  /** the viewport width * */
  var viewportWidth: Float = 0

  /** the viewport height * */
  var viewportHeight: Float = 0

  /** the frustum * */
  val frustum: Frustum = new Frustum()

  private val tmpVec = new Vector3()
  private val ray    = new Ray(new Vector3(), new Vector3())

  /** Recalculates the projection and view matrix of this camera and the Frustum planes. Use this after you've manipulated any of the attributes of the camera.
    */
  def update(): Unit

  /** Recalculates the projection and view matrix of this camera and the Frustum planes if updateFrustum is true. Use this after you've manipulated any of the attributes of the camera.
    */
  def update(updateFrustum: Boolean): Unit

  /** Recalculates the direction of the camera to look at the point (x, y, z). This function assumes the up vector is normalized.
    * @param x
    *   the x-coordinate of the point to look at
    * @param y
    *   the y-coordinate of the point to look at
    * @param z
    *   the z-coordinate of the point to look at
    */
  def lookAt(x: Float, y: Float, z: Float): Unit = {
    tmpVec.set(x, y, z).sub(position).nor()
    if (!tmpVec.isZero(0.000001f)) {
      val dot = tmpVec.dot(up) // up and direction must ALWAYS be orthonormal vectors
      if (Math.abs(dot - 1) < 0.000000001f) {
        // Collinear
        up.set(direction).scl(-1)
      } else if (Math.abs(dot + 1) < 0.000000001f) {
        // Collinear opposite
        up.set(direction)
      }
      direction.set(tmpVec)
      normalizeUp()
    }
  }

  /** Recalculates the direction of the camera to look at the point (x, y, z).
    * @param target
    *   the point to look at
    */
  def lookAt(target: Vector3): Unit =
    lookAt(target.x, target.y, target.z)

  /** Normalizes the up vector by first calculating the right vector via a cross product between direction and up, and then recalculating the up vector via a cross product between right and direction.
    */
  def normalizeUp(): Unit = {
    tmpVec.set(direction).crs(up)
    up.set(tmpVec).crs(direction).nor()
  }

  /** Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector will not be orthogonalized.
    *
    * @param angle
    *   the angle
    * @param axisX
    *   the x-component of the axis
    * @param axisY
    *   the y-component of the axis
    * @param axisZ
    *   the z-component of the axis
    */
  def rotate(angle: Float, axisX: Float, axisY: Float, axisZ: Float): Unit = {
    direction.rotateDeg(angle, axisX, axisY, axisZ)
    up.rotateDeg(angle, axisX, axisY, axisZ)
  }

  /** Rotates the direction and up vector of this camera by the given angle around the given axis. The direction and up vector will not be orthogonalized.
    *
    * @param axis
    *   the axis to rotate around
    * @param angle
    *   the angle, in degrees
    */
  def rotate(axis: Vector3, angle: Float): Unit = {
    direction.rotateDeg(angle, axis.x, axis.y, axis.z)
    up.rotateDeg(angle, axis.x, axis.y, axis.z)
  }

  /** Rotates the direction and up vector of this camera by the given rotation matrix. The direction and up vector will not be orthogonalized.
    *
    * @param transform
    *   The rotation matrix
    */
  def rotate(transform: Matrix4): Unit = {
    direction.mul(transform)
    up.mul(transform)
  }

  /** Rotates the direction and up vector of this camera by the given Quaternion. The direction and up vector will not be orthogonalized.
    *
    * @param quat
    *   The quaternion
    */
  def rotate(quat: Quaternion): Unit = {
    quat.transform(direction)
    quat.transform(up)
  }

  /** Rotates the direction and up vector of this camera by the given angle around the given axis, with the axis attached to given point. The direction and up vector will not be orthogonalized.
    *
    * @param point
    *   the point to attach the axis to
    * @param axis
    *   the axis to rotate around
    * @param angle
    *   the angle, in degrees
    */
  def rotateAround(point: Vector3, axis: Vector3, angle: Float): Unit = {
    tmpVec.set(point)
    tmpVec.sub(position)
    translate(tmpVec)
    rotate(axis, angle)
    tmpVec.rotateDeg(angle, axis.x, axis.y, axis.z)
    translate(-tmpVec.x, -tmpVec.y, -tmpVec.z)
  }

  /** Transform the position, direction and up vector by the given matrix
    *
    * @param transform
    *   The transform matrix
    */
  def transform(transform: Matrix4): Unit = {
    position.mul(transform)
    rotate(transform)
  }

  /** Moves the camera by the given amount on each axis.
    * @param x
    *   the displacement on the x-axis
    * @param y
    *   the displacement on the y-axis
    * @param z
    *   the displacement on the z-axis
    */
  def translate(x: Float, y: Float, z: Float): Unit =
    position.add(x, y, z)

  /** Moves the camera by the given vector.
    * @param vec
    *   the displacement vector
    */
  def translate(vec: Vector3): Unit =
    position.add(vec)

  /** Function to translate a point given in touch coordinates to world space. It's the same as GLU gluUnProject, but does not rely on OpenGL. The x- and y-coordinate of vec are assumed to be in touch
    * coordinates (origin is the top left corner, y pointing down, x pointing to the right) as reported by the touch methods in Input. A z-coordinate of 0 will return a point on the near plane, a
    * z-coordinate of 1 will return a point on the far plane. This method allows you to specify the viewport position and dimensions in the coordinate system expected by GL20.glViewport(int, int, int,
    * int), with the origin in the bottom left corner of the screen.
    * @param touchCoords
    *   the point in touch coordinates (origin top left)
    * @param viewportX
    *   the coordinate of the bottom left corner of the viewport in glViewport coordinates.
    * @param viewportY
    *   the coordinate of the bottom left corner of the viewport in glViewport coordinates.
    * @param viewportWidth
    *   the width of the viewport in pixels
    * @param viewportHeight
    *   the height of the viewport in pixels
    * @return
    *   the mutated and unprojected touchCoords Vector3
    */
  def unproject(touchCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 = {
    val x = touchCoords.x - viewportX
    val y = sge.graphics.getHeight() - touchCoords.y - viewportY
    touchCoords.x = (2 * x) / viewportWidth - 1
    touchCoords.y = (2 * y) / viewportHeight - 1
    touchCoords.z = 2 * touchCoords.z - 1
    touchCoords.prj(invProjectionView)
    touchCoords
  }

  /** Function to translate a point given in touch coordinates to world space. It's the same as GLU gluUnProject but does not rely on OpenGL. The viewport is assumed to span the whole screen and is
    * fetched from Graphics.getWidth() and Graphics.getHeight(). The x- and y-coordinate of vec are assumed to be in touch coordinates (origin is the top left corner, y pointing down, x pointing to
    * the right) as reported by the touch methods in Input. A z-coordinate of 0 will return a point on the near plane, a z-coordinate of 1 will return a point on the far plane.
    * @param touchCoords
    *   the point in touch coordinates
    * @return
    *   the mutated and unprojected touchCoords Vector3
    */
  def unproject(touchCoords: Vector3): Vector3 = {
    unproject(touchCoords, 0, 0, sge.graphics.getWidth().toFloat, sge.graphics.getHeight().toFloat)
    touchCoords
  }

  /** Projects the Vector3 given in world space to screen coordinates. It's the same as GLU gluProject with one small deviation: The viewport is assumed to span the whole screen. The screen coordinate
    * system has its origin in the bottom left, with the y-axis pointing upwards and the x-axis pointing to the right. This makes it easily useable in conjunction with Batch and similar classes.
    * @return
    *   the mutated and projected worldCoords Vector3
    */
  def project(worldCoords: Vector3): Vector3 = {
    project(worldCoords, 0, 0, sge.graphics.getWidth().toFloat, sge.graphics.getHeight().toFloat)
    worldCoords
  }

  /** Projects the Vector3 given in world space to screen coordinates. It's the same as GLU gluProject with one small deviation: The viewport is assumed to span the whole screen. The screen coordinate
    * system has its origin in the bottom left, with the y-axis pointing upwards and the x-axis pointing to the right. This makes it easily useable in conjunction with Batch and similar classes. This
    * method allows you to specify the viewport position and dimensions in the coordinate system expected by GL20.glViewport(int, int, int, int), with the origin in the bottom left corner of the
    * screen.
    * @param viewportX
    *   the coordinate of the bottom left corner of the viewport in glViewport coordinates.
    * @param viewportY
    *   the coordinate of the bottom left corner of the viewport in glViewport coordinates.
    * @param viewportWidth
    *   the width of the viewport in pixels
    * @param viewportHeight
    *   the height of the viewport in pixels
    * @return
    *   the mutated and projected worldCoords Vector3
    */
  def project(worldCoords: Vector3, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Vector3 = {
    worldCoords.prj(combined)
    worldCoords.x = viewportWidth * (worldCoords.x + 1) / 2 + viewportX
    worldCoords.y = viewportHeight * (worldCoords.y + 1) / 2 + viewportY
    worldCoords.z = (worldCoords.z + 1) / 2
    worldCoords
  }

  /** Creates a picking Ray from the coordinates given in touch coordinates. It is assumed that the viewport spans the whole screen. The touch coordinates origin is assumed to be in the top left
    * corner, its y-axis pointing down, the x-axis pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
    * @param viewportX
    *   the coordinate of the bottom left corner of the viewport in glViewport coordinates.
    * @param viewportY
    *   the coordinate of the bottom left corner of the viewport in glViewport coordinates.
    * @param viewportWidth
    *   the width of the viewport in pixels
    * @param viewportHeight
    *   the height of the viewport in pixels
    * @return
    *   the picking Ray.
    */
  def getPickRay(touchX: Float, touchY: Float, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float): Ray = {
    unproject(ray.origin.set(touchX, touchY, 0), viewportX, viewportY, viewportWidth, viewportHeight)
    unproject(ray.direction.set(touchX, touchY, 1), viewportX, viewportY, viewportWidth, viewportHeight)
    ray.direction.sub(ray.origin).nor()
    ray
  }

  /** Creates a picking Ray from the coordinates given in touch coordinates. It is assumed that the viewport spans the whole screen. The touch coordinates origin is assumed to be in the top left
    * corner, its y-axis pointing down, the x-axis pointing to the right. The returned instance is not a new instance but an internal member only accessible via this function.
    * @return
    *   the picking Ray.
    */
  def getPickRay(touchX: Float, touchY: Float): Ray =
    getPickRay(touchX, touchY, 0, 0, sge.graphics.getWidth().toFloat, sge.graphics.getHeight().toFloat)
}
