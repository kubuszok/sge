/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/ImmediateModeRenderer20.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses DynamicArray[VertexAttribute] for builder; close() delegates to dispose()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.VertexAttributes.Usage
import sge.graphics.{ Color, Mesh, VertexAttribute }
import sge.math.Matrix4
import sge.utils.{ DynamicArray, SgeError }
import sge.Sge
import scala.annotation.nowarn
import scala.compiletime.uninitialized

/** Immediate mode rendering class for GLES 2.0. The renderer will allow you to specify vertices on the fly and provides a default shader for (unlit) rendering.
  * @author
  *   mzechner
  */
class ImmediateModeRenderer20(
  maxVertices:  Int,
  hasNormals:   Boolean,
  hasColors:    Boolean,
  numTexCoords: Int,
  shader:       ShaderProgram
)(using Sge)
    extends ImmediateModeRenderer
    with AutoCloseable {

  @nowarn("msg=not read") // set in begin(), will be read when actual rendering is implemented
  private var primitiveType:   Int = uninitialized
  private var vertexIdx:       Int = uninitialized
  private var numSetTexCoords: Int = uninitialized
  private var numVertices:     Int = uninitialized

  private var shaderVar:     ShaderProgram = shader
  private var ownsShader:    Boolean       = false
  private val projModelView: Matrix4       = Matrix4()

  // Constructor body
  private val attribs = buildVertexAttributes(hasNormals, hasColors, numTexCoords)
  private val mesh: Mesh = Mesh(false, maxVertices, 0, VertexAttributes(attribs*))

  private val vertexSize: Int = mesh.getVertexAttributes().vertexSize / 4

  private val vertices: Array[Float] = Array.ofDim[Float](maxVertices * vertexSize)

  private val normalOffset: Int = mesh.getVertexAttribute(Usage.Normal).map(_.offset / 4).getOrElse(0)

  private val colorOffset: Int = mesh.getVertexAttribute(Usage.ColorPacked).map(_.offset / 4).getOrElse(0)

  private val texCoordOffset: Int = mesh.getVertexAttribute(Usage.TextureCoordinates).map(_.offset / 4).getOrElse(0)

  private val shaderUniformNames: Array[String] = Array.ofDim[String](numTexCoords)
  for (i <- 0 until numTexCoords)
    shaderUniformNames(i) = "u_sampler" + i

  def this(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int)(using Sge) = {
    this(5000, hasNormals, hasColors, numTexCoords, ImmediateModeRenderer20.createDefaultShader(hasNormals, hasColors, numTexCoords))
    ownsShader = true
  }

  def this(maxVertices: Int, hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int)(using Sge) = {
    this(
      maxVertices,
      hasNormals,
      hasColors,
      numTexCoords,
      ImmediateModeRenderer20.createDefaultShader(hasNormals, hasColors, numTexCoords)
    )
    ownsShader = true
  }

  private def buildVertexAttributes(hasNormals: Boolean, hasColor: Boolean, numTexCoords: Int): Array[VertexAttribute] = {
    val attribs = DynamicArray[VertexAttribute]()
    attribs.add(VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE))
    if (hasNormals) attribs.add(VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE))
    if (hasColor) attribs.add(VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE))
    for (i <- 0 until numTexCoords)
      attribs.add(VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + i))
    attribs.toArray
  }

  def setShader(shader: ShaderProgram): Unit = {
    if (ownsShader) this.shaderVar.close()
    this.shaderVar = shader
    ownsShader = false
  }

  def getShader(): ShaderProgram = shaderVar

  def begin(projModelView: Matrix4, primitiveType: Int): Unit = {
    this.projModelView.set(projModelView)
    this.primitiveType = primitiveType
  }

  def color(color: Color): Unit =
    vertices(vertexIdx + colorOffset) = color.toFloatBits()

  def color(r: Float, g: Float, b: Float, a: Float): Unit =
    vertices(vertexIdx + colorOffset) = Color.toFloatBits(r, g, b, a)

  def color(colorBits: Float): Unit =
    vertices(vertexIdx + colorOffset) = colorBits

  def texCoord(u: Float, v: Float): Unit = {
    val idx = vertexIdx + texCoordOffset
    vertices(idx + numSetTexCoords) = u
    vertices(idx + numSetTexCoords + 1) = v
    numSetTexCoords += 2
  }

  def normal(x: Float, y: Float, z: Float): Unit = {
    val idx = vertexIdx + normalOffset
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = z
  }

  def vertex(x: Float, y: Float, z: Float): Unit = {
    val idx = vertexIdx
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = z

    numSetTexCoords = 0
    vertexIdx += vertexSize
    numVertices += 1
  }

  def flush(): Unit =
    if (numVertices != 0) {
      shaderVar.bind()
      shaderVar.setUniformMatrix("u_projModelView", projModelView)
      for (i <- 0 until numTexCoords)
        shaderVar.setUniformi(shaderUniformNames(i), i)
      mesh.setVertices(vertices, 0, vertexIdx)
      // TODO: Fix mesh.render call when proper Mesh implementation is available
      // mesh.render(shaderVar, primitiveType)

      numSetTexCoords = 0
      vertexIdx = 0
      numVertices = 0
    }

  def end(): Unit =
    flush()

  def getNumVertices(): Int = numVertices

  override def getMaxVertices(): Int = maxVertices

  def dispose(): Unit = {
    if (ownsShader) shaderVar.close()
    mesh.close()
  }

  def close(): Unit = dispose()
}

