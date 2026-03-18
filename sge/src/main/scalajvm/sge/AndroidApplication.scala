/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidApplication.java
 * Original authors: mzechner, jshapcot
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidApplication (same name, different package)
 *   Renames: Gdx.* globals -> Sge context; GdxRuntimeException -> SgeError
 *   Convention: delegates to ops interfaces; no Activity subclass needed in sge core
 *   Idiom: split packages; Nullable; no return (boundary/break)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.files.AndroidFileHandle
import sge.net.AndroidNet
import sge.platform.android._
import sge.utils.{ DynamicArray, Nullable, ObjectMap }

/** An [[Application]] implementation for Android.
  *
  * Unlike LibGDX's AndroidApplication (which extends Activity), this is a plain class that delegates all Android-specific operations to ops interfaces from `sge.platform.android`. The user's Activity
  * creates this instance and forwards lifecycle callbacks to it.
  *
  * @param listenerFactory
  *   a context function that creates the application listener when given an [[Sge]] context
  * @param config
  *   the Android application configuration
  * @param provider
  *   the Android platform provider (creates all ops instances)
  * @param lifecycle
  *   lifecycle operations (UI thread, immersive mode, etc.)
  * @param context
  *   the Android Context (as AnyRef)
  * @param useExternalFiles
  *   whether to enable external storage access
  */
