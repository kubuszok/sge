/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/SceneManager.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Convenient manager class for: model instances, animators, camera, environment, lights, batch/shaderProvider
 */
package sge
package gltf
package scene3d
package scene

import sge.{ Sge, WorldUnits }
import sge.gltf.scene3d.attributes.PBRMatrixAttribute
import sge.gltf.scene3d.lights.{ DirectionalShadowLight, PointLightEx, SpotLightEx }
import sge.gltf.scene3d.shaders.{ PBRCommon, PBRShaderProvider }
import sge.gltf.scene3d.utils.{ EnvironmentCache, EnvironmentUtil }
import sge.graphics.Camera
import sge.graphics.g3d.Attribute
import sge.graphics.g3d.{ Environment, ModelBatch, RenderableProvider }
import sge.graphics.g3d.attributes.{ ColorAttribute, DirectionalLightsAttribute, PointLightsAttribute, SpotLightsAttribute }
import sge.graphics.g3d.environment.{ BaseLight, DirectionalLight, PointLight, SpotLight }
import sge.graphics.g3d.model.Node
import sge.graphics.g3d.utils.{ DepthShaderProvider, RenderableSorter, ShaderProvider }
import sge.utils.{ DynamicArray, Nullable }

class SceneManager(using sge: Sge) {

  private val renderableProviders: DynamicArray[RenderableProvider] = DynamicArray[RenderableProvider]()

  private var batch:              ModelBatch         = scala.compiletime.uninitialized
  private var depthBatch:         ModelBatch         = scala.compiletime.uninitialized
  private var skyBox:             SceneSkybox        = scala.compiletime.uninitialized
  private var transmissionSource: TransmissionSource = scala.compiletime.uninitialized
  private var mirrorSource:       MirrorSource       = scala.compiletime.uninitialized
  private var cascadeShadowMap:   CascadeShadowMap   = scala.compiletime.uninitialized

  /** Shouldn't be null. */
  var environment:                    Environment      = Environment()
  protected val computedEnvironement: EnvironmentCache = EnvironmentCache()

  var camera: Camera = scala.compiletime.uninitialized

  private var renderableSorter: RenderableSorter     = scala.compiletime.uninitialized
  private val pointLights:      PointLightsAttribute = PointLightsAttribute()
  private val spotLights:       SpotLightsAttribute  = SpotLightsAttribute()

  def this(maxBones: Int)(using Sge) = {
    this()
    init(PBRShaderProvider.createDefault(maxBones), PBRShaderProvider.createDefaultDepth(maxBones))
  }

  def this(shaderProvider: ShaderProvider, depthShaderProvider: DepthShaderProvider)(using Sge) = {
    this()
    init(shaderProvider, depthShaderProvider, SceneRenderableSorter())
  }

  def this(shaderProvider: ShaderProvider, depthShaderProvider: DepthShaderProvider, renderableSorter: RenderableSorter)(using Sge) = {
    this()
    init(shaderProvider, depthShaderProvider, renderableSorter)
  }