object ImmediateModeRenderer20 {
  private def createVertexShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): String = {
    var shader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
      (if (hasNormals) "attribute vec3 " + ShaderProgram.NORMAL_ATTRIBUTE + ";\n" else "") +
      (if (hasColors) "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" else "")

    for (i <- 0 until numTexCoords)
      shader += "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + i + ";\n"

    shader += "uniform mat4 u_projModelView;\n" +
      (if (hasColors) "varying vec4 v_col;\n" else "")

    for (i <- 0 until numTexCoords)
      shader += "varying vec2 v_tex" + i + ";\n"

    shader += "void main() {\n" + "   gl_Position = u_projModelView * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
    if (hasColors) {
      shader += "   v_col = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
        "   v_col.a *= 255.0 / 254.0;\n"
    }

    for (i <- 0 until numTexCoords)
      shader += "   v_tex" + i + " = " + ShaderProgram.TEXCOORD_ATTRIBUTE + i + ";\n"
    shader += "   gl_PointSize = 1.0;\n" +
      "}\n"
    shader
  }

  private def createFragmentShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int): String = {
    var shader = "#ifdef GL_ES\n" + "precision mediump float;\n" + "#endif\n"

    if (hasColors) shader += "varying vec4 v_col;\n"
    for (i <- 0 until numTexCoords) {
      shader += "varying vec2 v_tex" + i + ";\n"
      shader += "uniform sampler2D u_sampler" + i + ";\n"
    }

    shader += "void main() {\n" +
      "   gl_FragColor = " + (if (hasColors) "v_col" else "vec4(1, 1, 1, 1)")

    if (numTexCoords > 0) shader += " * "

    for (i <- 0 until numTexCoords)
      if (i == numTexCoords - 1) {
        shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ")"
      } else {
        shader += " texture2D(u_sampler" + i + ",  v_tex" + i + ") *"
      }

    shader += ";\n}"
    shader
  }

  /** Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified. */
  def createDefaultShader(hasNormals: Boolean, hasColors: Boolean, numTexCoords: Int)(using Sge): ShaderProgram = {
    val vertexShader   = createVertexShader(hasNormals, hasColors, numTexCoords)
    val fragmentShader = createFragmentShader(hasNormals, hasColors, numTexCoords)
    val program        = ShaderProgram(vertexShader, fragmentShader)
    if (!program.isCompiled()) throw SgeError.GraphicsError("Error compiling shader: " + program.getLog())
    program
  }
}
