/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/BaseShaderProvider.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - implements ShaderProvider -> extends ShaderProvider
 *   - Array<Shader> -> DynamicArray[Shader]
 *   - GdxRuntimeException -> SgeError.GraphicsError
 *   - dispose() -> close() (AutoCloseable convention); shader.dispose() -> shader.close()
 *   - No return statements -> boundary/break in getShader
 *   - All methods (getShader, createShader, close) fully ported
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import scala.util.boundary
import scala.util.boundary.break

import sge.utils.{ DynamicArray, Nullable, SgeError }

abstract class BaseShaderProvider extends ShaderProvider {

  protected val shaders: DynamicArray[Shader] = DynamicArray[Shader]()

  override def getShader(renderable: Renderable): Shader = boundary {
    renderable.shader.foreach { suggested =>
      if (suggested.canRender(renderable)) break(suggested)
    }
    for (shader <- shaders)
      if (shader.canRender(renderable)) break(shader)
    val shader = createShader(renderable)
    if (!shader.canRender(renderable)) throw SgeError.GraphicsError("unable to provide a shader for this renderable")
    shader.init()
    shaders.add(shader)
    shader
  }

  protected def createShader(renderable: Renderable): Shader

  override def close(): Unit = {
    for (shader <- shaders)
      shader.close()
    shaders.clear()
  }
}
