/*
 * SGE Gauntlet — live scene2d results grid for windowed runs.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.graphics.{ ClearMask, Color, Pixmap, Texture }
import sge.graphics.g2d.TextureRegion
import sge.scenes.scene2d.{ InputEvent, InputListener, Stage }
import sge.scenes.scene2d.ui.{ Image, Label }
import sge.scenes.scene2d.utils.ClickListener
import sge.Input.{ Key, Keys }
import lowlevel.Nullable

import scala.collection.mutable

/** Live results grid drawn to the window's default framebuffer while (and after) the probes run.
  *
  * Left column: one row per probe with a colored status tag; arrow keys or clicks select a row. Right panel: the selected probe's checks and log, plus the probe's FBO screenshot so a human SEES what
  * each GPU probe rendered. Escape exits.
  */
final class GauntletUi(onExit: () => Unit)(using sge: Sge) {

  private val font = {
    val f = GauntletFont.create()
    f.data.setScale(1.5f)
    f
  }

  private val stage = new Stage()

  private var results:   List[ProbeResult] = Nil
  private var running:   Option[String]    = None
  private var selected:  Int               = 0
  private var dirty:     Boolean           = true
  private val shots:     mutable.Map[String, TextureRegion] = mutable.Map.empty
  private val shotOwned: mutable.ListBuffer[Texture]        = mutable.ListBuffer.empty

  sge.input.setInputProcessor(stage)

  stage.addListener(new InputListener() {
    override def keyDown(event: InputEvent, keycode: Key): Boolean =
      keycode match {
        case Keys.UP =>
          select(selected - 1)
          true
        case Keys.DOWN =>
          select(selected + 1)
          true
        case Keys.ESCAPE =>
          onExit()
          true
        case _ => false
      }
  })

  def setResults(newResults: List[ProbeResult]): Unit = {
    results = newResults
    dirty = true
  }

  /** Keeps the probe's captured FBO content as a texture for the inspection panel. Called with the pixmap still open; the texture copies it to the GPU. */
  def addScreenshot(id: String, pixmap: Pixmap): Unit = {
    val texture = Texture(pixmap)
    shotOwned += texture
    val region = TextureRegion(texture)
    region.flip(false, true) // FBO readback is bottom-up
    shots.update(id, region)
  }

  def resize(width: Pixels, height: Pixels): Unit = {
    stage.viewport.update(width, height, true)
    dirty = true
  }

  private def select(index: Int): Unit = {
    if (results.nonEmpty) {
      selected = Math.max(0, Math.min(results.length - 1, index))
      dirty = true
    }
  }

  private def statusColor(status: ProbeStatus): Color = status match {
    case ProbeStatus.Passed         => new Color(0.25f, 0.85f, 0.35f, 1f)
    case ProbeStatus.Failed         => new Color(0.95f, 0.25f, 0.25f, 1f)
    case ProbeStatus.SkippedGpu     => new Color(0.85f, 0.85f, 0.3f, 1f)
    case ProbeStatus.KnownFail      => new Color(0.95f, 0.6f, 0.2f, 1f)
    case ProbeStatus.UnexpectedPass => new Color(0.95f, 0.3f, 0.9f, 1f)
  }

  private def label(text: String, x: Float, y: Float, color: Color): Label = {
    val l = new Label(Nullable(text), new Label.LabelStyle(font, Nullable(color)))
    l.setPosition(x, y)
    l
  }

  private def rebuild(): Unit = {
    stage.clear()
    val white = Color.WHITE
    val gray  = new Color(0.7f, 0.7f, 0.7f, 1f)

    val headline = running match {
      case Some(id) => s"SGE GAUNTLET - RUNNING $id"
      case None     => s"SGE GAUNTLET - DONE, HARD FAILURES: ${ProbeResult.hardFailures(results)} (ESC EXITS)"
    }
    stage.addActor(label(headline, 20f, 690f, white))

    results.zipWithIndex.foreach { case (r, i) =>
      val marker = if (i == selected) "> " else "  "
      val row    = label(f"$marker${r.status.wireName}%-15s ${r.id}", 20f, 660f - i * 24f, statusColor(r.status))
      val idx    = i
      row.addListener(new ClickListener() {
        override def clicked(event: InputEvent, x: Float, y: Float): Unit =
          select(idx)
      })
      stage.addActor(row)
    }

    results.lift(selected).foreach { r =>
      var y = 660f
      def line(text: String, color: Color): Unit = {
        stage.addActor(label(text, 620f, y, color))
        y -= 20f
      }
      line(s"${r.id} [${r.status.wireName}] ${r.durationMs}ms", white)
      r.checks.take(14).foreach { c =>
        val mark  = if (c.passed) "+" else "-"
        val color = if (c.passed) statusColor(ProbeStatus.Passed) else statusColor(ProbeStatus.Failed)
        line(s" $mark ${c.name}", color)
        if (!c.passed) {
          line(s"    expected ${c.expected.take(48)}", gray)
          line(s"    actual   ${c.actual.take(48)}", gray)
        }
      }
      r.logLines.take(6).foreach(l => line(s" | ${l.take(60)}", gray))
      shots.get(r.id).foreach { region =>
        val image = new Image(region)
        image.setBounds(620f, 20f, 512f, 288f)
        stage.addActor(image)
      }
    }
  }

  /** Renders the grid to the currently bound (default) framebuffer. */
  def render(currentlyRunning: Option[String]): Unit = {
    if (currentlyRunning != running) {
      running = currentlyRunning
      dirty = true
    }
    if (dirty) {
      rebuild()
      dirty = false
    }
    val gl = sge.graphics.gl20
    gl.glClearColor(0.08f, 0.08f, 0.1f, 1f)
    gl.glClear(ClearMask.ColorBufferBit | ClearMask.DepthBufferBit)
    stage.act()
    stage.draw()
  }

  def dispose(): Unit = {
    stage.close()
    font.close()
    shotOwned.foreach(_.close())
  }
}
