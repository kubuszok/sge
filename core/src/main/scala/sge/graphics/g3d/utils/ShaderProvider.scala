/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/ShaderProvider.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Java interface extends Disposable -> Scala trait extends AutoCloseable
 *   - dispose() -> close() (AutoCloseable convention)
 *   - getShader(Renderable) fully ported
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

/** Returns {@link Shader} instances for a {@link Renderable} on request. Also responsible for disposing of any created {@link Shader} instances on a call to {@link AutoCloseable#close()}.
  * @author
  *   badlogic
  */
trait ShaderProvider extends AutoCloseable {

  /** Returns a {@link Shader} for the given {@link Renderable}. The RenderInstance may already contain a Shader, in which case the provider may decide to return that.
    * @param renderable
    *   the Renderable
    * @return
    *   the Shader to be used for the RenderInstance
    */
  def getShader(renderable: Renderable): Shader
}
