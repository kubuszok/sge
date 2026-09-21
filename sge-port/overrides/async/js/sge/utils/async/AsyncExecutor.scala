/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/src/com/badlogic/gdx/backends/gwt/emu/com/badlogic/gdx/utils/async/AsyncExecutor.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Platform row: JS — libGDX's own single-threaded emulation (the browser has no threads; Scala.js
 *     ships no java.util.concurrent.Future): a submitted task runs to completion inside `submit`.
 *   Renames: Disposable -> java.lang.AutoCloseable, dispose -> close (as the port spells them)
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.utils.async

/** GWT emulation of [[AsyncExecutor]], will call tasks on the main thread. */
class AsyncExecutor(maxConcurrent$p: scala.Int, name$p: java.lang.String) extends java.lang.AutoCloseable {

  /** Creates a new AsyncExecutor that allows maxConcurrent [[java.lang.Runnable]] instances to run in parallel. */
  def this(maxConcurrent: scala.Int) = this(maxConcurrent, "AsyncExecutor-Thread")

  /** Submits a Runnable to be executed asynchronously. If maxConcurrent runnables are already running, the runnable will be queued.
    * @param task
    *   the task to execute asynchronously
    */
  def submit[T <: java.lang.Object](task: sge.utils.async.AsyncTask[T]): sge.utils.async.AsyncResult[T] = {
    val result: T =
      try task.call()
      catch {
        case t: java.lang.Throwable =>
          throw new sge.utils.GdxRuntimeException("Could not submit AsyncTask: " + t.getMessage(), t)
      }
    new sge.utils.async.AsyncResult[T](result)
  }

  /** Waits for running [[AsyncTask]] instances to finish, then destroys any resources like threads. Can not be used after this method is called. */
  @java.lang.Override
  override def close(): scala.Unit = ()
}
