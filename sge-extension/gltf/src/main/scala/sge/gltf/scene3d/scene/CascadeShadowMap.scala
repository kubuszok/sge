/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/CascadeShadowMap.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Cascade shadow map class contains several DirectionalShadowLight with different view boxes.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 154
 * Covenant-baseline-methods: CascadeShadowMap,attribute,box,center,centerLight,close,createLight,dot,far,h,halfFrustumDepth,hd,i,lightMatrix,lights,near,offset,rate,setBaseLightBounds,setCascadeBounds,setCascades,splitPoints,splitRates,syncExtraCascades,w
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package scene

import sge.Sge
import sge.gltf.scene3d.attributes.CascadeShadowMapAttribute
import sge.gltf.scene3d.lights.DirectionalShadowLight
import sge.graphics.Camera
import sge.math.{ Matrix4, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.DynamicArray

class CascadeShadowMap(protected val cascadeCount: Int)(using Sge) extends AutoCloseable {

  val lights:    DynamicArray[DirectionalShadowLight] = DynamicArray[DirectionalShadowLight](cascadeCount)
  val attribute: CascadeShadowMapAttribute            = CascadeShadowMapAttribute(this)

  protected val splitRates: DynamicArray[Float] = DynamicArray[Float](cascadeCount + 2)
  private val splitPoints:  Array[Vector3]      = Array.fill(8)(Vector3())
  private val lightMatrix:  Matrix4             = Matrix4()
  private val box:          BoundingBox         = BoundingBox()
  private val center:       Vector3             = Vector3()
  private val offset:       Vector3             = Vector3()

  override def close(): Unit = {
    var i = 0
    while (i < lights.size) {
      lights(i).close()
      i += 1
    }
    lights.clear()
  }

  /** Setup base light and extra cascades based on scene camera frustum. With automatic split rates. */
  def setCascades(sceneCamera: Camera, base: DirectionalShadowLight, minLightDepth: Float, splitDivisor: Float): Unit = {
    splitRates.clear()
    var rate = 1f
    var i    = 0
    while (i < cascadeCount + 1) {
      splitRates.add(rate)
      rate /= splitDivisor
      i += 1
    }
    splitRates.add(0f)
    splitRates.reverse()
    setCascades(sceneCamera, base, minLightDepth, splitRates)
  }

  /** Setup base light and extra cascades with user defined split rates. */
  def setCascades(sceneCamera: Camera, base: DirectionalShadowLight, minLightDepth: Float, splitRates: DynamicArray[Float]): Unit = {
    if (splitRates.size != cascadeCount + 2) {
      throw new IllegalArgumentException("Invalid splitRates, expected " + (cascadeCount + 2) + " items.")
    }

    base.direction.nor()
    syncExtraCascades(base)
    setBaseLightBounds(base, sceneCamera, minLightDepth)

    var i = 0
    while (i < cascadeCount) {
      val splitNear = splitRates(i)
      val splitFar  = splitRates(i + 1)
      val light     = lights(i)
      setCascadeBounds(light, base, sceneCamera, splitNear, splitFar, minLightDepth)
      i += 1
    }
  }

  protected def setBaseLightBounds(shadowLight: DirectionalShadowLight, cam: Camera, minLightDepth: Float): Unit = {
    lightMatrix.setToLookAt(shadowLight.direction, shadowLight.getCamera().up)
    box.inf()
    var i = 0
    while (i < splitPoints.length) {
      val v = splitPoints(i).set(cam.frustum.planePoints(i)).mul(lightMatrix)
      box.ext(v)
      i += 1
    }
    box.center(center)
    center.mul(lightMatrix.tra())
    val halfFrustumDepth = box.depth / 2f
    centerLight(shadowLight, center, box.width, box.height, -halfFrustumDepth - minLightDepth, halfFrustumDepth)
  }

  private def setCascadeBounds(shadowLight: DirectionalShadowLight, base: DirectionalShadowLight, cam: Camera, splitNear: Float, splitFar: Float, minLightDepth: Float): Unit = {

    var i = 0
    while (i < 4) {
      val a = cam.frustum.planePoints(i)
      val b = cam.frustum.planePoints(i + 4)
      splitPoints(i).set(a).lerp(b, splitNear)
      splitPoints(i + 4).set(a).lerp(b, splitFar)
      i += 1
    }

    lightMatrix.setToLookAt(shadowLight.direction, shadowLight.getCamera().up)
    box.inf()
    i = 0
    while (i < splitPoints.length) {
      val v = splitPoints(i).mul(lightMatrix)
      box.ext(v)
      i += 1
    }
    box.center(center)
    center.mul(lightMatrix.tra())

    val dot  = -offset.set(center).sub(base.getCamera().position).dot(base.direction)
    val near = base.getCamera().near + dot
    val far  = box.depth / 2f

    centerLight(shadowLight, center, box.width, box.height, near, far)
  }

  private def centerLight(shadowLight: DirectionalShadowLight, position: Vector3, w: Float, h: Float, n: Float, f: Float): Unit = {
    val hd = (f - n) / 2f
    shadowLight.setCenter(position.mulAdd(shadowLight.direction, n + hd))
    shadowLight.setViewport(w, h, -hd, hd)
  }

  protected def syncExtraCascades(base: DirectionalShadowLight): Unit = {
    val w = base.getFrameBuffer().width.toInt
    val h = base.getFrameBuffer().height.toInt
    var i = 0
    while (i < cascadeCount) {
      var light: DirectionalShadowLight = null.asInstanceOf[DirectionalShadowLight] // @nowarn — assigned below
      if (i < lights.size) {
        light = lights(i)
        if (light.getFrameBuffer().width.toInt != w || light.getFrameBuffer().height.toInt != h) {
          light.close()
          light = createLight(w, h)
          lights(i) = light
        }
      } else {
        light = createLight(w, h)
        lights.add(light)
      }
      light.direction.set(base.direction)
      light.getCamera().up.set(base.getCamera().up)
      i += 1
    }
  }

  protected def createLight(width: Int, height: Int): DirectionalShadowLight =
    DirectionalShadowLight(width, height)
}
