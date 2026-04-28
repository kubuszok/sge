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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 355
 * Covenant-baseline-methods: Config,DepthShader,_depthNumBones,alphaTestAttribute,attributes,begin,boneWeights,canRender,combineAttributes,createPrefix,defaultAlphaTest,defaultFragmentShader,defaultVertexShader,depthBufferOnly,end,isBlendedTextureRenderable,isBlendedTextureShader,numBones,prefix,render,this,tmpAttributes
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/shaders/DepthShader.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: e08a2cfaf5996f9666fdbd360f90c2e78da08951
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

    val boneWeights = renderable.meshPart.mesh.vertexAttributes.boneWeights
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

  def this(renderable: Renderable)(using Sge) =
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

  def this(renderable: Renderable, config: DepthShader.Config)(using Sge) =
    this(
      renderable,
      config, {
        val prefix = DepthShader.createPrefix(renderable, config)
        val vs     = config.vertexShader.getOrElse(DepthShader.defaultVertexShader)
        val fs     = config.fragmentShader.getOrElse(DepthShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )

  def this(renderable: Renderable, config: DepthShader.Config, prefix: String)(using Sge) =
    this(
      renderable,
      config, {
        val vs = config.vertexShader.getOrElse(DepthShader.defaultVertexShader)
        val fs = config.fragmentShader.getOrElse(DepthShader.defaultFragmentShader)
        ShaderProgram(prefix + vs, prefix + fs)
      }
    )

  def this(
    renderable:     Renderable,
    config:         DepthShader.Config,
    prefix:         String,
    vertexShader:   String,
    fragmentShader: String
  )(using Sge) =
    this(renderable, config, ShaderProgram(prefix + vertexShader, prefix + fragmentShader))

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
      if (renderable.meshPart.mesh.vertexAttributes.boneWeights > config.numBoneWeights) break(false)
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

  // Embedded GLSL shaders — originally loaded from classpath .glsl files.
  // Embedded as string constants so they work on all platforms (including Scala.js/browser).

  // @formatter:off
  val defaultVertexShader: String =
    """attribute vec3 a_position;
      |uniform mat4 u_projViewWorldTrans;
      |
      |#if defined(diffuseTextureFlag) && defined(blendedFlag)
      |#define blendedTextureFlag
      |attribute vec2 a_texCoord0;
      |varying vec2 v_texCoords0;
      |#endif
      |
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
      |#if defined(numBones)
      |#if numBones > 0
      |uniform mat4 u_bones[numBones];
      |#endif //numBones
      |#endif
      |
      |#ifdef PackedDepthFlag
      |varying float v_depth;
      |#endif //PackedDepthFlag
      |
      |void main() {
      |	#ifdef blendedTextureFlag
      |		v_texCoords0 = a_texCoord0;
      |	#endif // blendedTextureFlag
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
      |		vec4 pos = u_projViewWorldTrans * skinning * vec4(a_position, 1.0);
      |	#else
      |		vec4 pos = u_projViewWorldTrans * vec4(a_position, 1.0);
      |	#endif
      |
      |	#ifdef PackedDepthFlag
      |		v_depth = pos.z / pos.w * 0.5 + 0.5;
      |	#endif //PackedDepthFlag
      |
      |	gl_Position = pos;
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
      |#if defined(diffuseTextureFlag) && defined(blendedFlag)
      |#define blendedTextureFlag
      |varying MED vec2 v_texCoords0;
      |uniform sampler2D u_diffuseTexture;
      |uniform float u_alphaTest;
      |#endif
      |
      |#ifdef PackedDepthFlag
      |varying HIGH float v_depth;
      |#endif //PackedDepthFlag
      |
      |void main() {
      |	#ifdef blendedTextureFlag
      |		if (texture2D(u_diffuseTexture, v_texCoords0).a < u_alphaTest)
      |			discard;
      |	#endif // blendedTextureFlag
      |
      |	#ifdef PackedDepthFlag
      |		HIGH float depth = v_depth;
      |		const HIGH vec4 bias = vec4(1.0 / 255.0, 1.0 / 255.0, 1.0 / 255.0, 0.0);
      |		HIGH vec4 color = vec4(depth, fract(depth * 255.0), fract(depth * 65025.0), fract(depth * 16581375.0));
      |		gl_FragColor = color - (color.yzww * bias);
      |	#endif //PackedDepthFlag
      |}
      |""".stripMargin
  // @formatter:on

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
