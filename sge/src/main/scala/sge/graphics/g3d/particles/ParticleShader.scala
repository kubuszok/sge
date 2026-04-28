/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleShader.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods, enums, inner classes, and setters ported faithfully
 * - Fixes (2026-03-04): getDefaultCullFace/setDefaultCullFace → def defaultCullFace/defaultCullFace_=;
 *   getDefaultDepthFunc/setDefaultDepthFunc → def defaultDepthFunc/defaultDepthFunc_=;
 *   companion getDefaultVertexShader()/getDefaultFragmentShader() → def defaultVertexShader/defaultFragmentShader
 * - Java enums → Scala 3 enums (ParticleType, AlignMode)
 * - Config: null strings → Nullable[String]
 * - Gdx.graphics.getWidth() in screenWidth setter → camera.viewportWidth (approximation)
 * - Gdx.app.getType() → Sge().application.applicationType via (using Sge) context parameter
 * - Gdx.files.classpath → Sge().files.classpath for default shader loading
 * - Setters use GlobalSetter/LocalSetter abstract classes instead of anonymous Setter
 * - compareTo: Java returns -1 for null other; Scala omits null check
 * - equals(ParticleShader): Java has overloaded equals; Scala uses pattern match
 * - dispose() → close() (Disposable → AutoCloseable)
 * - context field: Nullable in BaseShader; Scala uses context.foreach{} patterns
 * - renderable.material: Nullable in Scala; uses fold/foreach instead of direct access
 * - register() calls wrap setter in Nullable() (BaseShader API difference)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 478
 * Covenant-baseline-methods: AlignMode,Config,Inputs,ParticleShader,ParticleType,Setters,TMP_VECTOR3,align,begin,bindMaterial,cameraInvDirection,cameraPosition,cameraRight,cameraUp,canRender,close,compareTo,config,createPrefix,currentMaterial,defaultCullFace,defaultCullFace_,defaultDepthFunc,defaultDepthFunc_,defaultFragmentShader,defaultVertexShader,end,equals,fragmentShader,ignoreUnimplemented,implementedFlags,init,materialMask,optionalAttributes,prefix,prog,regionSize,render,renderable,screenWidth,set,this,vertexMask,vertexShader,worldViewTrans
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/ParticleShader.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package particles

import sge.graphics.{ Camera, GL20 }
import sge.graphics.g3d.{ Attributes, Material, Renderable, Shader }
import sge.graphics.g3d.attributes.{ BlendingAttribute, DepthTestAttribute, IntAttribute, TextureAttribute }
import sge.graphics.g3d.shaders.{ BaseShader, DefaultShader }
import sge.graphics.g3d.utils.RenderContext
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Matrix4, Vector3 }
import sge.utils.{ Nullable, SgeError }

/** This is a custom shader to render the particles. Usually is not required, because the {@link DefaultShader} will be used instead. This shader will be used when dealing with billboards using GPU
  * mode or point sprites.
  * @author
  *   inferno
  */
