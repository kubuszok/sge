// SGE — Asynchronous Android audio engine wrapper
//
// Decorates an AudioEngineOps, posting sound play/loop/stop operations
// to a background HandlerThread to avoid blocking the main/GL thread.
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AsynchronousAndroidAudio
//   Renames: AsynchronousAndroidAudio → AsynchronousAndroidAudioImpl
//   Convention: decorator pattern on AudioEngineOps; HandlerThread for threading
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.os.{Handler, HandlerThread}

/** Asynchronous decorator for [[AudioEngineOps]] that wraps newly created sounds with [[AsynchronousSoundOps]].
  *
  * A dedicated [[HandlerThread]] is started at construction. All sounds created by `newSoundFromFd` and `newSoundFromPath` are
  * wrapped so that play/loop/stop operations are posted to the background thread, preventing the main/GL thread from blocking on
  * SoundPool calls.
  *
  * @param delegate
  *   the underlying audio engine to delegate to
  * @param maxStreams
  *   the maximum number of simultaneous streams (used as circular buffer size in [[AsynchronousSoundOps]])
  */
class AsynchronousAndroidAudioImpl(
  private val delegate:   AudioEngineOps,
  private val maxStreams: Int
) extends AudioEngineOps {

  private val handlerThread: HandlerThread = {
    val ht = new HandlerThread("SGE Sound Management")
    ht.start()
    ht
  }

  private val handler: Handler = new Handler(handlerThread.getLooper())

  override def newSoundFromFd(fd: java.io.FileDescriptor, offset: Long, length: Long): SoundOps = {
    val sound = delegate.newSoundFromFd(fd, offset, length)
    new AsynchronousSoundOps(sound, handler, maxStreams)
  }

  override def newSoundFromPath(path: String): SoundOps = {
    val sound = delegate.newSoundFromPath(path)
    new AsynchronousSoundOps(sound, handler, maxStreams)
  }

  // Music, devices, recorders, pause/resume, and output device management delegate directly

  override def newMusicFromFd(fd: java.io.FileDescriptor, offset: Long, length: Long): MusicOps =
    delegate.newMusicFromFd(fd, offset, length)

  override def newMusicFromPath(path: String): MusicOps =
    delegate.newMusicFromPath(path)

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDeviceOps =
    delegate.newAudioDevice(samplingRate, isMono)

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorderOps =
    delegate.newAudioRecorder(samplingRate, isMono)

  override def pauseAll(): Unit = delegate.pauseAll()

  override def resumeAll(): Unit = delegate.resumeAll()

  override def availableOutputDevices: Array[String] = delegate.availableOutputDevices

  override def switchOutputDevice(deviceIdentifier: String): Boolean = delegate.switchOutputDevice(deviceIdentifier)

  override def dispose(): Unit = {
    delegate.dispose()
    handlerThread.quit()
  }
}
