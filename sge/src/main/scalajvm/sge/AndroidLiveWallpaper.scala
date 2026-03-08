/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidLiveWallpaper.java
 * Original authors: mzechner, Jaroslaw Wisniewski
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidLiveWallpaper (same name, different package)
 *   Renames: Gdx.* globals -> Sge context; GdxRuntimeException -> SgeError
 *   Convention: delegates to ops interfaces; no WallpaperService subclass in sge core
 *   Idiom: split packages; Nullable; no return (boundary/break)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.net.AndroidNet
import sge.platform.android._
import sge.utils.{ DynamicArray, Nullable, ObjectMap }

/** An [[Application]] implementation for Android live wallpapers.
  *
  * Unlike the Activity-based [[AndroidApplication]], live wallpapers are driven by a WallpaperService. Multiple wallpaper engines can exist simultaneously, sharing this single Application instance.
  * The service delegates lifecycle events through [[LiveWallpaperAppCallbacks]].
  *
  * @param config
  *   the Android application configuration
  * @param provider
  *   the Android platform provider (creates all ops instances)
  * @param serviceOps
  *   the live wallpaper service operations
  */
class AndroidLiveWallpaper(
  private val config:     AndroidConfigOps,
  private val provider:   AndroidPlatformProvider,
  private val serviceOps: LiveWallpaperServiceOps
) extends Application
    with LiveWallpaperAppCallbacks {

  // ── Subsystems ──────────────────────────────────────────────────────

  private val context:    AnyRef         = serviceOps.getContext()
  private val _clipboard: ClipboardOps   = provider.createClipboard(context)
  private val _filesOps:  FilesOps       = provider.createFiles(context, false)
  private val _audioOps:  AudioEngineOps = provider.createAudioEngine(context, config)
  private val _net:       AndroidNet     = AndroidNet(provider, context)

  private val _files: AndroidFiles = AndroidFiles(_filesOps)
  private val _audio: AndroidAudio = AndroidAudio(_audioOps)

  /** The [[Sge]] context for this application. Set after all subsystems are initialized. */
  @volatile var sgeContext: Sge = null.asInstanceOf[Sge] // scalafix:ok

  // ── State ─────────────────────────────────────────────────────────────

  private val runnables:          DynamicArray[Runnable]          = DynamicArray[Runnable](4)
  private val executedRunnables:  DynamicArray[Runnable]          = DynamicArray[Runnable](4)
  private val lifecycleListeners: DynamicArray[LifecycleListener] = DynamicArray[LifecycleListener](4)
  private val preferences:        ObjectMap[String, Preferences]  = ObjectMap[String, Preferences]()

  @volatile private[sge] var running: Boolean = true
  private var firstResume:            Boolean = true

  // Register with the service
  serviceOps.setAppCallbacks(this)

  // ── Application trait ─────────────────────────────────────────────────

  override def getApplicationListener(): ApplicationListener =
    throw utils.SgeError.InvalidInput("Live wallpaper has no single ApplicationListener — use wallpaper callbacks")

  override def getGraphics(): Graphics =
    throw utils.SgeError.InvalidInput("AndroidLiveWallpaper.getGraphics() not yet wired — use Sge context")

  override def getAudio(): Audio = _audio

  override def getInput(): Input =
    throw utils.SgeError.InvalidInput("AndroidLiveWallpaper.getInput() not yet wired — use Sge context")

  override def getFiles(): Files = _files

  override def getNet(): Net = _net

  override def getType(): Application.ApplicationType = Application.ApplicationType.Android

  override def getVersion(): Int = 0 // overridden by lifecycle when available

  override def getJavaHeap(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

  override def getNativeHeap(): Long = 0L // android.os.Debug not accessible without lifecycle ops

  override def getPreferences(name: String): Preferences =
    preferences
      .get(name)
      .fold {
        val prefs = AndroidPreferencesAdapter(provider.createPreferences(context, name))
        preferences.put(name, prefs)
        prefs
      }(identity)

  override def getClipboard(): utils.Clipboard = AndroidClipboardAdapter(_clipboard)

  override def postRunnable(runnable: Runnable): Unit = runnables.synchronized {
    runnables += runnable
  }

  override def exit(): Unit = {
    // Live wallpapers cannot exit
  }

  override def addLifecycleListener(listener: LifecycleListener): Unit = lifecycleListeners.synchronized {
    lifecycleListeners += listener
  }

  override def removeLifecycleListener(listener: LifecycleListener): Unit = lifecycleListeners.synchronized {
    lifecycleListeners -= listener
  }

  // ── LiveWallpaperAppCallbacks ─────────────────────────────────────────

  override def onSurfaceCreated(holder: AnyRef, isFirstEngine: Boolean): Unit = {
    // Surface created — GL context available after this
  }

  override def onSurfaceChanged(holder: AnyRef, format: Int, width: Int, height: Int): Unit = {
    // Surface dimensions changed
  }

  override def onSurfaceDestroyed(holder: AnyRef, isLastEngine: Boolean): Unit = {
    // Surface going away
  }

  override def onVisibilityChanged(visible: Boolean, visibleEngineCount: Int): Unit =
    if (visible && visibleEngineCount == 1) onResume()
    else if (!visible && visibleEngineCount == 0) onPause()

  override def onOffsetsChanged(
    xOffset:      Float,
    yOffset:      Float,
    xOffsetStep:  Float,
    yOffsetStep:  Float,
    xPixelOffset: Int,
    yPixelOffset: Int
  ): Unit = {
    // Subclasses or listener can handle offset changes via postRunnable
  }

  override def onPreviewStateChanged(isPreview: Boolean): Unit = {
    // Subclasses or listener can handle preview state changes
  }

  override def onIconDropped(x: Int, y: Int): Unit = {
    // Subclasses or listener can handle icon drops
  }

  override def onDeepPause(): Unit = {
    // Free native resources — managed caches cleared here
  }

  override def onServiceDestroy(): Unit =
    onDestroy()

  // ── Lifecycle ─────────────────────────────────────────────────────────

  /** Resume subsystems after wallpaper becomes visible. */
  private def onResume(): Unit =
    if (!firstResume) {
      _audioOps.resumeAll()
    } else {
      firstResume = false
    }

  /** Pause subsystems when wallpaper is no longer visible.
    *
    * Note: unlike Activity-based apps, graphics.pause() is NOT called for live wallpapers because it can cause deadlocks on some devices (notably Samsung Galaxy Tab on Android 4.0.4). Audio is paused
    * directly instead.
    */
  private def onPause(): Unit =
    _audioOps.pauseAll()

  /** Destroy subsystems when the service is dying. */
  private[sge] def onDestroy(): Unit = {
    running = false
    _audioOps.dispose()
    lifecycleListeners.synchronized {
      lifecycleListeners.foreach(_.dispose())
    }
  }

  /** Execute pending runnables. Called from the render loop. */
  def executeRunnables(): Unit = {
    runnables.synchronized {
      executedRunnables.addAll(runnables)
      runnables.clear()
    }
    executedRunnables.foreach { r =>
      try r.run()
      catch { case t: Throwable => scribe.error("Exception in runnable", t) }
    }
    executedRunnables.clear()
  }
}
