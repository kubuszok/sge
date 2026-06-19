/*
 * SGE Demos — Game/Screen showcase.
 *
 * Unlike the other 11 demos (which run through the `DemoScene` / `SingleSceneApp`
 * launcher abstraction), this demo is built directly on SGE's public
 * `Game` / `Screen` API — the same path documented in docs/getting-started.md.
 *
 * It wires two screens together: a `MenuScreen` (prompt, press Space / tap to
 * start) and a `PlayScreen` (a bouncing rectangle, Escape returns to the menu).
 * Screen transitions go through `game.screen = ...`, which calls hide()/show()/
 * resize() on the old and new screens (see sge.Game).
 *
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package demos.gamescreens

import scala.compiletime.uninitialized

import sge.{ Game, Input, Pixels, Screen, Sge, WorldUnits }
import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.utils.Seconds
import sge.utils.ScreenUtils
import sge.utils.viewport.FitViewport

/** Root application: a `Game` built on the public `Game` / `Screen` API.
  *
  * The launcher hands us an `Sge` context (via the `(using Sge)` constructor
  * parameter); we thread it into every screen we create.
  */
class GameScreensDemo()(using Sge) extends Game {

  override def create(): Unit =
    // Set the first screen. The setter calls show()/resize() for us.
    screen = MenuScreen(this)
}

object GameScreensDemo {
  // Shared world dimensions for both screens' viewports.
  val WorldW = 800f
  val WorldH = 600f
}

/** Title screen: press Space (or tap) to start. Draws a pulsing prompt bar. */
class MenuScreen(game: GameScreensDemo)(using sge: Sge) extends Screen {

  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport:      FitViewport   = uninitialized
  private var elapsed:       Float         = 0f

  override def show(): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(WorldUnits(GameScreensDemo.WorldW), WorldUnits(GameScreensDemo.WorldH))
    elapsed = 0f
  }

  override def render(delta: Seconds): Unit = {
    elapsed += delta.toFloat

    // Dark blue background.
    ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f)

    viewport.apply()
    shapeRenderer.setProjectionMatrix(viewport.camera.combined)

    // A pulsing "Press Space / Tap to start" prompt bar, centred horizontally.
    val pulse = 0.4f + 0.3f * scala.math.sin(elapsed * 3f).toFloat
    val barW  = 360f
    val barH  = 40f
    val x     = (GameScreensDemo.WorldW - barW) / 2f
    val y     = GameScreensDemo.WorldH / 2f - barH / 2f
    shapeRenderer.drawing(ShapeRenderer.ShapeType.Filled) {
      shapeRenderer.setColor(Color(0.2f, 0.5f, pulse, 1f))
      shapeRenderer.rectangle(x, y, barW, barH)
    }
    shapeRenderer.drawing(ShapeRenderer.ShapeType.Line) {
      shapeRenderer.setColor(Color.WHITE)
      shapeRenderer.rectangle(x, y, barW, barH)
    }

    // isKeyJustPressed / justTouched fire once per press, not every frame.
    if (sge.input.isKeyJustPressed(Input.Keys.SPACE) || sge.input.justTouched()) {
      // Assigning a new screen hides this one and shows the next.
      game.screen = PlayScreen(game)
    }
  }

  override def resize(width: Pixels, height: Pixels): Unit =
    viewport.update(width, height, true)

  override def hide(): Unit =
    close()

  override def close(): Unit =
    shapeRenderer.close()
}

/** Play screen: a rectangle bounces around the world. Escape returns to the menu. */
class PlayScreen(game: GameScreensDemo)(using sge: Sge) extends Screen {

  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport:      FitViewport   = uninitialized

  // Sprite state (a square bouncing off the world edges).
  private val size = 50f
  private var posX = 100f
  private var posY = 100f
  private var velX = 220f
  private var velY = 170f

  override def show(): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(WorldUnits(GameScreensDemo.WorldW), WorldUnits(GameScreensDemo.WorldH))
    posX = 100f
    posY = 100f
    velX = 220f
    velY = 170f
  }

  override def render(delta: Seconds): Unit = {
    val dt = delta.toFloat

    // Integrate position and bounce off the world bounds.
    posX += velX * dt
    posY += velY * dt
    if (posX < 0f) { posX = 0f; velX = -velX }
    if (posY < 0f) { posY = 0f; velY = -velY }
    if (posX + size > GameScreensDemo.WorldW) { posX = GameScreensDemo.WorldW - size; velX = -velX }
    if (posY + size > GameScreensDemo.WorldH) { posY = GameScreensDemo.WorldH - size; velY = -velY }

    ScreenUtils.clear(0.1f, 0.12f, 0.1f, 1f)

    viewport.apply()
    shapeRenderer.setProjectionMatrix(viewport.camera.combined)

    shapeRenderer.drawing(ShapeRenderer.ShapeType.Filled) {
      shapeRenderer.setColor(Color(0.9f, 0.7f, 0.2f, 1f))
      shapeRenderer.rectangle(posX, posY, size, size)
    }

    // Press Escape to go back to the menu.
    if (sge.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      game.screen = MenuScreen(game)
    }
  }

  override def resize(width: Pixels, height: Pixels): Unit =
    viewport.update(width, height, true)

  override def hide(): Unit =
    close()

  override def close(): Unit =
    shapeRenderer.close()
}
