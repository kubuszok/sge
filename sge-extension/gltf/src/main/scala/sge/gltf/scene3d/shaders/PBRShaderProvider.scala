/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBRShaderProvider.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * PBR shader provider that creates PBRShader instances with appropriate prefix defines.
 */
package sge
package gltf
package scene3d
package shaders

import sge.{ Application, Sge }
import sge.gltf.scene3d.attributes.*
import sge.gltf.scene3d.utils.{ LightUtils, ShaderParser }
import sge.graphics.{ PrimitiveMode, Texture, VertexAttributes }
import sge.graphics.g3d.{ Renderable, Shader }
import sge.graphics.g3d.attributes.{ ColorAttribute, TextureAttribute }
import sge.graphics.g3d.shaders.{ DefaultShader, DepthShader }
import sge.graphics.g3d.utils.{ DefaultShaderProvider, DepthShaderProvider }
import sge.graphics.glutils.ShaderProgram
import sge.utils.{ Nullable, SgeError }

import scala.language.implicitConversions

class PBRShaderProvider(config: PBRShaderConfig)(using sge: Sge)
    extends DefaultShaderProvider(
      if (config != null) config else PBRShaderProvider.createDefaultConfig() // @nowarn
    ) {

  if (this.config.vertexShader.isEmpty) this.config.vertexShader = Nullable(PBRShaderProvider.getDefaultVertexShader())
  if (this.config.fragmentShader.isEmpty) this.config.fragmentShader = Nullable(PBRShaderProvider.getDefaultFragmentShader())

  val TAG: String = "PBRShader"

  def getShaderCount: Int = shaders.size

  protected def isGL3: Boolean =
    sge.graphics.glVersion.isVersionEqualToOrHigher(3, 0)

  def createPrefixBase(renderable: Renderable, config: PBRShaderConfig): String = {
    val defaultPrefix = DefaultShader.createPrefix(renderable, config)
    var version: String = config.glslVersion.getOrElse(null).asInstanceOf[String] // @nowarn

    if (isGL3) {
      if (sge.application.applicationType == Application.ApplicationType.Desktop) {
        if (version == null) version = "#version 130\n#define GLSL3\n" // @nowarn
      } else if (
        sge.application.applicationType == Application.ApplicationType.Android ||
        sge.application.applicationType == Application.ApplicationType.iOS ||
        sge.application.applicationType == Application.ApplicationType.WebGL
      ) {
        if (version == null) version = "#version 300 es\n#define GLSL3\n" // @nowarn
      }
    }

    val sb = new StringBuilder
    if (version != null) sb.append(version) // @nowarn
    config.prefix.foreach(sb.append)
    sb.append(defaultPrefix)
    sb.toString
  }

  def createPrefixSRGB(renderable: Renderable, config: PBRShaderConfig): String = {
    val sb = new StringBuilder
    if (config.manualSRGB != PBRShaderConfig.SRGB.NONE) {
      sb.append("#define MANUAL_SRGB\n")
      if (config.manualSRGB == PBRShaderConfig.SRGB.FAST) sb.append("#define SRGB_FAST_APPROXIMATION\n")
    }
    if (config.manualGammaCorrection) sb.append("#define GAMMA_CORRECTION ").append(config.gamma).append("\n")
    if (config.transmissionSRGB != PBRShaderConfig.SRGB.NONE) {
      sb.append("#define TS_MANUAL_SRGB\n")
      if (config.transmissionSRGB == PBRShaderConfig.SRGB.FAST) sb.append("#define TS_SRGB_FAST_APPROXIMATION\n")
    }
    if (config.mirrorSRGB != PBRShaderConfig.SRGB.NONE) {
      sb.append("#define MS_MANUAL_SRGB\n")
      if (config.mirrorSRGB == PBRShaderConfig.SRGB.FAST) sb.append("#define MS_SRGB_FAST_APPROXIMATION\n")
    }
    sb.toString
  }

  override protected def createShader(renderable: Renderable): Shader = {
    val cfg = this.config.asInstanceOf[PBRShaderConfig]
    val sb  = new StringBuilder(createPrefixBase(renderable, cfg))

    // Morph targets
    sb.append(PBRShaderProvider.morphTargetsPrefix(renderable))

    val mat = renderable.material.get
    val env = renderable.environment

    // Base color factor
    if (mat.has(PBRColorAttribute.BaseColorFactor)) sb.append("#define baseColorFactorFlag\n")

    // Lighting
    val primitiveType = renderable.meshPart.primitiveType
    val isLineOrPoint = primitiveType == PrimitiveMode.Points || primitiveType == PrimitiveMode.Lines ||
      primitiveType == PrimitiveMode.LineLoop || primitiveType == PrimitiveMode.LineStrip
    val unlit = isLineOrPoint || mat.has(PBRFlagAttribute.Unlit) ||
      renderable.meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.Normal).isEmpty

    if (unlit) {
      sb.append("#define unlitFlag\n")
    } else {
      if (mat.has(PBRTextureAttribute.MetallicRoughnessTexture)) sb.append("#define metallicRoughnessTextureFlag\n")
      if (mat.has(PBRTextureAttribute.OcclusionTexture)) sb.append("#define occlusionTextureFlag\n")
      if (mat.has(PBRFloatAttribute.TransmissionFactor)) sb.append("#define transmissionFlag\n")
      if (mat.has(PBRTextureAttribute.TransmissionTexture)) sb.append("#define transmissionTextureFlag\n")
      if (mat.has(PBRVolumeAttribute.Type)) sb.append("#define volumeFlag\n")
      if (mat.has(PBRTextureAttribute.ThicknessTexture)) sb.append("#define thicknessTextureFlag\n")
      if (mat.has(PBRFloatAttribute.IOR)) sb.append("#define iorFlag\n")

      // Specular
      var hasSpecular = false
      if (mat.has(PBRFloatAttribute.SpecularFactor)) { sb.append("#define specularFactorFlag\n"); hasSpecular = true }
      if (mat.has(PBRHDRColorAttribute.Specular)) { sb.append("#define specularColorFlag\n"); hasSpecular = true }
      if (mat.has(PBRTextureAttribute.SpecularFactorTexture)) { sb.append("#define specularFactorTextureFlag\n"); hasSpecular = true }
      if (mat.has(PBRTextureAttribute.SpecularColorTexture)) { sb.append("#define specularColorTextureFlag\n"); hasSpecular = true }
      if (hasSpecular) sb.append("#define specularFlag\n")

      // Iridescence
      if (mat.has(PBRIridescenceAttribute.Type)) sb.append("#define iridescenceFlag\n")
      if (mat.has(PBRTextureAttribute.IridescenceTexture)) sb.append("#define iridescenceTextureFlag\n")
      if (mat.has(PBRTextureAttribute.IridescenceThicknessTexture)) sb.append("#define iridescenceThicknessTextureFlag\n")

      // Clipping plane
      if (env.isDefined && env.exists(_.has(ClippingPlaneAttribute.Type))) { // @nowarn
        sb.append("#define clippingPlaneFlag\n")
      }

      // CSM
      if (env.isDefined) { // @nowarn
        env.get.getAs[CascadeShadowMapAttribute](CascadeShadowMapAttribute.Type).foreach { csm =>
          val numCSM = csm.cascadeShadowMap.lights.size
          if (numCSM > 8) throw SgeError.InvalidInput("more than 8 cascade shadow textures not supported")
          sb.append("#define numCSM ").append(numCSM).append("\n")
        }

        // IBL
        if (env.exists(_.has(PBRTextureAttribute.TransmissionSourceTexture))) sb.append("#define transmissionSourceFlag\n")

        var specularCubemapPresent = false
        if (env.exists(_.has(PBRCubemapAttribute.SpecularEnv))) {
          sb.append("#define diffuseSpecularEnvSeparateFlag\n")
          specularCubemapPresent = true
        } else if (env.exists(_.has(PBRCubemapAttribute.DiffuseEnv))) {
          specularCubemapPresent = true
        } else if (env.exists(_.has(PBRCubemapAttribute.EnvironmentMap))) {
          specularCubemapPresent = true
        }

        val hasMirrorSpecular = env.exists(_.has(MirrorSourceAttribute.Type)) && mat.has(MirrorAttribute.Specular)
        if (hasMirrorSpecular) sb.append("#define mirrorSpecularFlag\n")

        if (specularCubemapPresent || hasMirrorSpecular) {
          sb.append("#define USE_IBL\n")
          val textureLodSupported =
            if (isGL3) true
            else if (sge.graphics.supportsExtension("EXT_shader_texture_lod")) {
              sb.append("#define USE_TEXTURE_LOD_EXT\n"); true
            } else false

          if (specularCubemapPresent && textureLodSupported) {
            // Check if specular cubemap has mipmap filter
            val specCubemap = env.get
              .getAs[PBRCubemapAttribute](PBRCubemapAttribute.SpecularEnv)
              .orElse(env.get.getAs[PBRCubemapAttribute](PBRCubemapAttribute.DiffuseEnv))
              .orElse(env.get.getAs[PBRCubemapAttribute](PBRCubemapAttribute.EnvironmentMap))
            specCubemap.foreach { attr =>
              val filter = attr.textureDescription.minFilter.getOrElse(
                attr.textureDescription.texture.get.minFilter
              )
              if (filter == Texture.TextureFilter.MipMap) sb.append("#define USE_TEX_LOD\n")
            }
          }

          if (env.exists(_.has(PBRTextureAttribute.BRDFLUTTexture))) sb.append("#define brdfLUTTexture\n")
        }

        if (env.exists(_.has(ColorAttribute.AmbientLight))) sb.append("#define ambientLightFlag\n")
        if (env.exists(_.has(PBRMatrixAttribute.EnvRotation))) sb.append("#define ENV_ROTATION\n")
      }
    }

    // SRGB
    sb.append(createPrefixSRGB(renderable, cfg))

    // Multi UVs
    var maxUVIndex     = -1
    val uvTextureTypes = Array(
      (TextureAttribute.Diffuse, "v_diffuseUV"),
      (TextureAttribute.Emissive, "v_emissiveUV"),
      (TextureAttribute.Normal, "v_normalUV"),
      (PBRTextureAttribute.MetallicRoughnessTexture, "v_metallicRoughnessUV"),
      (PBRTextureAttribute.OcclusionTexture, "v_occlusionUV"),
      (PBRTextureAttribute.TransmissionTexture, "v_transmissionUV"),
      (PBRTextureAttribute.ThicknessTexture, "v_thicknessUV"),
      (PBRTextureAttribute.SpecularFactorTexture, "v_specularFactorUV"),
      (PBRTextureAttribute.SpecularColorTexture, "v_specularColorUV"),
      (PBRTextureAttribute.IridescenceTexture, "v_iridescenceUV"),
      (PBRTextureAttribute.IridescenceThicknessTexture, "v_iridescenceThicknessUV")
    )
    for ((texType, define) <- uvTextureTypes)
      renderable.material.foreach { mat2 =>
        mat2.getAs[TextureAttribute](texType).foreach { attribute =>
          sb.append("#define ").append(define).append(" v_texCoord").append(attribute.uvIndex).append("\n")
          maxUVIndex = Math.max(maxUVIndex, attribute.uvIndex)
        }
      }
    if (maxUVIndex >= 0) sb.append("#define textureFlag\n")
    if (maxUVIndex == 1) sb.append("#define textureCoord1Flag\n")
    else if (maxUVIndex > 1) throw SgeError.InvalidInput("more than 2 texture coordinates attribute not supported")

    // Fog
    if (env.isDefined && env.exists(_.has(FogAttribute.FogEquation))) {
      sb.append("#define fogEquationFlag\n")
    }

    // Colors
    val vertexAttributes = renderable.meshPart.mesh.vertexAttributes
    var vIdx             = 0
    while (vIdx < vertexAttributes.size) {
      val attribute = vertexAttributes.get(vIdx)
      if (attribute.usage == VertexAttributes.Usage.ColorUnpacked) {
        sb.append("#define color").append(attribute.unit).append("Flag\n")
      }
      vIdx += 1
    }

    PBRCommon.checkVertexAttributes(renderable)

    val prefix = sb.toString
    val shader = createShader(renderable, cfg, prefix)
    shader.program.foreach(checkShaderCompilation)

    if (!shader.canRender(renderable)) {
      throw SgeError.InvalidInput("cannot render with this shader")
    }
    shader
  }

  protected def createShader(renderable: Renderable, config: PBRShaderConfig, prefix: String): PBRShader =
    PBRShader(renderable, config, prefix)

  protected def checkShaderCompilation(program: ShaderProgram): Unit = {
    val shaderLog = program.log
    if (program.compiled) {
      if (!shaderLog.isEmpty) System.err.println("[" + TAG + "] Shader compilation warnings:\n" + shaderLog)
    } else {
      throw SgeError.InvalidInput("Shader compilation failed:\n" + shaderLog)
    }
  }
}

