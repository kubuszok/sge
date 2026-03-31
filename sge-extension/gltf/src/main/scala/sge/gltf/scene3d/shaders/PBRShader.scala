/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/shaders/PBRShader.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * PBR shader extending DefaultShader with PBR-specific uniforms and setters.
 */
package sge
package gltf
package scene3d
package shaders

import sge.gltf.scene3d.attributes.*
import sge.gltf.scene3d.lights.{ DirectionalLightEx, DirectionalShadowLight }
import sge.gltf.scene3d.model.WeightVector
import sge.graphics.{ Color, UniformLocation }
import sge.graphics.g3d.{ Attributes, Renderable }
import sge.graphics.g3d.attributes.{ ColorAttribute, DirectionalLightsAttribute, TextureAttribute }
import sge.graphics.g3d.environment.DirectionalLight
import sge.graphics.g3d.shaders.{ BaseShader, DefaultShader }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Matrix3, Vector2, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

/** Helper to set float/int uniform values since SGE BaseShader.set only supports typed (Vector3, Color, etc.) */
private[shaders] object PBRShaderUtils {

  /** Set a float uniform by inputID. Uses program.setUniformf via the shader's loc resolution. */
  def setFloat(shader: BaseShader, inputID: Int, value: Float): Unit = {
    val loc = shader.loc(inputID)
    if (loc.toInt >= 0) shader.program.foreach(_.setUniformf(loc, value))
  }

  def setFloat3(shader: BaseShader, inputID: Int, v1: Float, v2: Float, v3: Float): Unit = {
    val loc = shader.loc(inputID)
    if (loc.toInt >= 0) shader.program.foreach(_.setUniformf(loc, v1, v2, v3))
  }

  def setFloat4(shader: BaseShader, inputID: Int, v1: Float, v2: Float, v3: Float, v4: Float): Unit = {
    val loc = shader.loc(inputID)
    if (loc.toInt >= 0) shader.program.foreach(_.setUniformf(loc, v1, v2, v3, v4))
  }

  def setInt(shader: BaseShader, inputID: Int, value: Int): Unit = {
    val loc = shader.loc(inputID)
    if (loc.toInt >= 0) shader.program.foreach(_.setUniformi(loc, value))
  }
}

