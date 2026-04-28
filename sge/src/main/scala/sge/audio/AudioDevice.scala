/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/audio/AudioDevice.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: getLatency -> latency, Disposable -> Closeable
 *   Convention: Java interface -> Scala trait; raw float params replaced with opaque Volume type;
 *     setVolume kept as method (no getVolume in Java source)
 *   Idiom: split packages
 *   Fixes: volume_= without getter → setVolume (lone setter is non-idiomatic)
 *   Audited: 2026-03-04
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: AudioDevice,isMono,latency,pause,resume,setVolume,writeSamples
 * Covenant-source-reference: com/badlogic/gdx/audio/AudioDevice.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 990cedd5fc6c79a5181711ee67faccbb82a42c57
 */
package sge
package audio

/** Encapsulates an audio device in mono or stereo mode. Use the {@link #writeSamples(float[], int, int)} and {@link #writeSamples(short[], int, int)} methods to write float or 16-bit signed short PCM
  * data directly to the audio device. Stereo samples are interleaved in the order left channel sample, right channel sample. The {@link #dispose()} method must be called when this AudioDevice is no
  * longer needed.
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
trait AudioDevice extends java.io.Closeable {

  /** @return whether this AudioDevice is in mono or stereo mode. */
  def isMono: Boolean

  /** Writes the array of 16-bit signed PCM samples to the audio device and blocks until they have been processed.
    *
    * @param samples
    *   The samples.
    * @param offset
    *   The offset into the samples array
    * @param numSamples
    *   the number of samples to write to the device
    */
  def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit

  /** Writes the array of float PCM samples to the audio device and blocks until they have been processed.
    *
    * @param samples
    *   The samples.
    * @param offset
    *   The offset into the samples array
    * @param numSamples
    *   the number of samples to write to the device
    */
  def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit

  /** @return the latency in samples. */
  def latency: Int

  /** Sets the volume in the range [0,1]. */
  def setVolume(volume: Volume): Unit

  /** Pauses the audio device if supported */
  def pause(): Unit

  /** Unpauses the audio device if supported */
  def resume(): Unit
}
