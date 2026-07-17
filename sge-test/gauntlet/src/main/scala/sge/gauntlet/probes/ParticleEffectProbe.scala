/*
 * SGE Gauntlet — g2d: 2D ParticleEffect emission + completion. ISS-796.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.{ GL20, Pixmap, Texture }
import sge.graphics.g2d.{ ParticleEffect, Sprite }
import sge.utils.Seconds

import scala.collection.mutable.ListBuffer

/** Loads a REAL [[ParticleEffect]] from a hand-authored `.p` descriptor (a single point emitter: 500ms duration, ~250 particles/sec, additive white sprites, non-continuous), attaches a generated
  * white sprite, and steps it for many fixed frames. Asserts emission happened (the emitter spawns particles and additive white ink appears at the emitter position mid-run) and that the
  * non-continuous effect completes (all particles die and `isComplete` becomes true) after its duration + particle life elapse.
  */
object ParticleEffectProbe extends FeatureProbe {

  override def id: String = "g2d/particles"

  override def area: String = "g2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 80

  private val PosX       = 640
  private val PosY       = 360
  private val SampleAt   = 15 // frame at which particles are guaranteed alive
  private val CompleteBy = 78 // by this frame duration (500ms) + life (500ms) have elapsed

  // A compact but valid .p descriptor for the loader as implemented (LF newlines, no image-paths header).
  private val Descriptor: String = {
    def scaled(lowMin: String, lowMax: String, highMin: String, highMax: String): String =
      s"""lowMin: $lowMin
         |lowMax: $lowMax
         |highMin: $highMin
         |highMax: $highMax
         |relative: false
         |scalingCount: 1
         |scaling0: 1.0
         |timelineCount: 1
         |timeline0: 0.0""".stripMargin
    s"""gauntlet
       |- Delay -
       |active: false
       |- Duration -
       |lowMin: 500.0
       |lowMax: 500.0
       |- Count -
       |min: 0
       |max: 64
       |- Emission -
       |${scaled("0.0", "0.0", "250.0", "250.0")}
       |- Life -
       |${scaled("0.0", "0.0", "500.0", "500.0")}
       |independent: false
       |- Life Offset -
       |active: false
       |- X Offset -
       |active: false
       |- Y Offset -
       |active: false
       |- Spawn Shape -
       |shape: point
       |- Spawn Width -
       |${scaled("0.0", "0.0", "0.0", "0.0")}
       |- Spawn Height -
       |${scaled("0.0", "0.0", "0.0", "0.0")}
       |- X Scale -
       |${scaled("0.0", "0.0", "24.0", "24.0")}
       |- Y Scale -
       |active: false
       |- Velocity -
       |active: false
       |- Angle -
       |active: false
       |- Rotation -
       |active: false
       |- Wind -
       |active: false
       |- Gravity -
       |active: false
       |- Tint -
       |colorsCount: 3
       |colors0: 1.0
       |colors1: 1.0
       |colors2: 1.0
       |timelineCount: 1
       |timeline0: 0.0
       |- Transparency -
       |${scaled("0.0", "0.0", "1.0", "1.0")}
       |- Options -
       |attached: false
       |continuous: false
       |aligned: false
       |additive: true
       |behind: false
       |premultipliedAlpha: false
       |spriteMode: single
       |""".stripMargin
  }

  private val checks = ListBuffer.empty[Check]

  private var effect:  Option[ParticleEffect] = None
  private var texture: Option[Texture]        = None

  private var emitterCountAtStart = 0
  private var midBrightness       = 0
  private var midActive           = 0
  private var cornerBrightness    = 0
  private var completed           = false

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    emitterCountAtStart = 0; midBrightness = 0; midActive = 0; cornerBrightness = 0; completed = false
    given Sge  = ctx.sgeCtx
    val pixmap = Pixmap(4, 4, Pixmap.Format.RGBA8888)
    pixmap.setColor(0xffffffff)
    pixmap.fill()
    val tex = Texture(pixmap)
    pixmap.close()
    texture = Some(tex)

    val file = ctx.tempDir.child("effect.p")
    file.writeString(Descriptor, false)

    val fx = new ParticleEffect()
    fx.loadEmitters(file)
    emitterCountAtStart = fx.emitters.size
    if (fx.emitters.size > 0) {
      fx.emitters(0).setSprites(Array(new Sprite(tex)))
      fx.setPosition(PosX.toFloat, PosY.toFloat)
      fx.start()
    }
    effect = Some(fx)
    ctx.clear(0f, 0f, 0f, 1f)
  }

  private def brightnessAround(ctx: ProbeContext, cx: Int, cy: Int): Int = {
    var best = 0
    var dy   = -6
    while (dy <= 6) {
      var dx = -6
      while (dx <= 6) {
        val (r, g, b, _) = ctx.pixelRgba(cx + dx, cy + dy)
        val sum          = r + g + b
        if (sum > best) best = sum
        dx += 3
      }
      dy += 3
    }
    best
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    effect.foreach { fx =>
      ctx.clear(0f, 0f, 0f, 1f)
      val batch = ctx.batch
      batch.rendering {
        fx.update(Seconds(ctx.fixedDelta))
        fx.draw(batch)
      }
      // restore default blend (the additive emitter cleans up, but be explicit for later probes)
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
      if (frame == SampleAt) {
        midBrightness = brightnessAround(ctx, PosX, PosY)
        cornerBrightness = brightnessAround(ctx, 100, 100)
        midActive = if (fx.emitters.size > 0) fx.emitters(0).activeCount else 0
      }
      if (frame >= CompleteBy) completed = fx.isComplete()
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    checks += Check.eq("emitter-loaded", 1, emitterCountAtStart)
    checks += Check.cond("particles-active-mid-run", midActive > 0, "> 0 active particles at frame " + SampleAt, midActive.toString)
    checks += Check.cond("emission-ink-at-position", midBrightness > 120, "> 120 additive brightness at emitter pos", midBrightness.toString)
    checks += Check.cond("background-dark-off-position", cornerBrightness < 60, "< 60 brightness far from emitter", cornerBrightness.toString)
    checks += Check.cond("non-continuous-effect-completes", completed, "isComplete == true after duration+life", completed.toString)
    effect.foreach(_.close())
    texture.foreach(_.close())
    effect = None
    texture = None
    checks.toList
  }
}