class PBRShader(
  renderable: Renderable,
  config:     DefaultShader.Config,
  prefix:     String
)(using sge.Sge)
    extends DefaultShader(renderable, config, prefix) {

  import PBRShader.*

  @scala.annotation.nowarn("msg=deprecated")
  private var textureCoordinateMapMask: Long = getTextureCoordinateMapMask(renderable.material.orNull) // orNull — Java interop for shader init
  private var morphTargetsMask:         Long = computeMorphTargetsMask(renderable)
  private var vertexColorLayers:        Int  = computeVertexColorLayers(renderable)
  private var cascadeCount:             Int  = countShadowCascades(renderable)

  // Uniform locations
  val u_metallicRoughness:           Int = register(metallicRoughnessUniform, metallicRoughnessSetter)
  val u_occlusionStrength:           Int = register(occlusionStrengthUniform, occlusionStrengthSetter)
  val u_metallicRoughnessTexture:    Int = register(metallicRoughnessTextureUniform, metallicRoughnessTextureSetter)
  val u_occlusionTexture:            Int = register(occlusionTextureUniform, occlusionTextureSetter)
  val u_DiffuseEnvSampler:           Int = register(diffuseEnvTextureUniform, diffuseEnvTextureSetter)
  val u_SpecularEnvSampler:          Int = register(specularEnvTextureUniform, specularEnvTextureSetter)
  val u_envRotation:                 Int = register(envRotationUniform, envRotationSetter)
  val u_brdfLUTTexture:              Int = register(brdfLUTTextureUniform, brdfLUTTextureSetter)
  val u_NormalScale:                 Int = register(normalScaleUniform, normalScaleSetter)
  val u_BaseColorTexture:            Int = register(baseColorTextureUniform, baseColorTextureSetter)
  val u_NormalTexture:               Int = register(normalTextureUniform, normalTextureSetter)
  val u_EmissiveTexture:             Int = register(emissiveTextureUniform, emissiveTextureSetter)
  val u_BaseColorFactor:             Int = register(baseColorFactorUniform, baseColorFactorSetter)
  val u_FogEquation:                 Int = register(fogEquationUniform, fogEquationSetter)
  val u_ShadowBias:                  Int = register(shadowBiasUniform, shadowBiasSetter)
  var u_emissive:                    Int = register(DefaultShader.Inputs.emissiveColor, emissiveScaledColor)
  var u_transmissionFactor:          Int = register(transmissionFactorUniform, transmissionFactorSetter)
  var u_transmissionTexture:         Int = register(transmissionTextureUniform, transmissionTextureSetter)
  var u_ior:                         Int = register(iorUniform, iorSetter)
  var u_thicknessFactor:             Int = register(thicknessFactorUniform, thicknessFactorSetter)
  var u_volumeDistance:              Int = register(volumeDistanceUniform, volumeDistanceSetter)
  var u_volumeColor:                 Int = register(volumeColorUniform, volumeColorSetter)
  var u_thicknessTexture:            Int = register(thicknessTextureUniform, thicknessTextureSetter)
  var u_specularFactor:              Int = register(specularFactorUniform, specularFactorSetter)
  var u_specularColorFactor:         Int = register(specularColorFactorUniform, specularColorFactorSetter)
  var u_specularFactorTexture:       Int = register(specularFactorTextureUniform, specularFactorTextureSetter)
  var u_specularColorTexture:        Int = register(specularColorTextureUniform, specularColorTextureSetter)
  var u_iridescenceFactor:           Int = register(iridescenceFactorUniform, iridescenceFactorSetter)
  var u_iridescenceIOR:              Int = register(iridescenceIORUniform, iridescenceIORSetter)
  var u_iridescenceThicknessMin:     Int = register(iridescenceThicknessMinUniform, iridescenceThicknessMinSetter)
  var u_iridescenceThicknessMax:     Int = register(iridescenceThicknessMaxUniform, iridescenceThicknessMaxSetter)
  var u_iridescenceTexture:          Int = register(iridescenceTextureUniform, iridescenceTextureSetter)
  var u_iridescenceThicknessTexture: Int = register(iridescenceThicknessTextureUniform, iridescenceThicknessTextureSetter)
  var u_transmissionSourceTexture:   Int = register(transmissionSourceTextureUniform, transmissionSourceTextureSetter)
  var u_transmissionSourceMipmap:    Int = register(transmissionSourceMipmapUniform, transmissionSourceMipmapSetter)
  var u_specularMirrorSampler:       Int = register(specularMirrorTextureUniform, specularMirrorTextureSetter)
  var u_specularMirrorMipmapScale:   Int = register(specularMirrorMipmapUniform, specularMirrorMipmapSetter)
  var u_specularMirrorNormal:        Int = register(mirrorNormalUniform, mirrorNormalSetter)
  var u_viewportInv:                 Int = register(viewportInvUniform, viewportInvSetter)
  var u_clippingPlane:               Int = register(clippingPlaneUniform, clippingPlaneSetter)

  // Morph targets & misc
  private var u_morphTargets1:      UniformLocation = UniformLocation.notFound
  private var u_morphTargets2:      UniformLocation = UniformLocation.notFound
  private var u_mipmapScale:        UniformLocation = UniformLocation.notFound
  private var u_texCoord0Transform: UniformLocation = UniformLocation.notFound
  private var u_texCoord1Transform: UniformLocation = UniformLocation.notFound
  private var u_ambientLight:       UniformLocation = UniformLocation.notFound

  // CSM
  var u_csmSamplers:         Array[UniformLocation] = Array.fill(cascadeCount)(UniformLocation.notFound)
  var u_csmPCFClip:          UniformLocation        = UniformLocation.notFound
  var u_csmTransforms:       UniformLocation        = UniformLocation.notFound
  private var csmTransforms: Array[Float]           = new Array[Float](cascadeCount * 16)
  private var csmPCFClip:    Array[Float]           = new Array[Float](cascadeCount * 2)

  private def computeVertexColorLayers(renderable: Renderable): Int = {
    var num              = 0
    val vertexAttributes = renderable.meshPart.mesh.vertexAttributes
    val n                = vertexAttributes.size
    var i                = 0
    while (i < n) {
      val attr = vertexAttributes.get(i)
      if (attr.usage == sge.graphics.VertexAttributes.Usage.ColorUnpacked) num += 1
      i += 1
    }
    num
  }

  private def countShadowCascades(renderable: Renderable): Int =
    if (renderable.environment.isDefined) {
      renderable.environment.get
        .getAs[CascadeShadowMapAttribute](CascadeShadowMapAttribute.Type)
        .map { csm =>
          csm.cascadeShadowMap.lights.size
        }
        .getOrElse(0)
    } else 0

  override def canRender(renderable: Renderable): Boolean = {
    @scala.annotation.nowarn("msg=deprecated")
    val tcmMask = getTextureCoordinateMapMask(renderable.material.orNull) // orNull — shader init Java interop
    if (tcmMask != this.textureCoordinateMapMask) false
    else if (this.morphTargetsMask != computeMorphTargetsMask(renderable)) false
    else if (this.vertexColorLayers != computeVertexColorLayers(renderable)) false
    else if (this.cascadeCount != countShadowCascades(renderable)) false
    else super.canRender(renderable)
  }

  def computeMorphTargetsMask(renderable: Renderable): Long = {
    var morphTargetsFlag = 0
    val vertexAttributes = renderable.meshPart.mesh.vertexAttributes
    val n                = vertexAttributes.size
    var i                = 0
    while (i < n) {
      val attr = vertexAttributes.get(i)
      if (attr.usage == PBRVertexAttributes.Usage.PositionTarget) morphTargetsFlag |= (1 << attr.unit)
      if (attr.usage == PBRVertexAttributes.Usage.NormalTarget) morphTargetsFlag |= (1 << (attr.unit + 8))
      if (attr.usage == PBRVertexAttributes.Usage.TangentTarget) morphTargetsFlag |= (1 << (attr.unit + 16))
      i += 1
    }
    morphTargetsFlag.toLong
  }

  override def init(program: ShaderProgram, renderable: Renderable): Unit = {
    super.init(program, renderable)
    u_mipmapScale = program.fetchUniformLocation("u_mipmapScale", false)
    u_texCoord0Transform = program.fetchUniformLocation("u_texCoord0Transform", false)
    u_texCoord1Transform = program.fetchUniformLocation("u_texCoord1Transform", false)
    u_morphTargets1 = program.fetchUniformLocation("u_morphTargets1", false)
    u_morphTargets2 = program.fetchUniformLocation("u_morphTargets2", false)
    u_ambientLight = program.fetchUniformLocation("u_ambientLight", false)
    u_csmPCFClip = program.fetchUniformLocation("u_csmPCFClip", false)
    u_csmTransforms = program.fetchUniformLocation("u_csmTransforms", false)
    var i = 0
    while (i < cascadeCount) {
      u_csmSamplers(i) = program.fetchUniformLocation("u_csmSamplers" + i, false)
      i += 1
    }
  }

  override protected def bindMaterial(attributes: Attributes): Unit = {
    super.bindMaterial(attributes)

    val transformTexture = Array.ofDim[PBRTextureAttribute](2)
    for (textureType <- allTextureTypes)
      attributes.getAs[PBRTextureAttribute](textureType).foreach { attribute =>
        transformTexture(attribute.uvIndex) = attribute
      }

    if (u_texCoord0Transform != UniformLocation.notFound) {
      PBRCommon.setTextureTransform(textureTransform, sge.utils.Nullable(transformTexture(0)))
      program.foreach(_.setUniformMatrix(u_texCoord0Transform, textureTransform))
    }
    if (u_texCoord1Transform != UniformLocation.notFound) {
      PBRCommon.setTextureTransform(textureTransform, sge.utils.Nullable(transformTexture(1)))
      program.foreach(_.setUniformMatrix(u_texCoord1Transform, textureTransform))
    }
  }

  override def render(renderable: Renderable, combinedAttributes: Attributes): Unit = {
    program.foreach { p =>
      if (u_mipmapScale != UniformLocation.notFound) {
        val specularEnv  = combinedAttributes.getAs[PBRCubemapAttribute](PBRCubemapAttribute.SpecularEnv)
        val mipmapFactor = specularEnv
          .map { env =>
            (Math.log(env.textureDescription.texture.get.width.toInt.toDouble) / Math.log(2.0)).toFloat
          }
          .getOrElse(1f)
        p.setUniformf(u_mipmapScale, mipmapFactor)
      }

      if (u_morphTargets1 != UniformLocation.notFound) {
        renderable.userData match {
          case wv: WeightVector =>
            p.setUniformf(u_morphTargets1, wv.get(0), wv.get(1), wv.get(2), wv.get(3))
          case _ =>
            p.setUniformf(u_morphTargets1, 0f, 0f, 0f, 0f)
        }
      }
      if (u_morphTargets2 != UniformLocation.notFound) {
        renderable.userData match {
          case wv: WeightVector =>
            p.setUniformf(u_morphTargets2, wv.get(4), wv.get(5), wv.get(6), wv.get(7))
          case _ =>
            p.setUniformf(u_morphTargets2, 0f, 0f, 0f, 0f)
        }
      }
    }

    super.render(renderable, combinedAttributes)
  }

  override protected def bindLights(renderable: Renderable, attributes: Attributes): Unit = {
    // Update color (to apply intensity) before default binding
    attributes.getAs[DirectionalLightsAttribute](DirectionalLightsAttribute.Type).foreach { dla =>
      var i = 0
      while (i < dla.lights.size) {
        dla.lights(i) match {
          case ex: DirectionalLightEx => ex.updateColor()
          case _ => ()
        }
        i += 1
      }
    }

    super.bindLights(renderable, attributes)

    program.foreach { p =>
      attributes.getAs[ColorAttribute](ColorAttribute.AmbientLight).foreach { ambientLight =>
        p.setUniformf(u_ambientLight, ambientLight.color.r, ambientLight.color.g, ambientLight.color.b)
      }

      attributes.getAs[CascadeShadowMapAttribute](CascadeShadowMapAttribute.Type).foreach { csmAttrib =>
        if (u_csmSamplers.length > 0 && u_csmSamplers(0) != UniformLocation.notFound) {
          val lights = csmAttrib.cascadeShadowMap.lights
          var i      = 0
          while (i < lights.size) {
            val light   = lights(i)
            val mapSize = light.depthMap.texture.get.width.toInt.toFloat
            val pcf     = 1f / (2f * mapSize)
            val clip    = 3f / (2f * mapSize)
            val unit    = context.get.textureBinder.bind(light.depthMap)
            p.setUniformi(u_csmSamplers(i), unit)
            System.arraycopy(light.projViewTrans.values, 0, csmTransforms, i * 16, 16)
            csmPCFClip(i * 2) = pcf
            csmPCFClip(i * 2 + 1) = clip
            i += 1
          }
          p.setUniformMatrix4fv(u_csmTransforms, csmTransforms, 0, csmTransforms.length)
          p.setUniform2fv(u_csmPCFClip, csmPCFClip, 0, csmPCFClip.length)
        }
      }
    }
  }
}

