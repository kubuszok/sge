/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/shaders/DefaultShader.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package shaders

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

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
)(using sge: Sge)
    extends BaseShader {

  private val _combinedAttributes: Attributes = DefaultShader.combineAttributes(renderable)

  this.program = Nullable(shaderProgram)

  protected val lighting:           Boolean = renderable.environment.isDefined
  protected val environmentCubemap: Boolean = _combinedAttributes.has(CubemapAttribute.EnvironmentMap) ||
    (lighting && _combinedAttributes.has(CubemapAttribute.EnvironmentMap))
  protected val shadowMap: Boolean = lighting && renderable.environment.fold(false)(_.shadowMap.isDefined)

  /** The attributes that this shader supports */
  val attributesMask:     Long = _combinedAttributes.getMask | DefaultShader.optionalAttributes
  private val vertexMask: Long =
    renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked()
  private val textureCoordinates: Int =
    renderable.meshPart.mesh.getVertexAttributes().getTextureCoordinates()
  private var boneWeightsLocations: Nullable[Array[Int]] = Nullable.empty

  protected val directionalLights: Array[DirectionalLight] = {
    val n   = if (lighting && config.numDirectionalLights > 0) config.numDirectionalLights else 0
    val arr = new Array[DirectionalLight](n)
    var i   = 0
    while (i < arr.length) {
      arr(i) = new DirectionalLight()
      i += 1
    }
    arr
  }

  protected val pointLights: Array[PointLight] = {
    val n   = if (lighting && config.numPointLights > 0) config.numPointLights else 0
    val arr = new Array[PointLight](n)
    var i   = 0
    while (i < arr.length) {
      arr(i) = new PointLight()
      i += 1
    }
    arr
  }

  protected val spotLights: Array[SpotLight] = {
    val n   = if (lighting && config.numSpotLights > 0) config.numSpotLights else 0
    val arr = new Array[SpotLight](n)
    var i   = 0
    while (i < arr.length) {
      arr(i) = new SpotLight()
      i += 1
    }
    arr
  }

  protected val ambientCubemap: AmbientCubemap = new AmbientCubemap()

  /** The renderable used to create this shader, invalid after the call to init */
  private var _renderable: Nullable[Renderable] = Nullable(renderable)

  if (!config.ignoreUnimplemented && (DefaultShader.implementedFlags & attributesMask) != attributesMask)
    throw SgeError.GraphicsError("Some attributes not implemented yet (" + attributesMask + ")")

  if (renderable.bones.isDefined && renderable.bones.fold(0)(_.length) > config.numBones) {
    throw SgeError.GraphicsError(
      "too many bones: " + renderable.bones.fold(0)(_.length) + ", max configured: " + config.numBones
    )
  }

  locally {
    val boneWeights = renderable.meshPart.mesh.getVertexAttributes().getBoneWeights()
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
  val u_time: Int = register(new BaseShader.Uniform("u_time"))
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
  protected val u_dirLights0color:      Int = register(new BaseShader.Uniform("u_dirLights[0].color"))
  protected val u_dirLights0direction:  Int = register(new BaseShader.Uniform("u_dirLights[0].direction"))
  protected val u_dirLights1color:      Int = register(new BaseShader.Uniform("u_dirLights[1].color"))
  protected val u_pointLights0color:    Int = register(new BaseShader.Uniform("u_pointLights[0].color"))
  protected val u_pointLights0position: Int =
    register(new BaseShader.Uniform("u_pointLights[0].position"))
  protected val u_pointLights0intensity: Int =
    register(new BaseShader.Uniform("u_pointLights[0].intensity"))
  protected val u_pointLights1color:   Int = register(new BaseShader.Uniform("u_pointLights[1].color"))
  protected val u_spotLights0color:    Int = register(new BaseShader.Uniform("u_spotLights[0].color"))
  protected val u_spotLights0position: Int =
    register(new BaseShader.Uniform("u_spotLights[0].position"))
  protected val u_spotLights0intensity: Int =
    register(new BaseShader.Uniform("u_spotLights[0].intensity"))
  protected val u_spotLights0direction: Int =
    register(new BaseShader.Uniform("u_spotLights[0].direction"))
  protected val u_spotLights0cutoffAngle: Int =
    register(new BaseShader.Uniform("u_spotLights[0].cutoffAngle"))
  protected val u_spotLights0exponent: Int =
    register(new BaseShader.Uniform("u_spotLights[0].exponent"))
  protected val u_spotLights1color:       Int = register(new BaseShader.Uniform("u_spotLights[1].color"))
  protected val u_fogColor:               Int = register(new BaseShader.Uniform("u_fogColor"))
  protected val u_shadowMapProjViewTrans: Int = register(new BaseShader.Uniform("u_shadowMapProjViewTrans"))
  protected val u_shadowTexture:          Int = register(new BaseShader.Uniform("u_shadowTexture"))
  protected val u_shadowPCFOffset:        Int = register(new BaseShader.Uniform("u_shadowPCFOffset"))
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

  def this(renderable: Renderable)(using sge: Sge) =
    this(
      renderable,
      new DefaultShader.Config(), {
        val cfg    = new DefaultShader.Config()
        val prefix = DefaultShader.createPrefix(renderable, cfg)
        val vs     = cfg.vertexShader.getOrElse(DefaultShader.getDefaultVertexShader())
        val fs     = cfg.fragmentShader.getOrElse(DefaultShader.getDefaultFragmentShader())
        new ShaderProgram(prefix + vs, prefix + fs)
      }
    )

  def this(renderable: Renderable, config: DefaultShader.Config)(using sge: Sge) =
    this(
      renderable,
      config, {
        val prefix = DefaultShader.createPrefix(renderable, config)
        val vs     = config.vertexShader.getOrElse(DefaultShader.getDefaultVertexShader())
        val fs     = config.fragmentShader.getOrElse(DefaultShader.getDefaultFragmentShader())
        new ShaderProgram(prefix + vs, prefix + fs)
      }
    )

  def this(renderable: Renderable, config: DefaultShader.Config, prefix: String)(using sge: Sge) =
    this(
      renderable,
      config, {
        val vs = config.vertexShader.getOrElse(DefaultShader.getDefaultVertexShader())
        val fs = config.fragmentShader.getOrElse(DefaultShader.getDefaultFragmentShader())
        new ShaderProgram(prefix + vs, prefix + fs)
      }
    )

  def this(
    renderable:     Renderable,
    config:         DefaultShader.Config,
    prefix:         String,
    vertexShader:   String,
    fragmentShader: String
  )(using sge: Sge) =
    this(renderable, config, new ShaderProgram(prefix + vertexShader, prefix + fragmentShader))

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

  private val _normalMatrix: Matrix3 = new Matrix3()
  private var time:          Float   = 0f
  private var lightsSet:     Boolean = false

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
      time += sge.graphics.getDeltaTime()
      setFloat(u_time, time)
    }

    // set generic vertex attribute value for all bone weights in case a mesh has missing attributes.
    boneWeightsLocations.foreach { locs =>
      for (location <- locs)
        if (location >= 0) {
          sge.graphics.gl.glVertexAttrib2f(location, 0, 0)
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

  private val tmpV1: Vector3 = new Vector3()

  protected def bindLights(renderable: Renderable, attributes: Attributes): Unit = {
    val lights: Nullable[Environment]                = renderable.environment
    val dla:    Nullable[DirectionalLightsAttribute] =
      attributes.get(classOf[DirectionalLightsAttribute], DirectionalLightsAttribute.Type)
    val dirs: Nullable[DynamicArray[DirectionalLight]] = dla.map(_.lights)
    val pla:  Nullable[PointLightsAttribute]           =
      attributes.get(classOf[PointLightsAttribute], PointLightsAttribute.Type)
    val points: Nullable[DynamicArray[PointLight]] = pla.map(_.lights)
    val sla:    Nullable[SpotLightsAttribute]      =
      attributes.get(classOf[SpotLightsAttribute], SpotLightsAttribute.Type)
    val spots: Nullable[DynamicArray[SpotLight]] = sla.map(_.lights)

    if (dirLightsLoc >= 0) {
      var i = 0
      while (i < directionalLights.length) {
        if (dirs.isEmpty || dirs.fold(true)(d => i >= d.size)) {
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
        } else if (lightsSet && dirs.fold(false)(d => directionalLights(i).equals(d(i)))) {
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
        if (points.isEmpty || points.fold(true)(p => i >= p.size)) {
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
        } else if (lightsSet && points.fold(false)(p => pointLights(i).equals(p(i)))) {
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
        if (spots.isEmpty || spots.fold(true)(s => i >= s.size)) {
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
        } else if (lightsSet && spots.fold(false)(s => spotLights(i).equals(s(i)))) {
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
        set(u_shadowMapProjViewTrans, sm.getProjViewTrans())
        set(u_shadowTexture, sm.getDepthMap())
        val depthMap = sm.getDepthMap().asInstanceOf[TextureDescriptor[GLTexture]]
        setFloat(u_shadowPCFOffset, 1.0f / (2f * depthMap.texture.fold(1)(_.getWidth)))
      }
    }

    lightsSet = true
  }

  override def canRender(renderable: Renderable): Boolean =
    if (renderable.bones.isDefined) {
      if (renderable.bones.fold(0)(_.length) > config.numBones) false
      else if (renderable.meshPart.mesh.getVertexAttributes().getBoneWeights() > config.numBoneWeights)
        false
      else {
        if (renderable.meshPart.mesh.getVertexAttributes().getTextureCoordinates() != textureCoordinates)
          false
        else {
          val renderableMask = DefaultShader.combineAttributeMasks(renderable)
          (attributesMask == (renderableMask | DefaultShader.optionalAttributes)) &&
          (vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked()) &&
          renderable.environment.isDefined == lighting
        }
      }
    } else {
      if (renderable.meshPart.mesh.getVertexAttributes().getTextureCoordinates() != textureCoordinates)
        false
      else {
        val renderableMask = DefaultShader.combineAttributeMasks(renderable)
        (attributesMask == (renderableMask | DefaultShader.optionalAttributes)) &&
        (vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked()) &&
        renderable.environment.isDefined == lighting
      }
    }

  override def compareTo(other: Shader): Int =
    if (Nullable(other).isEmpty) -1
    else if (other eq this) 0
    else 0 // FIXME compare shaders on their impact on performance

  override def equals(obj: Any): Boolean = obj match {
    case ds: DefaultShader => ds eq this
    case _ => false
  }

  override def close(): Unit = {
    program.foreach(_.close())
    super.close()
  }

  def getDefaultCullFace(): Int =
    if (config.defaultCullFace == -1) DefaultShader.defaultCullFace: @scala.annotation.nowarn("msg=deprecated")
    else config.defaultCullFace

  def setDefaultCullFace(cullFace: Int): Unit =
    config.defaultCullFace = cullFace

  def getDefaultDepthFunc(): Int =
    if (config.defaultDepthFunc == -1) DefaultShader.defaultDepthFunc: @scala.annotation.nowarn("msg=deprecated")
    else config.defaultDepthFunc

  def setDefaultDepthFunc(depthFunc: Int): Unit =
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
    val projTrans:       BaseShader.Uniform = new BaseShader.Uniform("u_projTrans")
    val viewTrans:       BaseShader.Uniform = new BaseShader.Uniform("u_viewTrans")
    val projViewTrans:   BaseShader.Uniform = new BaseShader.Uniform("u_projViewTrans")
    val cameraPosition:  BaseShader.Uniform = new BaseShader.Uniform("u_cameraPosition")
    val cameraDirection: BaseShader.Uniform = new BaseShader.Uniform("u_cameraDirection")
    val cameraUp:        BaseShader.Uniform = new BaseShader.Uniform("u_cameraUp")
    val cameraNearFar:   BaseShader.Uniform = new BaseShader.Uniform("u_cameraNearFar")

    val worldTrans:         BaseShader.Uniform = new BaseShader.Uniform("u_worldTrans")
    val viewWorldTrans:     BaseShader.Uniform = new BaseShader.Uniform("u_viewWorldTrans")
    val projViewWorldTrans: BaseShader.Uniform = new BaseShader.Uniform("u_projViewWorldTrans")
    val normalMatrix:       BaseShader.Uniform = new BaseShader.Uniform("u_normalMatrix")
    val bones:              BaseShader.Uniform = new BaseShader.Uniform("u_bones")

    val shininess: BaseShader.Uniform =
      new BaseShader.Uniform("u_shininess", FloatAttribute.Shininess)
    val opacity:      BaseShader.Uniform = new BaseShader.Uniform("u_opacity", BlendingAttribute.Type)
    val diffuseColor: BaseShader.Uniform =
      new BaseShader.Uniform("u_diffuseColor", ColorAttribute.Diffuse)
    val diffuseTexture: BaseShader.Uniform =
      new BaseShader.Uniform("u_diffuseTexture", TextureAttribute.Diffuse)
    val diffuseUVTransform: BaseShader.Uniform =
      new BaseShader.Uniform("u_diffuseUVTransform", TextureAttribute.Diffuse)
    val specularColor: BaseShader.Uniform =
      new BaseShader.Uniform("u_specularColor", ColorAttribute.Specular)
    val specularTexture: BaseShader.Uniform =
      new BaseShader.Uniform("u_specularTexture", TextureAttribute.Specular)
    val specularUVTransform: BaseShader.Uniform =
      new BaseShader.Uniform("u_specularUVTransform", TextureAttribute.Specular)
    val emissiveColor: BaseShader.Uniform =
      new BaseShader.Uniform("u_emissiveColor", ColorAttribute.Emissive)
    val emissiveTexture: BaseShader.Uniform =
      new BaseShader.Uniform("u_emissiveTexture", TextureAttribute.Emissive)
    val emissiveUVTransform: BaseShader.Uniform =
      new BaseShader.Uniform("u_emissiveUVTransform", TextureAttribute.Emissive)
    val reflectionColor: BaseShader.Uniform =
      new BaseShader.Uniform("u_reflectionColor", ColorAttribute.Reflection)
    val reflectionTexture: BaseShader.Uniform =
      new BaseShader.Uniform("u_reflectionTexture", TextureAttribute.Reflection)
    val reflectionUVTransform: BaseShader.Uniform =
      new BaseShader.Uniform("u_reflectionUVTransform", TextureAttribute.Reflection)
    val normalTexture: BaseShader.Uniform =
      new BaseShader.Uniform("u_normalTexture", TextureAttribute.Normal)
    val normalUVTransform: BaseShader.Uniform =
      new BaseShader.Uniform("u_normalUVTransform", TextureAttribute.Normal)
    val ambientTexture: BaseShader.Uniform =
      new BaseShader.Uniform("u_ambientTexture", TextureAttribute.Ambient)
    val ambientUVTransform: BaseShader.Uniform =
      new BaseShader.Uniform("u_ambientUVTransform", TextureAttribute.Ambient)
    val alphaTest: BaseShader.Uniform = new BaseShader.Uniform("u_alphaTest")

    val ambientCube:        BaseShader.Uniform = new BaseShader.Uniform("u_ambientCubemap")
    val dirLights:          BaseShader.Uniform = new BaseShader.Uniform("u_dirLights")
    val pointLights:        BaseShader.Uniform = new BaseShader.Uniform("u_pointLights")
    val spotLights:         BaseShader.Uniform = new BaseShader.Uniform("u_spotLights")
    val environmentCubemap: BaseShader.Uniform = new BaseShader.Uniform("u_environmentCubemap")
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
      private val temp: Matrix4 = new Matrix4()
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, temp.set(cam.view).mul(renderable.worldTransform)))
    }
    val projViewWorldTrans: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val temp: Matrix4 = new Matrix4()
      override def set(
        shader:             BaseShader,
        inputID:            Int,
        renderable:         Renderable,
        combinedAttributes: Attributes
      ): Unit =
        shader.camera.foreach(cam => shader.set(inputID, temp.set(cam.combined).mul(renderable.worldTransform)))
    }
    val normalMatrix: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val tmpM: Matrix3 = new Matrix3()
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
          if (renderable.bones.isEmpty || renderable.bones.fold(true)(b => idx >= b.length || Nullable(b(idx)).isEmpty))
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
      private val idtMatrix: Matrix4 = new Matrix4()
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
          renderable.worldTransform.getTranslation(ACubemap.tmpV1)
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
      private val tmpV1: Vector3 = new Vector3()
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

  private var _defaultVertexShader: Nullable[String] = Nullable.empty

  def getDefaultVertexShader()(using sge: Sge): String = {
    if (_defaultVertexShader.isEmpty)
      _defaultVertexShader = Nullable(
        sge.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl").readString()
      )
    _defaultVertexShader.getOrElse("")
  }

  private var _defaultFragmentShader: Nullable[String] = Nullable.empty

  def getDefaultFragmentShader()(using sge: Sge): String = {
    if (_defaultFragmentShader.isEmpty)
      _defaultFragmentShader = Nullable(
        sge.files.classpath("com/badlogic/gdx/graphics/g3d/shaders/default.fragment.glsl").readString()
      )
    _defaultFragmentShader.getOrElse("")
  }

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

  private val tmpAttributes: Attributes = new Attributes()

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
    val vertexMask     = renderable.meshPart.mesh.getVertexAttributes().getMask()
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
        if (renderable.environment.fold(false)(_.shadowMap.isDefined))
          sb.append("#define shadowMapFlag\n")
        if (attributes.has(CubemapAttribute.EnvironmentMap))
          sb.append("#define environmentCubemapFlag\n")
      }
    }
    val n = renderable.meshPart.mesh.getVertexAttributes().size
    var i = 0
    while (i < n) {
      val attr = renderable.meshPart.mesh.getVertexAttributes().get(i)
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
