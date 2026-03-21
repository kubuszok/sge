// SGE — Android audio engine operations interface
//
// Self-contained (JDK types only). Creates and manages audio resources.
// Implemented in sge-jvm-platform-android using SoundPool + MediaPlayer.

package sge
package platform
package android

/** Audio engine for Android. Creates sound/music/device/recorder instances. Uses only JDK types.
  *
  * Files are identified by path strings and file type indicators (internal vs external).
  */
trait AudioEngineOps {

  /** Loads a sound effect from an internal (asset) file.
    * @param fd
    *   file descriptor
    * @param offset
    *   start offset in the file descriptor
    * @param length
    *   data length in bytes
    * @return
    *   a SoundOps handle
    */
  def newSoundFromFd(fd: java.io.FileDescriptor, offset: Long, length: Long): SoundOps

  /** Loads a sound effect from a file system path. */
  def newSoundFromPath(path: String): SoundOps

  /** Creates a music stream from an internal (asset) file.
    * @param fd
    *   file descriptor
    * @param offset
    *   start offset in the file descriptor
    * @param length
    *   data length in bytes
    * @return
    *   a MusicOps handle
    */
  def newMusicFromFd(fd: java.io.FileDescriptor, offset: Long, length: Long): MusicOps

  /** Creates a music stream from a file system path. */
  def newMusicFromPath(path: String): MusicOps

  /** Creates a PCM audio output device. */
  def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDeviceOps

  /** Creates a PCM audio input recorder. */
  def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorderOps

  /** Pauses all playing music and sounds (e.g. when app goes to background). */
  def pauseAll(): Unit

  /** Resumes music and sounds that were paused by pauseAll(). */
  def resumeAll(): Unit

  /** Returns available output device identifiers. */
  def availableOutputDevices: Array[String]

  /** Switches audio output to the named device. Returns true on success. */
  def switchOutputDevice(deviceIdentifier: String): Boolean

  /** Releases all audio resources. */
  def dispose(): Unit
}
