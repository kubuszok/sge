/*
 * SGE Gauntlet — audio: headless-safe Sound/Music create+dispose lifecycle.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import scala.collection.mutable.ListBuffer

/** Creates and disposes Sound and Music objects from a generated WAV without playing them. Headless runs exercise the Noop implementations (creation contract only); windowed runs exercise the real
  * miniaudio decode path. The backing implementation class is logged as the capability guard.
  */
object AudioLifecycleProbe extends FeatureProbe {

  override def id: String = "audio/lifecycle-headless"

  override def area: String = "audio"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val audio = ctx.sgeCtx.audio
    val wav   = WavFixture.write(ctx.tempDir.child("lifecycle.wav"), 0.1f)
    ctx.log(s"audio impl: ${audio.getClass.getName}")

    val sound = audio.newSound(wav)
    ctx.log(s"sound impl: ${sound.getClass.getName}")
    checks += Check.cond("new-sound", passed = true, "no exception", sound.getClass.getSimpleName)
    sound.close()
    checks += Check.cond("sound-dispose", passed = true, "no exception", "disposed")

    val music = audio.newMusic(wav)
    ctx.log(s"music impl: ${music.getClass.getName}")
    checks += Check.cond("new-music", passed = true, "no exception", music.getClass.getSimpleName)
    checks += Check.eq("music-not-playing-before-play", false, music.playing)
    music.close()
    checks += Check.cond("music-dispose", passed = true, "no exception", "disposed")
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
