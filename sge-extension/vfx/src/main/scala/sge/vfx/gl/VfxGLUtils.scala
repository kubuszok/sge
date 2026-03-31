/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package gl

import sge.files.FileHandle
import sge.graphics.GL20
import sge.graphics.glutils.ShaderProgram
import sge.utils.SgeError
import scala.compiletime.uninitialized
import java.nio.{ ByteBuffer, ByteOrder, IntBuffer }

object VfxGLUtils {

  private val tmpIntBuf: IntBuffer =
    ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()

  private val tmpViewport: VfxGlViewport = VfxGlViewport()

  /** The code that is always added to the vertex shader code. Note that this is added as-is, you should include a newline (`\n`) if needed.
    */
  var prependVertexCode: String = ""

  /** The code that is always added to every fragment shader code. Note that this is added as-is, you should include a newline (`\n`) if needed.
    */
  var prependFragmentCode: String = ""

  /** This field is used to provide custom GL calls implementation. */
  var glExtension: VfxGlExtension = uninitialized

  def initExtension()(using Sge): Unit =
    if (glExtension == null) { // @nowarn — Java interop init guard
      glExtension = DefaultVfxGlExtension()
    }

  def getBoundFboHandle()(using Sge): Int = {
    initExtension()
    glExtension.boundFboHandle
  }

  def getViewport()(using Sge): VfxGlViewport = {
    Sge().graphics.gl20.glGetIntegerv(GL20.GL_VIEWPORT, tmpIntBuf)
    tmpViewport.set(tmpIntBuf.get(0), tmpIntBuf.get(1), tmpIntBuf.get(2), tmpIntBuf.get(3))
  }

  def compileShader(vertexFile: FileHandle, fragmentFile: FileHandle)(using Sge): ShaderProgram =
    compileShader(vertexFile, fragmentFile, "")

  def compileShader(vertexFile: FileHandle, fragmentFile: FileHandle, defines: String)(using Sge): ShaderProgram = {
    require(vertexFile != null, "Vertex shader file cannot be null.") // @nowarn — Java interop boundary
    require(fragmentFile != null, "Fragment shader file cannot be null.") // @nowarn — Java interop boundary
    require(defines != null, "Defines cannot be null.") // @nowarn — Java interop boundary

    val prependVert = prependVertexCode + defines
    val prependFrag = prependFragmentCode + defines
    val srcVert     = vertexFile.readString()
    val srcFrag     = fragmentFile.readString()

    val shader = ShaderProgram(prependVert + "\n" + srcVert, prependFrag + "\n" + srcFrag)

    if (!shader.compiled) {
      throw SgeError.GraphicsError("Shader compile error: " + vertexFile.name + "/" + fragmentFile.name + "\n" + shader.log)
    }
    shader
  }

  /** Enable pipeline state queries: beware the pipeline can stall! */
  var enableGLQueryStates: Boolean = false

  /** Provides a simple mechanism to query OpenGL pipeline states. Note: state queries are costly and stall the pipeline, especially on mobile devices!
    *
    * Queries switched off by default. Update [[enableGLQueryStates]] flag to enable them.
    */
  def isGLEnabled(pName: Int)(using Sge): Boolean =
    if (!enableGLQueryStates) false
    else {
      pName match {
        case GL20.GL_BLEND =>
          val tmpByteBuffer = ByteBuffer.allocateDirect(32)
          Sge().graphics.gl20.glGetBooleanv(GL20.GL_BLEND, tmpByteBuffer)
          val result = tmpByteBuffer.get() == 1
          tmpByteBuffer.clear()
          result
        case _ => false
      }
    }
}
