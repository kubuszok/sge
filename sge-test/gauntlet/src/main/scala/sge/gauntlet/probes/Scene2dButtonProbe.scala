/*
 * SGE Gauntlet — scene2d: Button state cascade via synthetic input (ISS-758).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.graphics.{ Pixmap, Texture }
import sge.graphics.g2d.TextureRegion
import sge.scenes.scene2d.Stage
import sge.scenes.scene2d.ui.{ Button, Skin }
import sge.scenes.scene2d.utils.{ Drawable, TextureRegionDrawable }
import sge.utils.Seconds
import lowlevel.Nullable

import scala.collection.mutable.ListBuffer

/** Builds a Button from a minimal programmatic Skin, drives it with synthetic hover/click events through the Stage's input-processor interface, and asserts the state flags and the background drawable
  * cascade.
  *
  * knownIssue ISS-758: `Button.backgroundDrawable` turned the original fall-through into closed if/else nesting — a checked+hovered button whose style has no `checkedOver` must fall back to
  * `checked`, but renders `up` instead.
  */
object Scene2dButtonProbe extends FeatureProbe {

  final private class ProbeButton(style: Button.ButtonStyle)(using Sge) extends Button(style) {
    def bg: Nullable[Drawable] = backgroundDrawable
  }

  override def id: String = "scene2d/button-states"

  override def area: String = "scene2d"

  override def requiresGpu: Boolean = true

  override def frames: Int = 6

  private val checks = ListBuffer.empty[Check]

  private var stage:    Option[Stage]       = None
  private var button:   Option[ProbeButton] = None
  private var skin:     Option[Skin]        = None
  private var textures: List[Texture]       = Nil

  private var up:      Option[Drawable] = None
  private var down:    Option[Drawable] = None
  private var checked: Option[Drawable] = None

  private var overAfterHover    = false
  private var pressedDuringDown = false
  private var checkedAfterClick = false
  private var bgCheckedNoHover:   Option[String] = None
  private var bgCheckedWithHover: Option[String] = None

  // Button placed at world (100,100) size 200x100; ScreenViewport is 1:1, screen y is inverted.
  private val CenterScreenX = 200
  private val CenterScreenY = 720 - 150

  private def solidDrawable(rgba: Int)(using Sge): (Texture, Drawable) = {
    val pixmap = Pixmap(4, 4, Pixmap.Format.RGBA8888)
    pixmap.setColor(rgba)
    pixmap.fill()
    val texture = Texture(pixmap)
    pixmap.close()
    (texture, TextureRegionDrawable(TextureRegion(texture)))
  }

  private def describe(d: Nullable[Drawable]): String =
    d.fold("<empty>") { drawable =>
      if (up.exists(_ eq drawable)) "up"
      else if (down.exists(_ eq drawable)) "down"
      else if (checked.exists(_ eq drawable)) "checked"
      else "<other>"
    }

  override def init(ctx: ProbeContext): Unit = {
    checks.clear()
    overAfterHover = false
    pressedDuringDown = false
    checkedAfterClick = false
    bgCheckedNoHover = None
    bgCheckedWithHover = None
    given Sge = ctx.sgeCtx

    val (upTex, upD)           = solidDrawable(0x888888ff)
    val (downTex, downD)       = solidDrawable(0xff3333ff)
    val (checkedTex, checkedD) = solidDrawable(0x33cc33ff)
    textures = List(upTex, downTex, checkedTex)
    up = Some(upD)
    down = Some(downD)
    checked = Some(checkedD)

    val style = new Button.ButtonStyle()
    style.up = Nullable(upD)
    style.down = Nullable(downD)
    style.checked = Nullable(checkedD)
    // deliberately NO checkedOver — the ISS-758 fall-through case

    // exercise the programmatic-Skin path: register + resolve the style by name
    val s = new Skin()
    s.add("default", style)
    skin = Some(s)

    val st = new Stage()
    st.viewport.update(Pixels(ctx.width), Pixels(ctx.height), true)
    val b = new ProbeButton(s.get[Button.ButtonStyle]("default"))
    b.setBounds(100f, 100f, 200f, 100f)
    st.addActor(b)
    stage = Some(st)
    button = Some(b)
    ctx.setInputTarget(st)
    ctx.clear(0.05f, 0.05f, 0.05f, 1f)
  }

  override def render(ctx: ProbeContext, frame: Int): Unit =
    (stage, button) match {
      case (Some(st), Some(b)) =>
        st.act(Seconds(ctx.fixedDelta))
        frame match {
          case 1 => ()
          case 2 =>
            val _ = ctx.injectMouseMoved(CenterScreenX, CenterScreenY)
            st.act(Seconds(ctx.fixedDelta))
            overAfterHover = b.isOver
          case 3 =>
            val _ = ctx.injectTouchDown(CenterScreenX, CenterScreenY)
            pressedDuringDown = b.isPressed
          case 4 =>
            val _ = ctx.injectTouchUp(CenterScreenX, CenterScreenY)
            checkedAfterClick = b.isChecked
          case 5 =>
            // move the pointer away: checked, not hovered. ClickListener keeps the
            // button "visually pressed" for 0.1s wall time after touchUp — wait it
            // out (bounded) so we sample the settled checked state, not the tail of
            // the press.
            val _ = ctx.injectMouseMoved(1200, 20)
            st.act(Seconds(ctx.fixedDelta))
            val deadline = System.currentTimeMillis() + 1000L
            while (b.isPressed && System.currentTimeMillis() < deadline) {
              Thread.sleep(20L)
              st.act(Seconds(ctx.fixedDelta))
            }
            bgCheckedNoHover = Some(describe(b.bg))
          case _ =>
            // hover again: checked + over without checkedOver must fall back to checked (ISS-758)
            val _ = ctx.injectMouseMoved(CenterScreenX, CenterScreenY)
            st.act(Seconds(ctx.fixedDelta))
            bgCheckedWithHover = Some(describe(b.bg))
        }
        ctx.clear(0.05f, 0.05f, 0.05f, 1f)
        st.draw()
      case _ => ()
    }

  override def verify(ctx: ProbeContext): List[Check] = {
    checks += Check.eq("hover-sets-over", true, overAfterHover)
    checks += Check.eq("touchdown-sets-pressed", true, pressedDuringDown)
    checks += Check.eq("click-toggles-checked", true, checkedAfterClick)
    checks += Check.eq("checked-drawable-no-hover", "checked", bgCheckedNoHover.getOrElse("<not recorded>"))
    checks += Check.eq("checked-drawable-hover-fallback", "checked", bgCheckedWithHover.getOrElse("<not recorded>"))
    stage.foreach(_.close())
    skin.foreach(_.close())
    textures.foreach(_.close())
    stage = None
    button = None
    skin = None
    textures = Nil
    checks.toList
  }
}
