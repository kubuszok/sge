// SGE — Android audio engine implementation
//
// Uses SoundPool for short sounds, MediaPlayer for streaming music,
// AudioTrack for PCM output, AudioRecord for PCM input.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.DefaultAndroidAudio
//   Renames: DefaultAndroidAudio → AndroidAudioEngineImpl
//   Convention: ops interface pattern; _root_.android.* imports
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.app.Activity
import _root_.android.content.Context
import _root_.android.media.{ AudioAttributes, AudioManager, MediaPlayer, SoundPool }

import java.util.concurrent.CopyOnWriteArrayList

class AndroidAudioEngineImpl(context: Context, config: AndroidConfigOps) extends AudioEngineOps {

  private val soundPool: SoundPool = {
    val attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    new SoundPool.Builder().setAudioAttributes(attrs).setMaxStreams(config.maxSimultaneousSounds).build()
  }

  private val manager: AudioManager =
    context.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]

  // Set the volume control stream to music for Activity contexts
  context match {
    case activity: Activity => activity.setVolumeControlStream(AudioManager.STREAM_MUSIC)
    case _ => ()
  }

  private val musics = new CopyOnWriteArrayList[AndroidMusicOpsImpl]()

  // A Runnable poster — in a real Android app this would post to the UI thread.
  // For the ops interface, we just run directly; the sge adapter can replace this.
  private val postRunnable: Runnable => Unit = r => r.run()

  private def createMediaPlayer(): MediaPlayer = {
    val mp = new MediaPlayer()
    mp.setAudioAttributes(
      new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_GAME).build()
    )
    mp
  }

  override def newSoundFromFd(fd: java.io.FileDescriptor, offset: Long, length: Long): SoundOps = {
    val soundId = soundPool.load(fd, offset, length, 1)
    new AndroidSoundOpsImpl(soundPool, manager, soundId)
  }

  override def newSoundFromPath(path: String): SoundOps = {
    val soundId = soundPool.load(path, 1)
    new AndroidSoundOpsImpl(soundPool, manager, soundId)
  }

  override def newMusicFromFd(fd: java.io.FileDescriptor, offset: Long, length: Long): MusicOps = {
    val mp = createMediaPlayer()
    mp.setDataSource(fd, offset, length)
    mp.prepare()
    val music = new AndroidMusicOpsImpl(notifyMusicDisposed, postRunnable, mp)
    musics.add(music)
    music
  }

  override def newMusicFromPath(path: String): MusicOps = {
    val mp = createMediaPlayer()
    mp.setDataSource(path)
    mp.prepare()
    val music = new AndroidMusicOpsImpl(notifyMusicDisposed, postRunnable, mp)
    musics.add(music)
    music
  }

  override def newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDeviceOps =
    new AndroidAudioDeviceImpl(samplingRate, isMono)

  override def newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorderOps =
    new AndroidAudioRecorderImpl(samplingRate, isMono)

  override def pauseAll(): Unit = {
    val it = musics.iterator()
    while (it.hasNext) {
      val music = it.next()
      if (music.playing) {
        music.pause()
        music.wasPlaying = true
      } else {
        music.wasPlaying = false
      }
    }
    soundPool.autoPause()
  }

  override def resumeAll(): Unit = {
    val it = musics.iterator()
    while (it.hasNext) {
      val music = it.next()
      if (music.wasPlaying) music.play()
    }
    soundPool.autoResume()
  }

  override def availableOutputDevices: Array[String] = Array.empty

  override def switchOutputDevice(deviceIdentifier: String): Boolean = true

  override def dispose(): Unit = {
    // Copy to avoid concurrent modification (dispose removes from list)
    val copy = new java.util.ArrayList[AndroidMusicOpsImpl](musics)
    val it   = copy.iterator()
    while (it.hasNext) it.next().dispose()
    soundPool.release()
  }

  private def notifyMusicDisposed(music: AndroidMusicOpsImpl): Unit =
    musics.remove(music)
}
