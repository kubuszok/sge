/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/ShaderProgram.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses (using Sge) context parameter; ObjectMap for uniform/attribute maps; begin()/end() removed (Java begin() just calls bind())
 *   Idiom: split packages
 *   Issues: missing setUniform1iv/2iv/3iv/4iv (integer array uniform setters, 8 methods); missing begin()/end() convenience aliases used by some client code
 *   TODO: Java-style boolean getter -- isCompiled → def compiled
 *   TODO: typed GL enums -- ShaderType for glCreateShader -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.utils.BufferUtils
import sge.graphics.{ Color, GL20 }
import sge.math.{ Matrix3, Matrix4, Vector2, Vector3, Vector4 }
import sge.{ Application, Sge }
import sge.files.FileHandle
import sge.utils.DynamicArray
import scala.collection.mutable.Map as MutableMap
import java.nio.{ Buffer, ByteBuffer, ByteOrder, FloatBuffer }
import scala.compiletime.uninitialized

/** <p> A shader program encapsulates a vertex and fragment shader pair linked to form a shader program. </p>
  *
  * <p> After construction a ShaderProgram can be used to draw {@link Mesh} . To make the GPU use a specific ShaderProgram the programs {@link ShaderProgram#bind()} method must be used which
  * effectively binds the program. </p>
  *
  * <p> When a ShaderProgram is bound one can set uniforms, vertex attributes and attributes as needed via the respective methods. </p>
  *
  * <p> A ShaderProgram must be disposed via a call to {@link ShaderProgram#close()} when it is no longer needed </p>
  *
  * <p> ShaderPrograms are managed. In case the OpenGL context is lost all shaders get invalidated and have to be reloaded. This happens on Android when a user switches to another application or
  * receives an incoming call. Managed ShaderPrograms are automatically reloaded when the OpenGL context is recreated so you don't have to do this manually. </p>
  *
  * @author
  *   mzechner (original implementation)
  */
class ShaderProgram(vertexShader: String, fragmentShader: String)(using Sge) extends AutoCloseable {

  private val actualVertexShader =
    if (ShaderProgram.prependVertexCode.nonEmpty)
      ShaderProgram.prependVertexCode + vertexShader
    else vertexShader
  private val actualFragmentShader =
    if (ShaderProgram.prependFragmentCode.nonEmpty)
      ShaderProgram.prependFragmentCode + fragmentShader
    else fragmentShader

  /** the log * */
  private var log = ""

  /** whether this program compiled successfully * */
  private var compiledSuccessfully: Boolean = uninitialized

  /** uniform lookup * */
  private val uniforms: MutableMap[String, Int] = MutableMap.empty

  /** uniform types * */
  private val uniformTypes: MutableMap[String, Int] = MutableMap.empty

  /** uniform sizes * */
  private val uniformSizes: MutableMap[String, Int] = MutableMap.empty

  /** uniform names * */
  private var uniformNames: Array[String] = uninitialized

  /** attribute lookup * */
  private val attributes: MutableMap[String, Int] = MutableMap.empty

  /** attribute types * */
  private val attributeTypes: MutableMap[String, Int] = MutableMap.empty

  /** attribute sizes * */
  private val attributeSizes: MutableMap[String, Int] = MutableMap.empty

  /** attribute names * */
  private var attributeNames: Array[String] = uninitialized

  /** program handle * */
  private var program: Int = uninitialized

  /** vertex shader handle * */
  private var vertexShaderHandle: Int = uninitialized

  /** fragment shader handle * */
  private var fragmentShaderHandle: Int = uninitialized

  /** matrix float buffer * */
  BufferUtils.newFloatBuffer(16)

  /** vertex shader source * */
  private val vertexShaderSource: String = actualVertexShader

  /** fragment shader source * */
  private val fragmentShaderSource: String = actualFragmentShader

  /** whether this shader was invalidated * */
  private var invalidated: Boolean = false

  /** reference count * */

  compileShaders(actualVertexShader, actualFragmentShader)
  if (isCompiled()) {
    fetchAttributes()
    fetchUniforms()
    ShaderProgram.addManagedShader(Sge().application, this)
  }

  def this(vertexShader: FileHandle, fragmentShader: FileHandle)(using Sge) = {
    this(vertexShader.readString(), fragmentShader.readString())
  }

