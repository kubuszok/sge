/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessApplication.java
 * Original authors: Jon Renner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessApplication -> HeadlessApplication (same name)
 *   Renames: Gdx.app/files/net/audio/graphics/input globals -> Sge context
 *   Renames: GdxRuntimeException -> SgeError; Array<T> -> DynamicArray[T]
 *   Convention: main loop uses boundary/break instead of return; synchronized blocks for thread safety
 *   Idiom: Nullable for clipboard; split packages
 *   Audited: 2026-03-05
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.files.{ DesktopFiles, DesktopPreferences }
import sge.net.DesktopNet
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.utils.{ DynamicArray, Nanos, Nullable, ObjectMap, TimeUtils }
import scala.util.boundary
import scala.util.boundary.break

/** A headless [[Application]] primarily intended for servers and testing. No graphics context is created — all rendering-related calls go through [[NoopGraphics]].
  *
  * @param listenerFactory
  *   a context function that creates the application listener when given an [[Sge]] context
  * @param config
  *   optional configuration (defaults to 60 updates/second)
  * @author
  *   Jon Renner (original implementation)
  */
class HeadlessApplication(
  listenerFactory: Sge ?=> ApplicationListener,
  config:          HeadlessApplicationConfig = HeadlessApplicationConfig()
) extends Application {

  private val _files:    DesktopFiles = DesktopFiles()
  private val _graphics: NoopGraphics = NoopGraphics()
  private val _audio:    NoopAudio    = NoopAudio()
  private val _input:    NoopInput    = NoopInput()
  private val _net:      DesktopNet   = DesktopNet(this)

  /** The [[Sge]] context for this application. Pass as `given` to code that needs it. */
  val sgeContext: Sge = Sge(this, _graphics, _audio, _files, _input, _net)

  private val listener: ApplicationListener = { given Sge = sgeContext; listenerFactory }

  private val runnables:          DynamicArray[Runnable]          = DynamicArray[Runnable](4)
  private val executedRunnables:  DynamicArray[Runnable]          = DynamicArray[Runnable](4)
  private val lifecycleListeners: DynamicArray[LifecycleListener] = DynamicArray[LifecycleListener](4)
  private val preferences:        ObjectMap[String, Preferences]  = ObjectMap[String, Preferences]()
  private val preferencesDir:     String                          = config.preferencesDirectory

  @volatile private var running: Boolean = true

  private val mainLoopThread: Thread = Thread(
    () =>
      try mainLoop()
      catch {
        case re: RuntimeException => throw re
        case t:  Throwable        => throw utils.SgeError.InvalidInput(t.getMessage, Some(t))
      },
    "HeadlessApplication"
  )

  mainLoopThread.start()

  // ---- main loop ----

  private def mainLoop(): Unit = {
    listener.create()

    val targetInterval = Nanos(targetRenderInterval())
    if (targetInterval < Nanos.zero) {
      // Negative updatesPerSecond — never call render
      awaitExit()
    } else {
      var t = TimeUtils.nanoTime() + targetInterval
      boundary {
        while (running) {
          if (targetInterval > Nanos.zero) {
            val n = TimeUtils.nanoTime()
            if (t > n) {
              val sleep = t - n
              try Thread.sleep(sleep.toLong / 1000000L, (sleep.toLong % 1000000L).toInt)
              catch { case _: InterruptedException => () }
              t = t + targetInterval
            } else {
              t = n + targetInterval
            }
          }

          executeRunnables()
          _graphics.updateTime()
          listener.render()

          if (!running) break(())
        }
      }
    }

    // Shutdown lifecycle listeners
    lifecycleListeners.synchronized {
      lifecycleListeners.foreach { ll =>
        ll.pause()
        ll.dispose()
      }
    }
    listener.pause()
    listener.dispose()
  }

  /** Returns the target render interval in nanoseconds based on updatesPerSecond. Negative means don't render; zero means no sleep.
    */
  private def targetRenderInterval(): Long =
    if (config.updatesPerSecond <= 0) config.updatesPerSecond.toLong
    else 1000000000L / config.updatesPerSecond

  /** Block until exit() is called (for negative updatesPerSecond). */
  private def awaitExit(): Unit =
    boundary {
      while (running) {
        try Thread.sleep(100L)
        catch { case _: InterruptedException => () }
        executeRunnables()
        if (!running) break(())
      }
    }

  /** Executes all pending runnables. Returns true if any were executed. */
  def executeRunnables(): Boolean = {
    runnables.synchronized {
      var i = 0
      while (i < runnables.size) {
        executedRunnables.add(runnables(i))
        i += 1
      }
      runnables.clear()
    }
    if (executedRunnables.size == 0) false
    else {
      var i = 0
      while (i < executedRunnables.size) {
        executedRunnables(i).run()
        i += 1
      }
      executedRunnables.clear()
      true
    }
  }

  // ---- Application trait ----

  override def applicationListener: ApplicationListener = listener

  override def graphics: Graphics = _graphics

  override def audio: Audio = _audio

  override def input: Input = _input

  override def files: Files = _files

  override def net: Net = _net

  override def applicationType: Application.ApplicationType = Application.ApplicationType.HeadlessDesktop

  override def version: Int = 0

  override def javaHeap: Long =
    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

  override def nativeHeap: Long = javaHeap

  override def getPreferences(name: String): Preferences =
    preferences
      .get(name)
      .fold {
        val prefs = DesktopPreferences(name, preferencesDir, DesktopFiles.externalPath)
        preferences.put(name, prefs)
        prefs
      }(identity)

  override def clipboard: utils.Clipboard =
    HeadlessApplication.NoopClipboard

  override def postRunnable(runnable: Runnable): Unit =
    runnables.synchronized {
      runnables.add(runnable)
    }

  override def exit(): Unit =
    postRunnable(() => running = false)

  override def addLifecycleListener(listener: LifecycleListener): Unit =
    lifecycleListeners.synchronized {
      lifecycleListeners.add(listener)
    }

  override def removeLifecycleListener(listener: LifecycleListener): Unit =
    lifecycleListeners.synchronized {
      lifecycleListeners.removeValue(listener)
    }

}

object HeadlessApplication {

  private object NoopClipboard extends utils.Clipboard {
    def hasContents:                           Boolean          = false
    def contents:                              Nullable[String] = Nullable.empty
    def contents_=(content: Nullable[String]): Unit             = ()
  }
}
