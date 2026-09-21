/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/async/AsyncExecutor.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Platform row: JVM and Native (java.util.concurrent for real; Scala Native ships the JSR-166 ports).
 *     The JS row ships libGDX's own single-threaded GWT emulation of this class instead.
 *   Renames: Disposable -> java.lang.AutoCloseable, dispose -> close (as the port spells them)
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.utils.async

/** Allows asynchronous execution of [[AsyncTask]] instances on a separate thread. Needs to be disposed via a call to `close()` when no longer used, in which case the executor waits for running tasks
  * to finish. Scheduled but not yet running tasks will not be executed.
  */
class AsyncExecutor(maxConcurrent$p: scala.Int, name$p: java.lang.String) extends java.lang.AutoCloseable {

  private val executor: java.util.concurrent.ExecutorService =
    java.util.concurrent.Executors.newFixedThreadPool(
      maxConcurrent$p,
      ((r: java.lang.Runnable) => {
        val thread: java.lang.Thread = new java.lang.Thread(r, name$p)
        thread.setDaemon(true)
        thread
      }): java.util.concurrent.ThreadFactory
    )

  /** Creates a new AsyncExecutor with the specified number of threads. */
  def this(maxConcurrent: scala.Int) = this(maxConcurrent, "AsyncExecutor-Thread")

  /** Submits a Runnable to be executed asynchronously. If maxConcurrent runnables are already running, the runnable will be queued.
    * @param task
    *   the task to execute asynchronously
    */
  def submit[T <: java.lang.Object](task: sge.utils.async.AsyncTask[T]): sge.utils.async.AsyncResult[T] = {
    if (this.executor.isShutdown()) {
      throw new sge.utils.GdxRuntimeException("Cannot run tasks on an executor that has been shutdown (disposed)")
    }
    new sge.utils.async.AsyncResult[T](this.executor.submit((() => task.call()): java.util.concurrent.Callable[T]))
  }

  /** Waits for running [[AsyncTask]] instances to finish, then destroys any resources like threads. Can not be used after this method is called. */
  @java.lang.Override
  override def close(): scala.Unit = {
    this.executor.shutdown()
    try {
      this.executor.awaitTermination(java.lang.Long.MAX_VALUE, java.util.concurrent.TimeUnit.SECONDS)
    } catch {
      case e: java.lang.InterruptedException =>
        throw new sge.utils.GdxRuntimeException("Couldn't shutdown loading thread", e)
    }
  }
}
