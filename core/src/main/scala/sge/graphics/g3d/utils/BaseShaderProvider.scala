/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/BaseShaderProvider.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.utils.{ Nullable, SgeError }

abstract class BaseShaderProvider extends ShaderProvider {

  protected val shaders: ArrayBuffer[Shader] = ArrayBuffer[Shader]()

  override def getShader(renderable: Renderable): Shader = boundary {
    renderable.shader.foreach { suggested =>
      if (suggested.canRender(renderable)) break(suggested)
    }
    for (shader <- shaders)
      if (shader.canRender(renderable)) break(shader)
    val shader = createShader(renderable)
    if (!shader.canRender(renderable)) throw SgeError.GraphicsError("unable to provide a shader for this renderable")
    shader.init()
    shaders += shader
    shader
  }

  protected def createShader(renderable: Renderable): Shader

  override def close(): Unit = {
    for (shader <- shaders)
      shader.close()
    shaders.clear()
  }
}
