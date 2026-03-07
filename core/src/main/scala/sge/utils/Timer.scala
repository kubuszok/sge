/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Timer.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Gdx.app` -> `sge.Sge` context; `GdxRuntimeException` -> `SgeError.MathError`; `null` -> `Option`/`Nullable`
 *   Convention: uses `using` keyword; `timer` field uses `Option[Timer]`; `currentThread` uses `Option[TimerThread]`
 *   Idiom: split packages
 *   Fixes: LifecycleListener integration and postRunnable wiring implemented; getExecuteTimeMillis → executeTime
 *   Convention: opaque Seconds for delaySeconds/intervalSeconds params
 *   Idiom: Thread replaced with Future(ExecutionContext.global); wait/notifyAll replaced with Thread.sleep
 *   TODO: redesign with Gears structured concurrency -- synchronized+Thread.sleep won't work well on JS; see docs/improvements/dependencies.md B3
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.boundary
import scala.util.boundary.break

/** Executes tasks in the future on the main loop thread.
  * @author
  *   Nathan Sweet (original implementation)
  */
class Timer(using sge.Sge) {
  import Timer._

  private val tasks = DynamicArray[Task]()
  private var stopTimeMillis: Long = 0

  start()

  /** Schedules a task to occur once as soon as possible, but not sooner than the start of the next frame. */
  def postTask(task: Task): Task = scheduleTask(task)

  /** Schedules a task to occur once after the specified delay and then a number of additional times at the specified interval.
    * @param repeatCount
    *   If negative, the task will repeat forever.
    */
  def scheduleTask(task: Task, delaySeconds: Seconds = Seconds.zero, intervalSeconds: Seconds = Seconds.zero, repeatCount: Int = 0): Task = {
    threadLock.synchronized {
      this.synchronized {
        task.synchronized {
          if (task.timer.isDefined) throw new IllegalArgumentException("The same task may not be scheduled twice.")
          task.timer = Some(this)
          val timeMillis        = System.nanoTime() / 1000000
          var executeTimeMillis = timeMillis + delaySeconds.toMillis
          val currentThread     = thread()
          if (currentThread.pauseTimeMillis > 0) executeTimeMillis -= timeMillis - currentThread.pauseTimeMillis
          task.executeTimeMillis = executeTimeMillis
          task.intervalMillis = intervalSeconds.toMillis
          task.repeatCount = repeatCount
          tasks.add(task)
        }
      }
      // Thread.sleep-based loop will re-check on next iteration
    }
    task
  }

  /** Stops the timer if it was started. Tasks will not be executed while stopped. */
  def stop(): Unit =
    threadLock.synchronized {
      if (thread().instances.removeValue(this)) stopTimeMillis = System.nanoTime() / 1000000
    }

  /** Starts the timer if it was stopped. Tasks are delayed by the time passed while stopped. */
  def start(): Unit =
    threadLock.synchronized {
      val currentThread = thread()
      val instances     = currentThread.instances
      if (!instances.contains(this)) {
        instances.add(this)
        if (stopTimeMillis > 0) {
          delay(System.nanoTime() / 1000000 - stopTimeMillis)
          stopTimeMillis = 0
        }
        // Thread.sleep-based loop will re-check on next iteration
      }
    }

  /** Cancels all tasks. */
  def clear(): Unit =
    threadLock.synchronized {
      val currentThread = thread()
      this.synchronized {
        currentThread.postedTasks.synchronized {
          tasks.foreach { task =>
            currentThread.removePostedTask(task)
            task.reset()
          }
        }
        tasks.clear()
      }
    }

  /** Returns true if the timer has no tasks in the queue. Note that this can change at any time. Synchronize on the timer instance to prevent tasks being added, removed, or updated.
    */
  def isEmpty: Boolean = synchronized(tasks.isEmpty)

  private[Timer] def update(thread: TimerThread, timeMillis: Long, waitMillis: Long): Long = synchronized {
    var currentWaitMillis = waitMillis
    var i                 = 0
    while (i < tasks.size) {
      val task = tasks(i)
      task.synchronized {
        if (task.executeTimeMillis > timeMillis) {
          currentWaitMillis = scala.math.min(currentWaitMillis, task.executeTimeMillis - timeMillis)
          i += 1
        } else {
          if (task.repeatCount == 0) {
            task.timer = None
            tasks.removeIndex(i)
          } else {
            task.executeTimeMillis = timeMillis + task.intervalMillis
            currentWaitMillis = scala.math.min(currentWaitMillis, task.intervalMillis)
            if (task.repeatCount > 0) task.repeatCount -= 1
            i += 1
          }
          thread.addPostedTask(task)
        }
      }
    }
    currentWaitMillis
  }

  /** Adds the specified delay to all tasks. */
  def delay(delayMillis: Long): Unit = synchronized {
    tasks.foreach { task =>
      task.synchronized {
        task.executeTimeMillis += delayMillis
      }
    }
  }
}

object Timer {
  // TimerThread access is synchronized using threadLock.
  // Timer access is synchronized using the Timer instance.
  // Task access is synchronized using the Task instance.
  // Posted tasks are synchronized using TimerThread#postedTasks.

  private val threadLock = new Object()
  private var currentThread: Option[TimerThread] = None

  /** Timer instance singleton for general application wide usage. Static methods on {@link Timer} make convenient use of this instance.
    */
  def instance()(using sge.Sge): Timer =
    threadLock.synchronized {
      val t = thread()
      if (t.instance.isEmpty) t.instance = Some(Timer())
      t.instance.get
    }

  private def thread()(using sge.Sge): TimerThread =
    threadLock.synchronized {
      if (currentThread.isEmpty || currentThread.get.files != sge.Sge().files) {
        currentThread.foreach(_.dispose())
        currentThread = Some(TimerThread())
      }
      currentThread.get
    }

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#postTask(Task)
    */
  def post(task: Task)(using sge.Sge): Task = instance().postTask(task)

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#scheduleTask(Task, Float)
    */
  def schedule(task: Task, delaySeconds: Seconds)(using sge.Sge): Task =
    instance().scheduleTask(task, delaySeconds)

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#scheduleTask(Task, Float, Float, Int)
    */
  def schedule(task: Task, delaySeconds: Seconds, intervalSeconds: Seconds)(using sge.Sge): Task =
    instance().scheduleTask(task, delaySeconds, intervalSeconds, -1)

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#scheduleTask(Task, Float, Float, Int)
    */
  def schedule(task: Task, delaySeconds: Seconds, intervalSeconds: Seconds, repeatCount: Int)(using sge.Sge): Task =
    instance().scheduleTask(task, delaySeconds, intervalSeconds, repeatCount)

  /** Runnable that can be scheduled on a {@link Timer} .
    * @author
    *   Nathan Sweet
    */
  abstract class Task(using sge.Sge) extends Runnable {
    private[Timer] var executeTimeMillis: Long          = 0
    private[Timer] var intervalMillis:    Long          = 0
    private[Timer] var repeatCount:       Int           = 0
    @volatile private[Timer] var timer:   Option[Timer] = None

    /** If this is the last time the task will be ran or the task is first cancelled, it may be scheduled again in this method.
      */
    def run(): Unit

    /** Cancels the task. It will not be executed until it is scheduled again. This method can be called at any time. */
    def cancel(): Unit =
      threadLock.synchronized {
        thread().removePostedTask(this)
        timer.foreach { t =>
          t.synchronized {
            t.tasks.removeValue(this)
            reset()
          }
        }
        if (timer.isEmpty) reset()
      }

    private[Timer] def reset(): Unit = synchronized {
      executeTimeMillis = 0
      timer = None
    }

    /** Returns true if this task is scheduled to be executed in the future by a timer. The execution time may be reached at any time after calling this method, which may change the scheduled state.
      * To prevent the scheduled state from changing, synchronize on this task object, eg:
      *
      * <pre> synchronized (task) { if (!task.isScheduled()) { ... } } </pre>
      */
    def isScheduled: Boolean = timer.isDefined

    /** Returns the time in milliseconds when this task will be executed next. */
    def executeTime: Long = synchronized(executeTimeMillis)
  }

  /** Manages a single thread for updating timers. Uses application events to pause, resume, and dispose the thread.
    * @author
    *   Nathan Sweet
    */
  private class TimerThread(using sge.Sge) extends Runnable with LifecycleListener {
    val files     = sge.Sge().files
    val instances = DynamicArray[Timer]()
    var instance:        Option[Timer] = None
    var pauseTimeMillis: Long          = 0

    val postedTasks      = DynamicArray[Task]()
    private val runTasks = DynamicArray[Task]()
    private val runPostedTasksRunnable: Runnable = () => runPostedTasks()

    sge.Sge().application.addLifecycleListener(this)
    resume()

    Future(this.run())(using ExecutionContext.global)

    def run(): Unit = {
      boundary {
        while (true)
          threadLock.synchronized {
            if (currentThread.exists(_ != this) || files != sge.Sge().files) break(())

            var waitMillis = 5000L
            if (pauseTimeMillis == 0) {
              val timeMillis = System.nanoTime() / 1000000
              var i          = 0
              while (i < instances.size) {
                try
                  waitMillis = instances(i).update(this, timeMillis, waitMillis)
                catch {
                  case ex: Throwable =>
                    throw SgeError.MathError(s"Task failed: ${instances(i).getClass.getName}", Some(ex))
                }
                i += 1
              }
            }

            if (currentThread.exists(_ != this) || files != sge.Sge().files) break(())

            try
              if (waitMillis > 0) Thread.sleep(waitMillis)
            catch {
              case _: InterruptedException => // ignored
            }
          }
      }
      dispose()
    }

    private def runPostedTasks(): Unit = {
      postedTasks.synchronized {
        runTasks.addAll(postedTasks)
        postedTasks.clear()
      }
      runTasks.foreach(_.run())
      runTasks.clear()
    }

    def addPostedTask(task: Task): Unit =
      postedTasks.synchronized {
        if (postedTasks.isEmpty) sge.Sge().application.postRunnable(runPostedTasksRunnable)
        postedTasks.add(task)
      }

    def removePostedTask(task: Task): Unit =
      postedTasks.synchronized {
        postedTasks.removeValue(task)
      }

    def resume(): Unit =
      threadLock.synchronized {
        val delayMillis = System.nanoTime() / 1000000 - pauseTimeMillis
        instances.foreach(_.delay(delayMillis))
        pauseTimeMillis = 0
        // Thread.sleep-based loop will re-check on next iteration
      }

    def pause(): Unit =
      threadLock.synchronized {
        pauseTimeMillis = System.nanoTime() / 1000000
        // Thread.sleep-based loop will re-check on next iteration
      }

    def dispose(): Unit = // OK to call multiple times.
      threadLock.synchronized {
        postedTasks.synchronized {
          postedTasks.clear()
        }
        if (currentThread.exists(_ == this)) currentThread = None
        instances.clear()
        // Thread.sleep-based loop will re-check on next iteration
      }
    sge.Sge().application.removeLifecycleListener(this)
  }

}
