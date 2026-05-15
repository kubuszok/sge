/*
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: SGE-original opaque type wrapping Long; replaces raw long sound-instance IDs
 *     in Sound play/loop/stop/pause/resume/setLooping/setPitch/setVolume/setPan
 *   Audited: 2026-03-04
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 22
 * Covenant-baseline-methods: SoundId,apply,toLong
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package audio

import lowlevel.MkArray

opaque type SoundId = Long
object SoundId {

  def apply(value: Long): SoundId = value

  given lowlevel.MkArray[SoundId] = lowlevel.MkArray.mkLong.asInstanceOf[lowlevel.MkArray[SoundId]]

  extension (soundId: SoundId) {
    inline def toLong: Long = soundId
  }
}
