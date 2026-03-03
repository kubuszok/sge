/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/PerspectiveCamera.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Issues: old-style implicit sge: Sge instead of (using Sge)
 *   TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 *   TODO: opaque Pixels for viewportWidth/Height constructor params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.math.Vector3

/** A Camera with perspective projection.
  *
  * @author
  *   mzechner
  */
class PerspectiveCamera(implicit sge: Sge) extends Camera {

  /** the field of view of the height, in degrees * */
  var fieldOfView: Float = 67

  /** Constructs a new {@link PerspectiveCamera} with the given field of view and viewport size. The aspect ratio is derived from the viewport size.
    *
    * @param fieldOfViewY
    *   the field of view of the height, in degrees, the field of view for the width will be calculated according to the aspect ratio.
    * @param viewportWidth
    *   the viewport width
    * @param viewportHeight
    *   the viewport height
    */
  def this(fieldOfViewY: Float, viewportWidth: Float, viewportHeight: Float)(implicit sge: Sge) = {
    this()
    this.fieldOfView = fieldOfViewY
    this.viewportWidth = viewportWidth
    this.viewportHeight = viewportHeight
    update()
  }

  private val tmp = new Vector3()

  override def update(): Unit =
    update(true)

  override def update(updateFrustum: Boolean): Unit = {
    val aspect = viewportWidth / viewportHeight
    projection.setToProjection(Math.abs(near), Math.abs(far), fieldOfView, aspect)
    view.setToLookAt(position, tmp.set(position).add(direction), up)
    combined.set(projection)
    combined.mul(view)

    if (updateFrustum) {
      invProjectionView.set(combined)
      invProjectionView.inv()
      frustum.update(invProjectionView)
    }
  }
}
