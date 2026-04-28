/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/profiling/GLErrorListener.java
 * Original authors: Jan Polák
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: GdxRuntimeException -> RuntimeException, Gdx.app.error -> println
 *   Convention: Java interface -> Scala trait; static constants -> companion object vals
 *   Idiom: boundary/break (1 return), Nullable (1 null), split packages
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 72
 * Covenant-baseline-methods: GLErrorListener,LOGGING_LISTENER,THROWING_LISTENER,onError
 * Covenant-source-reference: com/badlogic/gdx/graphics/profiling/GLErrorListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3eede16860c339c639ae02552ec348f1432e9afe
 */
package sge
package graphics
package profiling

import sge.utils.Nullable

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
      var place: Nullable[String] = Nullable.empty
      try {
        val stack = Thread.currentThread().getStackTrace
        for (i <- stack.indices)
          if ("check".equals(stack(i).getMethodName)) {
            if (i + 1 < stack.length) {
              val glMethod = stack(i + 1)
              place = Nullable(glMethod.getMethodName)
            }
            scala.util.boundary.break()
          }
      } catch {
        case _: Exception => // ignored
      }

      place.fold {
        println(s"GLProfiler Error ${GLInterceptor.resolveErrorNumber(error)} at: ")
        new Exception().printStackTrace()
        // This will capture current stack trace for logging, if possible
      } { p =>
        println(s"GLProfiler Error ${GLInterceptor.resolveErrorNumber(error)} from $p")
      }
    }
  }

  /** Listener that will throw a GdxRuntimeException with error name. */
  val THROWING_LISTENER: GLErrorListener = new GLErrorListener {
    override def onError(error: Int): Unit =
      throw new RuntimeException("GLProfiler: Got GL error " + GLInterceptor.resolveErrorNumber(error))
  }
}
