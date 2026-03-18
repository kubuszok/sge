/*
 * SGE Demos — Classic Pong game using ShapeRenderer.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.pong

import scala.compiletime.uninitialized

import sge.{Input, Pixels, Sge}
import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.graphics.glutils.ShapeRenderer.ShapeType
import sge.math.{Interpolation, MathUtils, Vector2}
import sge.utils.ScreenUtils
import sge.utils.viewport.FitViewport
import demos.shared.DemoScene

/** Classic Pong: two paddles, a bouncing ball, 7-segment score display. */
object PongGame extends DemoScene {

  override def name: String = "Pong"

  // World dimensions
  private val W = 800f
  private val H = 600f

  // Paddle dimensions and speed
  private val PaddleW     = 12f
  private val PaddleH     = 80f
  private val PaddleSpeed = 400f
  private val PaddleGap   = 30f

  // Ball properties
  private val BallSize     = 10f
  private val InitBallSpd  = 300f
  private val MaxBallSpd   = 600f
  private val SpeedUpRatio = 1.08f

  // State
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport: FitViewport        = uninitialized
  private val touchWorld = Vector2()

  private var leftY  = 0f
  private var rightY = 0f
  private var ballX  = 0f
  private var ballY  = 0f
  private var ballDx = 0f
  private var ballDy = 0f
  private var ballSpd = 0f
  private var scoreL  = 0
  private var scoreR  = 0

  override def init()(using Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(W, H)
    resetBall()
    leftY = H / 2f - PaddleH / 2f
    rightY = leftY
  }

  override def render(dt: Float)(using sge: Sge): Unit = {
    val input = sge.input

    // --- Input: left paddle ---
    // Keyboard: W/Up to move up, S/Down to move down
    if (input.isKeyPressed(Input.Keys.W) || input.isKeyPressed(Input.Keys.UP)) {
      leftY += PaddleSpeed * dt
    }
    if (input.isKeyPressed(Input.Keys.S) || input.isKeyPressed(Input.Keys.DOWN)) {
      leftY -= PaddleSpeed * dt
    }

    // Touch/mouse: drag anywhere in the left half to control the left paddle
    if (input.touched) {
      touchWorld.set(input.x.toFloat, input.y.toFloat)
      viewport.unproject(touchWorld)
      // Only respond to touches in the left half of the world
      if (touchWorld.x < W / 2f) {
        val targetY = touchWorld.y - PaddleH / 2f
        leftY = MathUtils.lerp(leftY, targetY, 12f * dt)
      }
    }

    leftY = MathUtils.clamp(leftY, 0f, H - PaddleH)

    // --- AI: right paddle tracks ball Y ---
    val targetY = ballY - PaddleH / 2f
    rightY = MathUtils.lerp(rightY, targetY, 4f * dt)
    rightY = MathUtils.clamp(rightY, 0f, H - PaddleH)

    // --- Ball movement ---
    ballX += ballDx * ballSpd * dt
    ballY += ballDy * ballSpd * dt

    // Top/bottom bounce
    if (ballY <= 0f) { ballY = 0f; ballDy = scala.math.abs(ballDy) }
    if (ballY + BallSize >= H) { ballY = H - BallSize; ballDy = -scala.math.abs(ballDy) }

    // Left paddle collision
    val lPadX = PaddleGap
    if (ballDx < 0f && ballX <= lPadX + PaddleW && ballX + BallSize >= lPadX &&
      ballY + BallSize >= leftY && ballY <= leftY + PaddleH) {
      ballDx = scala.math.abs(ballDx)
      ballX = lPadX + PaddleW
      speedUp()
    }

    // Right paddle collision
    val rPadX = W - PaddleGap - PaddleW
    if (ballDx > 0f && ballX + BallSize >= rPadX && ballX <= rPadX + PaddleW &&
      ballY + BallSize >= rightY && ballY <= rightY + PaddleH) {
      ballDx = -scala.math.abs(ballDx)
      ballX = rPadX - BallSize
      speedUp()
    }

    // Scoring
    if (ballX < -BallSize) { scoreR += 1; resetBall() }
    if (ballX > W + BallSize) { scoreL += 1; resetBall() }

    // --- Draw ---
    ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f)
    viewport.apply()
    shapeRenderer.setProjectionMatrix(viewport.camera.combined)