class AndroidApplication(
  private val listenerFactory:  Sge ?=> ApplicationListener,
  private val config:           AndroidConfigOps,
  private val provider:         AndroidPlatformProvider,
  private val lifecycle:        AndroidLifecycleOps,
  private val context:          AnyRef,
  private val useExternalFiles: Boolean = false
) extends Application {

  // ── Subsystems ──────────────────────────────────────────────────────

  private val _clipboard: ClipboardOps   = provider.createClipboard(context)
  private val _filesOps:  FilesOps       = provider.createFiles(context, useExternalFiles)
  private val _audioOps:  AudioEngineOps = provider.createAudioEngine(context, config)
  private val _net:       AndroidNet     = AndroidNet(provider, context)

  // Subsystem facades
  private val _files: AndroidFiles = AndroidFiles(_filesOps)
  private val _audio: AndroidAudio = AndroidAudio(_audioOps)

  // Graphics and Input — initialized via initializeGraphicsAndInput() after GL surface is created
  @volatile private var _graphics: AndroidGraphics = scala.compiletime.uninitialized
  @volatile private var _input:    AndroidInput    = scala.compiletime.uninitialized

  /** The [[Sge]] context for this application. Set after all subsystems are initialized. */
  @volatile var sgeContext: Sge = null.asInstanceOf[Sge] // scalafix:ok

  /** The materialized application listener. Available after [[initializeSge]]. */
  private var _listener: ApplicationListener = scala.compiletime.uninitialized

  /** The application listener for this application. Available after [[initializeSge]]. */
  def listener: ApplicationListener = _listener

  // ── State ─────────────────────────────────────────────────────────────

  private val runnables:          DynamicArray[Runnable]          = DynamicArray[Runnable](4)
  private val executedRunnables:  DynamicArray[Runnable]          = DynamicArray[Runnable](4)
  private val lifecycleListeners: DynamicArray[LifecycleListener] = DynamicArray[LifecycleListener](4)
  private val preferences:        ObjectMap[String, Preferences]  = ObjectMap[String, Preferences]()

  @volatile private[sge] var running: Boolean = true

  // ── Application trait ─────────────────────────────────────────────────

  override def applicationListener: ApplicationListener = _listener

  override def graphics: Graphics = _graphics

  override def audio: Audio = _audio

  override def input: Input = _input

  override def files: Files = _files

  override def net: Net = _net

  override def applicationType: Application.ApplicationType = Application.ApplicationType.Android

  override def version: Int = lifecycle.getAndroidVersion()

  override def javaHeap: Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

  override def nativeHeap: Long = lifecycle.getNativeHeapAllocatedSize()

  override def getPreferences(name: String): Preferences =
    preferences
      .get(name)
      .fold {
        val prefs = AndroidPreferencesAdapter(provider.createPreferences(context, name))
        preferences.put(name, prefs)
        prefs
      }(identity)

  override def clipboard: utils.Clipboard = AndroidClipboardAdapter(_clipboard)

  override def postRunnable(runnable: Runnable): Unit = runnables.synchronized {
    runnables += runnable
  }

  override def exit(): Unit = {
    running = false
    lifecycle.finish()
  }

  override def addLifecycleListener(listener: LifecycleListener): Unit = lifecycleListeners.synchronized {
    lifecycleListeners += listener
  }

  override def removeLifecycleListener(listener: LifecycleListener): Unit = lifecycleListeners.synchronized {
    lifecycleListeners -= listener
  }

  // ── Lifecycle callbacks (called by the host Activity) ─────────────────

  /** Called from Activity.onResume(). Resumes subsystems and notifies listeners. */
  def onResume(): Unit = {
    _audioOps.resumeAll()
    lifecycle.resumeGLSurfaceView()
    lifecycleListeners.synchronized {
      lifecycleListeners.foreach(_.resume())
    }
  }

  /** Called from Activity.onPause(). Pauses subsystems and notifies listeners. */
  def onPause(): Unit = {
    lifecycle.pauseGLSurfaceView()
    _audioOps.pauseAll()
    lifecycleListeners.synchronized {
      lifecycleListeners.foreach(_.pause())
    }
  }

  /** Called from Activity.onDestroy(). Disposes subsystems and notifies listeners. */
  def onDestroy(): Unit = {
    running = false
    _audioOps.dispose()
    lifecycleListeners.synchronized {
      lifecycleListeners.foreach(_.dispose())
    }
  }

  /** Build and store the [[Sge]] context from initialized subsystems.
    *
    * Must be called after [[initializeGraphicsAndInput]] so that graphics and input are available.
    *
    * @return
    *   the newly created Sge context
    */
  def initializeSge(): Sge = {
    val sge = Sge(
      application = this,
      graphics = _graphics,
      audio = _audio,
      files = _files,
      input = _input,
      net = _net
    )
    sgeContext = sge
    given Sge = sge
    _listener = listenerFactory
    sge
  }

  /** Initialize graphics and input subsystems. Call after the GL surface view is created.
    *
    * @param windowManager
    *   the Android WindowManager (as AnyRef) for display metrics
    * @param handler
    *   the Android Handler (as AnyRef) for UI thread posting
    * @param resolutionStrategy
    *   the resolution strategy for measuring GL surface size
    * @return
    *   the GL surface view (as AnyRef — cast to `android.opengl.GLSurfaceView`) for setting as content view
    */
  def initializeGraphicsAndInput(
    windowManager:      AnyRef,
    handler:            AnyRef,
    resolutionStrategy: ResolutionStrategyOps = null // scalafix:ok
  ): AnyRef = {
    val displayMetrics = provider.createDisplayMetrics(windowManager)
    val sensorOps      = provider.createSensors(context, windowManager)
    val inputMethodOps = provider.createInputMethod(context, handler)
    val hapticsOps     = provider.createHaptics(context)
    val touchInputOps  = provider.createTouchInput(context)
    val cursorOps      = provider.createCursor(context)

    val rs        = if (resolutionStrategy != null) resolutionStrategy else FillResolutionStrategy // scalafix:ok
    val glSurface = provider.createGLSurfaceView(context, config, rs)

    _graphics = AndroidGraphics(config, provider, displayMetrics, glSurface, cursorOps)
    _input = AndroidInput(config, sensorOps, inputMethodOps, hapticsOps, touchInputOps, lifecycle)
    _input.registerSensors()
    lifecycle.setGLSurfaceView(glSurface)
    glSurface.view
  }

  /** Process touch/scroll/key events through the input processor. Call once per frame. */
  def processInputEvents(): Unit =
    if (_input != null) _input.processEvents() // scalafix:ok

  /** Execute pending runnables. Called from the render loop. */
  def executeRunnables(): Unit = {
    runnables.synchronized {
      executedRunnables.addAll(runnables)
      runnables.clear()
    }
    executedRunnables.foreach { r =>
      try r.run()
      catch { case t: Throwable => utils.Log.error(s"Exception in runnable: ${t.getMessage}", t) }
    }
    executedRunnables.clear()
  }
}

// ── Minimal adapters ──────────────────────────────────────────────────

/** Adapts [[FilesOps]] to [[Files]] trait. */
private[sge] class AndroidFiles(ops: FilesOps) extends Files {
  import sge.files._

  override def getFileHandle(path: String, fileType: FileType): FileHandle =
    AndroidFileHandle(new java.io.File(path), fileType, ops)

  override def classpath(path: String): FileHandle = getFileHandle(path, FileType.Classpath)
  override def internal(path:  String): FileHandle = getFileHandle(path, FileType.Internal)
  override def external(path:  String): FileHandle = getFileHandle(path, FileType.External)
  override def absolute(path:  String): FileHandle = getFileHandle(path, FileType.Absolute)
  override def local(path:     String): FileHandle = getFileHandle(path, FileType.Local)

  override def externalStoragePath: String = {
    val ext = ops.externalStoragePath
    if (ext != null) ext else ""
  }

  override def isExternalStorageAvailable: Boolean = ops.externalStoragePath != null

  override def localStoragePath: String = ops.localStoragePath

  override def isLocalStorageAvailable: Boolean = true
}