  private def init(shaderProvider: ShaderProvider, depthShaderProvider: DepthShaderProvider, renderableSorter: RenderableSorter = SceneRenderableSorter()): Unit = {
    this.renderableSorter = renderableSorter
    batch = ModelBatch(shaderProvider, renderableSorter)
    depthBatch = ModelBatch(depthShaderProvider)
    environment.set(ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f))
  }

  def setEnvironmentRotation(azymuthAngleDegree: Float): Unit = {
    val rotOpt = environment.getAs[PBRMatrixAttribute](PBRMatrixAttribute.EnvRotation)
    if (rotOpt.isDefined) {
      rotOpt.get.set(azymuthAngleDegree)
    } else {
      environment.set(PBRMatrixAttribute.createEnvRotation(azymuthAngleDegree))
    }
  }

  def removeEnvironmentRotation(): Unit = environment.remove(PBRMatrixAttribute.EnvRotation)

  def getBatch:                              ModelBatch = batch
  def setBatch(batch:           ModelBatch): Unit       = this.batch = batch
  def setDepthBatch(depthBatch: ModelBatch): Unit       = this.depthBatch = depthBatch
  def getDepthBatch:                         ModelBatch = depthBatch

  def setShaderProvider(shaderProvider: ShaderProvider): Unit = {
    batch.close()
    batch = ModelBatch(shaderProvider, renderableSorter)
  }

  def setDepthShaderProvider(depthShaderProvider: DepthShaderProvider): Unit = {
    depthBatch.close()
    depthBatch = ModelBatch(depthShaderProvider)
  }

  def setTransmissionSource(transmissionSource: TransmissionSource): Unit =
    if (this.transmissionSource ne transmissionSource) {
      if (this.transmissionSource != null) this.transmissionSource.close() // @nowarn
      this.transmissionSource = transmissionSource
    }

  def setMirrorSource(mirrorSource: MirrorSource): Unit =
    if (this.mirrorSource ne mirrorSource) {
      if (this.mirrorSource != null) this.mirrorSource.close() // @nowarn
      this.mirrorSource = mirrorSource
    }

  def setCascadeShadowMap(cascadeShadowMap: CascadeShadowMap): Unit =
    if (this.cascadeShadowMap ne cascadeShadowMap) {
      if (this.cascadeShadowMap != null) this.cascadeShadowMap.close() // @nowarn
      this.cascadeShadowMap = cascadeShadowMap
    }

  def addScene(scene: Scene): Unit = addScene(scene, appendLights = true)

  def addScene(scene: Scene, appendLights: Boolean): Unit = {
    renderableProviders.add(scene)
    if (appendLights) {
      scene.lights.foreachValue { light =>
        environment.add(light)
      }
    }
  }

  /** Should be called in order to perform light culling, skybox update and animations. */
  def update(delta: Float): Unit =
    if (camera != null) { // @nowarn
      updateEnvironment()
      var i = 0
      while (i < renderableProviders.size) {
        renderableProviders(i) match {
          case u: Updatable => u.update(camera, delta)
          case _ => ()
        }
        i += 1
      }
      if (skyBox != null) skyBox.update(camera, delta) // @nowarn
    }

  protected def updateSkyboxRotation(): Unit =
    if (skyBox != null) { // @nowarn
      environment.getAs[PBRMatrixAttribute](PBRMatrixAttribute.EnvRotation).foreach { rotationAttribute =>
        skyBox.setRotation(rotationAttribute.matrix)
      }
    }

  protected def updateEnvironment(): Unit = {
    updateSkyboxRotation()
    computedEnvironement.setCache(environment)
    pointLights.lights.clear()
    spotLights.lights.clear()
    if (environment != null) { // @nowarn
      environment.foreach {
        case pla: PointLightsAttribute =>
          var i = 0
          while (i < pla.lights.size) { pointLights.lights.add(pla.lights(i)); i += 1 }
          computedEnvironement.replaceCache(pointLights)
        case sla: SpotLightsAttribute =>
          var i = 0
          while (i < sla.lights.size) { spotLights.lights.add(sla.lights(i)); i += 1 }
          computedEnvironement.replaceCache(spotLights)
        case a =>
          computedEnvironement.set(a)
      }
    }
    cullLights()
  }

  protected def cullLights(): Unit = {
    environment.getAs[PointLightsAttribute](PointLightsAttribute.Type).foreach { pla =>
      var i = 0
      while (i < pla.lights.size) {
        pla.lights(i) match {
          case l: PointLightEx =>
            l.range.foreach { r =>
              if (!camera.frustum.sphereInFrustum(l.position, r)) {
                pointLights.lights.removeValue(l)
              }
            }
          case _ => ()
        }
        i += 1
      }
    }
    environment.getAs[SpotLightsAttribute](SpotLightsAttribute.Type).foreach { sla =>
      var i = 0
      while (i < sla.lights.size) {
        sla.lights(i) match {
          case l: SpotLightEx =>
            l.range.foreach { r =>
              if (!camera.frustum.sphereInFrustum(l.position, r)) {
                spotLights.lights.removeValue(l)
              }
            }
          case _ => ()
        }
        i += 1
      }
    }
  }

  /** Render all scenes. */
  def render(): Unit =
    if (camera != null) {
      PBRCommon.enableSeamlessCubemaps()
      renderShadows()
      renderMirror()
      renderTransmission()
      renderColors()
    }

  def renderMirror(): Unit =
    if (mirrorSource != null) { // @nowarn
      mirrorSource.begin(camera, computedEnvironement, skyBox)
      renderColors()
      mirrorSource.end()
    }

  def renderTransmission(): Unit =
    if (transmissionSource != null) { // @nowarn
      transmissionSource.begin(camera)
      var i = 0
      while (i < renderableProviders.size) {
        transmissionSource.render(renderableProviders(i), environment)
        i += 1
      }
      if (skyBox != null) transmissionSource.render(skyBox) // @nowarn
      transmissionSource.end()
      computedEnvironement.set(transmissionSource.attribute)
    }

  @SuppressWarnings(Array("deprecation"))
  def renderShadows(): Unit = {
    val shadowLight = getFirstDirectionalShadowLight
    if (shadowLight != null) { // @nowarn
      shadowLight.begin()
      renderDepth(shadowLight.getCamera())
      shadowLight.end()
      environment.shadowMap = Nullable(shadowLight)
    } else {
      environment.shadowMap = Nullable.empty
    }
    computedEnvironement.shadowMap = environment.shadowMap

    if (cascadeShadowMap != null) { // @nowarn
      var i = 0
      while (i < cascadeShadowMap.lights.size) {
        val light = cascadeShadowMap.lights(i)
        light.begin()
        renderDepth(light.getCamera())
        light.end()
        i += 1
      }
      computedEnvironement.set(cascadeShadowMap.attribute)
    }
  }

  def renderDepth(): Unit = renderDepth(camera)

  def renderDepth(camera: Camera): Unit = {
    depthBatch.begin(camera)
    renderableProviders.foreach(depthBatch.render(_))
    depthBatch.end()
  }

  def renderColors(): Unit = {
    batch.begin(camera)
    renderableProviders.foreach(batch.render(_, computedEnvironement))
    if (skyBox != null) batch.render(skyBox) // @nowarn
    batch.end()
  }

  def getFirstDirectionalLight: DirectionalLight =
    environment
      .getAs[DirectionalLightsAttribute](DirectionalLightsAttribute.Type)
      .map { dla =>
        var i = 0
        var result: DirectionalLight = null.asInstanceOf[DirectionalLight] // @nowarn
        while (i < dla.lights.size && result == null) {
          val dl = dla.lights(i)
          if (dl.isInstanceOf[DirectionalLight]) result = dl
          i += 1
        }
        result
      }
      .getOrElse(null.asInstanceOf[DirectionalLight]) // @nowarn

  def getFirstDirectionalShadowLight: DirectionalShadowLight =
    environment
      .getAs[DirectionalLightsAttribute](DirectionalLightsAttribute.Type)
      .map { dla =>
        var i = 0
        var result: DirectionalShadowLight = null.asInstanceOf[DirectionalShadowLight] // @nowarn
        while (i < dla.lights.size && result == null) {
          dla.lights(i) match {
            case dsl: DirectionalShadowLight => result = dsl
            case _ => ()
          }
          i += 1
        }
        result
      }
      .getOrElse(null.asInstanceOf[DirectionalShadowLight]) // @nowarn

  def setSkyBox(skyBox: SceneSkybox): Unit        = this.skyBox = skyBox
  def getSkyBox:                      SceneSkybox = skyBox

  def setAmbientLight(lum: Float): Unit =
    environment.getAs[ColorAttribute](ColorAttribute.AmbientLight).foreach { attr =>
      attr.color.set(lum, lum, lum, 1f)
    }

  def setCamera(camera: Camera): Unit = this.camera = camera

  def removeScene(scene: Scene): Unit = {
    renderableProviders.removeValue(scene)
    scene.lights.foreachValue { light =>
      // environment.remove expects a Long type key, not a light object
      // TODO: proper light removal from environment
    }
  }

  def getRenderableProviders: DynamicArray[RenderableProvider] = renderableProviders

  def updateViewport(width: Float, height: Float): Unit =
    if (camera != null) { // @nowarn
      camera.viewportWidth = WorldUnits(width)
      camera.viewportHeight = WorldUnits(height)
      camera.update(true)
    }

  def getActiveLightsCount: Int = EnvironmentUtil.getLightCount(computedEnvironement)
  def getTotalLightsCount:  Int = EnvironmentUtil.getLightCount(environment)

  def close(): Unit = {
    batch.close()
    depthBatch.close()
    if (transmissionSource != null) transmissionSource.close() // @nowarn
    if (mirrorSource != null) mirrorSource.close() // @nowarn
    if (cascadeShadowMap != null) cascadeShadowMap.close() // @nowarn
  }
}
