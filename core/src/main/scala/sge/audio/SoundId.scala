/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original opaque type wrapping Long; replaces raw long sound-instance IDs
 *     in Sound play/loop/stop/pause/resume/setLooping/setPitch/setVolume/setPan
 *   Audited: 2026-03-03
 */
package sge
package audio

opaque type SoundId = Long
object SoundId {

  def apply(value: Long): SoundId = value

  extension (soundId: SoundId) {
    // TODO: extension methods using Sound

    inline def toLong: Long = soundId
  }
}