object PBRShaderProvider {

  val TAG: String = "PBRShader"

  @volatile private var defaultVertexShader: String = null.asInstanceOf[String] // @nowarn — lazy init

  def getDefaultVertexShader()(using sge: Sge): String = {
    if (defaultVertexShader == null) { // @nowarn — null check for lazy init
      defaultVertexShader = ShaderParser.parse(sge.files.classpath("net/mgsx/gltf/shaders/pbr/pbr.vs.glsl"))
    }
    defaultVertexShader
  }

  @volatile private var defaultFragmentShader: String = null.asInstanceOf[String] // @nowarn — lazy init

  def getDefaultFragmentShader()(using sge: Sge): String = {
    if (defaultFragmentShader == null) { // @nowarn — null check for lazy init
      defaultFragmentShader = ShaderParser.parse(sge.files.classpath("net/mgsx/gltf/shaders/pbr/pbr.fs.glsl"))
    }
    defaultFragmentShader
  }

  def createDefaultConfig()(using Sge): PBRShaderConfig = {
    val config = PBRShaderConfig()
    config.vertexShader = Nullable(getDefaultVertexShader())
    config.fragmentShader = Nullable(getDefaultFragmentShader())
    config
  }

  def createDefaultDepthConfig()(using Sge): DepthShader.Config =
    PBRDepthShaderProvider.createDefaultConfig()

