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

  private var glInterceptor: GLInterceptor   = scala.compiletime.uninitialized
  private var listener:      GLErrorListener = scala.compiletime.uninitialized
  private var enabled:       Boolean         = false

  // Initialize in constructor
  locally {
    // TODO: These methods don't exist on Graphics trait yet - need proper Graphics implementation
    // For now, create a basic GL20 interceptor
    glInterceptor = new GL20Interceptor(this, graphics.gl20)
    listener = GLErrorListener.LOGGING_LISTENER
  }

  /** Enables profiling by replacing the {@code GL20} and {@code GL30} instances with profiling ones. */
  def enable(): Unit = {
    if (enabled) return

    // TODO: Graphics doesn't have these methods yet - commented out for compilation
    // if (glInterceptor.isInstanceOf[GL32]) {
    //   graphics.setGL32(glInterceptor.asInstanceOf[GL32])
    // }
    // if (glInterceptor.isInstanceOf[GL31]) {
    //   graphics.setGL31(glInterceptor.asInstanceOf[GL31])
    // }
    // if (glInterceptor.isInstanceOf[GL30]) {
    //   graphics.setGL30(glInterceptor.asInstanceOf[GL30])
    // }
    // graphics.setGL20(glInterceptor)

    // TODO: Gdx object doesn't exist yet - commented out
    // Gdx.gl32 = graphics.getGL32()
    // Gdx.gl31 = graphics.getGL31()
    // Gdx.gl30 = graphics.getGL30()
    // Gdx.gl20 = graphics.getGL20()
    // Gdx.gl = graphics.getGL20()

    enabled = true
  }

  /** Disables profiling by resetting the {@code GL20} and {@code GL30} instances with the original ones. */
  def disable(): Unit = {
    if (!enabled) return

    // TODO: Graphics doesn't have these methods yet - commented out for compilation
    // if (glInterceptor.isInstanceOf[GL32Interceptor]) {
    //   graphics.setGL32(glInterceptor.asInstanceOf[GL32Interceptor].gl32)
    // }
    // if (glInterceptor.isInstanceOf[GL31Interceptor]) {
    //   graphics.setGL31(glInterceptor.asInstanceOf[GL31Interceptor].gl31)
    // }
    // if (glInterceptor.isInstanceOf[GL30Interceptor]) {
    //   graphics.setGL30(glInterceptor.asInstanceOf[GL30Interceptor].gl30)
    // }
    // if (glInterceptor.isInstanceOf[GL20Interceptor]) {
    //   graphics.setGL20(glInterceptor.asInstanceOf[GL20Interceptor].gl20)
    // }

    // TODO: Gdx object doesn't exist yet - commented out
    // Gdx.gl32 = graphics.getGL32()
    // Gdx.gl31 = graphics.getGL31()
    // Gdx.gl30 = graphics.getGL30()
    // Gdx.gl20 = graphics.getGL20()
    // Gdx.gl = graphics.getGL20()

    enabled = false
  }

  /** Set the current listener for the {@link GLProfiler} to {@code errorListener} */
  def setListener(errorListener: GLErrorListener): Unit =
    this.listener = errorListener

  /** @return the current {@link GLErrorListener} */
  def getListener(): GLErrorListener =
    listener

  /** @return true if the GLProfiler is currently profiling */
  def isEnabled(): Boolean =
    enabled

  /** @return the total gl calls made since the last reset */
  def getCalls(): Int =
    glInterceptor.getCalls()

  /** @return the total amount of texture bindings made since the last reset */
  def getTextureBindings(): Int =
    glInterceptor.getTextureBindings()

  /** @return the total amount of draw calls made since the last reset */
  def getDrawCalls(): Int =
    glInterceptor.getDrawCalls()

  /** @return the total amount of shader switches made since the last reset */
  def getShaderSwitches(): Int =
    glInterceptor.getShaderSwitches()

  /** @return {@link FloatCounter} containing information about rendered vertices since the last reset */
  def getVertexCount(): sge.math.FloatCounter =
    glInterceptor.getVertexCount()

  /** Will reset the statistical information which has been collected so far. This should be called after every frame. Error listener is kept as it is.
    */
  def reset(): Unit =
    glInterceptor.reset()
}