/** Adapts [[AudioEngineOps]] to [[Audio]] trait. Creates adapter instances that delegate to ops. */
private[sge] class AndroidAudio(ops: AudioEngineOps) extends Audio {

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): audio.AudioDevice =
    AndroidAudioDeviceAdapter(ops.newAudioDevice(samplingRate, isMono))

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): audio.AudioRecorder =
    AndroidAudioRecorderAdapter(ops.newAudioRecorder(samplingRate, isMono))

  override def newSound(fileHandle: files.FileHandle): audio.Sound = {
    val fh = fileHandle
    fh.fileType match {
      case files.FileType.Internal =>
        // Internal files need file descriptor access (Android assets)
        val file = fh.internalFile
        // For non-asset files, use path-based loading
        AndroidSoundAdapter(ops.newSoundFromPath(file.getPath()))
      case _ =>
        AndroidSoundAdapter(ops.newSoundFromPath(fh.internalFile.getAbsolutePath()))
    }
  }

  override def newMusic(file: files.FileHandle): audio.Music = {
    val fh = file
    fh.fileType match {
      case files.FileType.Internal =>
        AndroidMusicAdapter(ops.newMusicFromPath(fh.internalFile.getPath()))
      case _ =>
        AndroidMusicAdapter(ops.newMusicFromPath(fh.internalFile.getAbsolutePath()))
    }
  }

  override def switchOutputDevice(deviceIdentifier: Nullable[String]): Boolean =
    deviceIdentifier.fold(false)(ops.switchOutputDevice)

  override def availableOutputDevices: Array[String] = ops.availableOutputDevices
}

/** Adapts [[PreferencesOps]] to [[Preferences]] trait. */
private[sge] class AndroidPreferencesAdapter(ops: PreferencesOps) extends Preferences {
  override def putBoolean(key: String, value: Boolean):                                        Preferences = { ops.putBoolean(key, value); this }
  override def putInteger(key: String, value: Int):                                            Preferences = { ops.putInteger(key, value); this }
  override def putLong(key:    String, value: Long):                                           Preferences = { ops.putLong(key, value); this }
  override def putFloat(key:   String, value: Float):                                          Preferences = { ops.putFloat(key, value); this }
  override def putString(key:  String, value: String):                                         Preferences = { ops.putString(key, value); this }
  override def put(vals: scala.collection.Map[String, Boolean | Int | Long | Float | String]): Preferences = {
    // Convert Scala map entries to individual puts
    vals.foreach { (k, v) =>
      v match {
        case b: Boolean => ops.putBoolean(k, b)
        case i: Int     => ops.putInteger(k, i)
        case l: Long    => ops.putLong(k, l)
        case f: Float   => ops.putFloat(k, f)
        case s: String  => ops.putString(k, s)
      }
    }
    this
  }
  override def getBoolean(key: String):                    Boolean                                                             = ops.getBoolean(key, false)
  override def getInteger(key: String):                    Int                                                                 = ops.getInteger(key, 0)
  override def getLong(key:    String):                    Long                                                                = ops.getLong(key, 0L)
  override def getFloat(key:   String):                    Float                                                               = ops.getFloat(key, 0f)
  override def getString(key:  String):                    String                                                              = ops.getString(key, "")
  override def getBoolean(key: String, defValue: Boolean): Boolean                                                             = ops.getBoolean(key, defValue)
  override def getInteger(key: String, defValue: Int):     Int                                                                 = ops.getInteger(key, defValue)
  override def getLong(key:    String, defValue: Long):    Long                                                                = ops.getLong(key, defValue)
  override def getFloat(key:   String, defValue: Float):   Float                                                               = ops.getFloat(key, defValue)
  override def getString(key:  String, defValue: String):  String                                                              = ops.getString(key, defValue)
  override def get():                                      scala.collection.Map[String, Boolean | Int | Long | Float | String] = {
    val all     = ops.getAll
    val builder = scala.collection.mutable.Map.empty[String, Boolean | Int | Long | Float | String]
    val it      = all.entrySet().iterator()
    while (it.hasNext()) {
      val entry = it.next()
      val v     = entry.getValue()
      v match {
        case b: java.lang.Boolean => builder(entry.getKey()) = b.booleanValue()
        case i: java.lang.Integer => builder(entry.getKey()) = i.intValue()
        case l: java.lang.Long    => builder(entry.getKey()) = l.longValue()
        case f: java.lang.Float   => builder(entry.getKey()) = f.floatValue()
        case s: String            => builder(entry.getKey()) = s
        case _ => () // skip unknown types
      }
    }
    builder
  }
  override def contains(key: String): Boolean = ops.contains(key)
  override def clear():               Unit    = ops.clear()
  override def remove(key:   String): Unit    = ops.remove(key)
  override def flush():               Unit    = ops.flush()
}

