/*
 * SGE Gauntlet — audio: Music state API contract (volume/looping/seek roundtrip, volume clamp edges). ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.audio.{ Position, Volume }

import scala.collection.mutable.ListBuffer

/** Exercises the [[sge.audio.Music]] state-tracking API contract without needing an audio device: volume, looping and playback-position (seek) must round-trip through the setters/getters, `playing`
  * is false before `play`, and the [[Volume]] opaque type clamps out-of-range values to [0,1]. Non-GPU: this is the honest, device-free surface (headless runs it against NoopMusic which tracks state;
  * the real miniaudio backend tracks the same state natively). Actual playback is covered by the GPU audio probes.
  */
object MusicStateProbe extends FeatureProbe {

  override def id: String = "audio/music-state"

  override def area: String = "audio"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val wav   = WavFixture.write(ctx.tempDir.child("music-state.wav"), 0.5f)
    val music = ctx.sgeCtx.audio.newMusic(wav)
    ctx.log(s"music impl: ${music.getClass.getName}")
    try {
      // Not playing until play() is called.
      checks += Check.eq("not-playing-initially", false, music.playing)

      // Volume round-trips through the setter/getter.
      music.volume = Volume.unsafeMake(0.7f)
      checks += Check.cond("volume-roundtrip", Math.abs(music.volume.toFloat - 0.7f) <= 0.001f, "volume == 0.7", music.volume.toFloat.toString)

      // Looping round-trips.
      music.looping = true
      checks += Check.eq("looping-roundtrip-true", true, music.looping)
      music.looping = false
      checks += Check.eq("looping-roundtrip-false", false, music.looping)

      // Seek: playback position round-trips (within tolerance) to a point inside the clip.
      music.position = Position.unsafeMake(0.2f)
      val pos = music.position.toFloatSeconds
      checks += Check.cond("seek-position-roundtrip", Math.abs(pos - 0.2f) <= 0.05f, "position ~ 0.2s", s"${pos}s")
      ctx.log(s"position after seek to 0.2s: ${pos}s")

      // Volume opaque-type clamps out-of-range values to [0,1] (the safe apply path).
      checks += Check.cond("volume-clamp-high", Volume(1.5f).toFloat == 1f, "Volume(1.5) clamps to 1.0", Volume(1.5f).toFloat.toString)
      checks += Check.cond("volume-clamp-low", Volume(-0.2f).toFloat == 0f, "Volume(-0.2) clamps to 0.0", Volume(-0.2f).toFloat.toString)
    } finally music.close()
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
