/*
 * SGE Gauntlet — audio: Music playback + onComplete callback (ISS-760).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.audio.{ Music, Volume }

import scala.collection.mutable.ListBuffer

/** Plays a short generated WAV as streaming Music to its natural end and asserts that playback was observable and that the `onComplete` callback fired.
  *
  * knownIssue ISS-760: `Music.onComplete` never fires on desktop (JVM+Native) — `MiniaudioMusic.fireOnComplete` has zero callers; JS and Android wire it.
  */
object MusicOnCompleteProbe extends FeatureProbe {

  override def id: String = "audio/music-oncomplete"

  override def area: String = "audio"

  override def requiresGpu: Boolean = true

  override def knownIssue: Option[String] = Some("ISS-760")

  override def frames: Int = 150

  private val checks = ListBuffer.empty[Check]

  private var music:           Option[Music] = None
  @volatile private var fired: Boolean       = false
  private var playingObserved: Boolean       = false

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    fired = false
    playingObserved = false
    val wav = WavFixture.write(ctx.tempDir.child("oncomplete.wav"), 0.3f)
    val m   = ctx.sgeCtx.audio.newMusic(wav)
    ctx.log(s"music impl: ${m.getClass.getName}")
    m.volume = Volume.unsafeMake(0.05f)
    m.onComplete(_ => fired = true)
    m.play()
    music = Some(m)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    music.foreach { m =>
      if (m.playing) {
        playingObserved = true
      }
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    checks += Check.eq("playing-observed-during-playback", true, playingObserved)
    music.foreach { m =>
      checks += Check.eq("stopped-after-track-end", false, m.playing)
    }
    checks += Check.eq("oncomplete-fired", true, fired)
    music.foreach(_.close())
    music = None
    checks.toList
  }
}
