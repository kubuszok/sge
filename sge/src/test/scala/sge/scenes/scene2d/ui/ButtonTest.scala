/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package ui

import sge.scenes.scene2d.utils.{ BaseDrawable, ChangeListener }
import sge.utils.Nullable

/** Tests for Button: checked state, toggle, disabled, programmatic change events, style. */
class ButtonTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  private def makeStyle(): Button.ButtonStyle = {
    val style = Button.ButtonStyle()
    style.up = Nullable(BaseDrawable())
    style.down = Nullable(BaseDrawable())
    style
  }

  // ---------------------------------------------------------------------------
  // Default state
  // ---------------------------------------------------------------------------

  test("Button default state") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    assert(!btn.checked)
    assert(!btn.disabled)
    assert(btn.programmaticChangeEvents)
    assertEquals(btn.touchable, Touchable.enabled)
  }

  // ---------------------------------------------------------------------------
  // Checked state
  // ---------------------------------------------------------------------------

  test("setChecked changes checked state") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    btn.programmaticChangeEvents = false
    btn.setChecked(true)
    assert(btn.checked)
    btn.setChecked(false)
    assert(!btn.checked)
  }

  test("toggle flips checked state") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    btn.programmaticChangeEvents = false
    assert(!btn.checked)
    btn.toggle()
    assert(btn.checked)
    btn.toggle()
    assert(!btn.checked)
  }

  // ---------------------------------------------------------------------------
  // Disabled
  // ---------------------------------------------------------------------------

  test("disabled property") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    assert(!btn.disabled)
    btn.disabled = true
    assert(btn.disabled)
  }

  // ---------------------------------------------------------------------------
  // Change events
  // ---------------------------------------------------------------------------

  test("setChecked fires ChangeEvent when programmaticChangeEvents is true") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    var fired = false
    btn.addListener(new ChangeListener() {
      def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        fired = true
    })

    btn.setChecked(true)
    assert(fired)
  }

  test("setChecked does not fire ChangeEvent when programmaticChangeEvents is false") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    var fired = false
    btn.programmaticChangeEvents = false
    btn.addListener(new ChangeListener() {
      def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        fired = true
    })

    btn.setChecked(true)
    assert(!fired)
  }

  test("ChangeEvent cancellation reverts checked state") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    btn.addListener(new ChangeListener() {
      def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        event.cancel()
    })

    btn.setChecked(true)
    // Cancelling the change event should revert the checked state
    assert(!btn.checked)
  }

  test("setChecked with same state is no-op") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    var fired = false
    btn.addListener(new ChangeListener() {
      def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        fired = true
    })

    // Button starts unchecked, setting to false should not fire
    btn.setChecked(false)
    assert(!fired)
  }

  // ---------------------------------------------------------------------------
  // Style
  // ---------------------------------------------------------------------------

  test("Button style can be set and retrieved") {
    given Sge = ctx()
    val style = makeStyle()
    val btn   = Button(style)
    assert(btn.style eq style)
  }

  test("Button setStyle changes style") {
    given Sge  = ctx()
    val style1 = makeStyle()
    val style2 = makeStyle()
    val btn    = Button(style1)
    btn.setStyle(style2)
    assert(btn.style eq style2)
  }

  // ---------------------------------------------------------------------------
  // ButtonStyle
  // ---------------------------------------------------------------------------

  test("ButtonStyle default fields are empty") {
    val style = Button.ButtonStyle()
    assert(style.up.isEmpty)
    assert(style.down.isEmpty)
    assert(style.over.isEmpty)
    assert(style.checked.isEmpty)
    assert(style.disabled.isEmpty)
    assertEquals(style.pressedOffsetX, 0f)
    assertEquals(style.pressedOffsetY, 0f)
  }

  test("ButtonStyle copy constructor") {
    val source = Button.ButtonStyle()
    source.up = Nullable(BaseDrawable())
    source.down = Nullable(BaseDrawable())
    source.pressedOffsetY = 3f

    val copy = Button.ButtonStyle(source)
    assert(copy.up.isDefined)
    assert(copy.down.isDefined)
    assertEquals(copy.pressedOffsetY, 3f)
  }

  test("ButtonStyle three-arg constructor") {
    val up      = BaseDrawable()
    val down    = BaseDrawable()
    val checked = BaseDrawable()
    val style   = Button.ButtonStyle(Nullable(up), Nullable(down), Nullable(checked))
    assert(style.up.isDefined)
    assert(style.down.isDefined)
    assert(style.checked.isDefined)
  }

  // ---------------------------------------------------------------------------
  // Drawable constructor variants
  // ---------------------------------------------------------------------------

  test("Button(up) constructor") {
    given Sge = ctx()
    val btn   = Button(Nullable(BaseDrawable()))
    assert(btn.style.up.isDefined)
  }

  test("Button(up, down) constructor") {
    given Sge = ctx()
    val btn   = Button(Nullable(BaseDrawable()), Nullable(BaseDrawable()))
    assert(btn.style.up.isDefined)
    assert(btn.style.down.isDefined)
  }

  test("Button(up, down, checked) constructor") {
    given Sge = ctx()
    val btn   = Button(Nullable(BaseDrawable()), Nullable(BaseDrawable()), Nullable(BaseDrawable()))
    assert(btn.style.up.isDefined)
    assert(btn.style.down.isDefined)
    assert(btn.style.checked.isDefined)
  }

  // ---------------------------------------------------------------------------
  // Button is a Table
  // ---------------------------------------------------------------------------

  test("Button is a Table and can add children") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    val child = Actor()
    btn.addActor(child)
    assertEquals(btn.children.size, 1)
  }

  // ---------------------------------------------------------------------------
  // clickListener is installed
  // ---------------------------------------------------------------------------

  test("Button has a clickListener installed") {
    given Sge = ctx()
    val btn   = Button(makeStyle())
    assert(btn.clickListener != null) // scalastyle:ignore null
    assert(btn.listeners.size >= 1)
  }
}
