package sge.utils

import lowlevel.Nullable
import lowlevel.util.DynamicArray

class Timer(using sge.Sge) {
  import Timer._

  private val tasks = DynamicArray[Timer.Task]()
  private var stopTimeMillis: Long = 0

  start()

  def postTask(task: Timer.Task): Timer.Task = scheduleTask(task)

  def scheduleTask(task: Timer.Task, delaySeconds: Seconds = Seconds.zero, intervalSeconds: Seconds = Seconds.zero, repeatCount: Int = 0): Timer.Task = {
    threadLock.synchronized {
      val currentThread = thread()
      this.synchronized {
        task.synchronized {
          if (task._timer.isDefined) throw new IllegalArgumentException("The same task may not be scheduled twice.")
          task._timer = Some(this)
          val timeMillis = System.nanoTime() / 1000000
          var executeTimeMillis = timeMillis + delaySeconds.toMillis
          if (currentThread.pauseTimeMillis > 0) executeTimeMillis -= timeMillis - currentThread.pauseTimeMillis
          task._executeTimeMillis = executeTimeMillis
          task._intervalMillis = intervalSeconds.toMillis
          task._repeatCount = repeatCount
          tasks.add(task)
        }
      }
      currentThread.wakeUp()
    }
    task
  }

  def scheduleTask(task: Timer.Task): Timer.Task = scheduleTask(task, Seconds.zero)

  def stop(): Unit =
    threadLock.synchronized {
      if (thread().instances.removeValue(this)) stopTimeMillis = System.nanoTime() / 1000000
    }

  def start(): Unit =
    threadLock.synchronized {
      val currentThread = thread()
      val instances = currentThread.instances
      if (!instances.contains(this)) {
        instances.add(this)
        if (stopTimeMillis > 0) {
          delay(System.nanoTime() / 1000000 - stopTimeMillis)
          stopTimeMillis = 0
        }
        currentThread.wakeUp()
      }
    }

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

  def isEmpty: Boolean = synchronized(tasks.isEmpty())

  private[Timer] def update(thread: TimerThread, timeMillis: Long, waitMillis: Long): Long = synchronized {
    var currentWaitMillis = waitMillis
    var i = 0
    while (i < tasks.size) {
      val task = tasks.get(i)
      task.synchronized {
        if (task._executeTimeMillis > timeMillis) {
          currentWaitMillis = scala.math.min(currentWaitMillis, task._executeTimeMillis - timeMillis)
          i += 1
        } else {
          if (task._repeatCount == 0) {
            task._timer = None
            tasks.removeIndex(i)
          } else {
            task._executeTimeMillis = timeMillis + task._intervalMillis
            currentWaitMillis = scala.math.min(currentWaitMillis, task._intervalMillis)
            if (task._repeatCount > 0) task._repeatCount -= 1
            i += 1
          }
          thread.addPostedTask(task)
        }
      }
    }
    currentWaitMillis
  }

  def delay(delayMillis: Long): Unit = synchronized {
    tasks.foreach { task =>
      task.synchronized {
        task._executeTimeMillis += delayMillis
      }
    }
  }
}

object Timer {
  private val threadLock = new Object()
  private var currentThread: Option[TimerThread] = None

  private[sge] def disposeThread(): Unit =
    threadLock.synchronized {
      currentThread.foreach(_.dispose())
      currentThread = None
    }

  def instance()(using sge.Sge): Timer =
    threadLock.synchronized {
      val t = thread()
      if (t._instance.isEmpty) t._instance = Some(Timer())
      t._instance.get
    }

  private[utils] def thread()(using sge.Sge): TimerThread =
    threadLock.synchronized {
      if (currentThread.isEmpty || currentThread.get.files != sge.Sge().files) {
        currentThread.foreach(_.dispose())
        currentThread = Some(TimerThread())
      }
      currentThread.get
    }

