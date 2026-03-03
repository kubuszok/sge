/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original utility object; pan-to-stereo conversion extracted from backend
 *     implementations into shared code; uses opaque types (Pan, Volume)
 *   Audited: 2026-03-03
 */
package sge
package audio

/** Shared audio utility methods used by multiple backend implementations. */
object AudioUtils {

  /** Convert pan + volume to left/right stereo channel volumes.
    *
    * Used by all backends that implement spatial panning. Pan range: -1 (full left) to 1 (full right).
    *
    * @return
    *   a tuple of (leftVolume, rightVolume)
    */
  def panToStereoVolumes(pan: Pan, volume: Volume): (Float, Float) = {
    val p     = pan.toFloat
    val v     = volume.toFloat
    val left  = v * scala.math.min(1.0f, 2.0f - (p + 1.0f))
    val right = v * scala.math.min(1.0f, p + 1.0f)
    (left, right)
  }
}
