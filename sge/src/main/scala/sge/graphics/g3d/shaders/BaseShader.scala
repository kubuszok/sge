/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/shaders/BaseShader.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Java interfaces Validator/Setter -> Scala traits (correct)
 * - Java IntIntMap -> ObjectMap[Int, Int] (functional equivalent)
 * - Java IntArray -> DynamicArray[Int] (functional equivalent)
 * - Java Array<String> -> DynamicArray[String] (functional equivalent)
 * - Java dispose() -> Scala close() (AutoCloseable convention)
 * - Nullable wrapping for program, context, camera, currentMesh (no null)
 * - begin() passes null.asInstanceOf[Renderable] to global setters (matches Java null semantics)
 * - set(float) renamed to setFloat, set(int) renamed to setInt (overload disambiguation)
 * - getInstancedAttributeLocations returns Nullable[Array[Int]] instead of nullable array
 * - combinedAttributes field is private val (matches Java private field)
 */
package sge
package graphics
package g3d
package shaders

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break
import sge.graphics.g3d.utils.{ RenderContext, TextureDescriptor }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Matrix3, Matrix4, Vector2, Vector3 }
import sge.utils.{ DynamicArray, Nullable, ObjectMap, SgeError }

/** @author
  *   Xoppa A BaseShader is a wrapper around a ShaderProgram that keeps track of the uniform and attribute locations. It does not manage the ShaderPogram, you are still responsible for disposing the
  *   ShaderProgram.
  */
abstract class BaseShader extends Shader {

  private val uniforms:   DynamicArray[String]                         = DynamicArray[String]()
  private val validators: DynamicArray[Nullable[BaseShader.Validator]] =
    DynamicArray[Nullable[BaseShader.Validator]]()
  private val setters:             DynamicArray[Nullable[BaseShader.Setter]] = DynamicArray[Nullable[BaseShader.Setter]]()
  private var locations:           Nullable[Array[Int]]                      = Nullable.empty
  private val globalUniforms:      DynamicArray[Int]                         = DynamicArray[Int]()
  private val localUniforms:       DynamicArray[Int]                         = DynamicArray[Int]()
  private val _attributes:         ObjectMap[Int, Int]                       = ObjectMap[Int, Int]()
  private val instancedAttributes: ObjectMap[Int, Int]                       = ObjectMap[Int, Int]()

  var program:             Nullable[ShaderProgram] = Nullable.empty
  var context:             Nullable[RenderContext] = Nullable.empty
  var camera:              Nullable[Camera]        = Nullable.empty
  private var currentMesh: Nullable[Mesh]          = Nullable.empty

  /** Register an uniform which might be used by this shader. Only possible prior to the call to init().
    * @return
    *   The ID of the uniform to use in this shader.
    */
  def register(alias: String, validator: Nullable[BaseShader.Validator], setter: Nullable[BaseShader.Setter]): Int = {
    if (locations.isDefined) throw SgeError.GraphicsError("Cannot register an uniform after initialization")
    val existing = getUniformID(alias)
    if (existing >= 0) {
      validators(existing) = validator
      setters(existing) = setter
      existing
    } else {
      uniforms.add(alias)
      validators.add(validator)
      setters.add(setter)
      uniforms.size - 1
    }
  }

  def register(alias: String, validator: BaseShader.Validator): Int =
    register(alias, Nullable(validator), Nullable.empty)

  def register(alias: String, setter: BaseShader.Setter): Int =
    register(alias, Nullable.empty, Nullable(setter))

  def register(alias: String): Int =
    register(alias, Nullable.empty, Nullable.empty)

  def register(uniform: BaseShader.Uniform, setter: Nullable[BaseShader.Setter]): Int =
    register(uniform.alias, Nullable(uniform), setter)

  def register(uniform: BaseShader.Uniform): Int =
    register(uniform, Nullable.empty)

  /** @return the ID of the input or negative if not available. */
  def getUniformID(alias: String): Int = boundary {
    val n = uniforms.size
    var i = 0
    while (i < n) {
      if (uniforms(i) == alias) break(i)
      i += 1
    }
    -1
  }

  /** @return The input at the specified id. */
  def getUniformAlias(id: Int): String =
    uniforms(id)

