/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package noop

/** A no-op [[sge.audio.AudioRecorder]] implementation for headless/testing use. */
class NoopAudioRecorder extends audio.AudioRecorder {

  override def read(samples: Array[Short], offset: Int, numSamples: Int): Unit = {}

  override def close(): Unit = {}
}
