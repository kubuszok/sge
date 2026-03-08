/*
 * SGE Demos — 3D model viewer with procedural shapes, lighting, and decals.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package viewer3d

import scala.compiletime.uninitialized

import _root_.sge.{Pixels, Sge}
import _root_.sge.graphics.{Color, PerspectiveCamera, Pixmap, Texture}
import _root_.sge.graphics.VertexAttributes.Usage
import _root_.sge.graphics.g2d.TextureRegion
import _root_.sge.graphics.g3d.{Environment, Material, Model, ModelBatch, ModelInstance}
import _root_.sge.graphics.g3d.attributes.ColorAttribute
import _root_.sge.graphics.g3d.decals.{CameraGroupStrategy, Decal, DecalBatch}
import _root_.sge.graphics.g3d.environment.{DirectionalLight, PointLight}
import _root_.sge.graphics.g3d.utils.ModelBuilder
import _root_.sge.graphics.glutils.ShapeRenderer
import _root_.sge.graphics.glutils.ShapeRenderer.ShapeType
import _root_.sge.math.{MathUtils, Vector3}
import _root_.sge.utils.{Nullable, ScreenUtils}
import _root_.sge.demos.shared.DemoScene

/** 3D model viewer: five procedural shapes with directional + point lighting,
  * orbiting camera, billboard decals, and a floor grid.
  */
object Viewer3dGame extends DemoScene {

  override def name: String = "3D Viewer"

  // Resources
  private var camera:       PerspectiveCamera       = uninitialized
  private var modelBatch:   ModelBatch               = uninitialized
  private var environment:  Environment              = uninitialized
  private var shapeRenderer: ShapeRenderer           = uninitialized
  private var decalBatch:   DecalBatch               = uninitialized
  private var decalStrategy: CameraGroupStrategy     = uninitialized

  // Models (one per shape)
  private var boxModel:      Model = uninitialized
  private var sphereModel:   Model = uninitialized
  private var cylinderModel: Model = uninitialized
  private var coneModel:     Model = uninitialized
  private var capsuleModel:  Model = uninitialized

  // Instances
  private var boxInstance:      ModelInstance = uninitialized
  private var sphereInstance:   ModelInstance = uninitialized
  private var cylinderInstance: ModelInstance = uninitialized
  private var coneInstance:     ModelInstance = uninitialized
  private var capsuleInstance:  ModelInstance = uninitialized

  // Decals
  private var decals:        Array[Decal]   = uninitialized
  private var decalTexture:  Texture        = uninitialized

  // Camera orbit state
  private var azimuth:    Float   = 45f
  private var elevation:  Float   = 30f
  private var orbitDist:  Float   = 14f
  private var autoRotate: Boolean = true
  private var tabWasDown: Boolean = false

  private val Attrs: Long = Usage.Position | Usage.Normal

  override def init()(using Sge): Unit = {
    // Camera
    camera = PerspectiveCamera(67f, Sge().graphics.getWidth().toFloat, Sge().graphics.getHeight().toFloat)
    camera.near = 1f
    camera.far = 100f
    updateCameraPosition()

    // Batches
    modelBatch = ModelBatch(Nullable.empty, Nullable.empty, Nullable.empty)
    shapeRenderer = ShapeRenderer()
    decalStrategy = CameraGroupStrategy(camera)
    decalBatch = DecalBatch(100, decalStrategy)

    // Environment
    environment = Environment()
    environment.add(
      DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f)
    )
    environment.add(
      PointLight().set(1f, 0.9f, 0.4f, 5f, 5f, 5f, 20f)
    )

    // Procedural models
    val builder = ModelBuilder()

    val redMat    = Material(ColorAttribute.createDiffuse(Color.RED))
    val blueMat   = Material(ColorAttribute.createDiffuse(Color.BLUE))
    val greenMat  = Material(ColorAttribute.createDiffuse(Color.GREEN))
    val yellowMat = Material(ColorAttribute.createDiffuse(Color.YELLOW))
    val cyanMat   = Material(ColorAttribute.createDiffuse(Color.CYAN))

    boxModel      = builder.createBox(2f, 2f, 2f, redMat, Attrs)
    sphereModel   = builder.createSphere(2f, 2f, 2f, 20, 20, blueMat, Attrs)
    cylinderModel = builder.createCylinder(1.5f, 2.5f, 1.5f, 20, greenMat, Attrs)
    coneModel     = builder.createCone(2f, 2.5f, 2f, 20, yellowMat, Attrs)
    capsuleModel  = builder.createCapsule(0.75f, 2.5f, 20, cyanMat, Attrs)

    // Instances positioned around the origin
    boxInstance = ModelInstance(boxModel)