  def createDefault(maxBones: Int)(using Sge): PBRShaderProvider = {
    val config = createDefaultConfig()
    config.numBones = maxBones
    createDefault(config)
  }

  def createDefault(config: PBRShaderConfig)(using Sge): PBRShaderProvider =
    PBRShaderProvider(config)

  def createDefaultDepth(maxBones: Int)(using Sge): DepthShaderProvider = {
    val config = createDefaultDepthConfig()
    config.numBones = maxBones
    createDefaultDepth(config)
  }

  def createDefaultDepth(config: DepthShader.Config)(using Sge): DepthShaderProvider =
    PBRDepthShaderProvider(config)

  def morphTargetsPrefix(renderable: Renderable): String = {
    val sb               = new StringBuilder
    val vertexAttributes = renderable.meshPart.mesh.vertexAttributes
    val n                = vertexAttributes.size
    var j                = 0
    while (j < n) {
      val att = vertexAttributes.get(j)
      var i   = 0
      while (i < PBRCommon.MAX_MORPH_TARGETS) {
        if (att.usage == PBRVertexAttributes.Usage.PositionTarget && att.unit == i) {
          sb.append("#define position").append(i).append("Flag\n")
        } else if (att.usage == PBRVertexAttributes.Usage.NormalTarget && att.unit == i) {
          sb.append("#define normal").append(i).append("Flag\n")
        } else if (att.usage == PBRVertexAttributes.Usage.TangentTarget && att.unit == i) {
          sb.append("#define tangent").append(i).append("Flag\n")
        }
        i += 1
      }
      j += 1
    }
    sb.toString
  }
}
