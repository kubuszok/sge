/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/shaders/DefaultShader.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Config.vertexShader/fragmentShader use Nullable[String] instead of null Strings
 * - Java static inner classes Inputs/Setters -> companion object members
 * - Java Gdx.files/Gdx.graphics/Gdx.gl -> Sge().files/Sge().graphics/Sge().graphics.gl
 * - (using Sge) context parameter added to constructor and related methods
 * - Java dispose() -> Scala close() (AutoCloseable convention)
 * - defaultCullFace/defaultDepthFunc are @deprecated vars in companion (matching Java @Deprecated)
 * - createPrefix uses StringBuilder (matches Java string concatenation semantics)
 * - Setters access camera/context/program via Nullable.foreach (safe null handling)
 * - Bones.idtMatrix moved to Bones companion object (static-like field)
 * - ACubemap.ones/tmpV1 moved to ACubemap companion object (static-like fields)
 * - Java `normalMatrix` private field (line 779) -> Scala has `new Matrix3()` orphan on line 325
 *   (appears to be a stray statement, but harmless)
 * - combineAttributes is private[shaders] (needed by DepthShader companion)
 * - All 5 constructors match Java constructor chain
 * - All methods present: init, begin, render, end, close, canRender, compareTo, equals,
 *   bindMaterial, bindLights, defaultCullFace, defaultCullFace_=, defaultDepthFunc,
 *   defaultDepthFunc_=
 * - Fixes (2026-03-04): getDefaultCullFace/setDefaultCullFace → def defaultCullFace/defaultCullFace_=;
 *   getDefaultDepthFunc/setDefaultDepthFunc → def defaultDepthFunc/defaultDepthFunc_=;
 *   companion getDefaultVertexShader()/getDefaultFragmentShader() → def defaultVertexShader/defaultFragmentShader
 * - Java equals(DefaultShader) overload -> Scala equals(Any) pattern match (equivalent)
 * - FIXME comments preserved from Java source
 */
package sge
package graphics
package g3d
package shaders

import scala.language.implicitConversions

import sge.graphics.g3d.attributes.{
  BlendingAttribute,
  ColorAttribute,
  CubemapAttribute,
  DepthTestAttribute,
  DirectionalLightsAttribute,
  FloatAttribute,
  IntAttribute,
  PointLightsAttribute,
  SpotLightsAttribute,
  TextureAttribute
}
import sge.graphics.g3d.environment.{ AmbientCubemap, DirectionalLight, PointLight, SpotLight }
import sge.graphics.GLTexture
import sge.graphics.g3d.utils.{ RenderContext, TextureDescriptor }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Matrix3, Matrix4, Vector3 }
import sge.utils.{ DynamicArray, Nullable, SgeError }

