// SGE — RED reproducer for ISS-785 (Native desktop factory omits recorderFactory), Scala Native.
//
// The JVM DesktopApplicationFactory wires a working recorder into the desktop app
// ("(rate, mono) => new sge.audio.DesktopAudioRecorder(rate, mono)",
// scalajvm/sge/DesktopApplicationFactory.scala:40). The Native DesktopApplicationFactory omits that
// argument (scalanative/sge/DesktopApplicationFactory.scala:29), so DesktopApplication falls back to
// its throwing default (DesktopApplication.scala:57), which is then handed to MiniaudioEngine
// (createAudio, DesktopApplication.scala:547-554). The result: Audio.newAudioRecorder throws a raw
// java.lang.UnsupportedOperationException on Scala Native while it works on JVM — an undocumented
// divergence INSIDE the same desktop backend.
//
// Running the real factory needs GLFW/ANGLE/miniaudio (impractical headless), so this suite pins the
// CONSEQUENCE with a pure-Scala fake AudioOps (no native library): a MiniaudioEngine wired the way
// the Native factory wires it — with NO recorderFactory — must not signal the missing recorder with a
// raw, undocumented UnsupportedOperationException. A faithful fix reaches parity with JVM by wiring a
// Native recorder (or by folding into the unified SgeError.Unsupported capability convention, ISS-771),
// either of which makes newAudioRecorder return a recorder or throw an SgeError. It FAILS today
// because the Native wiring throws java.lang.UnsupportedOperationException.

package sge
package audio

import munit.FunSuite
import sge.platform.AudioOps
import sge.utils.SgeError

class DesktopRecorderNativeParityIss785RedSuite extends FunSuite {

  /** Minimal headless AudioOps: nonzero engine handle so MiniaudioEngine takes its real code paths. */
  final private class FakeAudioOps extends AudioOps {
    override def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long = 1L
    override def shutdownEngine(engineHandle:    Long):                                   Unit = ()
    override def updateEngine(engineHandle:      Long):                                   Unit = ()

    override def createSound(engineHandle:       Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int):     Long = 7L
    override def disposeSound(soundHandle:       Long):                                                                          Unit = ()
    override def playSound(soundHandle:          Long, volume:  Float, pitch:          Float, pan:    Float, loop:     Boolean): Long = 1L
    override def stopSound(instanceId:           Long):                                                                          Unit = ()
    override def pauseSound(instanceId:          Long):                                                                          Unit = ()
    override def resumeSound(instanceId:         Long):                                                                          Unit = ()
    override def stopAllInstances(soundHandle:   Long):                                                                          Unit = ()
    override def pauseAllInstances(soundHandle:  Long):                                                                          Unit = ()
    override def resumeAllInstances(soundHandle: Long):                                                                          Unit = ()
    override def setSoundVolume(instanceId:      Long, volume:  Float):                                                          Unit = ()
    override def setSoundPitch(instanceId:       Long, pitch:   Float):                                                          Unit = ()
    override def setSoundPan(instanceId:         Long, pan:     Float, volume:         Float):                                   Unit = ()
    override def setSoundLooping(instanceId:     Long, looping: Boolean):                                                        Unit = ()

    override def createMusic(engineHandle:     Long, filePath: String):               Long    = 42L
    override def disposeMusic(musicHandle:     Long):                                 Unit    = ()
    override def playMusic(musicHandle:        Long):                                 Unit    = ()
    override def pauseMusic(musicHandle:       Long):                                 Unit    = ()
    override def stopMusic(musicHandle:        Long):                                 Unit    = ()
    override def isMusicPlaying(musicHandle:   Long):                                 Boolean = false
    override def getMusicVolume(musicHandle:   Long):                                 Float   = 1f
    override def setMusicVolume(musicHandle:   Long, volume:   Float):                Unit    = ()
    override def setMusicPitch(musicHandle:    Long, pitch:    Float):                Unit    = ()
    override def setMusicPan(musicHandle:      Long, pan:      Float, volume: Float): Unit    = ()
    override def isMusicLooping(musicHandle:   Long):                                 Boolean = false
    override def setMusicLooping(musicHandle:  Long, looping:  Boolean):              Unit    = ()
    override def setMusicPosition(musicHandle: Long, position: Float):                Unit    = ()
    override def getMusicPosition(musicHandle: Long):                                 Float   = 0f
    override def getMusicDuration(musicHandle: Long):                                 Float   = 1f

    override def createAudioDevice(engineHandle:     Long, sampleRate: Int, isMono:         Boolean):          Long = 9L
    override def disposeAudioDevice(deviceHandle:    Long):                                                    Unit = ()
    override def writeAudioDevice(deviceHandle:      Long, data:       Array[Byte], offset: Int, length: Int): Unit = ()
    override def setAudioDeviceVolume(deviceHandle:  Long, volume:     Float):                                 Unit = ()
    override def pauseAudioDevice(deviceHandle:      Long):                                                    Unit = ()
    override def resumeAudioDevice(deviceHandle:     Long):                                                    Unit = ()
    override def getAudioDeviceLatency(deviceHandle: Long):                                                    Int  = 0

    override def getAvailableOutputDevices(engineHandle: Long):                     Array[String] = Array.empty[String]
    override def switchOutputDevice(engineHandle:        Long, deviceName: String): Boolean       = true
  }

  test("ISS-785 (RED): Native desktop newAudioRecorder is not a raw, undocumented UnsupportedOperationException") {
    // MiniaudioEngine(ops) uses the throwing default recorderFactory — exactly the state the Native
    // DesktopApplicationFactory leaves the desktop backend in (it passes no recorderFactory).
    val engine = MiniaudioEngine(new FakeAudioOps)

    val outcome: Either[Throwable, AudioRecorder] =
      try Right(engine.newAudioRecorder(44100, isMono = true))
      catch { case t: Throwable => Left(t) }

    outcome match {
      case Right(_) => () // parity with JVM: a recorder was produced
      case Left(t)  =>
        assert(
          t.isInstanceOf[SgeError],
          s"Native desktop newAudioRecorder must reach parity with the JVM factory (which wires " +
            s"DesktopAudioRecorder): return a recorder, or signal via sge.utils.SgeError — not a raw " +
            s"${t.getClass.getName} (currently java.lang.UnsupportedOperationException from the omitted " +
            "recorderFactory, ISS-785)"
        )
    }
  }
}
