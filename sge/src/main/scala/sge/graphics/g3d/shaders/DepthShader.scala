/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/shaders/DepthShader.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Config.vertexShader/fragmentShader use Nullable[String] (no null)
 * - defaultVertexShader/defaultFragmentShader require (using Sge) context parameter
 * - Fixes (2026-03-04): getDefaultVertexShader()/getDefaultFragmentShader() → def defaultVertexShader/defaultFragmentShader
 * - Constructor ordering differs from Java: primary ctor is (Renderable, Config, ShaderProgram),
 *   secondary constructors delegate upward (Scala 3 constraint)
 * - Java Gdx.files -> Sge().files
 * - combineAttributes is private to companion, not private static
 * - Minor: DepthShader.Config secondary constructor doesn't set defaultCullFace = GL_FRONT
 *   (handled by Config primary constructor init block)
 * - All public methods match Java: begin, end, canRender, render, createPrefix
 * - TODO comment preserved from Java source
 * Convention: typed GL enums (EnableCap for glEnable/glDisable)
 */
package sge
package graphics
package g3d
package shaders

import scala.util.boundary
import scala.util.boundary.break
import sge.graphics.GL20
import sge.graphics.g3d.attributes.{ BlendingAttribute, FloatAttribute, TextureAttribute }
import sge.graphics.g3d.utils.RenderContext
import sge.graphics.glutils.ShaderProgram
import sge.utils.{ Nullable, SgeError }

