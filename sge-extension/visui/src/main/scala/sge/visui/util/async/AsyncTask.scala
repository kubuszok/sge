/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package async

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

import sge.utils.DynamicArray

/** Represents task that is executed asynchronously in another thread. AsyncTask and related classes are not available on GWT.
  * @author
  *   Kotcrab
  * @see
  *   [[AsyncTaskListener]]
  * @see
  *   [[SteppedAsyncTask]]
  * @see
  *   [[AsyncTaskProgressDialog]]
  */
abstract class AsyncTask(val threadName: String)(using Sge) {
  import AsyncTask._

  private var _status:   Status                          = Status.PENDING
  private val listeners: DynamicArray[AsyncTaskListener] = DynamicArray[AsyncTaskListener]()

  def execute(): Unit = {
    if (_status == Status.RUNNING) throw new IllegalStateException("Task is already running.")
    if (_status == Status.FINISHED) throw new IllegalStateException("Task has been already executed and can't be reused.")
    _status = Status.RUNNING
    val t = new Thread(() => executeInBackground(), threadName)
    t.start()
  }

  private def executeInBackground(): Unit = {
    try
      doInBackground()
    catch {
      case e: Exception => failed(e)
    }

    Sge().application.postRunnable { () =>
      var i = 0
      while (i < listeners.size) {
        listeners(i).finished()
        i += 1
      }
      _status = Status.FINISHED
    }
  }

  /** Called when this task should execute some action in background. This is always called from non-main thread. From this method only [[setProgressPercent]], [[setMessage]], [[failed]] should be
    * called.
    */
  protected def doInBackground(): Unit

  protected def failed(message: String): Unit =
    failed(message, new IllegalStateException(message))

  protected def failed(exception: Exception): Unit =
    failed(exception.getMessage, exception)

  protected def failed(message: String, exception: Exception): Unit =
    Sge().application.postRunnable { () =>
      var i = 0
      while (i < listeners.size) {
        listeners(i).failed(message, exception)
        i += 1
      }
    }

  protected def setProgressPercent(progressPercent: Int): Unit =
    Sge().application.postRunnable { () =>
      var i = 0
      while (i < listeners.size) {
        listeners(i).progressChanged(progressPercent)
        i += 1
      }
    }

  protected def setMessage(message: String): Unit =
    Sge().application.postRunnable { () =>
      var i = 0
      while (i < listeners.size) {
        listeners(i).messageChanged(message)
        i += 1
      }
    }

  /** Executes runnable on main GDX thread. This methods blocks until runnable has finished executing. Note that this runnable will also block main render thread.
    */
  protected def executeOnGdx(runnable: Runnable): Unit = {
    val latch       = new CountDownLatch(1)
    val exceptionAt = new AtomicReference[Exception]()

    Sge().application.postRunnable(() =>
      try
        runnable.run()
      catch {
        case e: Exception => exceptionAt.set(e)
      } finally
        latch.countDown()
    )

    try {
      latch.await()
      val e = exceptionAt.get()
      if (e != null) failed(e) // @nowarn -- AtomicReference returns null
    } catch {
      case e: InterruptedException => failed(e)
    }
  }

  def addListener(listener: AsyncTaskListener): Unit = listeners.add(listener)

  def removeListener(listener: AsyncTaskListener): Boolean = listeners.removeValue(listener)

  def status: Status = _status
}

object AsyncTask {
  enum Status extends java.lang.Enum[Status] {
    case PENDING, RUNNING, FINISHED
  }
}