class DefaultShader(
  renderable:    Renderable,
  val config:    DefaultShader.Config,
  shaderProgram: ShaderProgram
)(using Sge)
    extends BaseShader {

  private val _combinedAttributes: Attributes = DefaultShader.combineAttributes(renderable)

  this.program = Nullable(shaderProgram)

  protected val lighting:           Boolean = renderable.environment.isDefined
  protected val environmentCubemap: Boolean = _combinedAttributes.has(CubemapAttribute.EnvironmentMap) ||
    (lighting && _combinedAttributes.has(CubemapAttribute.EnvironmentMap))
  protected val shadowMap: Boolean = lighting && renderable.environment.exists(_.shadowMap.isDefined)

  /** The attributes that this shader supports */
  val attributesMask:     Long = _combinedAttributes.getMask | DefaultShader.optionalAttributes
  private val vertexMask: Long =
    renderable.meshPart.mesh.vertexAttributes.maskWithSizePacked
  private val textureCoordinates: Int =
    renderable.meshPart.mesh.vertexAttributes.getTextureCoordinates()
  private var boneWeightsLocations: Nullable[Array[Int]] = Nullable.empty

  protected val directionalLights: Array[DirectionalLight] = {
    val n   = if (lighting && config.numDirectionalLights > 0) config.numDirectionalLights else 0
    val arr = new Array[DirectionalLight](n)
    var i   = 0
    while (i < arr.length) {
      arr(i) = DirectionalLight()
      i += 1
    }
    arr
  }

  protected val pointLights: Array[PointLight] = {
    val n   = if (lighting && config.numPointLights > 0) config.numPointLights else 0
    val arr = new Array[PointLight](n)
    var i   = 0
    while (i < arr.length) {
      arr(i) = PointLight()
      i += 1
    }
    arr
  }

  protected val spotLights: Array[SpotLight] = {
    val n   = if (lighting && config.numSpotLights > 0) config.numSpotLights else 0
    val arr = new Array[SpotLight](n)
    var i   = 0
    while (i < arr.length) {
      arr(i) = SpotLight()
      i += 1
    }
    arr
  }

  protected val ambientCubemap: AmbientCubemap = new AmbientCubemap()

  /** The renderable used to create this shader, invalid after the call to init */
  private var _renderable: Nullable[Renderable] = Nullable(renderable)

  if (!config.ignoreUnimplemented && (DefaultShader.implementedFlags & attributesMask) != attributesMask)
    throw SgeError.GraphicsError("Some attributes not implemented yet (" + attributesMask + ")")

  if (renderable.bones.isDefined && renderable.bones.map(_.length).getOrElse(0) > config.numBones) {
    throw SgeError.GraphicsError(
      "too many bones: " + renderable.bones.map(_.length).getOrElse(0) + ", max configured: " + config.numBones
    )
  }

  locally {
    val boneWeights = renderable.meshPart.mesh.vertexAttributes.boneWeights
    if (boneWeights > config.numBoneWeights) {
      throw SgeError.GraphicsError(
        "too many bone weights: " + boneWeights + ", max configured: " + config.numBoneWeights
      )
    }
    if (renderable.bones.isDefined) {
      boneWeightsLocations = Nullable(new Array[Int](config.numBoneWeights))
    }
  }

  // Global uniforms
  val u_projTrans:     Int = register(DefaultShader.Inputs.projTrans, DefaultShader.Setters.projTrans)
  val u_viewTrans:     Int = register(DefaultShader.Inputs.viewTrans, DefaultShader.Setters.viewTrans)
  val u_projViewTrans: Int =
    register(DefaultShader.Inputs.projViewTrans, DefaultShader.Setters.projViewTrans)
  val u_cameraPosition: Int =
    register(DefaultShader.Inputs.cameraPosition, DefaultShader.Setters.cameraPosition)
  val u_cameraDirection: Int =
    register(DefaultShader.Inputs.cameraDirection, DefaultShader.Setters.cameraDirection)
  val u_cameraUp:      Int = register(DefaultShader.Inputs.cameraUp, DefaultShader.Setters.cameraUp)
  val u_cameraNearFar: Int =
    register(DefaultShader.Inputs.cameraNearFar, DefaultShader.Setters.cameraNearFar)
  val u_time: Int = register(BaseShader.Uniform("u_time"))
  // Object uniforms
  val u_worldTrans:     Int = register(DefaultShader.Inputs.worldTrans, DefaultShader.Setters.worldTrans)
  val u_viewWorldTrans: Int =
    register(DefaultShader.Inputs.viewWorldTrans, DefaultShader.Setters.viewWorldTrans)
  val u_projViewWorldTrans: Int =
    register(DefaultShader.Inputs.projViewWorldTrans, DefaultShader.Setters.projViewWorldTrans)
  val u_normalMatrix: Int =
    register(DefaultShader.Inputs.normalMatrix, DefaultShader.Setters.normalMatrix)
  val u_bones: Int =
    if (renderable.bones.isDefined && config.numBones > 0)
      register(DefaultShader.Inputs.bones, new DefaultShader.Setters.Bones(config.numBones))
    else -1

  // Material uniforms
  val u_shininess: Int =
    register(DefaultShader.Inputs.shininess, DefaultShader.Setters.shininess)
  val u_opacity:      Int = register(DefaultShader.Inputs.opacity)
  val u_diffuseColor: Int =
    register(DefaultShader.Inputs.diffuseColor, DefaultShader.Setters.diffuseColor)
  val u_diffuseTexture: Int =
    register(DefaultShader.Inputs.diffuseTexture, DefaultShader.Setters.diffuseTexture)
  val u_diffuseUVTransform: Int =
    register(DefaultShader.Inputs.diffuseUVTransform, DefaultShader.Setters.diffuseUVTransform)
  val u_specularColor: Int =
    register(DefaultShader.Inputs.specularColor, DefaultShader.Setters.specularColor)
  val u_specularTexture: Int =
    register(DefaultShader.Inputs.specularTexture, DefaultShader.Setters.specularTexture)
  val u_specularUVTransform: Int =
    register(DefaultShader.Inputs.specularUVTransform, DefaultShader.Setters.specularUVTransform)
  val u_emissiveColor: Int =
    register(DefaultShader.Inputs.emissiveColor, DefaultShader.Setters.emissiveColor)
  val u_emissiveTexture: Int =
    register(DefaultShader.Inputs.emissiveTexture, DefaultShader.Setters.emissiveTexture)
  val u_emissiveUVTransform: Int =
    register(DefaultShader.Inputs.emissiveUVTransform, DefaultShader.Setters.emissiveUVTransform)
  val u_reflectionColor: Int =
    register(DefaultShader.Inputs.reflectionColor, DefaultShader.Setters.reflectionColor)
  val u_reflectionTexture: Int =
    register(DefaultShader.Inputs.reflectionTexture, DefaultShader.Setters.reflectionTexture)
  val u_reflectionUVTransform: Int =
    register(DefaultShader.Inputs.reflectionUVTransform, DefaultShader.Setters.reflectionUVTransform)
  val u_normalTexture: Int =
    register(DefaultShader.Inputs.normalTexture, DefaultShader.Setters.normalTexture)
  val u_normalUVTransform: Int =
    register(DefaultShader.Inputs.normalUVTransform, DefaultShader.Setters.normalUVTransform)
  val u_ambientTexture: Int =
    register(DefaultShader.Inputs.ambientTexture, DefaultShader.Setters.ambientTexture)
  val u_ambientUVTransform: Int =
    register(DefaultShader.Inputs.ambientUVTransform, DefaultShader.Setters.ambientUVTransform)
  val u_alphaTest: Int = register(DefaultShader.Inputs.alphaTest)

  // Lighting uniforms
  protected val u_ambientCubemap: Int =
    if (lighting)
      register(
        DefaultShader.Inputs.ambientCube,
        new DefaultShader.Setters.ACubemap(config.numDirectionalLights, config.numPointLights)
      )
    else -1
  protected val u_environmentCubemap: Int =
    if (environmentCubemap)
      register(DefaultShader.Inputs.environmentCubemap, DefaultShader.Setters.environmentCubemap)
    else -1
  protected val u_dirLights0color:      Int = register(BaseShader.Uniform("u_dirLights[0].color"))
  protected val u_dirLights0direction:  Int = register(BaseShader.Uniform("u_dirLights[0].direction"))
  protected val u_dirLights1color:      Int = register(BaseShader.Uniform("u_dirLights[1].color"))
  protected val u_pointLights0color:    Int = register(BaseShader.Uniform("u_pointLights[0].color"))
  protected val u_pointLights0position: Int =
    register(BaseShader.Uniform("u_pointLights[0].position"))
  protected val u_pointLights0intensity: Int =
    register(BaseShader.Uniform("u_pointLights[0].intensity"))
  protected val u_pointLights1color:   Int = register(BaseShader.Uniform("u_pointLights[1].color"))
  protected val u_spotLights0color:    Int = register(BaseShader.Uniform("u_spotLights[0].color"))
  protected val u_spotLights0position: Int =
    register(BaseShader.Uniform("u_spotLights[0].position"))
  protected val u_spotLights0intensity: Int =
    register(BaseShader.Uniform("u_spotLights[0].intensity"))
  protected val u_spotLights0direction: Int =
    register(BaseShader.Uniform("u_spotLights[0].direction"))
  protected val u_spotLights0cutoffAngle: Int =
    register(BaseShader.Uniform("u_spotLights[0].cutoffAngle"))
  protected val u_spotLights0exponent: Int =
    register(BaseShader.Uniform("u_spotLights[0].exponent"))
  protected val u_spotLights1color:       Int = register(BaseShader.Uniform("u_spotLights[1].color"))
  protected val u_fogColor:               Int = register(BaseShader.Uniform("u_fogColor"))
  protected val u_shadowMapProjViewTrans: Int = register(BaseShader.Uniform("u_shadowMapProjViewTrans"))
  protected val u_shadowTexture:          Int = register(BaseShader.Uniform("u_shadowTexture"))
  protected val u_shadowPCFOffset:        Int = register(BaseShader.Uniform("u_shadowPCFOffset"))
  // FIXME Cache vertex attribute locations...

  protected var dirLightsLoc:                Int = 0
  protected var dirLightsColorOffset:        Int = 0
  protected var dirLightsDirectionOffset:    Int = 0
  protected var dirLightsSize:               Int = 0
  protected var pointLightsLoc:              Int = 0
  protected var pointLightsColorOffset:      Int = 0
  protected var pointLightsPositionOffset:   Int = 0
  protected var pointLightsIntensityOffset:  Int = 0
  protected var pointLightsSize:             Int = 0
  protected var spotLightsLoc:               Int = 0
  protected var spotLightsColorOffset:       Int = 0
  protected var spotLightsPositionOffset:    Int = 0
  protected var spotLightsDirectionOffset:   Int = 0
  protected var spotLightsIntensityOffset:   Int = 0
  protected var spotLightsCutoffAngleOffset: Int = 0
  protected var spotLightsExponentOffset:    Int = 0
  protected var spotLightsSize:              Int = 0

  def this(renderable: Renderable)(using Sge) = {
    this(
      renderable,
      DefaultShader.Config(), {
        val cfg    = DefaultShader.Config()
        val prefix = DefaultShader.createPrefix(renderable, cfg)
        val vs     = cfg.vertexShader.getOrElse(DefaultShader.defaultVertexShader)
        val fs     = cfg.fragmentShader.getOrElse(DefaultShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )
  }

  def this(renderable: Renderable, config: DefaultShader.Config)(using Sge) = {
    this(
      renderable,
      config, {
        val prefix = DefaultShader.createPrefix(renderable, config)
        val vs     = config.vertexShader.getOrElse(DefaultShader.defaultVertexShader)
        val fs     = config.fragmentShader.getOrElse(DefaultShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )
  }

  def this(renderable: Renderable, config: DefaultShader.Config, prefix: String)(using Sge) = {
    this(
      renderable,
      config, {
        val vs = config.vertexShader.getOrElse(DefaultShader.defaultVertexShader)
        val fs = config.fragmentShader.getOrElse(DefaultShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )
  }

  def this(
    renderable:     Renderable,
    config:         DefaultShader.Config,
    prefix:         String,
    vertexShader:   String,
    fragmentShader: String
  )(using Sge) = {
    this(renderable, config, ShaderProgram(prefix + vertexShader, prefix + fragmentShader))
  }

  override def init(): Unit = {
    val prog = this.program.getOrElse(throw SgeError.GraphicsError("No shader program"))
    this.program = Nullable.empty
    init(prog, _renderable.getOrElse(throw SgeError.GraphicsError("No renderable for init")))
    _renderable = Nullable.empty

    dirLightsLoc = loc(u_dirLights0color)
    dirLightsColorOffset = loc(u_dirLights0color) - dirLightsLoc
    dirLightsDirectionOffset = loc(u_dirLights0direction) - dirLightsLoc
    dirLightsSize = loc(u_dirLights1color) - dirLightsLoc
    if (dirLightsSize < 0) dirLightsSize = 0

    pointLightsLoc = loc(u_pointLights0color)
    pointLightsColorOffset = loc(u_pointLights0color) - pointLightsLoc
    pointLightsPositionOffset = loc(u_pointLights0position) - pointLightsLoc
    pointLightsIntensityOffset =
      if (has(u_pointLights0intensity)) loc(u_pointLights0intensity) - pointLightsLoc else -1
    pointLightsSize = loc(u_pointLights1color) - pointLightsLoc
    if (pointLightsSize < 0) pointLightsSize = 0

    spotLightsLoc = loc(u_spotLights0color)
    spotLightsColorOffset = loc(u_spotLights0color) - spotLightsLoc
    spotLightsPositionOffset = loc(u_spotLights0position) - spotLightsLoc
    spotLightsDirectionOffset = loc(u_spotLights0direction) - spotLightsLoc
    spotLightsIntensityOffset =
      if (has(u_spotLights0intensity)) loc(u_spotLights0intensity) - spotLightsLoc else -1
    spotLightsCutoffAngleOffset = loc(u_spotLights0cutoffAngle) - spotLightsLoc
    spotLightsExponentOffset = loc(u_spotLights0exponent) - spotLightsLoc
    spotLightsSize = loc(u_spotLights1color) - spotLightsLoc
    if (spotLightsSize < 0) spotLightsSize = 0

    boneWeightsLocations.foreach { locs =>
      var i = 0
      while (i < locs.length) {
        locs(i) = prog.getAttributeLocation(ShaderProgram.BONEWEIGHT_ATTRIBUTE + i)
        i += 1
      }
    }
  }

  Matrix3()
  private var time:      Float   = 0f
  private var lightsSet: Boolean = false

  override def begin(camera: Camera, context: RenderContext): Unit = {
    super.begin(camera, context)

    for (dirLight <- directionalLights)
      dirLight.set(0, 0, 0, 0, -1, 0)
    for (pointLight <- pointLights)
      pointLight.set(0, 0, 0, 0, 0, 0, 0)
    for (spotLight <- spotLights)
      spotLight.set(0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0)
    lightsSet = false

    if (has(u_time)) {
      time += Sge().graphics.deltaTime
      setFloat(u_time, time)
    }

    // set generic vertex attribute value for all bone weights in case a mesh has missing attributes.
    boneWeightsLocations.foreach { locs =>
      for (location <- locs)
        if (location >= 0) {
          Sge().graphics.gl.glVertexAttrib2f(location, 0, 0)
        }
    }
  }

  override def render(renderable: Renderable, combinedAttributes: Attributes): Unit = {
    if (!combinedAttributes.has(BlendingAttribute.Type))
      context.foreach(_.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA))
    bindMaterial(combinedAttributes)
    if (lighting) bindLights(renderable, combinedAttributes)
    super.render(renderable, combinedAttributes)
  }

  override def end(): Unit =
    super.end()

  protected def bindMaterial(attributes: Attributes): Unit = {
    var cullFace =
      if (config.defaultCullFace == -1) DefaultShader.defaultCullFace: @scala.annotation.nowarn("msg=deprecated")
      else config.defaultCullFace
    var depthFunc =
      if (config.defaultDepthFunc == -1) DefaultShader.defaultDepthFunc: @scala.annotation.nowarn("msg=deprecated")
      else config.defaultDepthFunc
    var depthRangeNear = 0f
    var depthRangeFar  = 1f
    var depthMask      = true

    for (attr <- attributes) {
      val t = attr.`type`
      if (BlendingAttribute.is(t)) {
        val ba = attr.asInstanceOf[BlendingAttribute]
        context.foreach(_.setBlending(true, ba.sourceFunction, ba.destFunction))
        setFloat(u_opacity, ba.opacity)
      } else if ((t & IntAttribute.CullFace) == IntAttribute.CullFace)
        cullFace = attr.asInstanceOf[IntAttribute].value
      else if ((t & FloatAttribute.AlphaTest) == FloatAttribute.AlphaTest)
        setFloat(u_alphaTest, attr.asInstanceOf[FloatAttribute].value)
      else if ((t & DepthTestAttribute.Type) == DepthTestAttribute.Type) {
        val dta = attr.asInstanceOf[DepthTestAttribute]
        depthFunc = dta.depthFunc
        depthRangeNear = dta.depthRangeNear
        depthRangeFar = dta.depthRangeFar
        depthMask = dta.depthMask
      } else if (!config.ignoreUnimplemented)
        throw SgeError.GraphicsError("Unknown material attribute: " + attr.toString)
    }

    context.foreach { ctx =>
      ctx.setCullFace(cullFace)
      ctx.setDepthTest(depthFunc, depthRangeNear, depthRangeFar)
      ctx.setDepthMask(depthMask)
    }
  }

  Vector3()

  protected def bindLights(renderable: Renderable, attributes: Attributes): Unit = {
    val lights: Nullable[Environment]                = renderable.environment
    val dla:    Nullable[DirectionalLightsAttribute] =
      attributes.getAs[DirectionalLightsAttribute](DirectionalLightsAttribute.Type)
    val dirs: Nullable[DynamicArray[DirectionalLight]] = dla.map(_.lights)
    val pla:  Nullable[PointLightsAttribute]           =
      attributes.getAs[PointLightsAttribute](PointLightsAttribute.Type)
    val points: Nullable[DynamicArray[PointLight]] = pla.map(_.lights)
    val sla:    Nullable[SpotLightsAttribute]      =
      attributes.getAs[SpotLightsAttribute](SpotLightsAttribute.Type)
    val spots: Nullable[DynamicArray[SpotLight]] = sla.map(_.lights)

    if (dirLightsLoc >= 0) {
      var i = 0
      while (i < directionalLights.length) {
        if (dirs.isEmpty || dirs.forall(d => i >= d.size)) {
          if (
            lightsSet && directionalLights(i).color.r == 0f && directionalLights(i).color.g == 0f
            && directionalLights(i).color.b == 0f
          ) {
            // continue - skip this iteration
          } else {
            directionalLights(i).color.set(0, 0, 0, 1)
            val idx = dirLightsLoc + i * dirLightsSize
            program.foreach { prog =>
              prog.setUniformf(
                idx + dirLightsColorOffset,
                directionalLights(i).color.r,
                directionalLights(i).color.g,
                directionalLights(i).color.b
              )
              prog.setUniformf(
                idx + dirLightsDirectionOffset,
                directionalLights(i).direction.x,
                directionalLights(i).direction.y,
                directionalLights(i).direction.z
              )
            }
            if (dirLightsSize <= 0) {
              i = directionalLights.length // break
            }
          }
        } else if (lightsSet && dirs.exists(d => directionalLights(i).equals(d(i)))) {
          // continue - skip this iteration
        } else {
          dirs.foreach(d => directionalLights(i).set(d(i)))
          val idx = dirLightsLoc + i * dirLightsSize
          program.foreach { prog =>
            prog.setUniformf(
              idx + dirLightsColorOffset,
              directionalLights(i).color.r,
              directionalLights(i).color.g,
              directionalLights(i).color.b
            )
            prog.setUniformf(
              idx + dirLightsDirectionOffset,
              directionalLights(i).direction.x,
              directionalLights(i).direction.y,
              directionalLights(i).direction.z
            )
          }
          if (dirLightsSize <= 0) {
            i = directionalLights.length // break
          }
        }
        i += 1
      }
    }

    if (pointLightsLoc >= 0) {
      var i = 0
      while (i < pointLights.length) {
        if (points.isEmpty || points.forall(p => i >= p.size)) {
          if (lightsSet && pointLights(i).intensity == 0f) {
            // continue
          } else {
            pointLights(i).intensity = 0f
            val idx = pointLightsLoc + i * pointLightsSize
            program.foreach { prog =>
              prog.setUniformf(
                idx + pointLightsColorOffset,
                pointLights(i).color.r * pointLights(i).intensity,
                pointLights(i).color.g * pointLights(i).intensity,
                pointLights(i).color.b * pointLights(i).intensity
              )
              prog.setUniformf(
                idx + pointLightsPositionOffset,
                pointLights(i).position.x,
                pointLights(i).position.y,
                pointLights(i).position.z
              )
              if (pointLightsIntensityOffset >= 0)
                prog.setUniformf(idx + pointLightsIntensityOffset, pointLights(i).intensity)
            }
            if (pointLightsSize <= 0) {
              i = pointLights.length // break
            }
          }
        } else if (lightsSet && points.exists(p => pointLights(i).equals(p(i)))) {
          // continue
        } else {
          points.foreach(p => pointLights(i).set(p(i)))
          val idx = pointLightsLoc + i * pointLightsSize
          program.foreach { prog =>
            prog.setUniformf(
              idx + pointLightsColorOffset,
              pointLights(i).color.r * pointLights(i).intensity,
              pointLights(i).color.g * pointLights(i).intensity,
              pointLights(i).color.b * pointLights(i).intensity
            )
            prog.setUniformf(
              idx + pointLightsPositionOffset,
              pointLights(i).position.x,
              pointLights(i).position.y,
              pointLights(i).position.z
            )
            if (pointLightsIntensityOffset >= 0)
              prog.setUniformf(idx + pointLightsIntensityOffset, pointLights(i).intensity)
          }
          if (pointLightsSize <= 0) {
            i = pointLights.length // break
          }
        }
        i += 1
      }
    }

    if (spotLightsLoc >= 0) {
      var i = 0
      while (i < spotLights.length) {
        if (spots.isEmpty || spots.forall(s => i >= s.size)) {
          if (lightsSet && spotLights(i).intensity == 0f) {
            // continue
          } else {
            spotLights(i).intensity = 0f
            val idx = spotLightsLoc + i * spotLightsSize
            program.foreach { prog =>
              prog.setUniformf(
                idx + spotLightsColorOffset,
                spotLights(i).color.r * spotLights(i).intensity,
                spotLights(i).color.g * spotLights(i).intensity,
                spotLights(i).color.b * spotLights(i).intensity
              )
              prog.setUniformf(idx + spotLightsPositionOffset, spotLights(i).position)
              prog.setUniformf(idx + spotLightsDirectionOffset, spotLights(i).direction)
              prog.setUniformf(idx + spotLightsCutoffAngleOffset, spotLights(i).cutoffAngle)
              prog.setUniformf(idx + spotLightsExponentOffset, spotLights(i).exponent)
              if (spotLightsIntensityOffset >= 0)
                prog.setUniformf(idx + spotLightsIntensityOffset, spotLights(i).intensity)
            }
            if (spotLightsSize <= 0) {
              i = spotLights.length // break
            }
          }
        } else if (lightsSet && spots.exists(s => spotLights(i).equals(s(i)))) {
          // continue
        } else {
          spots.foreach(s => spotLights(i).set(s(i)))
          val idx = spotLightsLoc + i * spotLightsSize
          program.foreach { prog =>
            prog.setUniformf(
              idx + spotLightsColorOffset,
              spotLights(i).color.r * spotLights(i).intensity,
              spotLights(i).color.g * spotLights(i).intensity,
              spotLights(i).color.b * spotLights(i).intensity
            )
            prog.setUniformf(idx + spotLightsPositionOffset, spotLights(i).position)
            prog.setUniformf(idx + spotLightsDirectionOffset, spotLights(i).direction)
            prog.setUniformf(idx + spotLightsCutoffAngleOffset, spotLights(i).cutoffAngle)
            prog.setUniformf(idx + spotLightsExponentOffset, spotLights(i).exponent)
            if (spotLightsIntensityOffset >= 0)
              prog.setUniformf(idx + spotLightsIntensityOffset, spotLights(i).intensity)
          }
          if (spotLightsSize <= 0) {
            i = spotLights.length // break
          }
        }
        i += 1
      }
    }

    if (attributes.has(ColorAttribute.Fog)) {
      attributes.get(ColorAttribute.Fog).foreach { attr =>
        set(u_fogColor, attr.asInstanceOf[ColorAttribute].color)
      }
    }

    lights.foreach { env =>
      env.shadowMap.foreach { sm =>
        set(u_shadowMapProjViewTrans, sm.projViewTrans)
        set(u_shadowTexture, sm.depthMap)
        val depthMap = sm.depthMap.asInstanceOf[TextureDescriptor[GLTexture]]
        setFloat(u_shadowPCFOffset, 1.0f / (2f * depthMap.texture.map(_.width.toFloat).getOrElse(1f)))
      }
    }

    lightsSet = true
  }

  override def canRender(renderable: Renderable): Boolean =
    if (renderable.bones.isDefined) {
      if (renderable.bones.map(_.length).getOrElse(0) > config.numBones) false
      else if (renderable.meshPart.mesh.vertexAttributes.boneWeights > config.numBoneWeights)
        false
      else {
        if (renderable.meshPart.mesh.vertexAttributes.getTextureCoordinates() != textureCoordinates)
          false
        else {
          val renderableMask = DefaultShader.combineAttributeMasks(renderable)
          (attributesMask == (renderableMask | DefaultShader.optionalAttributes)) &&
          (vertexMask == renderable.meshPart.mesh.vertexAttributes.maskWithSizePacked) &&
          renderable.environment.isDefined == lighting
        }
      }
    } else {
      if (renderable.meshPart.mesh.vertexAttributes.getTextureCoordinates() != textureCoordinates)
        false
      else {
        val renderableMask = DefaultShader.combineAttributeMasks(renderable)
        (attributesMask == (renderableMask | DefaultShader.optionalAttributes)) &&
        (vertexMask == renderable.meshPart.mesh.vertexAttributes.maskWithSizePacked) &&
        renderable.environment.isDefined == lighting
      }
    }

  override def compareTo(other: Shader): Int =
    if (Nullable(other).isEmpty) -1
    else if (other eq this) 0
    else { // Compare shaders by attribute mask as a proxy for performance impact
      val otherShader = other.asInstanceOf[DefaultShader]
      if (attributesMask == otherShader.attributesMask) 0
      else if (attributesMask < otherShader.attributesMask) -1
      else 1
    }

  override def equals(obj: Any): Boolean = obj match {
    case ds: DefaultShader => ds eq this
    case _ => false
  }

  override def close(): Unit = {
    program.foreach(_.close())
    super.close()
  }

  def defaultCullFace: Int =
    if (config.defaultCullFace == -1) DefaultShader.defaultCullFace: @scala.annotation.nowarn("msg=deprecated")
    else config.defaultCullFace

  def defaultCullFace_=(cullFace: Int): Unit =
    config.defaultCullFace = cullFace

  def defaultDepthFunc: Int =
    if (config.defaultDepthFunc == -1) DefaultShader.defaultDepthFunc: @scala.annotation.nowarn("msg=deprecated")
    else config.defaultDepthFunc

  def defaultDepthFunc_=(depthFunc: Int): Unit =
    config.defaultDepthFunc = depthFunc
}

object DefaultShader {

  class Config {

    /** The uber vertex shader to use, null to use the default vertex shader. */
    var vertexShader: Nullable[String] = Nullable.empty

    /** The uber fragment shader to use, null to use the default fragment shader. */
    var fragmentShader: Nullable[String] = Nullable.empty

    /** The number of directional lights to use */
    var numDirectionalLights: Int = 2

    /** The number of point lights to use */
    var numPointLights: Int = 5

    /** The number of spot lights to use */
    var numSpotLights: Int = 0

    /** The number of bones to use */
    var numBones: Int = 12

    /** The number of bone weights to use (up to 8 with default vertex shader), default is 4. */
    var numBoneWeights: Int = 4

    /** */
    var ignoreUnimplemented: Boolean = true

    /** Set to 0 to disable culling, -1 to inherit from [[DefaultShader.defaultCullFace]] */
    var defaultCullFace: Int = -1

    /** Set to 0 to disable depth test, -1 to inherit from [[DefaultShader.defaultDepthFunc]] */
    var defaultDepthFunc: Int = -1

    def this(vertexShader: String, fragmentShader: String) = {
      this()
      this.vertexShader = Nullable(vertexShader)
      this.fragmentShader = Nullable(fragmentShader)
    }
  }

  object Inputs {
    val projTrans:       BaseShader.Uniform = BaseShader.Uniform("u_projTrans")
    val viewTrans:       BaseShader.Uniform = BaseShader.Uniform("u_viewTrans")
    val projViewTrans:   BaseShader.Uniform = BaseShader.Uniform("u_projViewTrans")
    val cameraPosition:  BaseShader.Uniform = BaseShader.Uniform("u_cameraPosition")
    val cameraDirection: BaseShader.Uniform = BaseShader.Uniform("u_cameraDirection")
    val cameraUp:        BaseShader.Uniform = BaseShader.Uniform("u_cameraUp")
    val cameraNearFar:   BaseShader.Uniform = BaseShader.Uniform("u_cameraNearFar")

    val worldTrans:         BaseShader.Uniform = BaseShader.Uniform("u_worldTrans")
    val viewWorldTrans:     BaseShader.Uniform = BaseShader.Uniform("u_viewWorldTrans")
    val projViewWorldTrans: BaseShader.Uniform = BaseShader.Uniform("u_projViewWorldTrans")
    val normalMatrix:       BaseShader.Uniform = BaseShader.Uniform("u_normalMatrix")
    val bones:              BaseShader.Uniform = BaseShader.Uniform("u_bones")

    val shininess: BaseShader.Uniform =
      BaseShader.Uniform("u_shininess", FloatAttribute.Shininess)
    val opacity:      BaseShader.Uniform = BaseShader.Uniform("u_opacity", BlendingAttribute.Type)
    val diffuseColor: BaseShader.Uniform =
      BaseShader.Uniform("u_diffuseColor", ColorAttribute.Diffuse)
    val diffuseTexture: BaseShader.Uniform =
      BaseShader.Uniform("u_diffuseTexture", TextureAttribute.Diffuse)
    val diffuseUVTransform: BaseShader.Uniform =
      BaseShader.Uniform("u_diffuseUVTransform", TextureAttribute.Diffuse)
    val specularColor: BaseShader.Uniform =
      BaseShader.Uniform("u_specularColor", ColorAttribute.Specular)
    val specularTexture: BaseShader.Uniform =
      BaseShader.Uniform("u_specularTexture", TextureAttribute.Specular)
    val specularUVTransform: BaseShader.Uniform =
      BaseShader.Uniform("u_specularUVTransform", TextureAttribute.Specular)
    val emissiveColor: BaseShader.Uniform =
      BaseShader.Uniform("u_emissiveColor", ColorAttribute.Emissive)
    val emissiveTexture: BaseShader.Uniform =
      BaseShader.Uniform("u_emissiveTexture", TextureAttribute.Emissive)
    val emissiveUVTransform: BaseShader.Uniform =
      BaseShader.Uniform("u_emissiveUVTransform", TextureAttribute.Emissive)
    val reflectionColor: BaseShader.Uniform =
      BaseShader.Uniform("u_reflectionColor", ColorAttribute.Reflection)
    val reflectionTexture: BaseShader.Uniform =
      BaseShader.Uniform("u_reflectionTexture", TextureAttribute.Reflection)
    val reflectionUVTransform: BaseShader.Uniform =
      BaseShader.Uniform("u_reflectionUVTransform", TextureAttribute.Reflection)
    val normalTexture: BaseShader.Uniform =
      BaseShader.Uniform("u_normalTexture", TextureAttribute.Normal)
    val normalUVTransform: BaseShader.Uniform =
      BaseShader.Uniform("u_normalUVTransform", TextureAttribute.Normal)
    val ambientTexture: BaseShader.Uniform =
      BaseShader.Uniform("u_ambientTexture", TextureAttribute.Ambient)
    val ambientUVTransform: BaseShader.Uniform =
      BaseShader.Uniform("u_ambientUVTransform", TextureAttribute.Ambient)
    val alphaTest: BaseShader.Uniform = BaseShader.Uniform("u_alphaTest")

    val ambientCube:        BaseShader.Uniform = BaseShader.Uniform("u_ambientCubemap")
    val dirLights:          BaseShader.Uniform = BaseShader.Uniform("u_dirLights")
    val pointLights:        BaseShader.Uniform = BaseShader.Uniform("u_pointLights")
    val spotLights:         BaseShader.Uniform = BaseShader.Uniform("u_spotLights")
    val environmentCubemap: BaseShader.Uniform = BaseShader.Uniform("u_environmentCubemap")
  }

  object Setters {
    val projTrans: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, cam.projection))
    }
    val viewTrans: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, cam.view))
    }
    val projViewTrans: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, cam.combined))
    }
    val cameraPosition: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach { cam =>
          shader.setFloat(
            inputID,
            cam.position.x,
            cam.position.y,
            cam.position.z,
            1.1881f / (cam.far * cam.far)
          )
        }
    }
    val cameraDirection: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, cam.direction))
    }
    val cameraUp: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, cam.up))
    }
    val cameraNearFar: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.setFloat(inputID, cam.near, cam.far))
    }
    val worldTrans: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.set(inputID, renderable.worldTransform)
    }
    val viewWorldTrans: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val temp: Matrix4 = Matrix4()
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, temp.set(cam.view).mul(renderable.worldTransform)))
    }
    val projViewWorldTrans: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val temp: Matrix4 = Matrix4()
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, temp.set(cam.combined).mul(renderable.worldTransform)))
    }
    val normalMatrix: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val tmpM: Matrix3 = Matrix3()
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.set(inputID, tmpM.set(renderable.worldTransform).inv().transpose())
    }

    class Bones(numBones: Int) extends BaseShader.LocalSetter() {
      val bones: Array[Float] = new Array[Float](numBones * 16)
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit = {
        var i = 0
        while (i < bones.length) {
          val idx = i / 16
          if (renderable.bones.isEmpty || renderable.bones.forall(b => idx >= b.length || Nullable(b(idx)).isEmpty))
            System.arraycopy(Bones.idtMatrix.values, 0, bones, i, 16)
          else
            renderable.bones.foreach { b =>
              System.arraycopy(b(idx).values, 0, bones, i, 16)
            }
          i += 16
        }
        shader.program.foreach(_.setUniformMatrix4fv(shader.loc(inputID), bones, 0, bones.length))
      }
    }
    object Bones {
      private val idtMatrix: Matrix4 = Matrix4()
    }

    val shininess: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(FloatAttribute.Shininess).foreach { attr =>
          shader.setFloat(inputID, attr.asInstanceOf[FloatAttribute].value)
        }
    }
    val diffuseColor: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(ColorAttribute.Diffuse).foreach { attr =>
          shader.set(inputID, attr.asInstanceOf[ColorAttribute].color)
        }
    }
    val diffuseTexture: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Diffuse).foreach { attr =>
          shader.context.foreach { ctx =>
            val unit = ctx.textureBinder.bind(attr.asInstanceOf[TextureAttribute].textureDescription)
            shader.setInt(inputID, unit)
          }
        }
    }
    val diffuseUVTransform: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Diffuse).foreach { attr =>
          val ta = attr.asInstanceOf[TextureAttribute]
          shader.setFloat(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV)
        }
    }
    val specularColor: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(ColorAttribute.Specular).foreach { attr =>
          shader.set(inputID, attr.asInstanceOf[ColorAttribute].color)
        }
    }
    val specularTexture: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Specular).foreach { attr =>
          shader.context.foreach { ctx =>
            val unit = ctx.textureBinder.bind(attr.asInstanceOf[TextureAttribute].textureDescription)
            shader.setInt(inputID, unit)
          }
        }
    }
    val specularUVTransform: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Specular).foreach { attr =>
          val ta = attr.asInstanceOf[TextureAttribute]
          shader.setFloat(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV)
        }
    }
    val emissiveColor: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(ColorAttribute.Emissive).foreach { attr =>
          shader.set(inputID, attr.asInstanceOf[ColorAttribute].color)
        }
    }
    val emissiveTexture: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Emissive).foreach { attr =>
          shader.context.foreach { ctx =>
            val unit = ctx.textureBinder.bind(attr.asInstanceOf[TextureAttribute].textureDescription)
            shader.setInt(inputID, unit)
          }
        }
    }
    val emissiveUVTransform: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Emissive).foreach { attr =>
          val ta = attr.asInstanceOf[TextureAttribute]
          shader.setFloat(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV)
        }
    }
    val reflectionColor: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(ColorAttribute.Reflection).foreach { attr =>
          shader.set(inputID, attr.asInstanceOf[ColorAttribute].color)
        }
    }
    val reflectionTexture: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Reflection).foreach { attr =>
          shader.context.foreach { ctx =>
            val unit = ctx.textureBinder.bind(attr.asInstanceOf[TextureAttribute].textureDescription)
            shader.setInt(inputID, unit)
          }
        }
    }
    val reflectionUVTransform: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Reflection).foreach { attr =>
          val ta = attr.asInstanceOf[TextureAttribute]
          shader.setFloat(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV)
        }
    }
    val normalTexture: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Normal).foreach { attr =>
          shader.context.foreach { ctx =>
            val unit = ctx.textureBinder.bind(attr.asInstanceOf[TextureAttribute].textureDescription)
            shader.setInt(inputID, unit)
          }
        }
    }
    val normalUVTransform: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Normal).foreach { attr =>
          val ta = attr.asInstanceOf[TextureAttribute]
          shader.setFloat(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV)
        }
    }
    val ambientTexture: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Ambient).foreach { attr =>
          shader.context.foreach { ctx =>
            val unit = ctx.textureBinder.bind(attr.asInstanceOf[TextureAttribute].textureDescription)
            shader.setInt(inputID, unit)
          }
        }
    }
    val ambientUVTransform: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        combinedAttributes.get(TextureAttribute.Ambient).foreach { attr =>
          val ta = attr.asInstanceOf[TextureAttribute]
          shader.setFloat(inputID, ta.offsetU, ta.offsetV, ta.scaleU, ta.scaleV)
        }
    }

    class ACubemap(val dirLightsOffset: Int, val pointLightsOffset: Int) extends BaseShader.LocalSetter() {
      private val cacheAmbientCubemap: AmbientCubemap = new AmbientCubemap()
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        if (renderable.environment.isEmpty)
          shader.program.foreach(
            _.setUniform3fv(shader.loc(inputID), ACubemap.ones, 0, ACubemap.ones.length)
          )
        else {
          renderable.worldTransform.translation(ACubemap.tmpV1)
          if (combinedAttributes.has(ColorAttribute.AmbientLight))
            combinedAttributes.get(ColorAttribute.AmbientLight).foreach { attr =>
              cacheAmbientCubemap.set(attr.asInstanceOf[ColorAttribute].color)
            }

          if (combinedAttributes.has(DirectionalLightsAttribute.Type)) {
            combinedAttributes.get(DirectionalLightsAttribute.Type).foreach { attr =>
              val lights = attr.asInstanceOf[DirectionalLightsAttribute].lights
              var i      = dirLightsOffset
              while (i < lights.size) {
                cacheAmbientCubemap.add(lights(i).color, lights(i).direction)
                i += 1
              }
            }
          }

          if (combinedAttributes.has(PointLightsAttribute.Type)) {
            combinedAttributes.get(PointLightsAttribute.Type).foreach { attr =>
              val lights = attr.asInstanceOf[PointLightsAttribute].lights
              var i      = pointLightsOffset
              while (i < lights.size) {
                cacheAmbientCubemap.add(
                  lights(i).color,
                  lights(i).position,
                  ACubemap.tmpV1,
                  lights(i).intensity
                )
                i += 1
              }
            }
          }

          cacheAmbientCubemap.clamp()
          shader.program.foreach(
            _.setUniform3fv(
              shader.loc(inputID),
              cacheAmbientCubemap.data,
              0,
              cacheAmbientCubemap.data.length
            )
          )
        }
    }
    object ACubemap {
      private val ones: Array[Float] =
        Array(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
      private val tmpV1: Vector3 = Vector3()
    }

    val environmentCubemap: BaseShader.Setter = new BaseShader.LocalSetter() {
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        if (combinedAttributes.has(CubemapAttribute.EnvironmentMap)) {
          combinedAttributes.get(CubemapAttribute.EnvironmentMap).foreach { attr =>
            shader.context.foreach { ctx =>
              shader.setInt(
                inputID,
                ctx.textureBinder.bind(attr.asInstanceOf[CubemapAttribute].textureDescription)
              )
            }
          }
        }
    }
  }

  // Embedded GLSL shaders — originally loaded from classpath .glsl files.
  // Embedded as string constants so they work on all platforms (including Scala.js/browser).

  // @formatter:off
  val defaultVertexShader: String =
    """#if defined(diffuseTextureFlag) || defined(specularTextureFlag) || defined(emissiveTextureFlag)
      |#define textureFlag
      |#endif
      |
      |#if defined(specularTextureFlag) || defined(specularColorFlag)
      |#define specularFlag
      |#endif
      |
      |#if defined(specularFlag) || defined(fogFlag)
      |#define cameraPositionFlag
      |#endif
      |
      |attribute vec3 a_position;
      |uniform mat4 u_projViewTrans;
      |
      |#if defined(colorFlag)
      |varying vec4 v_color;
      |attribute vec4 a_color;
      |#endif // colorFlag
      |
      |#ifdef normalFlag
      |attribute vec3 a_normal;
      |uniform mat3 u_normalMatrix;
      |varying vec3 v_normal;
      |#endif // normalFlag
      |
      |#ifdef textureFlag
      |attribute vec2 a_texCoord0;
      |#endif // textureFlag
      |
      |#ifdef diffuseTextureFlag
      |uniform vec4 u_diffuseUVTransform;
      |varying vec2 v_diffuseUV;
      |#endif
      |
      |#ifdef emissiveTextureFlag
      |uniform vec4 u_emissiveUVTransform;
      |varying vec2 v_emissiveUV;
      |#endif
      |
      |#ifdef specularTextureFlag
      |uniform vec4 u_specularUVTransform;
      |varying vec2 v_specularUV;
      |#endif
      |
      |#ifdef boneWeight0Flag
      |#define boneWeightsFlag
      |attribute vec2 a_boneWeight0;
      |#endif //boneWeight0Flag
      |
      |#ifdef boneWeight1Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight1;
      |#endif //boneWeight1Flag
      |
      |#ifdef boneWeight2Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight2;
      |#endif //boneWeight2Flag
      |
      |#ifdef boneWeight3Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight3;
      |#endif //boneWeight3Flag
      |
      |#ifdef boneWeight4Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight4;
      |#endif //boneWeight4Flag
      |
      |#ifdef boneWeight5Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight5;
      |#endif //boneWeight5Flag
      |
      |#ifdef boneWeight6Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight6;
      |#endif //boneWeight6Flag
      |
      |#ifdef boneWeight7Flag
      |#ifndef boneWeightsFlag
      |#define boneWeightsFlag
      |#endif
      |attribute vec2 a_boneWeight7;
      |#endif //boneWeight7Flag
      |
      |#if defined(numBones) && defined(boneWeightsFlag)
      |#if (numBones > 0)
      |#define skinningFlag
      |#endif
      |#endif
      |
      |uniform mat4 u_worldTrans;
      |
      |#if defined(numBones)
      |#if numBones > 0
      |uniform mat4 u_bones[numBones];
      |#endif //numBones
      |#endif
      |
      |#ifdef shininessFlag
      |uniform float u_shininess;
      |#else
      |const float u_shininess = 20.0;
      |#endif // shininessFlag
      |
      |#ifdef blendedFlag
      |uniform float u_opacity;
      |varying float v_opacity;
      |
      |#ifdef alphaTestFlag
      |uniform float u_alphaTest;
      |varying float v_alphaTest;
      |#endif //alphaTestFlag
      |#endif // blendedFlag
      |
      |#ifdef lightingFlag
      |varying vec3 v_lightDiffuse;
      |
      |#ifdef ambientLightFlag
      |uniform vec3 u_ambientLight;
      |#endif // ambientLightFlag
      |
      |#ifdef ambientCubemapFlag
      |uniform vec3 u_ambientCubemap[6];
      |#endif // ambientCubemapFlag
      |
      |#ifdef sphericalHarmonicsFlag
      |uniform vec3 u_sphericalHarmonics[9];
      |#endif //sphericalHarmonicsFlag
      |
      |#ifdef specularFlag
      |varying vec3 v_lightSpecular;
      |#endif // specularFlag
      |
      |#ifdef cameraPositionFlag
      |uniform vec4 u_cameraPosition;
      |#endif // cameraPositionFlag
      |
      |#ifdef fogFlag
      |varying float v_fog;
      |#endif // fogFlag
      |
      |
      |#if numDirectionalLights > 0
      |struct DirectionalLight
      |{
      |	vec3 color;
      |	vec3 direction;
      |};
      |uniform DirectionalLight u_dirLights[numDirectionalLights];
      |#endif // numDirectionalLights
      |
      |#if numPointLights > 0
      |struct PointLight
      |{
      |	vec3 color;
      |	vec3 position;
      |};
      |uniform PointLight u_pointLights[numPointLights];
      |#endif // numPointLights
      |
      |#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
      |#define ambientFlag
      |#endif //ambientFlag
      |
      |#ifdef shadowMapFlag
      |uniform mat4 u_shadowMapProjViewTrans;
      |varying vec3 v_shadowMapUv;
      |#define separateAmbientFlag
      |#endif //shadowMapFlag
      |
      |#if defined(ambientFlag) && defined(separateAmbientFlag)
      |varying vec3 v_ambientLight;
      |#endif //separateAmbientFlag
      |
      |#endif // lightingFlag
      |
      |void main() {
      |	#ifdef diffuseTextureFlag
      |		v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;
      |	#endif //diffuseTextureFlag
      |
      |	#ifdef emissiveTextureFlag
      |		v_emissiveUV = u_emissiveUVTransform.xy + a_texCoord0 * u_emissiveUVTransform.zw;
      |	#endif //emissiveTextureFlag
      |
      |	#ifdef specularTextureFlag
      |		v_specularUV = u_specularUVTransform.xy + a_texCoord0 * u_specularUVTransform.zw;
      |	#endif //specularTextureFlag
      |
      |	#if defined(colorFlag)
      |		v_color = a_color;
      |	#endif // colorFlag
      |
      |	#ifdef blendedFlag
      |		v_opacity = u_opacity;
      |		#ifdef alphaTestFlag
      |			v_alphaTest = u_alphaTest;
      |		#endif //alphaTestFlag
      |	#endif // blendedFlag
      |
      |	#ifdef skinningFlag
      |		mat4 skinning = mat4(0.0);
      |		#ifdef boneWeight0Flag
      |			skinning += (a_boneWeight0.y) * u_bones[int(a_boneWeight0.x)];
      |		#endif //boneWeight0Flag
      |		#ifdef boneWeight1Flag
      |			skinning += (a_boneWeight1.y) * u_bones[int(a_boneWeight1.x)];
      |		#endif //boneWeight1Flag
      |		#ifdef boneWeight2Flag
      |			skinning += (a_boneWeight2.y) * u_bones[int(a_boneWeight2.x)];
      |		#endif //boneWeight2Flag
      |		#ifdef boneWeight3Flag
      |			skinning += (a_boneWeight3.y) * u_bones[int(a_boneWeight3.x)];
      |		#endif //boneWeight3Flag
      |		#ifdef boneWeight4Flag
      |			skinning += (a_boneWeight4.y) * u_bones[int(a_boneWeight4.x)];
      |		#endif //boneWeight4Flag
      |		#ifdef boneWeight5Flag
      |			skinning += (a_boneWeight5.y) * u_bones[int(a_boneWeight5.x)];
      |		#endif //boneWeight5Flag
      |		#ifdef boneWeight6Flag
      |			skinning += (a_boneWeight6.y) * u_bones[int(a_boneWeight6.x)];
      |		#endif //boneWeight6Flag
      |		#ifdef boneWeight7Flag
      |			skinning += (a_boneWeight7.y) * u_bones[int(a_boneWeight7.x)];
      |		#endif //boneWeight7Flag
      |	#endif //skinningFlag
      |
      |	#ifdef skinningFlag
      |		vec4 pos = u_worldTrans * skinning * vec4(a_position, 1.0);
      |	#else
      |		vec4 pos = u_worldTrans * vec4(a_position, 1.0);
      |	#endif
      |
      |	gl_Position = u_projViewTrans * pos;
      |
      |	#ifdef shadowMapFlag
      |		vec4 spos = u_shadowMapProjViewTrans * pos;
      |		v_shadowMapUv.xyz = (spos.xyz / spos.w) * 0.5 + 0.5;
      |		v_shadowMapUv.z = min(v_shadowMapUv.z, 0.998);
      |	#endif //shadowMapFlag
      |
      |	#if defined(normalFlag)
      |		#if defined(skinningFlag)
      |			vec3 normal = normalize((u_worldTrans * skinning * vec4(a_normal, 0.0)).xyz);
      |		#else
      |			vec3 normal = normalize(u_normalMatrix * a_normal);
      |		#endif
      |		v_normal = normal;
      |	#endif // normalFlag
      |
      |    #ifdef fogFlag
      |        vec3 flen = u_cameraPosition.xyz - pos.xyz;
      |        float fog = dot(flen, flen) * u_cameraPosition.w;
      |        v_fog = min(fog, 1.0);
      |    #endif
      |
      |	#ifdef lightingFlag
      |		#if	defined(ambientLightFlag)
      |        	vec3 ambientLight = u_ambientLight;
      |		#elif defined(ambientFlag)
      |        	vec3 ambientLight = vec3(0.0);
      |		#endif
      |
      |		#ifdef ambientCubemapFlag
      |			vec3 squaredNormal = normal * normal;
      |			vec3 isPositive  = step(0.0, normal);
      |			ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
      |					squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
      |					squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
      |		#endif // ambientCubemapFlag
      |
      |		#ifdef sphericalHarmonicsFlag
      |			ambientLight += u_sphericalHarmonics[0];
      |			ambientLight += u_sphericalHarmonics[1] * normal.x;
      |			ambientLight += u_sphericalHarmonics[2] * normal.y;
      |			ambientLight += u_sphericalHarmonics[3] * normal.z;
      |			ambientLight += u_sphericalHarmonics[4] * (normal.x * normal.z);
      |			ambientLight += u_sphericalHarmonics[5] * (normal.z * normal.y);
      |			ambientLight += u_sphericalHarmonics[6] * (normal.y * normal.x);
      |			ambientLight += u_sphericalHarmonics[7] * (3.0 * normal.z * normal.z - 1.0);
      |			ambientLight += u_sphericalHarmonics[8] * (normal.x * normal.x - normal.y * normal.y);
      |		#endif // sphericalHarmonicsFlag
      |
      |		#ifdef ambientFlag
      |			#ifdef separateAmbientFlag
      |				v_ambientLight = ambientLight;
      |				v_lightDiffuse = vec3(0.0);
      |			#else
      |				v_lightDiffuse = ambientLight;
      |			#endif //separateAmbientFlag
      |		#else
      |	        v_lightDiffuse = vec3(0.0);
      |		#endif //ambientFlag
      |
      |
      |		#ifdef specularFlag
      |			v_lightSpecular = vec3(0.0);
      |			vec3 viewVec = normalize(u_cameraPosition.xyz - pos.xyz);
      |		#endif // specularFlag
      |
      |		#if (numDirectionalLights > 0) && defined(normalFlag)
      |			for (int i = 0; i < numDirectionalLights; i++) {
      |				vec3 lightDir = -u_dirLights[i].direction;
      |				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
      |				vec3 value = u_dirLights[i].color * NdotL;
      |				v_lightDiffuse += value;
      |				#ifdef specularFlag
      |					float halfDotView = max(0.0, dot(normal, normalize(lightDir + viewVec)));
      |					v_lightSpecular += value * pow(halfDotView, u_shininess);
      |				#endif // specularFlag
      |			}
      |		#endif // numDirectionalLights
      |
      |		#if (numPointLights > 0) && defined(normalFlag)
      |			for (int i = 0; i < numPointLights; i++) {
      |				vec3 lightDir = u_pointLights[i].position - pos.xyz;
      |				float dist2 = dot(lightDir, lightDir);
      |				lightDir *= inversesqrt(dist2);
      |				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
      |				vec3 value = u_pointLights[i].color * (NdotL / (1.0 + dist2));
      |				v_lightDiffuse += value;
      |				#ifdef specularFlag
      |					float halfDotView = max(0.0, dot(normal, normalize(lightDir + viewVec)));
      |					v_lightSpecular += value * pow(halfDotView, u_shininess);
      |				#endif // specularFlag
      |			}
      |		#endif // numPointLights
      |	#endif // lightingFlag
      |}
      |""".stripMargin

  val defaultFragmentShader: String =
    """#ifdef GL_ES
      |#define LOWP lowp
      |#define MED mediump
      |#define HIGH highp
      |precision mediump float;
      |#else
      |#define MED
      |#define LOWP
      |#define HIGH
      |#endif
      |
      |#if defined(specularTextureFlag) || defined(specularColorFlag)
      |#define specularFlag
      |#endif
      |
      |#ifdef normalFlag
      |varying vec3 v_normal;
      |#endif //normalFlag
      |
      |#if defined(colorFlag)
      |varying vec4 v_color;
      |#endif
      |
      |#ifdef blendedFlag
      |varying float v_opacity;
      |#ifdef alphaTestFlag
      |varying float v_alphaTest;
      |#endif //alphaTestFlag
      |#endif //blendedFlag
      |
      |#if defined(diffuseTextureFlag) || defined(specularTextureFlag) || defined(emissiveTextureFlag)
      |#define textureFlag
      |#endif
      |
      |#ifdef diffuseTextureFlag
      |varying MED vec2 v_diffuseUV;
      |#endif
      |
      |#ifdef specularTextureFlag
      |varying MED vec2 v_specularUV;
      |#endif
      |
      |#ifdef emissiveTextureFlag
      |varying MED vec2 v_emissiveUV;
      |#endif
      |
      |#ifdef diffuseColorFlag
      |uniform vec4 u_diffuseColor;
      |#endif
      |
      |#ifdef diffuseTextureFlag
      |uniform sampler2D u_diffuseTexture;
      |#endif
      |
      |#ifdef specularColorFlag
      |uniform vec4 u_specularColor;
      |#endif
      |
      |#ifdef specularTextureFlag
      |uniform sampler2D u_specularTexture;
      |#endif
      |
      |#ifdef normalTextureFlag
      |uniform sampler2D u_normalTexture;
      |#endif
      |
      |#ifdef emissiveColorFlag
      |uniform vec4 u_emissiveColor;
      |#endif
      |
      |#ifdef emissiveTextureFlag
      |uniform sampler2D u_emissiveTexture;
      |#endif
      |
      |#ifdef lightingFlag
      |varying vec3 v_lightDiffuse;
      |
      |#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
      |#define ambientFlag
      |#endif //ambientFlag
      |
      |#ifdef specularFlag
      |varying vec3 v_lightSpecular;
      |#endif //specularFlag
      |
      |#ifdef shadowMapFlag
      |uniform sampler2D u_shadowTexture;
      |uniform float u_shadowPCFOffset;
      |varying vec3 v_shadowMapUv;
      |#define separateAmbientFlag
      |
      |float getShadowness(vec2 offset)
      |{
      |    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0);
      |    return step(v_shadowMapUv.z, dot(texture2D(u_shadowTexture, v_shadowMapUv.xy + offset), bitShifts));//+(1.0/255.0));
      |}
      |
      |float getShadow()
      |{
      |	return (//getShadowness(vec2(0,0)) +
      |			getShadowness(vec2(u_shadowPCFOffset, u_shadowPCFOffset)) +
      |			getShadowness(vec2(-u_shadowPCFOffset, u_shadowPCFOffset)) +
      |			getShadowness(vec2(u_shadowPCFOffset, -u_shadowPCFOffset)) +
      |			getShadowness(vec2(-u_shadowPCFOffset, -u_shadowPCFOffset))) * 0.25;
      |}
      |#endif //shadowMapFlag
      |
      |#if defined(ambientFlag) && defined(separateAmbientFlag)
      |varying vec3 v_ambientLight;
      |#endif //separateAmbientFlag
      |
      |#endif //lightingFlag
      |
      |#ifdef fogFlag
      |uniform vec4 u_fogColor;
      |varying float v_fog;
      |#endif // fogFlag
      |
      |void main() {
      |	#if defined(normalFlag)
      |		vec3 normal = v_normal;
      |	#endif // normalFlag
      |
      |	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
      |		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor * v_color;
      |	#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
      |		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * u_diffuseColor;
      |	#elif defined(diffuseTextureFlag) && defined(colorFlag)
      |		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV) * v_color;
      |	#elif defined(diffuseTextureFlag)
      |		vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV);
      |	#elif defined(diffuseColorFlag) && defined(colorFlag)
      |		vec4 diffuse = u_diffuseColor * v_color;
      |	#elif defined(diffuseColorFlag)
      |		vec4 diffuse = u_diffuseColor;
      |	#elif defined(colorFlag)
      |		vec4 diffuse = v_color;
      |	#else
      |		vec4 diffuse = vec4(1.0);
      |	#endif
      |
      |	#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
      |		vec4 emissive = texture2D(u_emissiveTexture, v_emissiveUV) * u_emissiveColor;
      |	#elif defined(emissiveTextureFlag)
      |		vec4 emissive = texture2D(u_emissiveTexture, v_emissiveUV);
      |	#elif defined(emissiveColorFlag)
      |		vec4 emissive = u_emissiveColor;
      |	#else
      |		vec4 emissive = vec4(0.0);
      |	#endif
      |
      |	#if (!defined(lightingFlag))
      |		gl_FragColor.rgb = diffuse.rgb + emissive.rgb;
      |	#elif (!defined(specularFlag))
      |		#if defined(ambientFlag) && defined(separateAmbientFlag)
      |			#ifdef shadowMapFlag
      |				gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + getShadow() * v_lightDiffuse)) + emissive.rgb;
      |				//gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy);
      |			#else
      |				gl_FragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse)) + emissive.rgb;
      |			#endif //shadowMapFlag
      |		#else
      |			#ifdef shadowMapFlag
      |				gl_FragColor.rgb = getShadow() * (diffuse.rgb * v_lightDiffuse) + emissive.rgb;
      |			#else
      |				gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + emissive.rgb;
      |			#endif //shadowMapFlag
      |		#endif
      |	#else
      |		#if defined(specularTextureFlag) && defined(specularColorFlag)
      |			vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * u_specularColor.rgb * v_lightSpecular;
      |		#elif defined(specularTextureFlag)
      |			vec3 specular = texture2D(u_specularTexture, v_specularUV).rgb * v_lightSpecular;
      |		#elif defined(specularColorFlag)
      |			vec3 specular = u_specularColor.rgb * v_lightSpecular;
      |		#else
      |			vec3 specular = v_lightSpecular;
      |		#endif
      |
      |		#if defined(ambientFlag) && defined(separateAmbientFlag)
      |			#ifdef shadowMapFlag
      |			gl_FragColor.rgb = (diffuse.rgb * (getShadow() * v_lightDiffuse + v_ambientLight)) + specular + emissive.rgb;
      |				//gl_FragColor.rgb = texture2D(u_shadowTexture, v_shadowMapUv.xy);
      |			#else
      |				gl_FragColor.rgb = (diffuse.rgb * (v_lightDiffuse + v_ambientLight)) + specular + emissive.rgb;
      |			#endif //shadowMapFlag
      |		#else
      |			#ifdef shadowMapFlag
      |				gl_FragColor.rgb = getShadow() * ((diffuse.rgb * v_lightDiffuse) + specular) + emissive.rgb;
      |			#else
      |				gl_FragColor.rgb = (diffuse.rgb * v_lightDiffuse) + specular + emissive.rgb;
      |			#endif //shadowMapFlag
      |		#endif
      |	#endif //lightingFlag
      |
      |	#ifdef fogFlag
      |		gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, v_fog);
      |	#endif // end fogFlag
      |
      |	#ifdef blendedFlag
      |		gl_FragColor.a = diffuse.a * v_opacity;
      |		#ifdef alphaTestFlag
      |			if (gl_FragColor.a <= v_alphaTest)
      |				discard;
      |		#endif
      |	#else
      |		gl_FragColor.a = 1.0;
      |	#endif
      |
      |}
      |""".stripMargin
  // @formatter:on

  protected var implementedFlags: Long = BlendingAttribute.Type | TextureAttribute.Diffuse |
    ColorAttribute.Diffuse | ColorAttribute.Specular | FloatAttribute.Shininess

  /** @deprecated Replaced by [[Config.defaultCullFace]] Set to 0 to disable culling */
  @deprecated("Replaced by Config.defaultCullFace", "")
  var defaultCullFace: Int = GL20.GL_BACK

  /** @deprecated Replaced by [[Config.defaultDepthFunc]] Set to 0 to disable depth test */
  @deprecated("Replaced by Config.defaultDepthFunc", "")
  var defaultDepthFunc: Int = GL20.GL_LEQUAL

  /** Attributes which are not required but always supported. */
  private val optionalAttributes: Long = IntAttribute.CullFace | DepthTestAttribute.Type

  private def and(mask: Long, flag: Long): Boolean =
    (mask & flag) == flag

  private def or(mask: Long, flag: Long): Boolean =
    (mask & flag) != 0

  private val tmpAttributes: Attributes = Attributes()

  // TODO: Perhaps move responsibility for combining attributes to RenderableProvider?
  private[shaders] def combineAttributes(renderable: Renderable): Attributes = {
    tmpAttributes.clear()
    renderable.environment.foreach(tmpAttributes.set(_))
    renderable.material.foreach(tmpAttributes.set(_))
    tmpAttributes
  }

  private def combineAttributeMasks(renderable: Renderable): Long = {
    var mask = 0L
    renderable.environment.foreach(mask |= _.getMask)
    renderable.material.foreach(mask |= _.getMask)
    mask
  }

  def createPrefix(renderable: Renderable, config: Config): String = {
    val attributes     = combineAttributes(renderable)
    val sb             = new StringBuilder
    val attributesMask = attributes.getMask
    val vertexMask     = renderable.meshPart.mesh.vertexAttributes.mask
    if (and(vertexMask, VertexAttributes.Usage.Position)) sb.append("#define positionFlag\n")
    if (or(vertexMask, VertexAttributes.Usage.ColorUnpacked | VertexAttributes.Usage.ColorPacked))
      sb.append("#define colorFlag\n")
    if (and(vertexMask, VertexAttributes.Usage.BiNormal)) sb.append("#define binormalFlag\n")
    if (and(vertexMask, VertexAttributes.Usage.Tangent)) sb.append("#define tangentFlag\n")
    if (and(vertexMask, VertexAttributes.Usage.Normal)) sb.append("#define normalFlag\n")
    if (
      and(vertexMask, VertexAttributes.Usage.Normal) || and(
        vertexMask,
        VertexAttributes.Usage.Tangent | VertexAttributes.Usage.BiNormal
      )
    ) {
      if (renderable.environment.isDefined) {
        sb.append("#define lightingFlag\n")
        sb.append("#define ambientCubemapFlag\n")
        sb.append("#define numDirectionalLights ").append(config.numDirectionalLights).append("\n")
        sb.append("#define numPointLights ").append(config.numPointLights).append("\n")
        sb.append("#define numSpotLights ").append(config.numSpotLights).append("\n")
        if (attributes.has(ColorAttribute.Fog)) {
          sb.append("#define fogFlag\n")
        }
        if (renderable.environment.exists(_.shadowMap.isDefined))
          sb.append("#define shadowMapFlag\n")
        if (attributes.has(CubemapAttribute.EnvironmentMap))
          sb.append("#define environmentCubemapFlag\n")
      }
    }
    val n = renderable.meshPart.mesh.vertexAttributes.size
    var i = 0
    while (i < n) {
      val attr = renderable.meshPart.mesh.vertexAttributes.get(i)
      if (attr.usage == VertexAttributes.Usage.TextureCoordinates)
        sb.append("#define texCoord").append(attr.unit).append("Flag\n")
      i += 1
    }
    if (renderable.bones.isDefined) {
      var j = 0
      while (j < config.numBoneWeights) {
        sb.append("#define boneWeight").append(j).append("Flag\n")
        j += 1
      }
    }
    if ((attributesMask & BlendingAttribute.Type) == BlendingAttribute.Type)
      sb.append("#define ").append(BlendingAttribute.Alias).append("Flag\n")
    if ((attributesMask & TextureAttribute.Diffuse) == TextureAttribute.Diffuse) {
      sb.append("#define ").append(TextureAttribute.DiffuseAlias).append("Flag\n")
      sb.append("#define ").append(TextureAttribute.DiffuseAlias).append("Coord texCoord0\n") // FIXME implement UV mapping
    }
    if ((attributesMask & TextureAttribute.Specular) == TextureAttribute.Specular) {
      sb.append("#define ").append(TextureAttribute.SpecularAlias).append("Flag\n")
      sb.append("#define ").append(TextureAttribute.SpecularAlias).append("Coord texCoord0\n") // FIXME implement UV mapping
    }
    if ((attributesMask & TextureAttribute.Normal) == TextureAttribute.Normal) {
      sb.append("#define ").append(TextureAttribute.NormalAlias).append("Flag\n")
      sb.append("#define ").append(TextureAttribute.NormalAlias).append("Coord texCoord0\n") // FIXME implement UV mapping
    }
    if ((attributesMask & TextureAttribute.Emissive) == TextureAttribute.Emissive) {
      sb.append("#define ").append(TextureAttribute.EmissiveAlias).append("Flag\n")
      sb.append("#define ").append(TextureAttribute.EmissiveAlias).append("Coord texCoord0\n") // FIXME implement UV mapping
    }
    if ((attributesMask & TextureAttribute.Reflection) == TextureAttribute.Reflection) {
      sb.append("#define ").append(TextureAttribute.ReflectionAlias).append("Flag\n")
      sb.append("#define ").append(TextureAttribute.ReflectionAlias).append("Coord texCoord0\n") // FIXME implement UV mapping
    }
    if ((attributesMask & TextureAttribute.Ambient) == TextureAttribute.Ambient) {
      sb.append("#define ").append(TextureAttribute.AmbientAlias).append("Flag\n")
      sb.append("#define ").append(TextureAttribute.AmbientAlias).append("Coord texCoord0\n") // FIXME implement UV mapping
    }
    if ((attributesMask & ColorAttribute.Diffuse) == ColorAttribute.Diffuse)
      sb.append("#define ").append(ColorAttribute.DiffuseAlias).append("Flag\n")
    if ((attributesMask & ColorAttribute.Specular) == ColorAttribute.Specular)
      sb.append("#define ").append(ColorAttribute.SpecularAlias).append("Flag\n")
    if ((attributesMask & ColorAttribute.Emissive) == ColorAttribute.Emissive)
      sb.append("#define ").append(ColorAttribute.EmissiveAlias).append("Flag\n")
    if ((attributesMask & ColorAttribute.Reflection) == ColorAttribute.Reflection)
      sb.append("#define ").append(ColorAttribute.ReflectionAlias).append("Flag\n")
    if ((attributesMask & FloatAttribute.Shininess) == FloatAttribute.Shininess)
      sb.append("#define ").append(FloatAttribute.ShininessAlias).append("Flag\n")
    if ((attributesMask & FloatAttribute.AlphaTest) == FloatAttribute.AlphaTest)
      sb.append("#define ").append(FloatAttribute.AlphaTestAlias).append("Flag\n")
    if (renderable.bones.isDefined && config.numBones > 0)
      sb.append("#define numBones ").append(config.numBones).append("\n")
    sb.toString
  }
}
