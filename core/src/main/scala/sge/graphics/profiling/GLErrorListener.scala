package sge
package graphics
package profiling

/** Listener for GL errors detected by {@link GLProfiler} .
  *
  * @see
  *   GLProfiler (original implementation)
  * @author
  *   Jan Polák (original implementation)
  */
trait GLErrorListener {

  /** Put your error logging code here.
    * @see
    *   GLInterceptor#resolveErrorNumber(int)
    */
  def onError(error: Int): Unit
}

object GLErrorListener {

  /** Listener that will log using Gdx.app.error GL error name and GL function. */
  val LOGGING_LISTENER: GLErrorListener = new GLErrorListener {
    override def onError(error: Int): Unit = scala.util.boundary {
      var place: String = null
      try {
        val stack = Thread.currentThread().getStackTrace
        for (i <- stack.indices)
          if ("check".equals(stack(i).getMethodName)) {
            if (i + 1 < stack.length) {
              val glMethod = stack(i + 1)
              place = glMethod.getMethodName
            }
            scala.util.boundary.break()
          }
      } catch {
        case _: Exception => // ignored
      }

      if (place != null) {
        println(s"GLProfiler Error ${GLInterceptor.resolveErrorNumber(error)} from $place")
      } else {
        println(s"GLProfiler Error ${GLInterceptor.resolveErrorNumber(error)} at: ")
        new Exception().printStackTrace()
        // This will capture current stack trace for logging, if possible
      }
    }
  }

  /** Listener that will throw a GdxRuntimeException with error name. */
  val THROWING_LISTENER: GLErrorListener = new GLErrorListener {
    override def onError(error: Int): Unit =
      throw new RuntimeException("GLProfiler: Got GL error " + GLInterceptor.resolveErrorNumber(error))
  }
}
