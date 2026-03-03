/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package noop

import sge.audio.Volume

/** A no-op [[sge.audio.AudioDevice]] implementation for headless/testing use. All write and control methods are no-ops.
  */
class NoopAudioDevice(override val isMono: Boolean) extends audio.AudioDevice {

  override def writeSamples(samples: Array[Short], offset: Int, numSamples: Int): Unit = {}

  override def writeSamples(samples: Array[Float], offset: Int, numSamples: Int): Unit = {}

  override def latency: Int = 0

  override def volume_=(volume: Volume): Unit = {}

  override def pause(): Unit = {}

  override def resume(): Unit = {}

  override def close(): Unit = {}
}
