/*
 * SGE Demos — Manual particle system showcasing DynamicArray + Pool APIs.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.particles

import scala.compiletime.uninitialized

import sge.{Input, Pixels, Sge}
import sge.graphics.{Color, Pixmap, Texture}
import sge.graphics.g2d.SpriteBatch
import sge.graphics.glutils.ShapeRenderer
import sge.graphics.glutils.ShapeRenderer.ShapeType
import sge.math.MathUtils
import sge.utils.{DynamicArray, Nullable, Pool, Poolable, ScreenUtils}
import sge.utils.viewport.FitViewport
import demos.shared.DemoScene

/** Manual particle system with three emitter modes, drawn via SpriteBatch. */
object ParticleShowGame extends DemoScene {

  override def name: String = "Particle Show"

  private val W = 800f
  private val H = 600f
  private val MaxParticles = 2000

  // --- Particle definition ---

  final class Particle {
    var x:       Float = 0f
    var y:       Float = 0f
    var vx:      Float = 0f
    var vy:      Float = 0f
    var life:    Float = 0f
    var maxLife: Float = 1f
    var r:       Float = 1f
    var g:       Float = 1f
    var b:       Float = 1f
    var size:    Float = 4f
  }

  private given Poolable[Particle] with {
    def reset(p: Particle): Unit = {
      p.x = 0f; p.y = 0f; p.vx = 0f; p.vy = 0f
      p.life = 0f; p.maxLife = 1f
      p.r = 1f; p.g = 1f; p.b = 1f; p.size = 4f
    }
  }

  private val pool: Pool.Default[Particle] = Pool.Default[Particle](() => Particle(), 256)
  private val active: DynamicArray[Particle] = DynamicArray[Particle]()

  // Emitter modes
  private val ModeExplosion = 0
  private val ModeFountain  = 1
  private val ModeRain      = 2
  private var mode: Int = ModeExplosion

  // Auto-spawn timer
  private var spawnTimer: Float = 0f
  private val SpawnInterval: Float = 0.08f

  // Resources
  private var batch:         SpriteBatch   = uninitialized
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport:      FitViewport   = uninitialized
  private var whiteTexture:  Texture       = uninitialized

  override def init()(using Sge): Unit = {
    batch = SpriteBatch()
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(W, H)

    val pm = Pixmap(2, 2, Pixmap.Format.RGBA8888)
    pm.setColor(Color.WHITE)
    pm.fill()
    whiteTexture = Texture(pm)
    pm.close()
  }

  override def render(dt: Float)(using Sge): Unit = {
    val input = Sge().input

    // Mode selection
    if (input.isKeyPressed(Input.Keys.NUM_1)) { mode = ModeExplosion }
    if (input.isKeyPressed(Input.Keys.NUM_2)) { mode = ModeFountain }
    if (input.isKeyPressed(Input.Keys.NUM_3)) { mode = ModeRain }

    // Click/touch burst
    if (input.touched) {
      val worldX = input.x.toFloat / Sge().graphics.width.toFloat * W
      val worldY = (1f - input.y.toFloat / Sge().graphics.height.toFloat) * H
      spawnBurst(worldX, worldY)
    }

    // Auto-spawn
    spawnTimer += dt
    while (spawnTimer >= SpawnInterval) {
      spawnTimer -= SpawnInterval
      autoSpawn()
    }

    // Update particles
    updateParticles(dt)

    // Draw
    ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1f)
    viewport.apply()

    batch.projectionMatrix = viewport.camera.combined
    batch.rendering {
      var i = 0
      while (i < active.size) {
        val p = active(i)
        val alpha = scala.math.max(0f, 1f - p.life / p.maxLife)
        batch.setColor(p.r, p.g, p.b, alpha)
        batch.draw(whiteTexture, p.x - p.size * 0.5f, p.y - p.size * 0.5f, p.size, p.size)
        i += 1
      }
    }

    // Particle count bar
    shapeRenderer.setProjectionMatrix(viewport.camera.combined)
    shapeRenderer.drawing(ShapeType.Filled) {
      val barW = (active.size.toFloat / MaxParticles.toFloat) * 120f
      shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f)
      shapeRenderer.rectangle(8f, H - 20f, 120f, 12f)
      shapeRenderer.setColor(0.2f, 0.8f, 0.3f, 1f)
      shapeRenderer.rectangle(8f, H - 20f, barW, 12f)
    }
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    batch.close()
    shapeRenderer.close()
    whiteTexture.close()
  }

  // --- Particle helpers ---

  private def updateParticles(dt: Float): Unit = {
    val gravity = -300f
    var i = 0
    while (i < active.size) {
      val p = active(i)
      p.life += dt
      if (p.life >= p.maxLife) {
        active.removeIndex(i)
        pool.free(p)
      } else {
        p.vy += gravity * dt
        p.x += p.vx * dt
        p.y += p.vy * dt
        i += 1
      }
    }
  }

  private def spawnBurst(cx: Float, cy: Float): Unit = {
    val count = MathUtils.random(20, 40)
    var i = 0
    while (i < count && active.size < MaxParticles) {
      val p = pool.obtain()
      p.x = cx
      p.y = cy
      p.maxLife = MathUtils.random(0.6f, 1.4f)
      p.life = 0f
      p.size = MathUtils.random(3f, 7f)
      assignWarmColor(p)
      mode match {
        case ModeExplosion =>
          val angle = MathUtils.random(0f, MathUtils.PI * 2f)
          val speed = MathUtils.random(80f, 250f)
          p.vx = MathUtils.cos(angle) * speed
          p.vy = MathUtils.sin(angle) * speed
        case ModeFountain =>
          p.vx = MathUtils.random(-60f, 60f)
          p.vy = MathUtils.random(200f, 450f)
        case ModeRain =>
          p.vx = MathUtils.random(-20f, 20f)
          p.vy = MathUtils.random(-50f, 50f)
        case _ => ()
      }
      active.add(p)
      i += 1
    }
  }

  private def autoSpawn(): Unit = {
    mode match {
      case ModeExplosion =>
        spawnBurst(MathUtils.random(100f, W - 100f), MathUtils.random(100f, H - 100f))
      case ModeFountain =>
        spawnBurst(W * 0.5f, 40f)
      case ModeRain =>
        var i = 0
        val count = MathUtils.random(3, 8)
        while (i < count && active.size < MaxParticles) {
          val p = pool.obtain()
          p.x = MathUtils.random(0f, W)
          p.y = H + 10f
          p.vx = MathUtils.random(-10f, 10f)
          p.vy = MathUtils.random(-80f, -30f)
          p.maxLife = MathUtils.random(1.5f, 3f)
          p.life = 0f
          p.size = MathUtils.random(2f, 5f)
          p.r = 0.5f; p.g = 0.7f; p.b = 1f
          active.add(p)
          i += 1
        }
      case _ => ()
    }
  }

  private def assignWarmColor(p: Particle): Unit = {
    val pick = MathUtils.random(0f, 1f)
    if (pick < 0.33f) {
      // Red-orange
      p.r = 1f; p.g = MathUtils.random(0.2f, 0.5f); p.b = 0.1f
    } else if (pick < 0.66f) {
      // Orange
      p.r = 1f; p.g = MathUtils.random(0.5f, 0.8f); p.b = 0.1f
    } else {
      // Yellow
      p.r = 1f; p.g = MathUtils.random(0.8f, 1f); p.b = MathUtils.random(0.1f, 0.4f)
    }
  }
}
