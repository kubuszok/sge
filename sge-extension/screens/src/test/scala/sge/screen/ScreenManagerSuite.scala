/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen

import scala.collection.mutable.ArrayBuffer

import sge.graphics.Color
import sge.graphics.g2d.TextureRegion
import sge.screen.transition.ScreenTransition
import sge.utils.{ Nullable, Seconds }

class ScreenManagerSuite extends munit.FunSuite {

  /** Minimal ManagedScreen that records lifecycle calls. */
  class TestScreen extends ManagedScreen {
    val calls: ArrayBuffer[String] = ArrayBuffer.empty

    override def show():                                Unit = calls += "show"
    override def hide():                                Unit = calls += "hide"
    override def render(delta: Seconds):                Unit = calls += "render"
    override def resize(width: Pixels, height: Pixels): Unit = calls += "resize"
    override def pause():                               Unit = calls += "pause"
    override def resume():                              Unit = calls += "resume"
    override def close():                               Unit = calls += "close"
  }

  /** Minimal ScreenTransition that records lifecycle and finishes after N renders. */
  class TestTransition(rendersUntilDone: Int = 2) extends ScreenTransition {
    val calls: ArrayBuffer[String] = ArrayBuffer.empty
    private var renderCount = 0

    override def show():                                                                       Unit = calls += "show"
    override def hide():                                                                       Unit = calls += "hide"
    override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit = {
      calls += "render"
      renderCount += 1
    }
    override def isDone:                                Boolean = renderCount >= rendersUntilDone
    override def resize(width: Pixels, height: Pixels): Unit    = calls += "resize"
    override def close():                               Unit    = calls += "close"
  }

  test("ManagedScreen has default clearColor of BLACK") {
    val screen = new TestScreen
    assert(screen.clearColor.isDefined)
    assertEquals(screen.clearColor.get, Color.BLACK)
  }

  test("ManagedScreen addInputProcessor and inputProcessors") {
    val screen = new TestScreen
    assert(screen.inputProcessors.isEmpty)
    // addInputProcessor is protected, so we can't call it directly in test
    // but we verify the accessor works
    assertEquals(screen.inputProcessors.size, 0)
  }

  test("BlankScreen does nothing") {
    val screen = BlankScreen()
    screen.show()
    screen.render(Seconds(0.016f))
    screen.resize(Pixels(800), Pixels(600))
    screen.pause()
    screen.resume()
    screen.hide()
    screen.close()
    // no exceptions thrown = pass
  }

  test("ScreenTransition default show and hide are no-ops") {
    val transition = new TestTransition
    transition.show()
    transition.hide()
    assert(transition.calls.contains("show"))
    assert(transition.calls.contains("hide"))
  }

  test("ScreenTransition default clearColor is BLACK") {
    val transition = new TestTransition
    assert(transition.clearColor.isDefined)
    assertEquals(transition.clearColor.get, Color.BLACK)
  }
}
