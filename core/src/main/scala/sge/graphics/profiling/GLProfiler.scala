/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/profiling/GLProfiler.java
 * Original authors: Daniel Holderbaum, Jan Polák
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> RuntimeException; getListener/setListener -> var listener;
 *     isEnabled -> def enabled; getCalls/getTextureBindings/etc -> Scala-style defs
 *   Convention: null fields -> scala.compiletime.uninitialized; constructor GL level detection simplified
 *   Fixes: enable()/disable() fully implemented — Graphics trait has setGL* methods; Gdx.gl* globals replaced by Sge() delegation
 *   Idiom: split packages
 *   Audited: 2026-03-04
 */
package sge
package graphics
package profiling

/** When enabled, collects statistics about GL calls and checks for GL errors. Enabling will wrap Gdx.gl* instances with delegate classes which provide described functionality and route GL calls to
  * the actual GL instances.
  *
  * @see
  *   GL20Interceptor
  * @see
  *   GL30Interceptor
  *
  * @author
  *   Daniel Holderbaum (original implementation)
  * @author
  *   Jan Polák (original implementation)
  */
class GLProfiler(graphics: Graphics) {

  private var glInterceptor: GLInterceptor = scala.compiletime.uninitialized
  private var _enabled:      Boolean       = false

  /** The current {@link GLErrorListener}. */
  var listener: GLErrorListener = scala.compiletime.uninitialized

  // Initialize in constructor — pick the highest available GL level
  locally {
    glInterceptor = graphics
      .getGL32()
      .map[GLInterceptor](gl => GL32Interceptor(this, gl))
      .orElse(graphics.getGL31().map[GLInterceptor](gl => GL31Interceptor(this, gl)))
      .orElse(graphics.getGL30().map[GLInterceptor](gl => GL30Interceptor(this, gl)))
      .getOrElse(GL20Interceptor(this, graphics.getGL20()))
    listener = GLErrorListener.LOGGING_LISTENER
  }

  /** Enables profiling by replacing the {@code GL20} and {@code GL30} instances with profiling ones. */
  def enable(): Unit =
    if (!_enabled) {
      if (glInterceptor.isInstanceOf[GL32]) graphics.setGL32(glInterceptor.asInstanceOf[GL32])
      if (glInterceptor.isInstanceOf[GL31]) graphics.setGL31(glInterceptor.asInstanceOf[GL31])
      if (glInterceptor.isInstanceOf[GL30]) graphics.setGL30(glInterceptor.asInstanceOf[GL30])
      graphics.setGL20(glInterceptor)
      // In SGE, Sge().graphics delegates to the Graphics trait getters, so no global Gdx.gl* update needed
      _enabled = true
    }

  /** Disables profiling by resetting the {@code GL20} and {@code GL30} instances with the original ones. */
  def disable(): Unit =
    if (_enabled) {
      if (glInterceptor.isInstanceOf[GL32Interceptor]) graphics.setGL32(glInterceptor.asInstanceOf[GL32Interceptor].gl32)
      if (glInterceptor.isInstanceOf[GL31Interceptor]) graphics.setGL31(glInterceptor.asInstanceOf[GL31Interceptor].gl31)
      if (glInterceptor.isInstanceOf[GL30Interceptor]) graphics.setGL30(glInterceptor.asInstanceOf[GL30Interceptor].gl30)
      if (glInterceptor.isInstanceOf[GL20Interceptor]) graphics.setGL20(glInterceptor.asInstanceOf[GL20Interceptor].gl20)
      // In SGE, Sge().graphics delegates to the Graphics trait getters, so no global Gdx.gl* update needed
      _enabled = false
    }

  /** @return true if the GLProfiler is currently profiling */
  def enabled: Boolean = _enabled

  /** @return the total gl calls made since the last reset */
  def calls: Int = glInterceptor.getCalls()

  /** @return the total amount of texture bindings made since the last reset */
  def textureBindings: Int = glInterceptor.getTextureBindings()

  /** @return the total amount of draw calls made since the last reset */
  def drawCalls: Int = glInterceptor.getDrawCalls()

  /** @return the total amount of shader switches made since the last reset */
  def shaderSwitches: Int = glInterceptor.getShaderSwitches()

  /** @return {@link FloatCounter} containing information about rendered vertices since the last reset */
  def vertexCount: sge.math.FloatCounter = glInterceptor.getVertexCount()

  /** Will reset the statistical information which has been collected so far. This should be called after every frame. Error listener is kept as it is.
    */
  def reset(): Unit =
    glInterceptor.reset()
}
