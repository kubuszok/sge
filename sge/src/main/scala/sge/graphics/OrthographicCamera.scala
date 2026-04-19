/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/OrthographicCamera.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: anonymous (using Sge) + Sge() accessor; removed dead Vector3 allocation
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 115
 * Covenant-baseline-methods: OrthographicCamera,rotate,setToOrtho,this,translate,update,vh,vw,zoom
 * Covenant-source-reference: com/badlogic/gdx/graphics/OrthographicCamera.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics

import sge.math.Vector2
import sge.WorldUnits

/** A camera with orthographic projection.
  *
  * @author
  *   mzechner
  */
class OrthographicCamera(using Sge) extends Camera {

  /** the zoom of the camera * */
  var zoom: Float = 1

  this.near = 0

  /** Constructs a new OrthographicCamera, using the given viewport width and height. For pixel perfect 2D rendering just supply the screen size, for other unit scales (e.g. meters for box2d) proceed
    * accordingly. The camera will show the region [-viewportWidth/2, -(viewportHeight/2-1)] - [(viewportWidth/2-1), viewportHeight/2]
    * @param viewportWidth
    *   the viewport width
    * @param viewportHeight
    *   the viewport height
    */
  def this(viewportWidth: WorldUnits, viewportHeight: WorldUnits)(using Sge) = {
    this()
    this.viewportWidth = viewportWidth
    this.viewportHeight = viewportHeight
    this.near = 0
    update()
  }

  override def update(): Unit =
    update(true)

  override def update(updateFrustum: Boolean): Unit = {
    val vw = viewportWidth.toFloat
    val vh = viewportHeight.toFloat
    projection.setToOrtho(zoom * -vw / 2, zoom * (vw / 2), zoom * -(vh / 2), zoom * vh / 2, near, far)
    view.setToLookAt(direction, up)
    view.translate(-position.x, -position.y, -position.z)
    combined.set(projection)
    combined.mul(view)

    if (updateFrustum) {
      invProjectionView.set(combined)
      invProjectionView.inv()
      frustum.update(invProjectionView)
    }
  }

  /** Sets this camera to an orthographic projection using a viewport fitting the screen resolution, centered at (sge.graphics.getWidth()/2, sge.graphics.getHeight()/2), with the y-axis pointing up or
    * down.
    * @param yDown
    *   whether y should be pointing down
    */
  def setToOrtho(yDown: Boolean): Unit =
    setToOrtho(yDown, WorldUnits(Sge().graphics.width.toFloat), WorldUnits(Sge().graphics.height.toFloat))

  /** Sets this camera to an orthographic projection, centered at (viewportWidth/2, viewportHeight/2), with the y-axis pointing up or down.
    * @param yDown
    *   whether y should be pointing down.
    * @param viewportWidth
    * @param viewportHeight
    */
  def setToOrtho(yDown: Boolean, viewportWidth: WorldUnits, viewportHeight: WorldUnits): Unit = {
    if (yDown) {
      up.set(0, -1, 0)
      direction.set(0, 0, 1)
    } else {
      up.set(0, 1, 0)
      direction.set(0, 0, -1)
    }
    position.set(zoom * viewportWidth.toFloat / 2.0f, zoom * viewportHeight.toFloat / 2.0f, 0)
    this.viewportWidth = viewportWidth
    this.viewportHeight = viewportHeight
    update()
  }

  /** Rotates the camera by the given angle around the direction vector. The direction and up vector will not be orthogonalized.
    * @param angle
    */
  def rotate(angle: Float): Unit =
    rotate(direction, angle)

  /** Moves the camera by the given amount on each axis.
    * @param x
    *   the displacement on the x-axis
    * @param y
    *   the displacement on the y-axis
    */
  def translate(x: Float, y: Float): Unit =
    translate(x, y, 0)

  /** Moves the camera by the given vector.
    * @param vec
    *   the displacement vector
    */
  def translate(vec: Vector2): Unit =
    translate(vec.x, vec.y, 0)
}
