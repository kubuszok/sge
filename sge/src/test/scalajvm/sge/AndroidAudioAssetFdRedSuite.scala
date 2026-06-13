// SGE — RED reproducer for ISS-520 (Android asset Sound/Music loaded by path, not fd)
//
// Contract (grounded in libgdx DefaultAndroidAudio.java):
//   * newSound  — lines 163-183: FileType.Internal -> getAssetFileDescriptor() ->
//                 soundPool.load(descriptor, 1); else -> soundPool.load(file().getPath(), 1)
//   * newMusic  — lines  92-126: FileType.Internal -> getAssetFileDescriptor() ->
//                 mediaPlayer.setDataSource(fd, startOffset, length); else -> setDataSource(file().getPath())
//
// On Android, FileType.Internal files are APK ASSETS, NOT filesystem paths. getPath()
// for an asset yields a path that does not exist on disk, so every asset-packed
// sound/music FAILS at runtime when loaded via newSoundFromPath/newMusicFromPath.
//
// This suite pins the OBSERVABLE routing outcome of AndroidAudio:
//   * Internal FileHandle  => the FD-based op MUST be invoked (newSoundFromFd / newMusicFromFd),
//                             and the path-based op MUST NOT be invoked.
//   * non-Internal handle  => the path-based op MUST still be invoked (green guard).
//
// ── Asset-fd seam (DESIGN NOTE for the IMPLEMENTER) ───────────────────────────
// The seam that yields an Internal asset's (fd, startOffset, length) ALREADY EXISTS:
//   FilesOps.openInternalFd(path): (FileDescriptor, Long, Long) | Null
// (the same seam AndroidFileHandle.map already uses, mirroring libgdx
//  AndroidFileHandle.getAssetFileDescriptor()). NO new trait method is required.
// The fix must make AndroidAudio, for an Internal AndroidFileHandle, obtain the
// asset fd via that handle's FilesOps and route to ops.newSoundFromFd /
// ops.newMusicFromFd. This test supplies an AndroidFileHandle whose backing
// FilesOps returns a real FileDescriptor from openInternalFd, so the FD path is
// reachable in a plain JVM test with NO android.jar / Robolectric.

package sge

import java.io.{ File, FileDescriptor, FileInputStream, FileOutputStream, InputStream }

import munit.FunSuite
import sge.files.{ AndroidFileHandle, FileType }
import sge.platform.android._

class AndroidAudioAssetFdRedSuite extends FunSuite {

  // Recording AudioEngineOps: records which load method was invoked and its args.
  final private class RecordingAudioEngineOps extends AudioEngineOps {
    var soundFromFdCalls:   Int                          = 0
    var soundFromPathCalls: Int                          = 0
    var musicFromFdCalls:   Int                          = 0
    var musicFromPathCalls: Int                          = 0
    var lastSoundFd:        (FileDescriptor, Long, Long) = null // scalafix:ok
    var lastMusicFd:        (FileDescriptor, Long, Long) = null // scalafix:ok
    var lastSoundPath:      String                       = null // scalafix:ok
    var lastMusicPath:      String                       = null // scalafix:ok

    override def newSoundFromFd(fd: FileDescriptor, offset: Long, length: Long): SoundOps = {
      soundFromFdCalls += 1
      lastSoundFd = (fd, offset, length)
      NoopSoundOps
    }
    override def newSoundFromPath(path: String): SoundOps = {
      soundFromPathCalls += 1
      lastSoundPath = path
      NoopSoundOps
    }
    override def newMusicFromFd(fd: FileDescriptor, offset: Long, length: Long): MusicOps = {
      musicFromFdCalls += 1
      lastMusicFd = (fd, offset, length)
      NoopMusicOps
    }
    override def newMusicFromPath(path: String): MusicOps = {
      musicFromPathCalls += 1
      lastMusicPath = path
      NoopMusicOps
    }

    override def newAudioDevice(samplingRate:         Int, isMono: Boolean): AudioDeviceOps   = ???
    override def newAudioRecorder(samplingRate:       Int, isMono: Boolean): AudioRecorderOps = ???
    override def pauseAll():                                                 Unit             = ()
    override def resumeAll():                                                Unit             = ()
    override def availableOutputDevices:                                     Array[String]    = Array.empty
    override def switchOutputDevice(deviceIdentifier: String):               Boolean          = false
    override def dispose():                                                  Unit             = ()
  }

  private object NoopSoundOps extends SoundOps {
    override def play(volume:         Float):                               Long = 0L
    override def play(volume:         Float, pitch:  Float, pan:    Float): Long = 0L
    override def loop(volume:         Float):                               Long = 0L
    override def loop(volume:         Float, pitch:  Float, pan:    Float): Long = 0L
    override def stop():                                                    Unit = ()
    override def stop(streamId:       Long):                                Unit = ()
    override def pause():                                                   Unit = ()
    override def pause(streamId:      Long):                                Unit = ()
    override def resume():                                                  Unit = ()
    override def resume(streamId:     Long):                                Unit = ()
    override def setPitch(streamId:   Long, pitch:   Float):                Unit = ()
    override def setVolume(streamId:  Long, volume:  Float):                Unit = ()
    override def setLooping(streamId: Long, looping: Boolean):              Unit = ()
    override def setPan(streamId:     Long, pan:     Float, volume: Float): Unit = ()
    override def dispose():                                                 Unit = ()
  }