class DepthShader(
  renderable:    Renderable,
  config:        DepthShader.Config,
  shaderProgram: ShaderProgram
)(using Sge)
    extends DefaultShader(renderable, config, shaderProgram) {

  private val _depthNumBones: Int = {
    DepthShader.combineAttributes(renderable)

    if (renderable.bones.isDefined && renderable.bones.map(_.length).getOrElse(0) > config.numBones) {
      throw SgeError.GraphicsError(
        "too many bones: " + renderable.bones.map(_.length).getOrElse(0) + ", max configured: " + config.numBones
      )
    }

    val boneWeights = renderable.meshPart.mesh.getVertexAttributes().getBoneWeights()
    if (boneWeights > config.numBoneWeights) {
      throw SgeError.GraphicsError(
        "too many bone weights: " + boneWeights + ", max configured: " + config.numBoneWeights
      )
    }

    renderable.bones.fold(0)(_ => config.numBones)
  }

  val numBones: Int = _depthNumBones

  private val alphaTestAttribute: FloatAttribute =
    FloatAttribute(FloatAttribute.AlphaTest, config.defaultAlphaTest)

  def this(renderable: Renderable)(using Sge) = {
    this(
      renderable,
      DepthShader.Config(), {
        val cfg    = DepthShader.Config()
        val prefix = DepthShader.createPrefix(renderable, cfg)
        val vs     = DepthShader.defaultVertexShader
        val fs     = DepthShader.defaultFragmentShader
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )
  }

  def this(renderable: Renderable, config: DepthShader.Config)(using Sge) = {
    this(
      renderable,
      config, {
        val prefix = DepthShader.createPrefix(renderable, config)
        val vs     = config.vertexShader.getOrElse(DepthShader.defaultVertexShader)
        val fs     = config.fragmentShader.getOrElse(DepthShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )
  }

  def this(renderable: Renderable, config: DepthShader.Config, prefix: String)(using Sge) = {
    this(
      renderable,
      config, {
        val vs = config.vertexShader.getOrElse(DepthShader.defaultVertexShader)
        val fs = config.fragmentShader.getOrElse(DepthShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )
  }

  def this(
    renderable:     Renderable,
    config:         DepthShader.Config,
    prefix:         String,
    vertexShader:   String,
    fragmentShader: String
  )(using Sge) = {
    this(renderable, config, ShaderProgram(prefix + vertexShader, prefix + fragmentShader))
  }

  override def begin(camera: Camera, context: RenderContext): Unit = {
    super.begin(camera, context)
    Sge().graphics.gl20.glEnable(EnableCap.PolygonOffsetFill)
    Sge().graphics.gl20.glPolygonOffset(2f, 100f)
  }

  override def end(): Unit = {
    super.end()
    Sge().graphics.gl20.glDisable(EnableCap.PolygonOffsetFill)
  }

  override def canRender(renderable: Renderable): Boolean = boundary {
    if (renderable.bones.isDefined) {
      if (renderable.bones.map(_.length).getOrElse(0) > config.numBones) break(false)
      if (renderable.meshPart.mesh.getVertexAttributes().getBoneWeights() > config.numBoneWeights) break(false)
    }
    val attributes = DepthShader.combineAttributes(renderable)

    val isBlendedTextureShader = (attributesMask & BlendingAttribute.Type) == BlendingAttribute.Type &&
      (attributesMask & TextureAttribute.Diffuse) == TextureAttribute.Diffuse

    val isBlendedTextureRenderable =
      attributes.has(BlendingAttribute.Type) && attributes.has(TextureAttribute.Diffuse)

    if (isBlendedTextureShader != isBlendedTextureRenderable) break(false)

    renderable.bones.isDefined == (numBones > 0)
  }

  override def render(renderable: Renderable, combinedAttributes: Attributes): Unit =
    if (combinedAttributes.has(BlendingAttribute.Type)) {
      val blending = combinedAttributes.get(BlendingAttribute.Type).map(_.asInstanceOf[BlendingAttribute]).getOrElse(throw SgeError.GraphicsError("Expected BlendingAttribute when has() is true"))
      combinedAttributes.remove(BlendingAttribute.Type)
      val hasAlphaTest = combinedAttributes.has(FloatAttribute.AlphaTest)
      if (!hasAlphaTest) combinedAttributes.set(alphaTestAttribute)
      if (blending.opacity >= combinedAttributes.get(FloatAttribute.AlphaTest).map(_.asInstanceOf[FloatAttribute].value).getOrElse(0f)) {
        super.render(renderable, combinedAttributes)
      }
      if (!hasAlphaTest) combinedAttributes.remove(FloatAttribute.AlphaTest)
      combinedAttributes.set(blending)
    } else {
      super.render(renderable, combinedAttributes)
    }
}

object DepthShader {

  class Config extends DefaultShader.Config {
    var depthBufferOnly:  Boolean = false
    var defaultAlphaTest: Float   = 0.5f

    defaultCullFace = GL20.GL_FRONT

    def this(vertexShader: String, fragmentShader: String) = {
      this()
      this.vertexShader = Nullable(vertexShader)
      this.fragmentShader = Nullable(fragmentShader)
    }
  }

  private var _defaultVertexShader: Nullable[String] = Nullable.empty

  def defaultVertexShader(using Sge): String = {
    if (_defaultVertexShader.isEmpty) {
      _defaultVertexShader = Nullable(Sge().files.classpath("com/badlogic/gdx/graphics/g3d/shaders/depth.vertex.glsl").readString())
    }
    _defaultVertexShader.getOrElse("")
  }

  private var _defaultFragmentShader: Nullable[String] = Nullable.empty

  def defaultFragmentShader(using Sge): String = {
    if (_defaultFragmentShader.isEmpty) {
      _defaultFragmentShader = Nullable(Sge().files.classpath("com/badlogic/gdx/graphics/g3d/shaders/depth.fragment.glsl").readString())
    }
    _defaultFragmentShader.getOrElse("")
  }

  def createPrefix(renderable: Renderable, config: Config): String = {
    var prefix = DefaultShader.createPrefix(renderable, config)
    if (!config.depthBufferOnly) prefix += "#define PackedDepthFlag\n"
    prefix
  }

  private val tmpAttributes: Attributes = Attributes()

  // TODO: Move responsibility for combining attributes to RenderableProvider
  private def combineAttributes(renderable: Renderable): Attributes = {
    tmpAttributes.clear()
    renderable.environment.foreach(tmpAttributes.set(_))
    renderable.material.foreach(tmpAttributes.set(_))
    tmpAttributes
  }
}