  /** Loads and compiles the shaders, creates a new program and links the shaders.
    *
    * @param vertexShader
    * @param fragmentShader
    */
  private def compileShaders(vertexShader: String, fragmentShader: String): Unit = {
    vertexShaderHandle = loadShader(GL20.GL_VERTEX_SHADER, vertexShader)
    fragmentShaderHandle = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentShader)

    if (vertexShaderHandle == -1 || fragmentShaderHandle == -1) {
      compiledSuccessfully = false
    } else {
      program = linkProgram(createProgram())
      if (program == -1) {
        compiledSuccessfully = false
      } else {
        compiledSuccessfully = true
      }
    }
  }

  private def loadShader(shaderType: Int, source: String): Int = scala.util.boundary {
    val gl     = Sge().graphics.gl20
    val intbuf = BufferUtils.newIntBuffer(1)

    val shader = gl.glCreateShader(shaderType)
    if (shader == 0) scala.util.boundary.break(-1)

    gl.glShaderSource(shader, source)
    gl.glCompileShader(shader)
    gl.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intbuf)

    val compiled = intbuf.get(0)
    if (compiled == 0) {
      val infoLog = gl.glGetShaderInfoLog(shader)
      log += (if (shaderType == GL20.GL_VERTEX_SHADER) "Vertex shader\n" else "Fragment shader:\n")
      log += infoLog
      scala.util.boundary.break(-1)
    }

    shader
  }

  protected def createProgram(): Int = {
    val gl      = Sge().graphics.gl20
    val program = gl.glCreateProgram()
    if (program != 0) program else -1
  }

  private def linkProgram(program: Int): Int = scala.util.boundary {
    val gl = Sge().graphics.gl20
    if (program == -1) scala.util.boundary.break(-1)

    gl.glAttachShader(program, vertexShaderHandle)
    gl.glAttachShader(program, fragmentShaderHandle)
    gl.glLinkProgram(program)

    val tmp = ByteBuffer.allocateDirect(4)
    tmp.order(ByteOrder.nativeOrder())
    val intbuf = tmp.asIntBuffer()

    gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, intbuf)
    val linked = intbuf.get(0)
    if (linked == 0) {
      log = Sge().graphics.gl20.glGetProgramInfoLog(program)
      scala.util.boundary.break(-1)
    }

    program
  }

  /** @return
    *   the log info for the shader compilation and program linking stage. The shader needs to be bound for this method to have an effect.
    */
  def getLog(): String =
    if (isCompiled()) {
      log = Sge().graphics.gl20.glGetProgramInfoLog(program)
      log
    } else {
      log
    }

  /** @return whether this ShaderProgram compiled successfully. */
  def isCompiled(): Boolean = compiledSuccessfully

  private def fetchAttributeLocation(name: String): Int = {
    val gl = Sge().graphics.gl20
    // -2 == not yet cached
    // -1 == cached but not found
    attributes.get(name) match {
      case Some(location) => location
      case None           =>
        val location = gl.glGetAttribLocation(program, name)
        attributes.put(name, location)
        location
    }
  }

  private def fetchUniformLocation(name: String): Int =
    fetchUniformLocation(name, ShaderProgram.pedantic)

  def fetchUniformLocation(name: String, pedantic: Boolean): Int =
    // -2 == not yet cached
    // -1 == cached but not found
    uniforms.get(name) match {
      case Some(location) => location
      case None           =>
        val location = Sge().graphics.gl20.glGetUniformLocation(program, name)
        if (location == -1 && pedantic) {
          if (isCompiled()) throw new IllegalArgumentException(s"No uniform with name '$name' in shader")
          throw new IllegalStateException(s"An attempted fetch uniform from uncompiled shader \n${getLog()}")
        }
        uniforms.put(name, location)
        location
    }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value
    *   the value
    */
  def setUniformi(name: String, value: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform1i(location, value)
  }

  def setUniformi(location: Int, value: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform1i(location, value)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    */
  def setUniformi(name: String, value1: Int, value2: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform2i(location, value1, value2)
  }

  def setUniformi(location: Int, value1: Int, value2: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform2i(location, value1, value2)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    * @param value3
    *   the third value
    */
  def setUniformi(name: String, value1: Int, value2: Int, value3: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform3i(location, value1, value2, value3)
  }

  def setUniformi(location: Int, value1: Int, value2: Int, value3: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform3i(location, value1, value2, value3)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    * @param value3
    *   the third value
    * @param value4
    *   the fourth value
    */
  def setUniformi(name: String, value1: Int, value2: Int, value3: Int, value4: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform4i(location, value1, value2, value3, value4)
  }

  def setUniformi(location: Int, value1: Int, value2: Int, value3: Int, value4: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform4i(location, value1, value2, value3, value4)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value
    *   the value
    */
  def setUniformf(name: String, value: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform1f(location, value)
  }

  def setUniformf(location: Int, value: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform1f(location, value)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    */
  def setUniformf(name: String, value1: Float, value2: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform2f(location, value1, value2)
  }

  def setUniformf(location: Int, value1: Float, value2: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform2f(location, value1, value2)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    * @param value3
    *   the third value
    */
  def setUniformf(name: String, value1: Float, value2: Float, value3: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform3f(location, value1, value2, value3)
  }

  def setUniformf(location: Int, value1: Float, value2: Float, value3: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform3f(location, value1, value2, value3)
  }

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    * @param value3
    *   the third value
    * @param value4
    *   the fourth value
    */
  def setUniformf(name: String, value1: Float, value2: Float, value3: Float, value4: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform4f(location, value1, value2, value3, value4)
  }

  def setUniformf(location: Int, value1: Float, value2: Float, value3: Float, value4: Float): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform4f(location, value1, value2, value3, value4)
  }

  def setUniform1fv(name: String, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform1fv(location, length, values, offset)
  }

  def setUniform1fv(location: Int, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform1fv(location, length, values, offset)
  }

  def setUniform2fv(name: String, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform2fv(location, length / 2, values, offset)
  }

  def setUniform2fv(location: Int, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform2fv(location, length / 2, values, offset)
  }

  def setUniform3fv(name: String, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform3fv(location, length / 3, values, offset)
  }

  def setUniform3fv(location: Int, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform3fv(location, length / 3, values, offset)
  }

  def setUniform4fv(name: String, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchUniformLocation(name)
    gl.glUniform4fv(location, length / 4, values, offset)
  }

  def setUniform4fv(location: Int, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniform4fv(location, length / 4, values, offset)
  }

  /** Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param matrix
    *   the matrix
    */
  def setUniformMatrix(name: String, matrix: Matrix4): Unit =
    setUniformMatrix(name, matrix, false)

  /** Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param matrix
    *   the matrix
    * @param transpose
    *   whether the matrix should be transposed
    */
  def setUniformMatrix(name: String, matrix: Matrix4, transpose: Boolean): Unit =
    setUniformMatrix(fetchUniformLocation(name), matrix, transpose)

  def setUniformMatrix(location: Int, matrix: Matrix4): Unit =
    setUniformMatrix(location, matrix, false)

  def setUniformMatrix(location: Int, matrix: Matrix4, transpose: Boolean): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniformMatrix4fv(location, 1, transpose, matrix.values, 0)
  }

  /** Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param matrix
    *   the matrix
    */
  def setUniformMatrix(name: String, matrix: Matrix3): Unit =
    setUniformMatrix(name, matrix, false)

  /** Sets the uniform matrix with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param matrix
    *   the matrix
    * @param transpose
    *   whether the uniform matrix should be transposed
    */
  def setUniformMatrix(name: String, matrix: Matrix3, transpose: Boolean): Unit =
    setUniformMatrix(fetchUniformLocation(name), matrix, transpose)

  def setUniformMatrix(location: Int, matrix: Matrix3): Unit =
    setUniformMatrix(location, matrix, false)

  def setUniformMatrix(location: Int, matrix: Matrix3, transpose: Boolean): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniformMatrix3fv(location, 1, transpose, matrix.values, 0)
  }

  /** Sets an array of uniform matrices with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param buffer
    *   buffer containing the matrix data
    * @param transpose
    *   whether the uniform matrix should be transposed
    */
  def setUniformMatrix3fv(name: String, buffer: FloatBuffer, count: Int, transpose: Boolean): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    buffer.asInstanceOf[Buffer].position(0)
    val location = fetchUniformLocation(name)
    gl.glUniformMatrix3fv(location, count, transpose, buffer)
  }

  /** Sets an array of uniform matrices with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param buffer
    *   buffer containing the matrix data
    * @param transpose
    *   whether the uniform matrix should be transposed
    */
  def setUniformMatrix4fv(name: String, buffer: FloatBuffer, count: Int, transpose: Boolean): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    buffer.asInstanceOf[Buffer].position(0)
    val location = fetchUniformLocation(name)
    gl.glUniformMatrix4fv(location, count, transpose, buffer)
  }

  def setUniformMatrix4fv(location: Int, values: Array[Float], offset: Int, length: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUniformMatrix4fv(location, length / 16, false, values, offset)
  }

  def setUniformMatrix4fv(name: String, values: Array[Float], offset: Int, length: Int): Unit =
    setUniformMatrix4fv(fetchUniformLocation(name), values, offset, length)

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param values
    *   x and y as the first and second values respectively
    */
  def setUniformf(name: String, values: Vector2): Unit =
    setUniformf(name, values.x, values.y)

  def setUniformf(location: Int, values: Vector2): Unit =
    setUniformf(location, values.x, values.y)

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param values
    *   x, y and z as the first, second and third values respectively
    */
  def setUniformf(name: String, values: Vector3): Unit =
    setUniformf(name, values.x, values.y, values.z)

  def setUniformf(location: Int, values: Vector3): Unit =
    setUniformf(location, values.x, values.y, values.z)

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param values
    *   x, y, z, and w as the first, second, third, and fourth values respectively
    */
  def setUniformf(name: String, values: Vector4): Unit =
    setUniformf(name, values.x, values.y, values.z, values.w)

  def setUniformf(location: Int, values: Vector4): Unit =
    setUniformf(location, values.x, values.y, values.z, values.w)

  /** Sets the uniform with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the name of the uniform
    * @param values
    *   r, g, b and a as the first through fourth values respectively
    */
  def setUniformf(name: String, values: Color): Unit =
    setUniformf(name, values.r, values.g, values.b, values.a)

  def setUniformf(location: Int, values: Color): Unit =
    setUniformf(location, values.r, values.g, values.b, values.a)

  /** Sets the vertex attribute with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the attribute name
    * @param size
    *   the number of components, must be >= 1 and <= 4
    * @param shaderType
    *   the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT, GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
    * @param normalize
    *   whether fixed point data should be normalized. Will not work on the desktop
    * @param stride
    *   the stride in bytes between successive attributes
    * @param buffer
    *   the buffer containing the vertex attributes.
    */
  def setVertexAttribute(name: String, size: Int, shaderType: Int, normalize: Boolean, stride: Int, buffer: Buffer): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchAttributeLocation(name)
    if (location != -1) {
      gl.glVertexAttribPointer(location, size, shaderType, normalize, stride, buffer)
    }
  }

  def setVertexAttribute(location: Int, size: Int, shaderType: Int, normalize: Boolean, stride: Int, buffer: Buffer): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glVertexAttribPointer(location, size, shaderType, normalize, stride, buffer)
  }

  /** Sets the vertex attribute with the given name. The {@link ShaderProgram} must be bound for this to work.
    *
    * @param name
    *   the attribute name
    * @param size
    *   the number of components, must be >= 1 and <= 4
    * @param shaderType
    *   the type, must be one of GL20.GL_BYTE, GL20.GL_UNSIGNED_BYTE, GL20.GL_SHORT, GL20.GL_UNSIGNED_SHORT,GL20.GL_FIXED, or GL20.GL_FLOAT. GL_FIXED will not work on the desktop
    * @param normalize
    *   whether fixed point data should be normalized. Will not work on the desktop
    * @param stride
    *   the stride in bytes between successive attributes
    * @param offset
    *   byte offset into the vertex buffer object bound to GL20.GL_ARRAY_BUFFER.
    */
  def setVertexAttribute(name: String, size: Int, shaderType: Int, normalize: Boolean, stride: Int, offset: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchAttributeLocation(name)
    if (location != -1) {
      gl.glVertexAttribPointer(location, size, shaderType, normalize, stride, offset)
    }
  }

  def setVertexAttribute(location: Int, size: Int, shaderType: Int, normalize: Boolean, stride: Int, offset: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glVertexAttribPointer(location, size, shaderType, normalize, stride, offset)
  }

  def bind(): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glUseProgram(program)
  }

  /** Disposes all resources associated with this shader. Must be called when the shader is no longer used. */
  def close(): Unit = {
    val gl = Sge().graphics.gl20
    gl.glUseProgram(0)
    gl.glDeleteShader(vertexShaderHandle)
    gl.glDeleteShader(fragmentShaderHandle)
    gl.glDeleteProgram(program)
    ShaderProgram.shaders.get(Sge().application).foreach(_.removeValue(this))
  }

  /** Disables the vertex attribute with the given name
    *
    * @param name
    *   the vertex attribute name
    */
  def disableVertexAttribute(name: String): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchAttributeLocation(name)
    if (location != -1) {
      gl.glDisableVertexAttribArray(location)
    }
  }

  def disableVertexAttribute(location: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glDisableVertexAttribArray(location)
  }

  /** Enables the vertex attribute with the given name
    *
    * @param name
    *   the vertex attribute name
    */
  def enableVertexAttribute(name: String): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    val location = fetchAttributeLocation(name)
    if (location != -1) {
      gl.glEnableVertexAttribArray(location)
    }
  }

  def enableVertexAttribute(location: Int): Unit = {
    val gl = Sge().graphics.gl20
    checkManaged()
    gl.glEnableVertexAttribArray(location)
  }

  private def checkManaged(): Unit =
    if (invalidated) {
      compileShaders(vertexShaderSource, fragmentShaderSource)
      invalidated = false
    }

  def invalidate(): Unit = {
    invalidated = true
    checkManaged()
  }

  /** Sets the given attribute
    *
    * @param name
    *   the name of the attribute
    * @param value1
    *   the first value
    * @param value2
    *   the second value
    * @param value3
    *   the third value
    * @param value4
    *   the fourth value
    */
  def setAttributef(name: String, value1: Float, value2: Float, value3: Float, value4: Float): Unit = {
    val gl       = Sge().graphics.gl20
    val location = fetchAttributeLocation(name)
    gl.glVertexAttrib4f(location, value1, value2, value3, value4)
  }

  private val params     = BufferUtils.newIntBuffer(1)
  private val shaderType = BufferUtils.newIntBuffer(1)

  private def fetchUniforms(): Unit = {
    params.asInstanceOf[Buffer].clear()
    Sge().graphics.gl20.glGetProgramiv(program, GL20.GL_ACTIVE_UNIFORMS, params)
    val numUniforms = params.get(0)

    uniformNames = new Array[String](numUniforms)

    for (i <- 0 until numUniforms) {
      params.asInstanceOf[Buffer].clear()
      params.put(0, 1)
      shaderType.asInstanceOf[Buffer].clear()
      val name     = Sge().graphics.gl20.glGetActiveUniform(program, i, params, shaderType)
      val location = Sge().graphics.gl20.glGetUniformLocation(program, name)
      uniforms.put(name, location)
      uniformTypes.put(name, shaderType.get(0))
      uniformSizes.put(name, params.get(0))
      uniformNames(i) = name
    }
  }

  private def fetchAttributes(): Unit = {
    params.asInstanceOf[Buffer].clear()
    Sge().graphics.gl20.glGetProgramiv(program, GL20.GL_ACTIVE_ATTRIBUTES, params)
    val numAttributes = params.get(0)

    attributeNames = new Array[String](numAttributes)

    for (i <- 0 until numAttributes) {
      params.asInstanceOf[Buffer].clear()
      params.put(0, 1)
      shaderType.asInstanceOf[Buffer].clear()
      val name     = Sge().graphics.gl20.glGetActiveAttrib(program, i, params, shaderType)
      val location = Sge().graphics.gl20.glGetAttribLocation(program, name)
      attributes.put(name, location)
      attributeTypes.put(name, shaderType.get(0))
      attributeSizes.put(name, params.get(0))
      attributeNames(i) = name
    }
  }

  /** @param name
    *   the name of the attribute
    * @return
    *   whether the attribute is available in the shader
    */
  def hasAttribute(name: String): Boolean =
    attributes.contains(name)

  /** @param name
    *   the name of the attribute
    * @return
    *   the type of the attribute, one of {@link GL20#GL_FLOAT} , {@link GL20#GL_FLOAT_VEC2} etc.
    */
  def getAttributeType(name: String): Int =
    attributeTypes.getOrElse(name, 0)

  /** @param name
    *   the name of the attribute
    * @return
    *   the location of the attribute or -1.
    */
  def getAttributeLocation(name: String): Int =
    attributes.getOrElse(name, -1)

  /** @param name
    *   the name of the attribute
    * @return
    *   the size of the attribute or 0.
    */
  def getAttributeSize(name: String): Int =
    attributeSizes.getOrElse(name, 0)

  /** @param name
    *   the name of the uniform
    * @return
    *   whether the uniform is available in the shader
    */
  def hasUniform(name: String): Boolean =
    uniforms.contains(name)

  /** @param name
    *   the name of the uniform
    * @return
    *   the type of the uniform, one of {@link GL20#GL_FLOAT} , {@link GL20#GL_FLOAT_VEC2} etc.
    */
  def getUniformType(name: String): Int =
    uniformTypes.getOrElse(name, 0)

  /** @param name
    *   the name of the uniform
    * @return
    *   the location of the uniform or -1.
    */
  def getUniformLocation(name: String): Int =
    uniforms.getOrElse(name, -1)

  /** @param name
    *   the name of the uniform
    * @return
    *   the size of the uniform or 0.
    */
  def getUniformSize(name: String): Int =
    uniformSizes.getOrElse(name, 0)

  /** @return the attributes */
  def getAttributes(): Array[String] =
    attributeNames

  /** @return the uniforms */
  def getUniforms(): Array[String] =
    uniformNames

  /** @return the source of the vertex shader */
  def getVertexShaderSource(): String =
    vertexShaderSource

  /** @return the source of the fragment shader */
  def getFragmentShaderSource(): String =
    fragmentShaderSource

  /** @return the handle of the shader program */
  def getHandle(): Int =
    program
}