  /** Initialize this shader, causing all registered uniforms/attributes to be fetched. */
  def init(program: ShaderProgram, renderable: Renderable): Unit = {
    if (locations.isDefined) throw SgeError.GraphicsError("Already initialized")
    if (!program.compiled) throw SgeError.GraphicsError(program.getLog())
    this.program = Nullable(program)

    val n    = uniforms.size
    val locs = new Array[Int](n)
    var i    = 0
    while (i < n) {
      val input     = uniforms(i)
      val validator = validators(i)
      val setter    = setters(i)
      if (validator.isDefined && !validator.exists(_.validate(this, i, renderable))) {
        locs(i) = -1
      } else {
        locs(i) = program.fetchUniformLocation(input, false)
        if (locs(i) >= 0) {
          setter.foreach { s =>
            if (s.isGlobal(this, i))
              globalUniforms.add(i)
            else
              localUniforms.add(i)
          }
        }
      }
      if (locs(i) < 0) {
        validators(i) = Nullable.empty
        setters(i) = Nullable.empty
      }
      i += 1
    }
    locations = Nullable(locs)
    Nullable(renderable).foreach { r =>
      val attrs = r.meshPart.mesh.getVertexAttributes()
      val c     = attrs.size
      var j     = 0
      while (j < c) {
        val attr     = attrs.get(j)
        val location = program.getAttributeLocation(attr.alias)
        if (location >= 0) _attributes.put(attr.key, location)
        j += 1
      }
      val iattrs = r.meshPart.mesh.getInstancedAttributes()
      iattrs.foreach { ia =>
        val ic = ia.size
        var k  = 0
        while (k < ic) {
          val attr     = ia.get(k)
          val location = program.getAttributeLocation(attr.alias)
          if (location >= 0) instancedAttributes.put(attr.key, location)
          k += 1
        }
      }
    }
  }

  override def begin(camera: Camera, context: RenderContext): Unit = {
    this.camera = Nullable(camera)
    this.context = Nullable(context)
    program.foreach(_.bind())
    currentMesh = Nullable.empty
    var i = 0
    while (i < globalUniforms.size) {
      val u = globalUniforms(i)
      setters(u).foreach(_.set(this, u, null.asInstanceOf[Renderable], null.asInstanceOf[Attributes]))
      i += 1
    }
  }

  private val tempArray:  DynamicArray[Int] = DynamicArray[Int]()
  private val tempArray2: DynamicArray[Int] = DynamicArray[Int]()

  private def getAttributeLocations(attrs: VertexAttributes): Array[Int] = {
    tempArray.clear()
    val n = attrs.size
    var i = 0
    while (i < n) {
      tempArray.add(_attributes.get(attrs.get(i).key, -1))
      i += 1
    }
    tempArray.shrink()
    tempArray.items
  }

  private def getInstancedAttributeLocations(attrs: Nullable[VertexAttributes]): Nullable[Array[Int]] =
    // Instanced attributes may be null
    attrs.fold(Nullable.empty[Array[Int]]) { a =>
      tempArray2.clear()
      val n = a.size
      var i = 0
      while (i < n) {
        tempArray2.add(instancedAttributes.get(a.get(i).key, -1))
        i += 1
      }
      tempArray2.shrink()
      Nullable(tempArray2.items)
    }

  private val combinedAttributes: Attributes = Attributes()

  override def render(renderable: Renderable): Unit = boundary {
    if (renderable.worldTransform.det3x3() == 0) break(())
    combinedAttributes.clear()
    renderable.environment.foreach(combinedAttributes.set(_))
    renderable.material.foreach(combinedAttributes.set(_))
    render(renderable, combinedAttributes)
  }

  def render(renderable: Renderable, combinedAttributes: Attributes): Unit = {
    var i = 0
    while (i < localUniforms.size) {
      val u = localUniforms(i)
      setters(u).foreach(_.set(this, u, renderable, combinedAttributes))
      i += 1
    }
    val meshChanged = currentMesh.forall(m => !(m eq renderable.meshPart.mesh))
    if (meshChanged) {
      program.foreach { prog =>
        currentMesh.foreach(_.unbind(prog, tempArray.items, tempArray2.items))
        currentMesh = Nullable(renderable.meshPart.mesh)
        renderable.meshPart.mesh.bind(
          prog,
          getAttributeLocations(renderable.meshPart.mesh.getVertexAttributes()),
          getInstancedAttributeLocations(renderable.meshPart.mesh.getInstancedAttributes())
        )
      }
    }
    program.foreach(prog => renderable.meshPart.render(prog, false))
  }

  override def end(): Unit = {
    program.foreach { prog =>
      currentMesh.foreach(_.unbind(prog, tempArray.items, tempArray2.items))
    }
    currentMesh = Nullable.empty
  }

