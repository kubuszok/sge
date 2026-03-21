// SGE — Desktop integration test: Multiple simultaneous sounds
//
// Plays the same WAV three times concurrently with different panning,
// verifying all SoundIds are valid (not -1).

package sge.it.desktop.checks

import sge.Sge
import sge.audio.{ Pan, Pitch, Volume }
import sge.it.desktop.CheckResult

/** Verifies multiple concurrent sound instances. */
object MultiSoundCheck {

  def run()(using Sge): CheckResult =
    try {
      val audio   = Sge().audio
      val files   = Sge().files
      val wavFile = files.internal("test.wav")

      if (!wavFile.exists()) {
        return CheckResult("multi_sound", passed = false, "test.wav not found in resources")
      }

      val sound = audio.newSound(wavFile)
      val vol   = Volume.unsafeMake(0.05f)

      val id1 = sound.play(vol, Pitch.normal, Pan.maxLeft)
      val id2 = sound.play(vol, Pitch.normal, Pan.center)
      val id3 = sound.play(vol, Pitch.max, Pan.maxRight)

      Thread.sleep(100)
      sound.stop()
      sound.close()

      // SoundId is opaque Long — -1L means failure
      val allValid = id1.toLong != -1L && id2.toLong != -1L && id3.toLong != -1L
      if (allValid) {
        CheckResult("multi_sound", passed = true, s"3 concurrent sounds OK: ${id1.toLong},${id2.toLong},${id3.toLong}")
      } else {
        CheckResult("multi_sound", passed = false, s"Some SoundIds invalid: ${id1.toLong},${id2.toLong},${id3.toLong}")
      }
    } catch {
      case e: Exception =>
        CheckResult("multi_sound", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