object ShaderProgram {

  /** default name for position attributes * */
  final val POSITION_ATTRIBUTE = "a_position"

  /** default name for normal attributes * */
  final val NORMAL_ATTRIBUTE = "a_normal"

  /** default name for color attributes * */
  final val COLOR_ATTRIBUTE = "a_color"

  /** default name for texcoords attributes, append texture unit number * */
  final val TEXCOORD_ATTRIBUTE = "a_texCoord"

  /** default name for tangent attribute * */
  final val TANGENT_ATTRIBUTE = "a_tangent"

  /** default name for binormal attribute * */
  final val BINORMAL_ATTRIBUTE = "a_binormal"

  /** default name for boneweight attribute * */
  final val BONEWEIGHT_ATTRIBUTE = "a_boneWeight"

  /** flag indicating whether attributes & uniforms must be present at all times * */
  var pedantic = true

  /** code that is always added to the vertex shader code, typically used to inject a #version line. Note that this is added as-is, you should include a newline (`\n`) if needed.
    */
  var prependVertexCode = ""

  /** code that is always added to every fragment shader code, typically used to inject a #version line. Note that this is added as-is, you should include a newline (`\n`) if needed.
    */
  var prependFragmentCode = ""

  /** the list of currently available shaders * */
  private val shaders: MutableMap[Application, DynamicArray[ShaderProgram]] = MutableMap.empty

  private def addManagedShader(app: Application, shaderProgram: ShaderProgram): Unit = {
    val managedResources = shaders.getOrElseUpdate(app, DynamicArray[ShaderProgram]())
    managedResources.add(shaderProgram)
  }

  /** Invalidates all shaders so the next time they are used new handles are generated
    * @param app
    */
  def invalidateAllShaderPrograms(app: Application)(using Sge): Unit =
    shaders.get(app) match {
      case Some(shaderArray) =>
        var i = 0
        val n = shaderArray.size
        while (i < n) {
          shaderArray(i).invalidate()
          i += 1
        }
      case None =>
    }

  def clearAllShaderPrograms(app: Application): Unit =
    shaders.remove(app)

  def getManagedStatus(): String = {
    val builder = new StringBuilder()
    builder.append("Managed shaders/app: { ")
    for (app <- shaders.keys) {
      builder.append(shaders(app).size)
      builder.append(" ")
    }
    builder.append("}")
    builder.toString()
  }

  /** @return the number of managed shader programs currently loaded */
  def getNumManagedShaderPrograms()(using Sge): Int =
    shaders.get(Sge().application).map(_.size).getOrElse(0)
}