    sphereInstance = ModelInstance(sphereModel)
    sphereInstance.transform.setToTranslation(4f, 0f, 0f)

    cylinderInstance = ModelInstance(cylinderModel)
    cylinderInstance.transform.setToTranslation(-4f, 0f, 0f)

    coneInstance = ModelInstance(coneModel)
    coneInstance.transform.setToTranslation(0f, 0f, 4f)

    capsuleInstance = ModelInstance(capsuleModel)
    capsuleInstance.transform.setToTranslation(0f, 0f, -4f)

    // Decal texture — small solid-white square from a Pixmap
    val pm = Pixmap(4, 4, Pixmap.Format.RGBA8888)
    pm.setColor(Color.WHITE)
    pm.fill()
    decalTexture = Texture(pm)
    pm.close()

    val region = TextureRegion(decalTexture)
    decals = Array(
      createDecal(region, 0f, 3.5f, 0f, Color.RED),
      createDecal(region, 4f, 3.5f, 0f, Color.BLUE),
      createDecal(region, -4f, 3.5f, 0f, Color.GREEN),
      createDecal(region, 0f, 3.5f, 4f, Color.YELLOW)
    )
  }

  override def render(dt: Float)(using Sge): Unit = {
    val input = Sge().input

    // TAB toggles auto-rotate
    val tabDown = input.isKeyPressed(Input.Keys.TAB)
    if (tabDown && !tabWasDown) {
      autoRotate = !autoRotate
    }
    tabWasDown = tabDown

    // Camera orbit controls
    if (autoRotate) {
      azimuth += 20f * dt
    }
    if (input.isKeyPressed(Input.Keys.LEFT)) {
      azimuth -= 60f * dt
    }
    if (input.isKeyPressed(Input.Keys.RIGHT)) {
      azimuth += 60f * dt
    }
    if (input.isKeyPressed(Input.Keys.UP)) {
      elevation = scala.math.min(elevation + 40f * dt, 85f)
    }
    if (input.isKeyPressed(Input.Keys.DOWN)) {
      elevation = scala.math.max(elevation - 40f * dt, 5f)
    }
    updateCameraPosition()

    // Clear
    ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f)

    // Floor grid (drawn first, in Line mode)
    shapeRenderer.setProjectionMatrix(camera.combined)
    shapeRenderer.begin(ShapeType.Line)
    shapeRenderer.setColor(0.3f, 0.3f, 0.35f, 1f)
    val gridHalf = 10
    var gi = -gridHalf
    while (gi <= gridHalf) {
      shapeRenderer.line(gi.toFloat, 0f, -gridHalf.toFloat, gi.toFloat, 0f, gridHalf.toFloat)
      shapeRenderer.line(-gridHalf.toFloat, 0f, gi.toFloat, gridHalf.toFloat, 0f, gi.toFloat)
      gi += 1
    }
    shapeRenderer.end()

    // 3D models
    modelBatch.begin(camera)
    modelBatch.render(boxInstance, environment)
    modelBatch.render(sphereInstance, environment)
    modelBatch.render(cylinderInstance, environment)
    modelBatch.render(coneInstance, environment)
    modelBatch.render(capsuleInstance, environment)
    modelBatch.end()

    // Decals — billboards facing camera
    val camPos = camera.position
    val up = Vector3(0f, 1f, 0f)
    var di = 0
    while (di < decals.length) {
      decals(di).lookAt(camPos, up)
      decalBatch.add(decals(di))
      di += 1
    }
    decalBatch.flush()
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    camera.viewportWidth = width.toFloat
    camera.viewportHeight = height.toFloat
    camera.update()
  }

  override def dispose()(using Sge): Unit = {
    modelBatch.close()
    shapeRenderer.close()
    decalBatch.close()
    decalStrategy.close()
    boxModel.close()
    sphereModel.close()
    cylinderModel.close()
    coneModel.close()
    capsuleModel.close()
    decalTexture.close()
  }

  // --- Helpers ---

  private def updateCameraPosition(): Unit = {
    val azRad  = azimuth * MathUtils.degreesToRadians
    val elRad  = elevation * MathUtils.degreesToRadians
    val cosEl  = MathUtils.cos(elRad)
    camera.position.set(
      orbitDist * cosEl * MathUtils.cos(azRad),
      orbitDist * MathUtils.sin(elRad),
      orbitDist * cosEl * MathUtils.sin(azRad)
    )
    camera.lookAt(0f, 0f, 0f)
    camera.update()
  }

  private def createDecal(region: TextureRegion, x: Float, y: Float, z: Float, color: Color)(using Sge): Decal = {
    val d = Decal.newDecal(1.2f, 1.2f, region)
    d.setPosition(x, y, z)
    d.setColor(color.r, color.g, color.b, color.a)
    d
  }
}
