/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/environment/DirectionalShadowLight.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Audit: pass (2026-03-03)
 *   - All 11 methods ported: update x2, begin x3, end, frameBuffer,
 *     camera, projViewTrans, depthMap, close
 *   - Disposable → AutoCloseable; dispose() → close()
 *   - Gdx.gl → Sge().graphics.gl (using Sge context parameter)
 *   - fbo: FrameBuffer → Nullable[FrameBuffer] for null safety
 *   - begin() wraps in fbo.foreach for null safety
 *   - Fixes (2026-03-04): getFrameBuffer/getCamera/getProjViewTrans/getDepthMap → property accessors
 */
package sge
package graphics
package g3d
package environment

import sge.graphics.Camera
import sge.graphics.ClearMask
import sge.graphics.EnableCap
import sge.graphics.OrthographicCamera
import sge.graphics.Pixmap
import sge.graphics.Texture
import sge.graphics.g3d.utils.TextureDescriptor
import sge.graphics.glutils.FrameBuffer
import sge.math.Matrix4
import sge.math.Vector3
import sge.utils.Nullable

import scala.annotation.publicInBinary

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
    Nullable(FrameBuffer(Pixmap.Format.RGBA8888, Pixels(shadowMapWidth), Pixels(shadowMapHeight), true))
  protected val cam: Camera = {
    val c = OrthographicCamera(shadowViewportWidth, shadowViewportHeight)
    c.near = shadowNear
    c.far = shadowFar
    c
  }
  protected val halfDepth:   Float                      = shadowNear + 0.5f * (shadowFar - shadowNear)
  protected val halfHeight:  Float                      = shadowViewportHeight * 0.5f
  protected val tmpV:        Vector3                    = Vector3()
  protected val textureDesc: TextureDescriptor[Texture] = {
    val td = TextureDescriptor[Texture]()
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

  @publicInBinary private[sge] def begin(camera: Camera): Unit = {
    update(camera)
    begin()
  }

  @publicInBinary private[sge] def begin(center: Vector3, forward: Vector3): Unit = {
    update(center, forward)
    begin()
  }

  @publicInBinary private[sge] def begin(): Unit =
    fbo.foreach { fb =>
      val w = fb.getWidth()
      val h = fb.getHeight()
      fb.begin()
      Sge().graphics.gl.glViewport(Pixels.zero, Pixels.zero, w, h)
      Sge().graphics.gl.glClearColor(1, 1, 1, 1)
      Sge().graphics.gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
      Sge().graphics.gl.glEnable(EnableCap.ScissorTest)
      Sge().graphics.gl.glScissor(Pixels(1), Pixels(1), w - Pixels(2), h - Pixels(2))
    }

  @publicInBinary private[sge] def end(): Unit = {
    Sge().graphics.gl.glDisable(EnableCap.ScissorTest)
    fbo.foreach(_.end())
  }

  /** Executes `body` between [[begin]] and [[end]], ensuring [[end]] is called even if `body` throws. */
  inline def rendering[A](inline body: => A): A = {
    begin()
    try body
    finally end()
  }

  def frameBuffer: Nullable[FrameBuffer] = fbo

  def camera: Camera = cam

  override def projViewTrans: Matrix4 =
    cam.combined

  override def depthMap: TextureDescriptor[Texture] = {
    fbo.foreach { fb =>
      textureDesc.texture = Nullable(fb.colorBufferTexture)
    }
    textureDesc
  }

  override def close(): Unit = {
    fbo.foreach(_.close())
    fbo = Nullable.empty
  }
}
