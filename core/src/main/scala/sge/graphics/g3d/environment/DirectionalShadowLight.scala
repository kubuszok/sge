/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/DirectionalShadowLight.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package environment

import sge.graphics.Camera
import sge.graphics.GL20
import sge.graphics.OrthographicCamera
import sge.graphics.Pixmap
import sge.graphics.Texture
import sge.graphics.g3d.utils.TextureDescriptor
import sge.graphics.glutils.FrameBuffer
import sge.math.Matrix4
import sge.math.Vector3
import sge.utils.Nullable

/** @author Xoppa */
class DirectionalShadowLight(
  shadowMapWidth:       Int,
  shadowMapHeight:      Int,
  shadowViewportWidth:  Float,
  shadowViewportHeight: Float,
  shadowNear:           Float,
  shadowFar:            Float
)(using Sge)
    extends DirectionalLight
    with ShadowMap
    with AutoCloseable {

  protected var fbo: Nullable[FrameBuffer] =
    Nullable(new FrameBuffer(Pixmap.Format.RGBA8888, shadowMapWidth, shadowMapHeight, true))
  protected val cam: Camera = {
    val c = new OrthographicCamera(shadowViewportWidth, shadowViewportHeight)
    c.near = shadowNear
    c.far = shadowFar
    c
  }
  protected val halfDepth:   Float                      = shadowNear + 0.5f * (shadowFar - shadowNear)
  protected val halfHeight:  Float                      = shadowViewportHeight * 0.5f
  protected val tmpV:        Vector3                    = new Vector3()
  protected val textureDesc: TextureDescriptor[Texture] = {
    val td = new TextureDescriptor[Texture]()
    td.minFilter = Nullable(Texture.TextureFilter.Nearest)
    td.magFilter = Nullable(Texture.TextureFilter.Nearest)
    td.uWrap = Nullable(Texture.TextureWrap.ClampToEdge)
    td.vWrap = Nullable(Texture.TextureWrap.ClampToEdge)
    td
  }

  def update(camera: Camera): Unit =
    update(tmpV.set(camera.direction).scl(halfHeight), camera.direction)

  def update(center: Vector3, forward: Vector3): Unit = {
    cam.position.set(direction).scl(-halfDepth).add(center)
    cam.direction.set(direction).nor()
    cam.normalizeUp()
    cam.update()
  }

  def begin(camera: Camera): Unit = {
    update(camera)
    begin()
  }

  def begin(center: Vector3, forward: Vector3): Unit = {
    update(center, forward)
    begin()
  }

  def begin(): Unit =
    fbo.foreach { fb =>
      val w = fb.getWidth()
      val h = fb.getHeight()
      fb.begin()
      Sge().graphics.gl.glViewport(0, 0, w, h)
      Sge().graphics.gl.glClearColor(1, 1, 1, 1)
      Sge().graphics.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT)
      Sge().graphics.gl.glEnable(GL20.GL_SCISSOR_TEST)
      Sge().graphics.gl.glScissor(1, 1, w - 2, h - 2)
    }

  def end(): Unit = {
    Sge().graphics.gl.glDisable(GL20.GL_SCISSOR_TEST)
    fbo.foreach(_.end())
  }

  def getFrameBuffer(): Nullable[FrameBuffer] = fbo

  def getCamera(): Camera = cam

  override def getProjViewTrans(): Matrix4 =
    cam.combined

  override def getDepthMap(): TextureDescriptor[Texture] = {
    fbo.foreach { fb =>
      textureDesc.texture = Nullable(fb.getColorBufferTexture())
    }
    textureDesc
  }

  override def close(): Unit = {
    fbo.foreach(_.close())
    fbo = Nullable.empty
  }
}
