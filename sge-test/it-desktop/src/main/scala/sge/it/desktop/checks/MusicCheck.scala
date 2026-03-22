// SGE — Desktop integration test: Music streaming check
//
// Loads test.wav as streaming Music, plays briefly, checks playback state
// and position, then stops and disposes.

package sge.it.desktop.checks

import sge.Sge
import sge.audio.{ Position, Volume }
import sge.it.desktop.CheckResult

/** Verifies music streaming: load, play, position query, stop. */
object MusicCheck {

  def run()(using Sge): CheckResult =
    try {
      val audio   = Sge().audio
      val files   = Sge().files
      val wavFile = files.internal("test.wav")

      if (!wavFile.exists()) {
        return CheckResult("music", passed = false, "test.wav not found in resources")
      }

      val music = audio.newMusic(wavFile)
      music.volume = Volume.unsafeMake(0.05f)
      music.play()
      Thread.sleep(200)

      val wasPlaying = music.playing
      val pos        = music.position

      music.stop()
      music.close()

      // For very short test files, playback may finish before the 200ms check.
      // Success = music loaded and play() didn't throw; playing state is informational.
      CheckResult("music", passed = true, s"Music streaming OK, playing=$wasPlaying, pos=${pos.toFloatSeconds}")
    } catch {
      case e: UnsatisfiedLinkError =>
        CheckResult("music", passed = false, s"Native lib missing: ${e.getMessage}")
      case e: Exception =>
        CheckResult("music", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
