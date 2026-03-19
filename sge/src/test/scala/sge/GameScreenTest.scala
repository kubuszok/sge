/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge

import sge.utils.Nullable
import sge.utils.Seconds

class GameScreenTest extends munit.FunSuite {

  /** A Screen that records which lifecycle methods were called and in what order. */
  private class RecordingScreen extends Screen {
    var calls:            List[String] = Nil
    var lastRenderDelta:  Seconds      = Seconds(-1f)
    var lastResizeWidth:  Pixels       = Pixels(0)
    var lastResizeHeight: Pixels       = Pixels(0)

    def show():                 Unit = calls = calls :+ "show"
    def hide():                 Unit = calls = calls :+ "hide"
    def render(delta: Seconds): Unit = {
      calls = calls :+ "render"
      lastRenderDelta = delta
    }
    def resize(width: Pixels, height: Pixels): Unit = {
      calls = calls :+ "resize"
      lastResizeWidth = width
      lastResizeHeight = height
    }
    def pause():  Unit = calls = calls :+ "pause"
    def resume(): Unit = calls = calls :+ "resume"
    def close():  Unit = calls = calls :+ "close"
  }

  private given Sge = SgeTestFixture.testSge()

  private def makeGame(): Game =
    new Game() {
      override def create(): Unit = ()
    }

  // ---- getScreen / setScreen ----

  test("screen is initially empty") {
    val game = makeGame()
    assert(game.screen.isEmpty)
  }

  test("screen returns the screen that was set") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    assert(game.screen.isDefined)
    assertEquals(game.screen.get, s)
  }

  // ---- setScreen lifecycle ----

  test("setScreen calls show() on new screen") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    assert(s.calls.contains("show"))
  }

  test("setScreen calls resize() on new screen with current graphics dimensions") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    assert(s.calls.contains("resize"))
    assertEquals(s.lastResizeWidth, Pixels(640))
    assertEquals(s.lastResizeHeight, Pixels(480))
  }

  test("setScreen calls show before resize on new screen") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    val showIdx   = s.calls.indexOf("show")
    val resizeIdx = s.calls.indexOf("resize")
    assert(showIdx >= 0 && resizeIdx >= 0 && showIdx < resizeIdx)
  }

  test("setScreen calls hide() on old screen") {
    val game = new Game() { override def create(): Unit = () }
    val old  = new RecordingScreen
    val next = new RecordingScreen
    game.screen = Nullable(old)
    old.calls = Nil // reset
    game.screen = Nullable(next)
    assert(old.calls.contains("hide"))
  }

  test("setScreen calls hide on old screen before show on new screen") {
    val game = new Game() { override def create(): Unit = () }
    var order: List[String] = Nil
    val trackingOld = new Screen {
      def show():                                Unit = ()
      def hide():                                Unit = order = order :+ "old-hide"
      def render(delta: Seconds):                Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def close():                               Unit = ()
    }
    val trackingNew = new Screen {
      def show():                                Unit = order = order :+ "new-show"
      def hide():                                Unit = ()
      def render(delta: Seconds):                Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def close():                               Unit = ()
    }

    game.screen = Nullable(trackingOld)
    order = Nil
    game.screen = Nullable(trackingNew)
    assertEquals(order, List("old-hide", "new-show"))
  }

  test("setting screen to empty hides old screen") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    s.calls = Nil
    game.screen = Nullable.empty
    assert(s.calls.contains("hide"))
    assert(game.screen.isEmpty)
  }

  // ---- render delegation ----

  test("render delegates to screen.render with delta time") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    s.calls = Nil
    game.render()
    assert(s.calls.contains("render"))
  }

  test("render does nothing when screen is empty") {
    val game = makeGame()
    game.render() // should not throw
  }

  // ---- resize delegation ----

  test("resize delegates to screen.resize") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    s.calls = Nil
    game.resize(Pixels(800), Pixels(600))
    assert(s.calls.contains("resize"))
    assertEquals(s.lastResizeWidth, Pixels(800))
    assertEquals(s.lastResizeHeight, Pixels(600))
  }

  test("resize does nothing when screen is empty") {
    val game = makeGame()
    game.resize(Pixels(800), Pixels(600)) // should not throw
  }

  // ---- pause delegation ----

  test("pause delegates to screen.pause") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    s.calls = Nil
    game.pause()
    assert(s.calls.contains("pause"))
  }

  test("pause does nothing when screen is empty") {
    val game = makeGame()
    game.pause() // should not throw
  }

  // ---- resume delegation ----

  test("resume delegates to screen.resume") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    s.calls = Nil
    game.resume()
    assert(s.calls.contains("resume"))
  }

  test("resume does nothing when screen is empty") {
    val game = makeGame()
    game.resume() // should not throw
  }

  // ---- dispose delegation ----

  test("dispose calls hide on current screen") {
    val game = new Game() { override def create(): Unit = () }
    val s    = new RecordingScreen
    game.screen = Nullable(s)
    s.calls = Nil
    game.dispose()
    assert(s.calls.contains("hide"))
  }

  test("dispose does nothing when screen is empty") {
    val game = makeGame()
    game.dispose() // should not throw
  }

  // ---- multiple screen transitions ----

  test("cycling through multiple screens calls correct lifecycle methods") {
    val game = new Game() { override def create(): Unit = () }
    val s1   = new RecordingScreen
    val s2   = new RecordingScreen
    val s3   = new RecordingScreen

    game.screen = Nullable(s1)
    assert(s1.calls.contains("show"))

    s1.calls = Nil
    game.screen = Nullable(s2)
    assert(s1.calls == List("hide"))
    assert(s2.calls.contains("show"))

    s2.calls = Nil
    game.screen = Nullable(s3)
    assert(s2.calls == List("hide"))
    assert(s3.calls.contains("show"))
  }

  // ---- ScreenAdapter-style default no-ops ----

  test("a minimal Screen implementation with no-op methods does not throw") {
    val game = new Game() { override def create(): Unit = () }

    /** A screen adapter: all methods are no-ops. */
    val adapter = new Screen {
      def show():                                Unit = ()
      def hide():                                Unit = ()
      def render(delta: Seconds):                Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def close():                               Unit = ()
    }

    game.screen = Nullable(adapter)
    game.render()
    game.resize(Pixels(1024), Pixels(768))
    game.pause()
    game.resume()
    game.dispose()
    // No exception means success
  }
}
