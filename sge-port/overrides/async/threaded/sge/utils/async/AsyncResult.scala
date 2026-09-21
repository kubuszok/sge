/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/async/AsyncResult.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Platform row: JVM and Native (over java.util.concurrent.Future). The JS row ships libGDX's own
 *     GWT emulation, whose result is complete on construction.
 *   Renames: isDone -> done (parenless getter, as the port spells it)
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.utils.async

/** Returned by [[AsyncExecutor.submit]], allows to poll for the result of the asynch workload. */
class AsyncResult[T <: java.lang.Object] private[async] (private val future: java.util.concurrent.Future[T]) {

  /** @return whether the [[AsyncTask]] is done */
  def done: scala.Boolean = this.future.isDone()

  /** @return waits if necessary for the computation to complete and then returns the result
    * @throws sge.utils.GdxRuntimeException
    *   if there was an error
    */
  def get(): T = {
    try {
      this.future.get()
    } catch {
      case _: java.lang.InterruptedException =>
        null.asInstanceOf[T]
      case ex: java.util.concurrent.ExecutionException =>
        throw new sge.utils.GdxRuntimeException(ex.getCause())
    }
  }
}
