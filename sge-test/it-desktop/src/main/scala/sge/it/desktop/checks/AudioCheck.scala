// SGE — Desktop integration test: Audio subsystem check
//
// Initializes the miniaudio engine, loads a small WAV from resources,
// plays it briefly, and disposes. No crash = pass.

package sge.it.desktop.checks

import sge.Sge
import sge.audio.Volume
import sge.it.desktop.CheckResult

/** Verifies audio engine initialization and sound loading. */
object AudioCheck {

  def run()(using Sge): CheckResult =
    try {
      val audio   = Sge().audio
      val files   = Sge().files
      val wavFile = files.internal("test.wav")

      if (!wavFile.exists()) {
        return CheckResult("audio", passed = false, "test.wav not found in resources")
      }

      // Load and play a brief sound
      val sound = audio.newSound(wavFile)
      val id    = sound.play(Volume.unsafeMake(0.1f)) // low volume
      Thread.sleep(100) // let it play briefly
      sound.stop()
      sound.close()

      CheckResult("audio", passed = true, "Audio engine init + sound load/play OK")
    } catch {
      case e: UnsatisfiedLinkError =>
        CheckResult("audio", passed = false, s"Native lib missing: ${e.getMessage}")
      case e: Exception =>
        CheckResult("audio", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
