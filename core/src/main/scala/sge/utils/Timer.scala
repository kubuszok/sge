/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Timer.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.collection.mutable.ArrayBuffer

/** Executes tasks in the future on the main loop thread.
  * @author
  *   Nathan Sweet (original implementation)
  */
class Timer(implicit sde: sge.Sge) {
  import Timer._

  private val tasks = ArrayBuffer.empty[Task]
  private var stopTimeMillis: Long = 0

  start()

  /** Schedules a task to occur once as soon as possible, but not sooner than the start of the next frame. */
  def postTask(task: Task): Task = scheduleTask(task, 0, 0, 0)

  /** Schedules a task to occur once after the specified delay. */
  def scheduleTask(task: Task, delaySeconds: Float): Task =
    scheduleTask(task, delaySeconds, 0, 0)

  /** Schedules a task to occur once after the specified delay and then repeatedly at the specified interval until cancelled. */
  def scheduleTask(task: Task, delaySeconds: Float, intervalSeconds: Float): Task =
    scheduleTask(task, delaySeconds, intervalSeconds, -1)

  /** Schedules a task to occur once after the specified delay and then a number of additional times at the specified interval.
    * @param repeatCount
    *   If negative, the task will repeat forever.
    */
  def scheduleTask(task: Task, delaySeconds: Float, intervalSeconds: Float, repeatCount: Int): Task = {
    threadLock.synchronized {
      this.synchronized {
        task.synchronized {
          if (task.timer.isDefined) throw new IllegalArgumentException("The same task may not be scheduled twice.")
          task.timer = Some(this)
          val timeMillis        = System.nanoTime() / 1000000
          var executeTimeMillis = timeMillis + (delaySeconds * 1000).toLong
          val currentThread     = thread()
          if (currentThread.pauseTimeMillis > 0) executeTimeMillis -= timeMillis - currentThread.pauseTimeMillis
          task.executeTimeMillis = executeTimeMillis
          task.intervalMillis = (intervalSeconds * 1000).toLong
          task.repeatCount = repeatCount
          tasks += task
        }
      }
      threadLock.notifyAll()
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
      if (instances.contains(this)) return
      instances += this
      if (stopTimeMillis > 0) {
        delay(System.nanoTime() / 1000000 - stopTimeMillis)
        stopTimeMillis = 0
      }
      threadLock.notifyAll()
    }

  /** Cancels all tasks. */
  def clear(): Unit =
    threadLock.synchronized {
      val currentThread = thread()
      this.synchronized {
        currentThread.postedTasks.synchronized {
          for (task <- tasks) {
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
    while (i < tasks.length) {
      val task = tasks(i)
      task.synchronized {
        if (task.executeTimeMillis > timeMillis) {
          currentWaitMillis = scala.math.min(currentWaitMillis, task.executeTimeMillis - timeMillis)
          i += 1
        } else {
          if (task.repeatCount == 0) {
            task.timer = None
            tasks.remove(i)
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
    for (task <- tasks)
      task.synchronized {
        task.executeTimeMillis += delayMillis
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
  def instance()(implicit sde: sge.Sge): Timer =
    threadLock.synchronized {
      val t = thread()
      if (t.instance.isEmpty) t.instance = Some(new Timer())
      t.instance.get
    }

  private def thread()(implicit sde: sge.Sge): TimerThread =
    threadLock.synchronized {
      if (currentThread.isEmpty || currentThread.get.files != sde.files) {
        currentThread.foreach(_.dispose())
        currentThread = Some(new TimerThread())
      }
      currentThread.get
    }

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#postTask(Task)
    */
  def post(task: Task)(implicit sde: sge.Sge): Task = instance().postTask(task)

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#scheduleTask(Task, Float)
    */
  def schedule(task: Task, delaySeconds: Float)(implicit sde: sge.Sge): Task =
    instance().scheduleTask(task, delaySeconds)

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#scheduleTask(Task, Float, Float)
    */
  def schedule(task: Task, delaySeconds: Float, intervalSeconds: Float)(implicit sde: sge.Sge): Task =
    instance().scheduleTask(task, delaySeconds, intervalSeconds)

  /** Schedules a task on {@link #instance} .
    * @see
    *   Timer#scheduleTask(Task, Float, Float, Int)
    */
  def schedule(task: Task, delaySeconds: Float, intervalSeconds: Float, repeatCount: Int)(implicit sde: sge.Sge): Task =
    instance().scheduleTask(task, delaySeconds, intervalSeconds, repeatCount)

  /** Runnable that can be scheduled on a {@link Timer} .
    * @author
    *   Nathan Sweet
    */
  abstract class Task(implicit sde: sge.Sge) extends Runnable {
    private val app = sde // Store which app to postRunnable (eg for multiple LwjglAWTCanvas).
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
    def getExecuteTimeMillis: Long = synchronized(executeTimeMillis)
  }

  /** Manages a single thread for updating timers. Uses application events to pause, resume, and dispose the thread.
    * @author
    *   Nathan Sweet
    */
  private class TimerThread(implicit sde: sge.Sge) extends Runnable {
    val files       = sde.files
    private val app = sde
    val instances   = ArrayBuffer.empty[Timer]
    var instance:        Option[Timer] = None
    var pauseTimeMillis: Long          = 0

    val postedTasks            = ArrayBuffer.empty[Task]
    private val runTasks       = ArrayBuffer.empty[Task]
    private val runPostedTasks = new Runnable {
      def run(): Unit = runPostedTasksImpl()
    }

    // app.addLifecycleListener(this) // TODO: Implement LifecycleListener
    resume()

    val thread = new Thread(this, "Timer")
    thread.setDaemon(true)
    thread.start()

    def run(): Unit = {
      while (true)
        threadLock.synchronized {
          if (currentThread.exists(_ != this) || files != sde.files) return

          var waitMillis = 5000L
          if (pauseTimeMillis == 0) {
            val timeMillis = System.nanoTime() / 1000000
            for (i <- instances.indices)
              try
                waitMillis = instances(i).update(this, timeMillis, waitMillis)
              catch {
                case ex: Throwable =>
                  throw SgeError.MathError(s"Task failed: ${instances(i).getClass.getName}", Some(ex))
              }
          }

          if (currentThread.exists(_ != this) || files != sde.files) return

          try
            if (waitMillis > 0) threadLock.wait(waitMillis)
          catch {
            case _: InterruptedException => // ignored
          }
        }
      dispose()
    }

    private def runPostedTasksImpl(): Unit = {
      postedTasks.synchronized {
        runTasks ++= postedTasks
        postedTasks.clear()
      }
      for (task <- runTasks)
        task.run()
      runTasks.clear()
    }

    def addPostedTask(task: Task): Unit =
      postedTasks.synchronized {
        if (postedTasks.isEmpty) {
          // TODO: Implement postRunnable when Application interface is converted
          // app.postRunnable(runPostedTasks)
        }
        postedTasks += task
      }

    def removePostedTask(task: Task): Unit =
      postedTasks.synchronized {
        postedTasks.removeValue(task)
      }

    def resume(): Unit =
      threadLock.synchronized {
        val delayMillis = System.nanoTime() / 1000000 - pauseTimeMillis
        for (instance <- instances)
          instance.delay(delayMillis)
        pauseTimeMillis = 0
        threadLock.notifyAll()
      }

    def pause(): Unit =
      threadLock.synchronized {
        pauseTimeMillis = System.nanoTime() / 1000000
        threadLock.notifyAll()
      }

    def dispose(): Unit = // OK to call multiple times.
      threadLock.synchronized {
        postedTasks.synchronized {
          postedTasks.clear()
        }
        if (currentThread.exists(_ == this)) currentThread = None
        instances.clear()
        threadLock.notifyAll()
      }
    // app.removeLifecycleListener(this) // TODO: Implement LifecycleListener
  }

  // Extension methods for ArrayBuffer to match the original Array API
  implicit class ArrayBufferExtensions[T](buffer: ArrayBuffer[T]) {
    def removeValue(value: T): Boolean = {
      val index = buffer.indexOf(value)
      if (index >= 0) {
        buffer.remove(index)
        true
      } else {
        false
      }
    }
  }
}
