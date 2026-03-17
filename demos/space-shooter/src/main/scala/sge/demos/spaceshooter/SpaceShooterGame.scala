/*
 * SGE Demos — vertical scrolling space shooter using ShapeRenderer.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package spaceshooter

import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.math.{FloatCounter, MathUtils, WindowedMean}
import sge.utils.{DynamicArray, ObjectSet, Poolable, Pool, ScreenUtils, Sort}
import sge.utils.viewport.FitViewport

import scala.compiletime.uninitialized

/** Vertical scrolling space shooter demo.
  *
  * Demonstrates: DynamicArray, Pool, ObjectSet, WindowedMean, FloatCounter, Sort, ShapeRenderer.
  */
object SpaceShooterGame extends shared.DemoScene {

  override def name: String = "Space Shooter"

  // --- World constants ---
  private val WorldW = 480f
  private val WorldH = 800f

  // --- Inner data types ---

  final class Bullet {
    var x: Float      = 0f
    var y: Float      = 0f
    var active: Boolean = false
  }
  object Bullet {
    given Poolable[Bullet] with {
      def reset(b: Bullet): Unit = {
        b.x = 0f
        b.y = 0f
        b.active = false
      }
    }
  }

  final class Enemy {
    var x: Float     = 0f
    var y: Float     = 0f
    var hp: Int      = 1
    var speed: Float = 80f
  }
  object Enemy {
    given Poolable[Enemy] with {
      def reset(e: Enemy): Unit = {
        e.x = 0f
        e.y = 0f
        e.hp = 1
        e.speed = 80f
      }
    }
  }

  final class Star {
    var x: Float     = 0f
    var y: Float     = 0f
    var speed: Float = 0f
  }

  // --- State ---
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport: FitViewport        = uninitialized

  private var playerX: Float = 0f
  private var playerY: Float = 0f
  private val PlayerSpeed    = 300f
  private val PlayerW        = 30f
  private val PlayerH        = 40f
  private val BulletSpeed    = 500f
  private val BulletW        = 4f
  private val BulletH        = 12f
  private val EnemySize      = 28f
  private val FireCooldown   = 0.15f

  private var fireCooldownTimer: Float = 0f
  private var score: Int               = 0
  private var spawnTimer: Float        = 0f

  private val bullets: DynamicArray[Bullet] = DynamicArray[Bullet]()
  private val enemies: DynamicArray[Enemy]  = DynamicArray[Enemy]()
  private var bulletPool: Pool.Default[Bullet] = uninitialized
  private var enemyPool: Pool.Default[Enemy]   = uninitialized

  private val hitThisFrame: ObjectSet[Enemy] = ObjectSet[Enemy]()
  private val killRate: WindowedMean         = WindowedMean(30)
  private val scoreStats: FloatCounter       = FloatCounter(50)
  private val stars: DynamicArray[Star]      = DynamicArray[Star]()

  // Ordering for back-to-front enemy sort (higher Y = further away = rendered first)
  private val enemyOrdering: Ordering[Enemy] = (a: Enemy, b: Enemy) =>
    java.lang.Float.compare(b.y, a.y)

  // --- Lifecycle ---

  override def init()(using Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(WorldW, WorldH)
    bulletPool = Pool.Default[Bullet](() => Bullet(), 64)
    enemyPool = Pool.Default[Enemy](() => Enemy(), 32)

    playerX = WorldW / 2f
    playerY = 60f
    score = 0
    spawnTimer = 0f
    fireCooldownTimer = 0f

    // Seed background stars
    var i = 0
    while (i < 80) {
      val s = Star()
      s.x = MathUtils.random(0f, WorldW)
      s.y = MathUtils.random(0f, WorldH)
      s.speed = MathUtils.random(40f, 160f)
      stars.add(s)
      i += 1
    }
  }

  override def render(dt: Float)(using Sge): Unit = {
    update(dt)
    draw()
  }

  override def resize(width: _root_.sge.Pixels, height: _root_.sge.Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    shapeRenderer.close()
  }

  // --- Update ---