  override def close(): Unit = {
    program = Nullable.empty
    uniforms.clear()
    validators.clear()
    setters.clear()
    localUniforms.clear()
    globalUniforms.clear()
    locations = Nullable.empty
  }

  /** Whether this Shader instance implements the specified uniform, only valid after a call to init(). */
  final def has(inputID: Int): Boolean =
    locations.exists { locs =>
      inputID >= 0 && inputID < locs.length && locs(inputID) >= 0
    }

  final def loc(inputID: Int): Int =
    locations.fold(-1) { locs =>
      if (inputID >= 0 && inputID < locs.length) locs(inputID) else -1
    }

  final def set(uniform: Int, value: Matrix4): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformMatrix(locs(uniform), value)); true }
    }

  final def set(uniform: Int, value: Matrix3): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformMatrix(locs(uniform), value)); true }
    }

  final def set(uniform: Int, value: Vector3): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), value)); true }
    }

  final def set(uniform: Int, value: Vector2): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), value)); true }
    }

  final def set(uniform: Int, value: Color): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), value)); true }
    }

  final def setFloat(uniform: Int, value: Float): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), value)); true }
    }

  final def setFloat(uniform: Int, v1: Float, v2: Float): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), v1, v2)); true }
    }

  final def setFloat(uniform: Int, v1: Float, v2: Float, v3: Float): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), v1, v2, v3)); true }
    }

  final def setFloat(uniform: Int, v1: Float, v2: Float, v3: Float, v4: Float): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformf(locs(uniform), v1, v2, v3, v4)); true }
    }

  final def setInt(uniform: Int, value: Int): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformi(locs(uniform), value)); true }
    }

  final def setInt(uniform: Int, v1: Int, v2: Int): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformi(locs(uniform), v1, v2)); true }
    }

  final def setInt(uniform: Int, v1: Int, v2: Int, v3: Int): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformi(locs(uniform), v1, v2, v3)); true }
    }

  final def setInt(uniform: Int, v1: Int, v2: Int, v3: Int, v4: Int): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else { program.foreach(_.setUniformi(locs(uniform), v1, v2, v3, v4)); true }
    }

  final def set(uniform: Int, textureDesc: TextureDescriptor[?]): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else {
        context.foreach { ctx =>
          program.foreach(_.setUniformi(locs(uniform), ctx.textureBinder.bind(textureDesc)))
        }
        true
      }
    }

  final def set(uniform: Int, texture: GLTexture): Boolean =
    locations.fold(false) { locs =>
      if (locs(uniform) < 0) false
      else {
        context.foreach { ctx =>
          program.foreach(_.setUniformi(locs(uniform), ctx.textureBinder.bind(texture)))
        }
        true
      }
    }
}

object BaseShader {

  trait Validator {

    /** @return True if the input is valid for the renderable, false otherwise. */
    def validate(shader: BaseShader, inputID: Int, renderable: Renderable): Boolean
  }

  trait Setter {

    /** @return
      *   True if the uniform only has to be set once per render call, false if the uniform must be set for each renderable.
      */
    def isGlobal(shader: BaseShader, inputID: Int): Boolean

    def set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes): Unit
  }

  abstract class GlobalSetter extends Setter {
    override def isGlobal(shader: BaseShader, inputID: Int): Boolean = true
  }

  abstract class LocalSetter extends Setter {
    override def isGlobal(shader: BaseShader, inputID: Int): Boolean = false
  }

  class Uniform(
    val alias:           String,
    val materialMask:    Long = 0L,
    val environmentMask: Long = 0L,
    val overallMask:     Long = 0L
  ) extends Validator {

    def this(alias: String, materialMask: Long, environmentMask: Long) = {
      this(alias, materialMask, environmentMask, 0L)
    }

    def this(alias: String, overallMask: Long) = {
      this(alias, 0L, 0L, overallMask)
    }

    def this(alias: String) = {
      this(alias, 0L, 0L, 0L)
    }

    override def validate(shader: BaseShader, inputID: Int, renderable: Renderable): Boolean = {
      val r        = Nullable(renderable)
      val matFlags =
        r.flatMap(_.material).map(_.getMask).getOrElse(0L)
      val envFlags =
        r.flatMap(_.environment).map(_.getMask).getOrElse(0L)
      ((matFlags & materialMask) == materialMask) && ((envFlags & environmentMask) == environmentMask) &&
      (((matFlags | envFlags) & overallMask) == overallMask)
    }
  }
}