    shapeRenderer.drawing(ShapeType.Filled) {
      // Center line (dashed)
      shapeRenderer.setColor(Color.GRAY)
      var dashY = 0f
      while (dashY < H) {
        shapeRenderer.rectangle(W / 2f - 1f, dashY, 2f, 10f)
        dashY += 20f
      }

      // Paddles
      shapeRenderer.setColor(Color.WHITE)
      shapeRenderer.rectangle(PaddleGap, leftY, PaddleW, PaddleH)
      shapeRenderer.rectangle(W - PaddleGap - PaddleW, rightY, PaddleW, PaddleH)

      // Ball
      shapeRenderer.setColor(Color.YELLOW)
      shapeRenderer.rectangle(ballX, ballY, BallSize, BallSize)

      // Scores
      drawDigit(W / 2f - 60f, H - 60f, scoreL, Color.WHITE)
      drawDigit(W / 2f + 30f, H - 60f, scoreR, Color.WHITE)
    }
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    shapeRenderer.close()
  }

  // --- Helpers ---

  private def resetBall(): Unit = {
    ballX = W / 2f - BallSize / 2f
    ballY = H / 2f - BallSize / 2f
    ballSpd = InitBallSpd
    val angle = MathUtils.random(-0.4f, 0.4f)
    val dir = if (MathUtils.random() > 0.5f) 1f else -1f
    ballDx = dir * MathUtils.cos(angle)
    ballDy = MathUtils.sin(angle)
  }

  private def speedUp(): Unit = {
    val t = MathUtils.clamp((ballSpd - InitBallSpd) / (MaxBallSpd - InitBallSpd), 0f, 1f)
    ballSpd = MathUtils.lerp(ballSpd * SpeedUpRatio, MaxBallSpd, Interpolation.pow2.apply(t))
    ballSpd = MathUtils.clamp(ballSpd, InitBallSpd, MaxBallSpd)
  }

  /** Draw a single digit (0-9) using 7-segment style filled rectangles.
    * Origin is top-left of the digit cell (40 wide x 50 tall).
    */
  private def drawDigit(ox: Float, oy: Float, value: Int, color: Color): Unit = {
    shapeRenderer.setColor(color)
    val d  = value % 10
    val sw = 20f  // segment width
    val sh = 4f   // segment thickness
    val sl = 20f  // segment length (vertical)

    // Segment encoding: bits = top, top-right, bot-right, bottom, bot-left, top-left, middle
    //                          6     5          4          3       2         1         0
    val segs = Array(0x7e, 0x30, 0x6d, 0x79, 0x33, 0x5b, 0x5f, 0x70, 0x7f, 0x7b)
    val s    = segs(d)

    // Top horizontal
    if ((s & 0x40) != 0) shapeRenderer.rectangle(ox, oy, sw, sh)
    // Top-left vertical
    if ((s & 0x02) != 0) shapeRenderer.rectangle(ox - sh, oy - sl, sh, sl)
    // Top-right vertical
    if ((s & 0x20) != 0) shapeRenderer.rectangle(ox + sw, oy - sl, sh, sl)
    // Middle horizontal
    if ((s & 0x01) != 0) shapeRenderer.rectangle(ox, oy - sl, sw, sh)
    // Bottom-left vertical
    if ((s & 0x04) != 0) shapeRenderer.rectangle(ox - sh, oy - 2f * sl - sh, sh, sl)
    // Bottom-right vertical
    if ((s & 0x10) != 0) shapeRenderer.rectangle(ox + sw, oy - 2f * sl - sh, sh, sl)
    // Bottom horizontal
    if ((s & 0x08) != 0) shapeRenderer.rectangle(ox, oy - 2f * sl - sh, sw, sh)
  }
}