object PBRShader {

  private val v2:               Vector2 = Vector2()
  private val textureTransform: Matrix3 = Matrix3()

  // --- Uniforms and Setters ---

  val baseColorTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_diffuseTexture", PBRTextureAttribute.BaseColorTexture)
  val baseColorTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.BaseColorTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val baseColorFactorUniform: BaseShader.Uniform = BaseShader.Uniform("u_BaseColorFactor", PBRColorAttribute.BaseColorFactor)
  val baseColorFactorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val color = combinedAttributes.getAs[ColorAttribute](PBRColorAttribute.BaseColorFactor).map(_.color).getOrElse(Color.WHITE)
      shader.set(inputID, color)
    }
  }

  val emissiveTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_emissiveTexture", PBRTextureAttribute.EmissiveTexture)
  val emissiveTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.EmissiveTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val normalTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_normalTexture", PBRTextureAttribute.NormalTexture)
  val normalTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.NormalTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val metallicRoughnessTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_MetallicRoughnessSampler", PBRTextureAttribute.MetallicRoughnessTexture)
  val metallicRoughnessTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.MetallicRoughnessTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val metallicRoughnessUniform: BaseShader.Uniform = BaseShader.Uniform("u_MetallicRoughnessValues")
  val metallicRoughnessSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val metallic  = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.Metallic).map(_.value).getOrElse(1f)
      val roughness = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.Roughness).map(_.value).getOrElse(1f)
      shader.set(inputID, v2.set(metallic, roughness))
    }
  }

  val normalScaleUniform: BaseShader.Uniform = BaseShader.Uniform("u_NormalScale")
  val normalScaleSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val normalScale = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.NormalScale).map(_.value).getOrElse(1f)
      shader.setFloat(inputID, normalScale)
    }
  }

  val occlusionStrengthUniform: BaseShader.Uniform = BaseShader.Uniform("u_OcclusionStrength")
  val occlusionStrengthSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val occlusionStrength = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.OcclusionStrength).map(_.value).getOrElse(1f)
      shader.setFloat(inputID, occlusionStrength)
    }
  }

  val occlusionTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_OcclusionSampler", PBRTextureAttribute.OcclusionTexture)
  val occlusionTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.OcclusionTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val diffuseEnvTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_DiffuseEnvSampler", PBRCubemapAttribute.DiffuseEnv)
  val diffuseEnvTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val attr = combinedAttributes.getAs[PBRCubemapAttribute](PBRCubemapAttribute.DiffuseEnv).get
      val unit = shader.context.get.textureBinder.bind(attr.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val specularEnvTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_SpecularEnvSampler", PBRCubemapAttribute.SpecularEnv)
  val specularEnvTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val attr = combinedAttributes.getAs[PBRCubemapAttribute](PBRCubemapAttribute.SpecularEnv).get
      val unit = shader.context.get.textureBinder.bind(attr.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val envRotationUniform: BaseShader.Uniform = BaseShader.Uniform("u_envRotation", PBRMatrixAttribute.EnvRotation)
  val envRotationSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    private val mat3:                                                                                           Matrix3 = Matrix3()
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit    = {
      val attr = combinedAttributes.getAs[PBRMatrixAttribute](PBRMatrixAttribute.EnvRotation).get
      shader.set(inputID, mat3.set(attr.matrix))
    }
  }

  val brdfLUTTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_brdfLUT")
  val brdfLUTTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      combinedAttributes.getAs[PBRTextureAttribute](PBRTextureAttribute.BRDFLUTTexture).foreach { attr =>
        val unit = shader.context.get.textureBinder.bind(attr.textureDescription)
        PBRShaderUtils.setInt(shader, inputID, unit)
      }
  }

  val shadowBiasUniform: BaseShader.Uniform = BaseShader.Uniform("u_shadowBias")
  val shadowBiasSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val value = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.ShadowBias).map(_.value).getOrElse(0f)
      PBRShaderUtils.setFloat(shader, inputID, value)
    }
  }

  val fogEquationUniform: BaseShader.Uniform = BaseShader.Uniform("u_fogEquation")
  val fogEquationSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val value = combinedAttributes.getAs[FogAttribute](FogAttribute.FogEquation).map(_.value).getOrElse(Vector3.Zero)
      shader.set(inputID, value)
    }
  }

  val emissiveScaledColor: BaseShader.Setter = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val emissive = combinedAttributes.getAs[ColorAttribute](ColorAttribute.Emissive).get
      val eiOpt    = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.EmissiveIntensity)
      if (eiOpt.isDefined) {
        val ei = eiOpt.get
        PBRShaderUtils.setFloat4(
          shader,
          inputID,
          emissive.color.r * ei.value,
          emissive.color.g * ei.value,
          emissive.color.b * ei.value,
          emissive.color.a * ei.value
        )
      } else {
        shader.set(inputID, emissive.color)
      }
    }
  }

  val transmissionFactorUniform: BaseShader.Uniform = BaseShader.Uniform("u_transmissionFactor")
  val transmissionFactorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val value = combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.TransmissionFactor).map(_.value).getOrElse(0f)
      PBRShaderUtils.setFloat(shader, inputID, value)
    }
  }

  val transmissionTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_transmissionSampler", PBRTextureAttribute.TransmissionTexture)
  val transmissionTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.TransmissionTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val iorUniform: BaseShader.Uniform = BaseShader.Uniform("u_ior")
  val iorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.IOR).get.value)
  }

  val thicknessFactorUniform: BaseShader.Uniform = BaseShader.Uniform("u_thicknessFactor")
  val thicknessFactorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRVolumeAttribute](PBRVolumeAttribute.Type).get.thicknessFactor)
  }

  val volumeDistanceUniform: BaseShader.Uniform = BaseShader.Uniform("u_attenuationDistance")
  val volumeDistanceSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRVolumeAttribute](PBRVolumeAttribute.Type).get.attenuationDistance)
  }

  val volumeColorUniform: BaseShader.Uniform = BaseShader.Uniform("u_attenuationColor")
  val volumeColorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val a = combinedAttributes.getAs[PBRVolumeAttribute](PBRVolumeAttribute.Type).get
      PBRShaderUtils.setFloat3(shader, inputID, a.attenuationColor.r, a.attenuationColor.g, a.attenuationColor.b)
    }
  }

  val thicknessTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_thicknessSampler", PBRTextureAttribute.ThicknessTexture)
  val thicknessTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.ThicknessTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val specularFactorUniform: BaseShader.Uniform = BaseShader.Uniform("u_specularFactor")
  val specularFactorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRFloatAttribute](PBRFloatAttribute.SpecularFactor).get.value)
  }

  val specularColorFactorUniform: BaseShader.Uniform = BaseShader.Uniform("u_specularColorFactor")
  val specularColorFactorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val a = combinedAttributes.getAs[PBRHDRColorAttribute](PBRHDRColorAttribute.Specular).get
      PBRShaderUtils.setFloat3(shader, inputID, a.r, a.g, a.b)
    }
  }

  val specularFactorTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_specularFactorSampler", PBRTextureAttribute.SpecularFactorTexture)
  val specularFactorTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.SpecularFactorTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val specularColorTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_specularColorSampler", PBRTextureAttribute.SpecularColorTexture)
  val specularColorTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.SpecularColorTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val iridescenceFactorUniform: BaseShader.Uniform = BaseShader.Uniform("u_iridescenceFactor")
  val iridescenceFactorSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRIridescenceAttribute](PBRIridescenceAttribute.Type).get.factor)
  }

  val iridescenceIORUniform: BaseShader.Uniform = BaseShader.Uniform("u_iridescenceIOR")
  val iridescenceIORSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRIridescenceAttribute](PBRIridescenceAttribute.Type).get.ior)
  }

  val iridescenceThicknessMinUniform: BaseShader.Uniform = BaseShader.Uniform("u_iridescenceThicknessMin")
  val iridescenceThicknessMinSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRIridescenceAttribute](PBRIridescenceAttribute.Type).get.thicknessMin)
  }

  val iridescenceThicknessMaxUniform: BaseShader.Uniform = BaseShader.Uniform("u_iridescenceThicknessMax")
  val iridescenceThicknessMaxSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      PBRShaderUtils.setFloat(shader, inputID, combinedAttributes.getAs[PBRIridescenceAttribute](PBRIridescenceAttribute.Type).get.thicknessMax)
  }

  val iridescenceTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_iridescenceSampler", PBRTextureAttribute.IridescenceTexture)
  val iridescenceTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.IridescenceTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val iridescenceThicknessTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_iridescenceThicknessSampler", PBRTextureAttribute.IridescenceThicknessTexture)
  val iridescenceThicknessTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.IridescenceThicknessTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val transmissionSourceTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_transmissionSourceSampler", PBRTextureAttribute.TransmissionSourceTexture)
  val transmissionSourceTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[TextureAttribute](PBRTextureAttribute.TransmissionSourceTexture).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val transmissionSourceMipmapUniform: BaseShader.Uniform = BaseShader.Uniform("u_transmissionSourceMipmapScale")
  val transmissionSourceMipmapSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val mipmapFactor = combinedAttributes
        .getAs[PBRTextureAttribute](PBRTextureAttribute.TransmissionSourceTexture)
        .map { a =>
          (Math.log(a.textureDescription.texture.get.width.toDouble) / Math.log(2.0)).toFloat
        }
        .getOrElse(1f)
      PBRShaderUtils.setFloat(shader, inputID, mipmapFactor)
    }
  }

  val specularMirrorTextureUniform: BaseShader.Uniform = BaseShader.Uniform("u_mirrorSpecularSampler", MirrorSourceAttribute.Type)
  val specularMirrorTextureSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val unit = shader.context.get.textureBinder.bind(combinedAttributes.getAs[MirrorSourceAttribute](MirrorSourceAttribute.Type).get.textureDescription)
      PBRShaderUtils.setInt(shader, inputID, unit)
    }
  }

  val specularMirrorMipmapUniform: BaseShader.Uniform = BaseShader.Uniform("u_mirrorMipmapScale")
  val specularMirrorMipmapSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val mipmapFactor = combinedAttributes
        .getAs[MirrorSourceAttribute](MirrorSourceAttribute.Type)
        .map { a =>
          (Math.log(a.textureDescription.texture.get.width.toDouble) / Math.log(2.0)).toFloat
        }
        .getOrElse(1f)
      PBRShaderUtils.setFloat(shader, inputID, mipmapFactor)
    }
  }

  val mirrorNormalUniform: BaseShader.Uniform = BaseShader.Uniform("u_mirrorNormal")
  val mirrorNormalSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val a = combinedAttributes.getAs[MirrorSourceAttribute](MirrorSourceAttribute.Type).get
      shader.set(inputID, a.normal)
    }
  }

  val viewportInvUniform: BaseShader.Uniform = BaseShader.Uniform("u_viewportInv")
  val viewportInvSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
      shader.camera.foreach { cam =>
        shader.setFloat(inputID, 1f / cam.viewportWidth.toFloat, 1f / cam.viewportHeight.toFloat)
      }
  }

  val clippingPlaneUniform: BaseShader.Uniform = BaseShader.Uniform("u_clippingPlane")
  val clippingPlaneSetter:  BaseShader.Setter  = new BaseShader.LocalSetter {
    override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit = {
      val a = combinedAttributes.getAs[ClippingPlaneAttribute](ClippingPlaneAttribute.Type).get
      shader.setFloat(inputID, a.plane.normal.x, a.plane.normal.y, a.plane.normal.z, a.plane.d)
    }
  }

  private val allTextureTypes: Array[Long] = Array(
    PBRTextureAttribute.BaseColorTexture,
    PBRTextureAttribute.EmissiveTexture,
    PBRTextureAttribute.NormalTexture,
    PBRTextureAttribute.MetallicRoughnessTexture,
    PBRTextureAttribute.OcclusionTexture,
    PBRTextureAttribute.TransmissionTexture,
    PBRTextureAttribute.ThicknessTexture,
    PBRTextureAttribute.IridescenceTexture,
    PBRTextureAttribute.IridescenceThicknessTexture
  )

  private def getTextureCoordinateMapMask(attributes: Attributes): Long = {
    var mask      = 0L
    var maskShift = 0
    for (textureType <- allTextureTypes) {
      attributes.getAs[PBRTextureAttribute](textureType).foreach { attribute =>
        mask |= (attribute.uvIndex & 1).toLong << maskShift
      }
      maskShift += 1
    }
    mask
  }
}