class ParticleShader private (
  initRenderable: Renderable,
  val config:     ParticleShader.Config,
  shaderProgram:  ShaderProgram
)(using val sge: Sge)
    extends BaseShader {

  import ParticleShader.*

  this.program = Nullable(shaderProgram)

  /** The renderable used to create this shader, invalid after the call to init */
  private var renderable:   Nullable[Renderable] = Nullable(initRenderable)
  private val materialMask: Long                 = initRenderable.material.map(_.getMask).getOrElse(0L) | optionalAttributes
  private val vertexMask:   Long                 = initRenderable.meshPart.mesh.vertexAttributes.mask

  if (!config.ignoreUnimplemented && (implementedFlags & materialMask) != materialMask)
    throw SgeError.GraphicsError("Some attributes not implemented yet (" + materialMask + ")")

  // Global uniforms
  register(DefaultShader.Inputs.viewTrans, Nullable(DefaultShader.Setters.viewTrans))
  register(DefaultShader.Inputs.projViewTrans, Nullable(DefaultShader.Setters.projViewTrans))
  register(DefaultShader.Inputs.projTrans, Nullable(DefaultShader.Setters.projTrans))
  register(Inputs.screenWidth, Nullable(Setters.screenWidth))
  register(DefaultShader.Inputs.cameraUp, Nullable(Setters.cameraUp))
  register(Inputs.cameraRight, Nullable(Setters.cameraRight))
  register(Inputs.cameraInvDirection, Nullable(Setters.cameraInvDirection))
  register(DefaultShader.Inputs.cameraPosition, Nullable(Setters.cameraPosition))

  // Object uniforms
  register(DefaultShader.Inputs.diffuseTexture, Nullable(DefaultShader.Setters.diffuseTexture))

  def this(renderable: Renderable, config: ParticleShader.Config, prefix: String, vertexShader: String, fragmentShader: String)(using Sge) =
    this(renderable, config, ShaderProgram(prefix + vertexShader, prefix + fragmentShader))

  def this(renderable: Renderable, config: ParticleShader.Config, prefix: String)(using Sge) =
    this(
      renderable,
      config,
      prefix,
      config.vertexShader.getOrElse(ParticleShader.defaultVertexShader),
      config.fragmentShader.getOrElse(ParticleShader.defaultFragmentShader)
    )

  def this(renderable: Renderable, config: ParticleShader.Config)(using Sge) =
    this(renderable, config, ParticleShader.createPrefix(renderable, config))

  def this(renderable: Renderable)(using Sge) =
    this(renderable, ParticleShader.Config())

  override def init(): Unit = {
    val prog = this.program
    this.program = Nullable.empty
    prog.foreach { p =>
      renderable.foreach { r =>
        init(p, r)
      }
    }
    renderable = Nullable.empty
  }

  override def canRender(renderable: Renderable): Boolean =
    (materialMask == (renderable.material.map(_.getMask).getOrElse(0L) | optionalAttributes)) &&
      (vertexMask == renderable.meshPart.mesh.vertexAttributes.mask)

  override def compareTo(other: Shader): Int =
    if (other eq this) 0
    else 0 // FIXME compare shaders on their impact on performance

  override def equals(obj: Any): Boolean = obj match {
    case ps: ParticleShader => ps eq this
    case _ => false
  }

  override def begin(camera: Camera, context: RenderContext): Unit =
    super.begin(camera, context)

  override def render(renderable: Renderable): Unit = {
    if (!renderable.material.exists(_.has(BlendingAttribute.Type)))
      context.foreach(_.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA))
    bindMaterial(renderable)
    super.render(renderable)
  }

  override def end(): Unit = {
    currentMaterial = Nullable.empty
    super.end()
  }

  var currentMaterial: Nullable[Material] = Nullable.empty

  protected def bindMaterial(renderable: Renderable): Unit =
    if (currentMaterial.exists(cm => renderable.material.exists(rm => cm eq rm))) {
      // same material, nothing to do
    } else {
      val cullFace       = if (config.defaultCullFace == -1) GL20.GL_BACK else config.defaultCullFace
      var depthFunc      = if (config.defaultDepthFunc == -1) GL20.GL_LEQUAL else config.defaultDepthFunc
      var depthRangeNear = 0f
      var depthRangeFar  = 1f
      var depthMask      = true

      currentMaterial = renderable.material
      currentMaterial.foreach { mat =>
        for (attr <- mat) {
          val t = attr.`type`
          if (BlendingAttribute.is(t)) {
            val ba = attr.asInstanceOf[BlendingAttribute]
            context.foreach(_.setBlending(true, ba.sourceFunction, ba.destFunction))
          } else if ((t & DepthTestAttribute.Type) == DepthTestAttribute.Type) {
            val dta = attr.asInstanceOf[DepthTestAttribute]
            depthFunc = dta.depthFunc
            depthRangeNear = dta.depthRangeNear
            depthRangeFar = dta.depthRangeFar
            depthMask = dta.depthMask
          } else if (!config.ignoreUnimplemented) {
            throw SgeError.GraphicsError("Unknown material attribute: " + attr.toString)
          }
        }
      }

      context.foreach { ctx =>
        ctx.setCullFace(cullFace)
        ctx.setDepthTest(depthFunc, depthRangeNear, depthRangeFar)
        ctx.setDepthMask(depthMask)
      }
    }

  override def close(): Unit = {
    program.foreach(_.close())
    super.close()
  }

  def defaultCullFace: Int =
    if (config.defaultCullFace == -1) GL20.GL_BACK else config.defaultCullFace

  def defaultCullFace_=(cullFace: Int): Unit =
    config.defaultCullFace = cullFace

  def defaultDepthFunc: Int =
    if (config.defaultDepthFunc == -1) GL20.GL_LEQUAL else config.defaultDepthFunc

  def defaultDepthFunc_=(depthFunc: Int): Unit =
    config.defaultDepthFunc = depthFunc
}

