/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/profiling/GLInterceptor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: static resolveErrorNumber -> companion object; switch -> match
 *   Convention: added abstract protected def check() (Java subclasses each had private check())
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package graphics
package profiling

import sge.math.FloatCounter

abstract class GLInterceptor(profiler: GLProfiler) extends GL20 {

  protected var calls:           Int = 0
  protected var textureBindings: Int = 0
  protected var drawCalls:       Int = 0
  protected var shaderSwitches:  Int = 0
  final protected val vertexCount = new FloatCounter(0)

  protected var glProfiler: GLProfiler = profiler

  def getCalls(): Int = calls

  def getTextureBindings(): Int = textureBindings

  def getDrawCalls(): Int = drawCalls

  def getShaderSwitches(): Int = shaderSwitches

  def getVertexCount(): FloatCounter = vertexCount

  def reset(): Unit = {
    calls = 0
    textureBindings = 0
    drawCalls = 0
    shaderSwitches = 0
    vertexCount.reset()
  }

  protected def check(): Unit
}

object GLInterceptor {
  def resolveErrorNumber(error: Int): String =
    error match {
      case GL20.GL_INVALID_VALUE =>
        "GL_INVALID_VALUE"
      case GL20.GL_INVALID_OPERATION =>
        "GL_INVALID_OPERATION"
      case GL20.GL_INVALID_FRAMEBUFFER_OPERATION =>
        "GL_INVALID_FRAMEBUFFER_OPERATION"
      case GL20.GL_INVALID_ENUM =>
        "GL_INVALID_ENUM"
      case GL20.GL_OUT_OF_MEMORY =>
        "GL_OUT_OF_MEMORY"
      case _ =>
        "number " + error
    }
}
