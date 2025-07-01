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
