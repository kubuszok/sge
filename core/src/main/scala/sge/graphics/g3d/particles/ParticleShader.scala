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
 * - Java enums → Scala 3 enums (ParticleType, AlignMode)
 * - Config: null strings → Nullable[String]
 * - Gdx.graphics.getWidth() in screenWidth setter → camera.viewportWidth (approximation)
 * - Gdx.app.getType() → Sge().application.getType() via (using Sge) context parameter
 * - Gdx.files.classpath → Sge().files.classpath for default shader loading
 * - Setters use GlobalSetter/LocalSetter abstract classes instead of anonymous Setter
 * - compareTo: Java returns -1 for null other; Scala omits null check
 * - equals(ParticleShader): Java has overloaded equals; Scala uses pattern match
 * - dispose() → close() (Disposable → AutoCloseable)
 * - context field: Nullable in BaseShader; Scala uses context.foreach{} patterns
 * - renderable.material: Nullable in Scala; uses fold/foreach instead of direct access
 * - register() calls wrap setter in Nullable() (BaseShader API difference)
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
  private val materialMask: Long                 = initRenderable.material.fold(0L)(_.getMask) | optionalAttributes
  private val vertexMask:   Long                 = initRenderable.meshPart.mesh.getVertexAttributes().getMask()

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

  def this(renderable: Renderable, config: ParticleShader.Config, prefix: String, vertexShader: String, fragmentShader: String)(using Sge) = {
    this(renderable, config, new ShaderProgram(prefix + vertexShader, prefix + fragmentShader))
  }

  def this(renderable: Renderable, config: ParticleShader.Config, prefix: String)(using Sge) = {
    this(
      renderable,
      config,
      prefix,
      config.vertexShader.getOrElse(ParticleShader.getDefaultVertexShader()),
      config.fragmentShader.getOrElse(ParticleShader.getDefaultFragmentShader())
    )
  }

  def this(renderable: Renderable, config: ParticleShader.Config)(using Sge) = {
    this(renderable, config, ParticleShader.createPrefix(renderable, config))
  }

  def this(renderable: Renderable)(using Sge) = {
    this(renderable, new ParticleShader.Config())
  }

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
    (materialMask == (renderable.material.fold(0L)(_.getMask) | optionalAttributes)) &&
      (vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMask())

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
    if (!renderable.material.fold(false)(_.has(BlendingAttribute.Type)))
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
    if (currentMaterial.fold(false)(cm => renderable.material.fold(false)(rm => cm eq rm))) {
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

  def getDefaultCullFace(): Int =
    if (config.defaultCullFace == -1) GL20.GL_BACK else config.defaultCullFace

  def setDefaultCullFace(cullFace: Int): Unit =
    config.defaultCullFace = cullFace

  def getDefaultDepthFunc(): Int =
    if (config.defaultDepthFunc == -1) GL20.GL_LEQUAL else config.defaultDepthFunc

  def setDefaultDepthFunc(depthFunc: Int): Unit =
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

  private var defaultVertexShader: Nullable[String] = Nullable.empty

  def getDefaultVertexShader()(using Sge): String =
    defaultVertexShader.getOrElse {
      val s = Sge().files.classpath("com/badlogic/gdx/graphics/g3d/particles/particles.vertex.glsl").readString()
      defaultVertexShader = Nullable(s)
      s
    }

  private var defaultFragmentShader: Nullable[String] = Nullable.empty

  def getDefaultFragmentShader()(using Sge): String =
    defaultFragmentShader.getOrElse {
      val s = Sge().files.classpath("com/badlogic/gdx/graphics/g3d/particles/particles.fragment.glsl").readString()
      defaultFragmentShader = Nullable(s)
      s
    }

  protected val implementedFlags: Long = BlendingAttribute.Type | TextureAttribute.Diffuse

  private val TMP_VECTOR3: Vector3 = new Vector3()

  object Inputs {
    val cameraRight:        BaseShader.Uniform = new BaseShader.Uniform("u_cameraRight")
    val cameraInvDirection: BaseShader.Uniform = new BaseShader.Uniform("u_cameraInvDirection")
    val screenWidth:        BaseShader.Uniform = new BaseShader.Uniform("u_screenWidth")
    val regionSize:         BaseShader.Uniform = new BaseShader.Uniform("u_regionSize")
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
          shader.setFloat(inputID, cam.viewportWidth)
        }
    }

    val worldViewTrans: BaseShader.Setter = new BaseShader.LocalSetter() {
      private val temp:                                                                                           Matrix4 = new Matrix4()
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
    if (Sge().application.getType() == Application.ApplicationType.Desktop)
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
