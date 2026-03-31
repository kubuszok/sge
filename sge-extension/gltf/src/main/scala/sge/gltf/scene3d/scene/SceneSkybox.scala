/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/SceneSkybox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package scene

import sge.{ Application, Sge }
import sge.gltf.scene3d.attributes.PBRMatrixAttribute
import sge.gltf.scene3d.shaders.PBRShaderConfig
import sge.graphics.{ Camera, Color, Cubemap, GL20, Texture, TextureTarget }
import sge.graphics.g3d.{ Attributes, Environment, Material, Model as G3dModel, Renderable, RenderableProvider, Shader }
import sge.graphics.g3d.attributes.{ ColorAttribute, CubemapAttribute, DepthTestAttribute }
import sge.graphics.g3d.shaders.DefaultShader
import sge.graphics.g3d.utils.{ DefaultShaderProvider, ModelBuilder, ShaderProvider }
import sge.graphics.glutils.ShaderProgram
import sge.graphics.VertexAttributes
import sge.math.Matrix4
import sge.utils.{ DynamicArray, Nullable, Pool }

class SceneSkybox(using sge: Sge) extends RenderableProvider with Updatable with AutoCloseable {

  /** Dynamically change cubemap mipmap bias. */
  var lodBias: Float = 0f

  /** Environment used by skybox. */
  val environment: Environment = Environment()

  private val directionInverse:   Matrix4        = Matrix4()
  private val envRotationInverse: Matrix4        = Matrix4()
  private var lodEnabled:         Boolean        = false
  private var quadModel:          G3dModel       = scala.compiletime.uninitialized
  private var quad:               Renderable     = scala.compiletime.uninitialized
  private var shaderProvider:     ShaderProvider = scala.compiletime.uninitialized
  private var ownShaderProvider:  Boolean        = false

  def this(cubemap: Cubemap)(using Sge) = {
    this()
    createShaderProvider(PBRShaderConfig.SRGB.NONE, null) // @nowarn — null means no gamma
    createQuad(cubemap)
  }

  def this(cubemap: Cubemap, manualSRGB: PBRShaderConfig.SRGB, gammaCorrection: java.lang.Float, lod: Boolean)(using Sge) = {
    this()
    lodEnabled = lod
    createShaderProvider(manualSRGB, gammaCorrection)
    createQuad(cubemap)
  }

  def this(cubemap: Cubemap, manualSRGB: PBRShaderConfig.SRGB, gammaCorrection: Boolean, lod: Boolean)(using Sge) = {
    this()
    lodEnabled = lod
    createShaderProvider(manualSRGB, if (gammaCorrection) java.lang.Float.valueOf(PBRShaderConfig.DEFAULT_GAMMA) else null) // @nowarn
    createQuad(cubemap)
  }

  def this(cubemap: Cubemap, shaderProvider: ShaderProvider)(using Sge) = {
    this()
    if (shaderProvider == null) { // @nowarn
      createShaderProvider(PBRShaderConfig.SRGB.NONE, null) // @nowarn
    } else {
      this.shaderProvider = shaderProvider
    }
    createQuad(cubemap)
  }

  private def createShaderProvider(manualSRGB: PBRShaderConfig.SRGB, gammaCorrection: java.lang.Float): Unit = {
    val sb = new StringBuilder
    if (lodEnabled) {
      if (!Sge().graphics.glVersion.isVersionEqualToOrHigher(3, 0)) {
        throw new IllegalArgumentException("GDX-GLTF Skybox LOD requires GLES 3+")
      }
      if (Sge().application.applicationType == Application.ApplicationType.Desktop) {
        sb.append("#version 130\n")
      } else {
        sb.append("#version 300 es\n")
      }
      sb.append("#define GLSL3\n")
    }
    if (manualSRGB != PBRShaderConfig.SRGB.NONE) {
      sb.append("#define MANUAL_SRGB\n")
      if (manualSRGB == PBRShaderConfig.SRGB.FAST) sb.append("#define SRGB_FAST_APPROXIMATION\n")
    }
    if (gammaCorrection != null) { // @nowarn
      sb.append("#define GAMMA_CORRECTION ").append(gammaCorrection.floatValue()).append("\n")
    }
    if (lodEnabled) sb.append("#define ENV_LOD\n")

    val shaderConfig = DefaultShader.Config()
    val basePathName = "net/mgsx/gltf/shaders/skybox"
    shaderConfig.vertexShader = Nullable(Sge().files.classpath(basePathName + ".vs.glsl").readString())
    shaderConfig.fragmentShader = Nullable(Sge().files.classpath(basePathName + ".fs.glsl").readString())
    ownShaderProvider = true
    this.shaderProvider = DefaultShaderProvider(shaderConfig)
  }

