/*
 * SGE Gauntlet — audio: real Sound playback through miniaudio.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.audio.{ Sound, SoundId, Volume }

import scala.collection.mutable.ListBuffer

/** Plays a generated WAV through the real desktop audio backend and asserts the play id and clean stop/dispose. Marked requiresGpu because headless mode runs NoopAudio — there is nothing honest to
  * assert there (see audio/lifecycle-headless for the headless-safe lifecycle probe).
  */
object SoundPlaybackProbe extends FeatureProbe {

  override def id: String = "audio/sound-playback"

  override def area: String = "audio"

  override def requiresGpu: Boolean = true

  override def frames: Int = 30

  private val checks = ListBuffer.empty[Check]

  private var sound:  Option[Sound]   = None
  private var playId: Option[SoundId] = None

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    playId = None
    val wav = WavFixture.write(ctx.tempDir.child("playback.wav"), 0.5f)
    val s   = ctx.sgeCtx.audio.newSound(wav)
    ctx.log(s"sound impl: ${s.getClass.getName}")
    sound = Some(s)
    playId = Some(s.play(Volume.unsafeMake(0.05f)))
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    () // let the sound play for ~0.5s of frames

  override def verify(ctx: ProbeContext): List[Check] = {
    checks += Check.cond(
      "play-returns-valid-id",
      playId.exists(_.toLong != -1L),
      "SoundId != -1",
      playId.fold("<none>")(_.toLong.toString)
    )
    sound.foreach { s =>
      s.stop()
      s.close()
    }
    // Sound has no queryable playback state after stop — this is honestly only a
    // no-exception check (an exception above would abort the probe as failed).
    checks += Check.cond("no-exception-on-stop-dispose", passed = true, "no exception", "stopped and disposed")
    sound = None
    checks.toList
  }
}
