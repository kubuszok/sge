/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/lights/DirectionalShadowLight.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Copied from original deprecated DirectionalShadowLight with new features.
 */
package sge
package gltf
package scene3d
package lights

import sge.{ Pixels, Sge, WorldUnits }
import sge.graphics.{ Camera, ClearMask, EnableCap, OrthographicCamera, Pixmap, Texture }
import sge.graphics.g3d.environment.ShadowMap
import sge.graphics.g3d.utils.TextureDescriptor
import sge.graphics.glutils.FrameBuffer
import sge.math.{ Matrix4, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.Nullable

class DirectionalShadowLight(
  shadowMapWidth: Int,
  shadowMapHeight: Int,
  shadowViewportWidth: Float,
  shadowViewportHeight: Float,
  shadowNear: Float,
  shadowFar: Float
)(using Sge) extends DirectionalLightEx with ShadowMap with AutoCloseable {

  protected var fbo: FrameBuffer = createFrameBuffer(shadowMapWidth, shadowMapHeight)
  protected var cam: Camera = {
    val c = OrthographicCamera(WorldUnits(shadowViewportWidth), WorldUnits(shadowViewportHeight))
    c.near = shadowNear
    c.far = shadowFar
    c
  }
  protected val tmpV: Vector3 = Vector3()
  protected val textureDesc: TextureDescriptor[Texture] = {
    val td = TextureDescriptor[Texture]()
    td.minFilter = Texture.TextureFilter.Nearest
    td.magFilter = Texture.TextureFilter.Nearest
    td.uWrap = Texture.TextureWrap.ClampToEdge
    td.vWrap = Texture.TextureWrap.ClampToEdge
    td
  }
  protected val center: Vector3 = Vector3()

  def this()(using Sge) =
    this(1024, 1024, 100f, 100f, 0f, 100f)

  def this(shadowMapWidth: Int, shadowMapHeight: Int)(using Sge) =
    this(shadowMapWidth, shadowMapHeight, 100f, 100f, 0f, 100f)

  def setShadowMapSize(shadowMapWidth: Int, shadowMapHeight: Int): DirectionalShadowLight = {
    if (fbo == null || fbo.width.toInt != shadowMapWidth || fbo.height.toInt != shadowMapHeight) { // @nowarn — lifecycle null check
      if (fbo != null) fbo.close() // @nowarn — lifecycle null check
      fbo = createFrameBuffer(shadowMapWidth, shadowMapHeight)
    }
    this
  }

  protected def createFrameBuffer(width: Int, height: Int): FrameBuffer =
    FrameBuffer(Pixmap.Format.RGBA8888, Pixels(width), Pixels(height), true)

  def setViewport(shadowViewportWidth: Float, shadowViewportHeight: Float, shadowNear: Float, shadowFar: Float): DirectionalShadowLight = {
    cam.viewportWidth = WorldUnits(shadowViewportWidth)
    cam.viewportHeight = WorldUnits(shadowViewportHeight)
    cam.near = shadowNear
    cam.far = shadowFar
    this
  }

  def setCenter(center: Vector3): DirectionalShadowLight = {
    this.center.set(center)
    this
  }

  def getCenter(center: Vector3): Vector3 =
    center.set(this.center)

  def setCenter(x: Float, y: Float, z: Float): DirectionalShadowLight = {
    this.center.set(x, y, z)
    this
  }

  def setBounds(box: BoundingBox): DirectionalShadowLight = {
    val w = box.width
    val h = box.height
    val d = box.depth
    val s = Math.max(Math.max(w, h), d)
    val wd = s * DirectionalShadowLight.SQRT2
    box.center(center)
    setViewport(wd, wd, 0f, wd)
  }

  protected def validate()(using sge: Sge): Unit = {
    val halfDepth = cam.near + 0.5f * (cam.far - cam.near)
    cam.position.set(direction).scl(-halfDepth).add(center)
    cam.direction.set(direction).nor()
    cam.normalizeUp()
    cam.update()
  }

  def begin()(using sge: Sge): Unit = {
    validate()
    val w = fbo.width
    val h = fbo.height
    fbo.begin()
    sge.graphics.gl.glViewport(Pixels.zero, Pixels.zero, w, h)
    sge.graphics.gl.glClearColor(1f, 1f, 1f, 1f)
    sge.graphics.gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
    sge.graphics.gl.glEnable(EnableCap.ScissorTest)
    sge.graphics.gl.glScissor(Pixels(1), Pixels(1), w - Pixels(2), h - Pixels(2))
  }

  def end()(using sge: Sge): Unit = {
    sge.graphics.gl.glDisable(EnableCap.ScissorTest)
    fbo.end()
  }

  def getFrameBuffer(): FrameBuffer = fbo

  def getCamera(): Camera = cam

  override def projViewTrans: Matrix4 = cam.combined

  override def depthMap: TextureDescriptor[Texture] = {
    textureDesc.texture = Nullable(fbo.colorBufferTexture)
    textureDesc
  }

  override def close(): Unit = {
    if (fbo != null) fbo.close() // @nowarn — lifecycle null check
    fbo = null.asInstanceOf[FrameBuffer] // @nowarn — Java interop lifecycle pattern
  }

  override def equals(other: Any): Boolean = other match {
    case dsl: DirectionalShadowLight => (dsl eq this)
    case _                           => false
  }
}

object DirectionalShadowLight {
  protected val SQRT2: Float = Math.sqrt(2.0).toFloat
}
