/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/audio/AudioRecorder.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Disposable -> Closeable
 *   Convention: Java interface -> Scala trait; dispose() inherited via Closeable.close()
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package audio

/** An AudioRecorder allows to record input from an audio device. It has a sampling rate and is either stereo or mono. Samples are returned in signed 16-bit PCM format. Stereo samples are interleaved
  * in the order left channel, right channel. The AudioRecorder has to be disposed if no longer needed via the {@link #dispose()} .
  *
  * @author
  *   mzechner (original implementation)
  */
trait AudioRecorder extends java.io.Closeable {

  /** Reads in numSamples samples into the array samples starting at offset. If the recorder is in stereo you have to multiply numSamples by 2.
    *
    * @param samples
    *   the array to write the samples to
    * @param offset
    *   the offset into the array
    * @param numSamples
    *   the number of samples to be read
    */
  def read(samples: Array[Short], offset: Int, numSamples: Int): Unit
}