  def post(task: Task)(using sge.Sge): Task = instance().postTask(task)
  def schedule(task: Task, delaySeconds: Seconds)(using sge.Sge): Task = instance().scheduleTask(task, delaySeconds)
  def schedule(task: Task, delaySeconds: Seconds, intervalSeconds: Seconds)(using sge.Sge): Task = instance().scheduleTask(task, delaySeconds, intervalSeconds, -1)
  def schedule(task: Task, delaySeconds: Seconds, intervalSeconds: Seconds, repeatCount: Int)(using sge.Sge): Task = instance().scheduleTask(task, delaySeconds, intervalSeconds, repeatCount)

  abstract class Task(using sge.Sge) extends Runnable {
    private[Timer] var _executeTimeMillis: Long = 0
    private[Timer] var _intervalMillis: Long = 0
    private[Timer] var _repeatCount: Int = 0
    @volatile private[Timer] var _timer: Option[Timer] = None

    def run(): Unit

    def cancel(): Unit =
      threadLock.synchronized {
        thread().removePostedTask(this)
        _timer.foreach { t =>
          t.synchronized {
            t.tasks.removeValue(this)
            reset()
          }
        }
        if (_timer.isEmpty) reset()
      }

    private[Timer] def reset(): Unit = synchronized {
      _executeTimeMillis = 0
      _timer = None
    }

    def isScheduled: Boolean = _timer.isDefined
    def executeTime: Long = synchronized(_executeTimeMillis)
    def executeTimeMillis: Long = synchronized(_executeTimeMillis)
    def intervalMillis: Long = _intervalMillis
    def repeatCount: Int = _repeatCount
  }

  private[utils] class TimerThread(using sge.Sge) extends sge.LifecycleListener {
    val files = sge.Sge().files
    val instances = DynamicArray[Timer]()
    var _instance: Option[Timer] = None
    var pauseTimeMillis: Long = 0

    val postedTasks = DynamicArray[Task]()
    private val runTasks = DynamicArray[Task]()
    private val runPostedTasksRunnable: Runnable = () => runPostedTasks()

    private var loopHandle: Nullable[TimerPlatformOps.LoopHandle] = Nullable.empty

    sge.Sge().application.addLifecycleListener(this)
    resume()

    loopHandle = Nullable(
      TimerPlatformOps.runLoop(
        lock = threadLock,
        step = () => loopStep(),
        onDone = () => dispose()
      )
    )

    def wakeUp(): Unit = loopHandle.foreach(_.wakeUp())

    private def loopStep(): Long = threadLock.synchronized {
      if (!currentThread.contains(this) || files != sge.Sge().files) -1L
      else {
        var waitMillis = 5000L
        if (pauseTimeMillis == 0) {
          val timeMillis = System.nanoTime() / 1000000
          var i = 0
          while (i < instances.size) {
            try waitMillis = instances.get(i).update(this, timeMillis, waitMillis)
            catch { case ex: Throwable => throw new RuntimeException("Task failed: " + instances.get(i).getClass.getName, ex) }
            i += 1
          }
        }
        if (!currentThread.contains(this) || files != sge.Sge().files) -1L
        else waitMillis
      }
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
        if (postedTasks.isEmpty()) sge.Sge().application.postRunnable(runPostedTasksRunnable)
        postedTasks.add(task)
      }

    def removePostedTask(task: Task): Unit =
      postedTasks.synchronized { postedTasks.removeValue(task) }

    def resume(): Unit =
      threadLock.synchronized {
        val delayMillis = System.nanoTime() / 1000000 - pauseTimeMillis
        instances.foreach(_.delay(delayMillis))
        pauseTimeMillis = 0
        wakeUp()
      }

    def pause(): Unit =
      threadLock.synchronized {
        pauseTimeMillis = System.nanoTime() / 1000000
        wakeUp()
      }

    def dispose(): Unit = {
      threadLock.synchronized {
        postedTasks.synchronized { postedTasks.clear() }
        if (currentThread.exists(_ == this)) currentThread = None
        instances.clear()
        wakeUp()
      }
      sge.Sge().application.removeLifecycleListener(this)
    }
  }
}
