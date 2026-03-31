/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/MirrorSource.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Mirror source renders reflected scene into a framebuffer.
 */
package sge
package gltf
package scene3d
package scene

import sge.{ Pixels, Sge }
import sge.gltf.scene3d.attributes.{ ClippingPlaneAttribute, MirrorSourceAttribute }
import sge.graphics.{ Camera, ClearMask, Pixmap, Texture }
import sge.graphics.g3d.Environment
import sge.graphics.glutils.FrameBuffer
import sge.math.{ Plane, Vector3 }
import sge.utils.Nullable

class MirrorSource(using Sge) extends AutoCloseable {

  protected var fbo: FrameBuffer = scala.compiletime.uninitialized
  private var width:  Int = 0
  private var height: Int = 0
  private val originalCameraPosition:  Vector3 = Vector3()
  private val originalCameraDirection: Vector3 = Vector3()
  private val originalCameraUp:        Vector3 = Vector3()
  private val planeOrigin:  Vector3 = Vector3()
  private val planeToCamera: Vector3 = Vector3()

  /** enable/disable scene clipping. When enabled, objects behind mirror are not rendered. */
  var clipScene: Boolean = true

  protected val mirrorAttribute: MirrorSourceAttribute = MirrorSourceAttribute()

  private val clippingPlane: ClippingPlaneAttribute = ClippingPlaneAttribute(Vector3.Y, 0f)
  private var camera:      Camera      = scala.compiletime.uninitialized
  private var environment: Environment = scala.compiletime.uninitialized
  private var skyBox:      SceneSkybox = scala.compiletime.uninitialized

  mirrorAttribute.textureDescription.minFilter = Texture.TextureFilter.MipMap
  mirrorAttribute.textureDescription.magFilter = Texture.TextureFilter.Linear
  mirrorAttribute.normal.set(clippingPlane.plane.normal)

  protected def createFrameBuffer(width: Int, height: Int): FrameBuffer =
    FrameBuffer(Pixmap.Format.RGBA8888, Pixels(width), Pixels(height), true)

  def setSize(width: Int, height: Int): Unit = {
    this.width = width
    this.height = height
  }

  def setPlane(nx: Float, ny: Float, nz: Float, d: Float): Unit = {
    clippingPlane.plane.normal.set(nx, ny, nz).nor()
    clippingPlane.plane.d = d
    mirrorAttribute.normal.set(clippingPlane.plane.normal)
  }

  def set(nx: Float, ny: Float, nz: Float, d: Float, clipScene: Boolean): MirrorSource = {
    setPlane(nx, ny, nz, d)
    this.clipScene = clipScene
    this
  }

  def begin(camera: Camera, environment: Environment, skyBox: SceneSkybox)(using sge: Sge): Unit = {
    this.camera = camera
    this.environment = environment
    this.skyBox = skyBox
    setupCamera(camera, clippingPlane.plane)
    if (skyBox != null) skyBox.update(camera, 0f) // @nowarn
    if (clipScene) environment.set(clippingPlane)
    ensureFrameBufferSize(width, height)
    fbo.begin()
    sge.graphics.gl.glClearColor(0f, 0f, 0f, 0f)
    sge.graphics.gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
  }

  def end()(using sge: Sge): Unit = {
    fbo.end()
    val texture = fbo.colorBufferTexture
    texture.bind()
    sge.graphics.gl.glGenerateMipmap(texture.glTarget)
    mirrorAttribute.textureDescription.texture = Nullable(fbo.colorBufferTexture)
    restoreCamera(camera)
    environment.set(mirrorAttribute)
    environment.remove(ClippingPlaneAttribute.Type)
    if (skyBox != null) skyBox.update(camera, 0f) // @nowarn
    this.camera = null.asInstanceOf[Camera] // @nowarn
    this.environment = null.asInstanceOf[Environment] // @nowarn
    this.skyBox = null.asInstanceOf[SceneSkybox] // @nowarn
  }

  private def setupCamera(camera: Camera, plane: Plane): Unit = {
    originalCameraPosition.set(camera.position)
    originalCameraDirection.set(camera.direction)
    originalCameraUp.set(camera.up)
    planeOrigin.set(clippingPlane.plane.normal).scl(clippingPlane.plane.d)
    planeToCamera.set(camera.position).sub(planeOrigin)
    camera.position.sub(planeToCamera)
    reflect(planeToCamera, clippingPlane.plane.normal)
    camera.position.add(planeToCamera)
    reflect(camera.direction, clippingPlane.plane.normal)
    reflect(camera.up, clippingPlane.plane.normal)
    camera.update()
  }

  private def reflect(vector: Vector3, normal: Vector3): Unit = {
    vector.mulAdd(normal, -2f * vector.dot(normal))
  }

  private def restoreCamera(camera: Camera): Unit = {
    camera.position.set(originalCameraPosition)
    camera.direction.set(originalCameraDirection)
    camera.up.set(originalCameraUp)
    camera.update()
  }

  private def ensureFrameBufferSize(width: Int, height: Int)(using sge: Sge): Unit = {
    val w = if (width <= 0) sge.graphics.backBufferWidth.toInt else width
    val h = if (height <= 0) sge.graphics.backBufferHeight.toInt else height
    if (fbo == null || fbo.width.toInt != w || fbo.height.toInt != h) { // @nowarn
      if (fbo != null) fbo.close() // @nowarn
      fbo = createFrameBuffer(w, h)
    }
  }

  override def close(): Unit = {
    if (fbo != null) fbo.close() // @nowarn
  }
}