  private def update(dt: Float)(using Sge): Unit = {
    val input = Sge().input

    // Player movement — keyboard
    if (input.isKeyPressed(Input.Keys.LEFT) || input.isKeyPressed(Input.Keys.A)) {
      playerX -= PlayerSpeed * dt
    }
    if (input.isKeyPressed(Input.Keys.RIGHT) || input.isKeyPressed(Input.Keys.D)) {
      playerX += PlayerSpeed * dt
    }

    // Player movement — touch: move toward touch X position + auto-fire
    if (input.isTouched()) {
      val touchWorldX = input.getX().toFloat / Sge().graphics.getWidth().toFloat * WorldW
      val diff = touchWorldX - playerX
      if (scala.math.abs(diff) > 4f) {
        playerX += MathUtils.clamp(diff, -PlayerSpeed * dt, PlayerSpeed * dt)
      }
    }

    playerX = MathUtils.clamp(playerX, PlayerW / 2f, WorldW - PlayerW / 2f)

    // Firing — keyboard SPACE or while touching
    fireCooldownTimer -= dt
    if ((input.isKeyPressed(Input.Keys.SPACE) || input.isTouched()) && fireCooldownTimer <= 0f) {
      fireCooldownTimer = FireCooldown
      val b = bulletPool.obtain()
      b.x = playerX
      b.y = playerY + PlayerH / 2f
      b.active = true
      bullets.add(b)
    }

    // Update bullets
    var i = bullets.size - 1
    while (i >= 0) {
      val b = bullets(i)
      b.y += BulletSpeed * dt
      if (b.y > WorldH + BulletH) {
        b.active = false
        bullets.removeIndex(i)
        bulletPool.free(b)
      }
      i -= 1
    }

    // Spawn enemies — rate increases with kill rate
    val meanKillRate = killRate.getMean()
    val spawnInterval = MathUtils.clamp(1.2f - meanKillRate * 0.15f, 0.25f, 1.2f)
    spawnTimer += dt
    if (spawnTimer >= spawnInterval) {
      spawnTimer -= spawnInterval
      val e = enemyPool.obtain()
      e.x = MathUtils.random(EnemySize, WorldW - EnemySize)
      e.y = WorldH + EnemySize
      e.hp = 1
      e.speed = MathUtils.random(60f, 140f + meanKillRate * 20f)
      enemies.add(e)
    }

    // Update enemies
    i = enemies.size - 1
    while (i >= 0) {
      val e = enemies(i)
      e.y -= e.speed * dt
      if (e.y < -EnemySize) {
        enemies.removeIndex(i)
        enemyPool.free(e)
      }
      i -= 1
    }

    // Collision detection
    hitThisFrame.clear()
    i = bullets.size - 1
    while (i >= 0) {
      val b = bullets(i)
      var j = enemies.size - 1
      var bulletConsumed = false
      while (j >= 0 && !bulletConsumed) {
        val e = enemies(j)
        if (!hitThisFrame.contains(e)) {
          val dx = b.x - e.x
          val dy = b.y - e.y
          val dist = dx * dx + dy * dy
          val collisionR = EnemySize * 0.5f + BulletW
          if (dist < collisionR * collisionR) {
            hitThisFrame.add(e)
            e.hp -= 1
            if (e.hp <= 0) {
              enemies.removeIndex(j)
              enemyPool.free(e)
              score += 1
              killRate.addValue(1f)
              scoreStats.put(score.toFloat)
            }
            bullets.removeIndex(i)
            bulletPool.free(b)
            bulletConsumed = true
          }
        }
        j -= 1
      }
      i -= 1
    }

    // Sort enemies back-to-front for rendering
    if (enemies.size > 1) {
      Sort.sort(enemies, enemyOrdering)
    }

    // Update stars
    stars.foreach { s =>
      s.y -= s.speed * dt
      if (s.y < 0f) {
        s.y = WorldH
        s.x = MathUtils.random(0f, WorldW)
      }
    }
  }

  // --- Draw ---

  private def draw()(using Sge): Unit = {
    ScreenUtils.clear(0.05f, 0.02f, 0.1f, 1f)
    viewport.apply()
    val cam = viewport.camera
    shapeRenderer.setProjectionMatrix(cam.combined)

    // Stars (filled white dots)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color.WHITE)
    stars.foreach { s =>
      shapeRenderer.circle(s.x, s.y, 1.2f)
    }

    // Enemies (red rectangles)
    shapeRenderer.setColor(Color.RED)
    enemies.foreach { e =>
      shapeRenderer.rectangle(e.x - EnemySize / 2f, e.y - EnemySize / 2f, EnemySize, EnemySize)
    }

    // Bullets (yellow rectangles)
    shapeRenderer.setColor(Color.YELLOW)
    bullets.foreach { b =>
      shapeRenderer.rectangle(b.x - BulletW / 2f, b.y - BulletH / 2f, BulletW, BulletH)
    }

    // Player (green triangle)
    shapeRenderer.setColor(Color.GREEN)
    shapeRenderer.triangle(
      playerX - PlayerW / 2f, playerY - PlayerH / 2f,
      playerX + PlayerW / 2f, playerY - PlayerH / 2f,
      playerX, playerY + PlayerH / 2f
    )
    shapeRenderer.end()

    // Score indicator — small cyan bar at top proportional to score
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color.CYAN)
    val barW = MathUtils.clamp(score.toFloat * 4f, 0f, WorldW)
    shapeRenderer.rectangle(0f, WorldH - 6f, barW, 4f)
    shapeRenderer.end()
  }
}