  private def createQuad(cubemap: Cubemap): Unit = {
    quadModel = ModelBuilder().createRect(-1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f, -1f, 1f, 0f, 0f, 0f, -1f, Material(), VertexAttributes.Usage.Position.toLong)

    quad = quadModel.nodes(0).parts(0).setRenderable(Renderable())
    environment.set(CubemapAttribute(CubemapAttribute.EnvironmentMap, cubemap))
    quad.environment = Nullable(environment)
    quad.userData = Nullable(SceneRenderableSorter.Hints.OPAQUE_LAST.asInstanceOf[AnyRef])
    quad.material = Nullable(Material(ColorAttribute.createDiffuse(Color.WHITE)))
    quad.material.foreach(_.set(DepthTestAttribute(false)))
  }

  def set(cubemap: Cubemap): SceneSkybox = {
    quad.environment.foreach(_.set(CubemapAttribute(CubemapAttribute.EnvironmentMap, cubemap)))
    this
  }

  def getColor: Color =
    quad.material.get.getAs[ColorAttribute](ColorAttribute.Diffuse).get.color

  override def update(camera: Camera, delta: Float): Unit = {
    directionInverse.set(camera.view)
    directionInverse.setTranslation(0f, 0f, 1e-30f)

    quad.environment.foreach { env =>
      env.getAs[PBRMatrixAttribute](PBRMatrixAttribute.EnvRotation).foreach { a =>
        directionInverse.mul(envRotationInverse.set(a.matrix).tra())
      }
    }
    quad.worldTransform.set(camera.projection).mul(directionInverse).inv()
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit = {
    quad.shader = Nullable(shaderProvider.getShader(quad))
    renderables.add(quad)
  }

  override def close(): Unit = {
    if (shaderProvider != null && ownShaderProvider) shaderProvider.close() // @nowarn
    quadModel.close()
  }

  def setRotation(azymuthAngleDegree: Float): Unit =
    quad.environment.foreach { env =>
      val rotOpt = env.getAs[PBRMatrixAttribute](PBRMatrixAttribute.EnvRotation)
      if (rotOpt.isDefined) {
        rotOpt.get.set(azymuthAngleDegree)
      } else {
        env.set(PBRMatrixAttribute.createEnvRotation(azymuthAngleDegree))
      }
    }

  def setRotation(envRotation: Matrix4): Unit =
    quad.environment.foreach { env =>
      if (envRotation != null) { // @nowarn
        val attrOpt = env.getAs[PBRMatrixAttribute](PBRMatrixAttribute.EnvRotation)
        if (attrOpt.isDefined) {
          attrOpt.get.matrix.set(envRotation)
        } else {
          env.set(PBRMatrixAttribute.createEnvRotation(envRotation))
        }
      } else {
        env.remove(PBRMatrixAttribute.EnvRotation)
      }
    }
}

object SceneSkybox {

  def enableMipmaps(cubemap: Cubemap)(using sge: Sge): Unit = {
    cubemap.bind()
    Sge().graphics.gl.glGenerateMipmap(TextureTarget.TextureCubeMap)
    cubemap.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.Linear)
  }
}