object ParticleShader {

  enum ParticleType {
    case Billboard, Point
  }

  enum AlignMode {
    case Screen, ViewPoint // , ParticleDirection
  }

  class Config {

    /** The uber vertex shader to use, null to use the default vertex shader. */
    var vertexShader: Nullable[String] = Nullable.empty

    /** The uber fragment shader to use, null to use the default fragment shader. */
    var fragmentShader:      Nullable[String] = Nullable.empty
    var ignoreUnimplemented: Boolean          = true

    /** Set to 0 to disable culling */
    var defaultCullFace: Int = -1

    /** Set to 0 to disable depth test */
    var defaultDepthFunc: Int          = -1
    var align:            AlignMode    = AlignMode.Screen
    var `type`:           ParticleType = ParticleType.Billboard

    def this(align: AlignMode, `type`: ParticleType) = {
      this()
      this.align = align
      this.`type` = `type`
    }

    def this(align: AlignMode) = {
      this()
      this.align = align
    }

    def this(`type`: ParticleType) = {
      this()
      this.`type` = `type`
    }

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
      |
      |#ifdef billboard
      |//Billboard particles
      |//In
      |attribute vec3 a_position;
      |attribute vec2 a_texCoord0;
      |attribute vec4 a_sizeAndRotation;
      |attribute vec4 a_color;
      |
      |//out
      |varying MED vec2 v_texCoords0;
      |varying vec4 v_color;
      |
      |//Camera
      |uniform mat4 u_projViewTrans;
      |
      |//Billboard to screen
      |#ifdef screenFacing
      |uniform vec3 u_cameraInvDirection;
      |uniform vec3 u_cameraRight;
      |uniform vec3 u_cameraUp;
      |#endif
      |#ifdef viewPointFacing
      |uniform vec3 u_cameraPosition;
      |uniform vec3 u_cameraUp;
      |#endif
      |#ifdef paticleDirectionFacing
      |uniform vec3 u_cameraPosition;
      |attribute vec3 a_direction;
      |#endif
      |
      |void main() {
      |
      |#ifdef screenFacing
      |	vec3 right = u_cameraRight;
      |	vec3 up = u_cameraUp;
      |	vec3 look = u_cameraInvDirection;
      |#endif
      |#ifdef viewPointFacing
      |	vec3 look = normalize(u_cameraPosition - a_position);
      |	vec3 right = normalize(cross(u_cameraUp, look));
      |	vec3 up = normalize(cross(look, right));
      |#endif
      |#ifdef paticleDirectionFacing
      |	vec3 up = a_direction;
      |	vec3 look = normalize(u_cameraPosition - a_position);
      |	vec3 right = normalize(cross(up, look));
      |	look = normalize(cross(right, up));
      |#endif
      |
      |	//Rotate around look
      |	vec3 axis = look;
      |	float c = a_sizeAndRotation.z;
      |    float s = a_sizeAndRotation.w;
      |    float oc = 1.0 - c;
      |
      |    mat3 rot = mat3(oc * axis.x * axis.x + c, oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,
      |                oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,
      |                oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c);
      |	vec3 offset = rot*(right*a_sizeAndRotation.x + up*a_sizeAndRotation.y );
      |
      |	gl_Position = u_projViewTrans * vec4(a_position + offset, 1.0);
      |	v_texCoords0 = a_texCoord0;
      |	v_color = a_color;
      |}
      |#else
      |//Point particles
      |attribute vec3 a_position;
      |attribute vec3 a_sizeAndRotation;
      |attribute vec4 a_color;
      |attribute vec4 a_region;
      |
      |//out
      |varying vec4 v_color;
      |varying vec4 v_rotation;
      |varying MED vec4 v_region;
      |varying vec2 v_uvRegionCenter;
      |
      |//Camera
      |uniform mat4 u_projTrans;
      |//should be modelView but particles are already in world coordinates
      |uniform mat4 u_viewTrans;
      |uniform float u_screenWidth;
      |uniform vec2 u_regionSize;
      |
      |void main(){
      |
      |	float halfSize = 0.5*a_sizeAndRotation.x;
      |	vec4 eyePos = u_viewTrans * vec4(a_position, 1);
      |	vec4 projCorner = u_projTrans * vec4(halfSize, halfSize, eyePos.z, eyePos.w);
      |	gl_PointSize = u_screenWidth * projCorner.x / projCorner.w;
      |	gl_Position = u_projTrans * eyePos;
      |	v_rotation = vec4(a_sizeAndRotation.y, a_sizeAndRotation.z, -a_sizeAndRotation.z, a_sizeAndRotation.y);
      |	v_color = a_color;
      |	v_region.xy = a_region.xy;
      |	v_region.zw = a_region.zw -a_region.xy;
      |	v_uvRegionCenter = a_region.xy +v_region.zw*0.5;
      |}
      |
      |#endif
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
      |
      |#ifdef billboard
      |//Billboard particles
      |varying vec4 v_color;
      |varying MED vec2 v_texCoords0;
      |uniform sampler2D u_diffuseTexture;
      |
      |void main() {
      |	gl_FragColor = texture2D(u_diffuseTexture, v_texCoords0) * v_color;
      |}
      |#else
      |
      |//Point particles
      |varying vec4 v_color;
      |varying vec4 v_rotation;
      |varying MED vec4 v_region;
      |varying vec2 v_uvRegionCenter;
      |
      |uniform sampler2D u_diffuseTexture;
      |uniform vec2 u_regionSize;
      |
      |void main() {
      |	vec2 uv = v_region.xy + gl_PointCoord*v_region.zw - v_uvRegionCenter;
      |	vec2 texCoord = mat2(v_rotation.x, v_rotation.y, v_rotation.z, v_rotation.w) * uv  +v_uvRegionCenter;
      |	gl_FragColor = texture2D(u_diffuseTexture, texCoord)* v_color;
      |}
      |
      |#endif
      |""".stripMargin
  // @formatter:on

  protected val implementedFlags: Long = BlendingAttribute.Type | TextureAttribute.Diffuse

  private val TMP_VECTOR3: Vector3 = Vector3()

  object Inputs {
    val cameraRight:        BaseShader.Uniform = BaseShader.Uniform("u_cameraRight")
    val cameraInvDirection: BaseShader.Uniform = BaseShader.Uniform("u_cameraInvDirection")
    val screenWidth:        BaseShader.Uniform = BaseShader.Uniform("u_screenWidth")
    val regionSize:         BaseShader.Uniform = BaseShader.Uniform("u_regionSize")
  }

  object Setters {
    val cameraRight: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
        shader.camera.foreach { cam =>
          shader.set(inputID, TMP_VECTOR3.set(cam.direction).crs(cam.up).nor())
        }
    }

    val cameraUp: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
        shader.camera.foreach { cam =>
          shader.set(inputID, TMP_VECTOR3.set(cam.up).nor())
        }
    }

    val cameraInvDirection: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
        shader.camera.foreach { cam =>
          shader.set(inputID, TMP_VECTOR3.set(-cam.direction.x, -cam.direction.y, -cam.direction.z).nor())
        }
    }

    val cameraPosition: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
        shader.camera.foreach { cam =>
          shader.set(inputID, cam.position)
        }
    }

    val screenWidth: BaseShader.Setter = new BaseShader.GlobalSetter() {
      override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit =
        // Use camera viewport width as approximation for screen width
        shader.camera.foreach { cam =>
          shader.setFloat(inputID, cam.viewportWidth.toFloat)
        }
    }

    val worldViewTrans: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val temp:                                                                                           Matrix4 = Matrix4()
      override def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit    =
        shader.camera.foreach { cam =>
          shader.set(inputID, temp.set(cam.view).mul(renderable.worldTransform))
        }
    }
  }

  /** Material attributes which are not required but always supported. */
  private val optionalAttributes: Long = IntAttribute.CullFace | DepthTestAttribute.Type

  def createPrefix(renderable: Renderable, config: Config)(using Sge): String = {
    var prefix = ""
    if (Sge().application.applicationType == Application.ApplicationType.Desktop)
      prefix += "#version 120\n"
    else
      prefix += "#version 100\n"
    if (config.`type` == ParticleType.Billboard) {
      prefix += "#define billboard\n"
      if (config.align == AlignMode.Screen)
        prefix += "#define screenFacing\n"
      else if (config.align == AlignMode.ViewPoint) prefix += "#define viewPointFacing\n"
      // else if(config.align == AlignMode.ParticleDirection)
      // prefix += "#define paticleDirectionFacing\n";
    }
    prefix
  }
}