/** Adapts [[ClipboardOps]] to [[sge.utils.Clipboard]] trait. */
private[sge] class AndroidClipboardAdapter(ops: ClipboardOps) extends utils.Clipboard {
  override def hasContents:                           Boolean          = ops.hasContents
  override def contents:                              Nullable[String] = Nullable(ops.getContents)
  override def contents_=(content: Nullable[String]): Unit             =
    content.fold(ops.setContents(""))(ops.setContents)
}

// ── Audio adapters ──────────────────────────────────────────────────

/** Adapts [[SoundOps]] to [[audio.Sound]] trait. */
private[sge] class AndroidSoundAdapter(ops: SoundOps) extends audio.Sound {
  import audio.{ Pan, Pitch, SoundId, Volume }

  override def play():                                       SoundId = SoundId(ops.play(1f))
  override def play(volume: Volume):                         SoundId = SoundId(ops.play(volume.toFloat))
  override def play(volume: Volume, pitch: Pitch, pan: Pan): SoundId = SoundId(ops.play(volume.toFloat, pitch.toFloat, pan.toFloat))

  override def loop():                                       SoundId = SoundId(ops.loop(1f))
  override def loop(volume: Volume):                         SoundId = SoundId(ops.loop(volume.toFloat))
  override def loop(volume: Volume, pitch: Pitch, pan: Pan): SoundId = SoundId(ops.loop(volume.toFloat, pitch.toFloat, pan.toFloat))

  override def stop():                   Unit = ops.stop()
  override def stop(soundId:   SoundId): Unit = ops.stop(soundId.toLong)
  override def pause():                  Unit = ops.pause()
  override def pause(soundId:  SoundId): Unit = ops.pause(soundId.toLong)
  override def resume():                 Unit = ops.resume()
  override def resume(soundId: SoundId): Unit = ops.resume(soundId.toLong)

  override def setLooping(soundId: SoundId, looping: Boolean):             Unit = ops.setLooping(soundId.toLong, looping)
  override def setPitch(soundId:   SoundId, pitch:   Pitch):               Unit = ops.setPitch(soundId.toLong, pitch.toFloat)
  override def setVolume(soundId:  SoundId, volume:  Volume):              Unit = ops.setVolume(soundId.toLong, volume.toFloat)
  override def setPan(soundId:     SoundId, pan:     Pan, volume: Volume): Unit = ops.setPan(soundId.toLong, pan.toFloat, volume.toFloat)

  override def close(): Unit = ops.dispose()
}

/** Adapts [[MusicOps]] to [[audio.Music]] trait. */
private[sge] class AndroidMusicAdapter(ops: MusicOps) extends audio.Music {
  import audio.{ Pan, Position, Volume }

  override def play():  Unit = ops.play()
  override def pause(): Unit = ops.pause()
  override def stop():  Unit = ops.stop()

  override def playing:                       Boolean = ops.playing
  override def looping:                       Boolean = ops.looping
  override def looping_=(isLooping: Boolean): Unit    = ops.looping = isLooping

  override def volume:              Volume = Volume.unsafeMake(ops.volume)
  override def volume_=(v: Volume): Unit   = ops.volume = v.toFloat

  override def setPan(pan: Pan, volume: Volume): Unit = ops.setPan(pan.toFloat, volume.toFloat)

  override def position:                Position = Position.unsafeMake(ops.position)
  override def position_=(p: Position): Unit     = ops.position = p.toFloatSeconds

  override def onComplete(listener: audio.Music => Unit): Unit =
    ops.onComplete(() => listener(this))

  override def close(): Unit = ops.dispose()
}

/** Adapts [[AudioDeviceOps]] to [[audio.AudioDevice]] trait. */
private[sge] class AndroidAudioDeviceAdapter(ops: AudioDeviceOps) extends audio.AudioDevice {
  override def isMono:                                                            Boolean = ops.isMono
  override def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit    = ops.writeSamples(samples, offset, numSamples)
  override def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit    = ops.writeSamples(samples, offset, numSamples)
  override def latency:                                                           Int     = 0 // Not available through AudioDeviceOps currently
  override def setVolume(volume:     audio.Volume):                               Unit    = ops.setVolume(volume.toFloat)
  override def pause():                                                           Unit    = ops.pause()
  override def resume():                                                          Unit    = ops.resume()
  override def close():                                                           Unit    = ops.dispose()
}

/** Adapts [[AudioRecorderOps]] to [[audio.AudioRecorder]] trait. */
private[sge] class AndroidAudioRecorderAdapter(ops: AudioRecorderOps) extends audio.AudioRecorder {
  override def read(samples: Array[Short], offset: Int, numSamples: Int): Unit = {
    val recorded = ops.read(numSamples)
    System.arraycopy(recorded, 0, samples, offset, Math.min(recorded.length, numSamples))
  }
  override def close(): Unit = ops.dispose()
}