  private object NoopMusicOps extends MusicOps {
    override def play():                                            Unit    = ()
    override def pause():                                           Unit    = ()
    override def stop():                                            Unit    = ()
    override def playing:                                           Boolean = false
    override def looping:                                           Boolean = false
    override def looping_=(isLooping:        Boolean):              Unit    = ()
    override def volume_=(volume:            Float):                Unit    = ()
    override def volume:                                            Float   = 0f
    override def setPan(pan:                 Float, volume: Float): Unit    = ()
    override def position_=(positionSeconds: Float):                Unit    = ()
    override def position:                                          Float   = 0f
    override def duration:                                          Float   = 0f
    override def onComplete(callback:        () => Unit):           Unit    = ()
    override def dispose():                                         Unit    = ()
    var wasPlaying:                                                 Boolean = false
  }

  // FilesOps backed by a real temp file. openInternalFd returns a genuine
  // FileDescriptor so the Internal asset-fd routing path is reachable in plain JVM.
  // This is the seam the implementer must consume (libgdx getAssetFileDescriptor()).
  final private class TempFileFilesOps(backing: File) extends FilesOps {
    val assetOffset: Long = 7L
    val assetLength: Long = backing.length()

    override def openInternalFd(path: String): (FileDescriptor, Long, Long) | Null = {
      val fis = new FileInputStream(backing)
      (fis.getFD(), assetOffset, assetLength)
    }
    override def openInternal(path:       String): InputStream   = new FileInputStream(backing)
    override def listInternal(path:       String): Array[String] = Array.empty
    override def internalFileLength(path: String): Long          = assetLength
    override def localStoragePath:                 String        = backing.getParent + "/"
    override def externalStoragePath:              String | Null = null // scalafix:ok
  }

  private def writeTemp(name: String): File = {
    val f   = File.createTempFile(name, ".ogg")
    val out = new FileOutputStream(f)
    try out.write(Array.fill[Byte](64)(0))
    finally out.close()
    f.deleteOnExit()
    f
  }

  // ── Internal asset routing (RED on current code) ───────────────────────────

  test("newSound(Internal asset) loads via fd, NOT via path") {
    val backing  = writeTemp("iss520-sound-internal")
    val filesOps = TempFileFilesOps(backing)
    val handle   = new AndroidFileHandle(backing.getName, FileType.Internal, filesOps)

    val ops   = RecordingAudioEngineOps()
    val audio = AndroidAudio(ops)
    audio.newSound(handle)

    assertEquals(
      ops.soundFromFdCalls,
      1,
      "Internal asset sound MUST be loaded via newSoundFromFd (asset fd), not a non-existent filesystem path"
    )
    assertEquals(
      ops.soundFromPathCalls,
      0,
      "Internal asset sound MUST NOT be loaded via newSoundFromPath (getPath() of an asset does not exist on disk)"
    )
    assert(ops.lastSoundFd != null, "fd-based load must receive the asset's file descriptor") // scalafix:ok
  }

  test("newMusic(Internal asset) loads via fd, NOT via path") {
    val backing  = writeTemp("iss520-music-internal")
    val filesOps = TempFileFilesOps(backing)
    val handle   = new AndroidFileHandle(backing.getName, FileType.Internal, filesOps)

    val ops   = RecordingAudioEngineOps()
    val audio = AndroidAudio(ops)
    audio.newMusic(handle)

    assertEquals(
      ops.musicFromFdCalls,
      1,
      "Internal asset music MUST be loaded via newMusicFromFd (asset fd), not a non-existent filesystem path"
    )
    assertEquals(
      ops.musicFromPathCalls,
      0,
      "Internal asset music MUST NOT be loaded via newMusicFromPath (getPath() of an asset does not exist on disk)"
    )
    assert(ops.lastMusicFd != null, "fd-based load must receive the asset's file descriptor") // scalafix:ok
  }

  // ── Green guard: non-Internal handles MUST keep using the path-based op ─────
  // Prevents a blanket switch-everything-to-fd "fix": Absolute/External/Local/Classpath
  // are real filesystem paths and libgdx loads them via file().getPath().

  test("newSound(Absolute file) still loads via path, NOT via fd (green guard)") {
    val backing  = writeTemp("iss520-sound-absolute")
    val filesOps = TempFileFilesOps(backing)
    val handle   = new AndroidFileHandle(backing.getAbsolutePath, FileType.Absolute, filesOps)

    val ops   = RecordingAudioEngineOps()
    val audio = AndroidAudio(ops)
    audio.newSound(handle)

    assertEquals(ops.soundFromPathCalls, 1, "Absolute (real filesystem) sound must load via newSoundFromPath")
    assertEquals(ops.soundFromFdCalls, 0, "Absolute (real filesystem) sound must NOT load via newSoundFromFd")
  }

  test("newMusic(Absolute file) still loads via path, NOT via fd (green guard)") {
    val backing  = writeTemp("iss520-music-absolute")
    val filesOps = TempFileFilesOps(backing)
    val handle   = new AndroidFileHandle(backing.getAbsolutePath, FileType.Absolute, filesOps)

    val ops   = RecordingAudioEngineOps()
    val audio = AndroidAudio(ops)
    audio.newMusic(handle)

    assertEquals(ops.musicFromPathCalls, 1, "Absolute (real filesystem) music must load via newMusicFromPath")
    assertEquals(ops.musicFromFdCalls, 0, "Absolute (real filesystem) music must NOT load via newMusicFromFd")
  }
}
