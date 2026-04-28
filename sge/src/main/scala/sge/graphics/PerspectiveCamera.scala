/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/PerspectiveCamera.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Convention: anonymous (using Sge) + Sge() accessor
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: PerspectiveCamera,aspect,fieldOfView,this,tmp,update
 * Covenant-source-reference: com/badlogic/gdx/graphics/PerspectiveCamera.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics

import sge.math.Vector3
import sge.WorldUnits

/** A Camera with perspective projection.
  *
  * @author
  *   mzechner
  */
class PerspectiveCamera(using Sge) extends Camera {

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
  def this(fieldOfViewY: Float, viewportWidth: WorldUnits, viewportHeight: WorldUnits)(using Sge) = {
    this()
    this.fieldOfView = fieldOfViewY
    this.viewportWidth = viewportWidth
    this.viewportHeight = viewportHeight
    update()
  }

  private val tmp = Vector3()

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
