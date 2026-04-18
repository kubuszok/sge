/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original test: de/eskalon/commons/screen/ScreenManagerTest.java
 *                de/eskalon/commons/screen/ScreenManagerTest2.java
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen

import sge.graphics.g2d.TextureRegion
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.screen.transition.ScreenTransition
import sge.utils.{ Nullable, Seconds }

class ScreenManagerIntegrationSuite extends munit.FunSuite {

  // -- Test infrastructure --------------------------------------------------

  /** Minimal Sge for headless tests (no GL context). */
  private def testSge(): Sge = {
    val app = new Application {
      def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
      def graphics:                                             Graphics                    = throw new UnsupportedOperationException
      def audio:                                                Audio                       = throw new UnsupportedOperationException
      def input:                                                Input                       = throw new UnsupportedOperationException
      def files:                                                Files                       = throw new UnsupportedOperationException
      def net:                                                  Net                         = throw new UnsupportedOperationException
      def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
      def version:                                              Int                         = 0
      def javaHeap:                                             Long                        = 0L
      def nativeHeap:                                           Long                        = 0L
      def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
      def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
      def postRunnable(runnable:            Runnable):          Unit                        = ()
      def exit():                                               Unit                        = ()
      def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
      def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
    }
    Sge(app, new NoopGraphics(), new NoopAudio(), null.asInstanceOf[Files], new NoopInput(), null.asInstanceOf[Net]) // @nowarn — null fields not used in tests
  }

  private given Sge = testSge()

  /** A ScreenManager subclass that bypasses all GL/FBO operations. The original Java test achieves this by mocking initBuffers() and ScreenFboUtils.screenToTexture().
    */
  private def createTestScreenManager(): ScreenManager[ManagedScreen, ScreenTransition] =
    new ScreenManager[ManagedScreen, ScreenTransition] {
      override protected def initBuffers():                             Unit          = () // no FBOs
      override protected def clearScreen(screen:    ManagedScreen):     Unit          = () // no GL clear
      override protected def clearTransition(trans: ScreenTransition):  Unit          = () // no GL clear
      override protected def renderLastScreenToTexture(delta: Seconds): TextureRegion = {
        // Replicate the original mock: render the screen, return a dummy texture region
        lastScreen.foreach(_.render(delta))
        TextureRegion()
      }
      override protected def renderCurrScreenToTexture(delta: Seconds): TextureRegion = {
        currScreen.foreach(_.render(delta))
        TextureRegion()
      }
    }

  /** A simple ManagedScreen adapter that provides defaults for all abstract methods. */
  private class TestManagedScreenAdapter extends ManagedScreen {
    override def render(delta: Seconds):                Unit = ()
    override def resize(width: Pixels, height: Pixels): Unit = ()
    override def close():                               Unit = ()
  }

  // -- Tests from ScreenManagerTest.java ------------------------------------

  /** Tests exceptions: IllegalStateException before init, NullPointerException on null push. */
  test("testExceptions") {
    val sm = createTestScreenManager()

    // Screen manager not initialized
    intercept[IllegalArgumentException](sm.render(Seconds(1f)))
    intercept[IllegalArgumentException](sm.pause())
    intercept[IllegalArgumentException](sm.resume())
    intercept[IllegalArgumentException](sm.resize(Pixels(5), Pixels(5)))

    // Push null screen
    intercept[IllegalArgumentException] {
      sm.pushScreen(null.asInstanceOf[ManagedScreen], Nullable.empty) // scalastyle:ignore null
    }
  }

  /** Tests whether pause(), resume() and resize() are called on the right screens. */
  test("testApplicationListenerEvents") {
    val sm = createTestScreenManager()
    sm.initialize(InputMultiplexer(), Pixels(5), Pixels(5), false)

    var resizeCount = 0
    var pauseState  = 0
    var pauseCount  = 0
    var resumeState = 0
    var resumeCount = 0

    /** The last screen; rendered as part of a transition. */
    val s1 = new TestManagedScreenAdapter {
      override def resize(width: Pixels, height: Pixels): Unit = resizeCount += 1
      override def pause():                               Unit = pauseCount += 1
      override def resume():                              Unit = resumeCount += 1
    }

    /** The current screen. */
    val s2 = new TestManagedScreenAdapter {
      override def resize(width: Pixels, height: Pixels): Unit = resizeCount += 1
      override def pause():                               Unit = {
        pauseCount += 1
        if (pauseState == 0) fail("s2.pause should not be called yet")
      }
      override def resume(): Unit = {
        resumeCount += 1
        if (resumeState == 0) fail("s2.resume should not be called yet")
      }
    }

    /** A screen that is queued, but never shown. */
    val s3 = new TestManagedScreenAdapter {
      override def show():                                Unit = fail("s3.show should not be called")
      override def hide():                                Unit = fail("s3.hide should not be called")
      override def resize(width: Pixels, height: Pixels): Unit = fail("s3.resize should not be called")
      override def pause():                               Unit = fail("s3.pause should not be called")
      override def resume():                              Unit = fail("s3.resume should not be called")
    }

    /** A never ending transition. */
    val t1 = new ScreenTransition {
      override def close():                                                                      Unit    = ()
      override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit    = ()
      override def isDone:                                                                       Boolean = false
      override def resize(width: Pixels, height:      Pixels):                                   Unit    = resizeCount += 1
    }

    /** A transition that is queued, but never shown. */
    val t2 = new ScreenTransition {
      override def close():                                                                      Unit    = ()
      override def show():                                                                       Unit    = fail("t2.show should not be called")
      override def hide():                                                                       Unit    = fail("t2.hide should not be called")
      override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit    = fail("t2.render should not be called")
      override def isDone:                                                                       Boolean = false
      override def resize(width: Pixels, height:      Pixels):                                   Unit    = fail("t2.resize should not be called")
    }

    // Make screen1 the current screen
    sm.pushScreen(s1, Nullable.empty)
    sm.render(Seconds(1f))

    // Only screen1 is paused and resumed
    sm.pause()
    assertEquals(pauseCount, 1)
    pauseState += 1

    sm.resume()
    assertEquals(resumeCount, 1)
    resumeState += 1

    // Make screen2 the current screen; the transition is going on
    sm.pushScreen(s2, Nullable(t1))
    sm.render(Seconds(1f))

    // the two screens and the transition were automatically resized when they were first shown
    assertEquals(resizeCount, 3)

    // these are never rendered, because t1 never ends
    sm.pushScreen(s3, Nullable(t2))

    // resize()
    assertEquals(resizeCount, 3) // nothing changes just by pushing another screen
    sm.resize(Pixels(5), Pixels(5)) // ignored (same size)
    sm.resize(Pixels(10), Pixels(10))
    assertEquals(resizeCount, 6)
    sm.resize(Pixels(10), Pixels(10)) // ignored (same size)
    assertEquals(resizeCount, 6)

    // only change width _or_ height
    sm.resize(Pixels(10), Pixels(15))
    assertEquals(resizeCount, 9)
    sm.resize(Pixels(20), Pixels(15))
    assertEquals(resizeCount, 12)

    // pause() & resume()
    sm.pause()
    assertEquals(pauseCount, 3)

    sm.resume()
    assertEquals(resumeCount, 3)
  }

  /** Tests the functionality of the close() method. */
  test("testDispose") {
    val sm = createTestScreenManager()
    sm.initialize(InputMultiplexer(), Pixels(5), Pixels(5), false)

    var disposeCount = 0

    val s1 = new TestManagedScreenAdapter {
      override def close(): Unit = disposeCount += 1
    }

    val s2 = new TestManagedScreenAdapter {
      override def close(): Unit = disposeCount += 1
    }

    val t1 = new ScreenTransition {
      override def close():                                                                      Unit    = disposeCount += 1
      override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit    = ()
      override def isDone:                                                                       Boolean = false
      override def resize(width: Pixels, height:      Pixels):                                   Unit    = ()
    }

    val t2 = new ScreenTransition {
      override def close():                                                                      Unit    = disposeCount += 1
      override def render(delta: Seconds, lastScreen: TextureRegion, currScreen: TextureRegion): Unit    = ()
      override def isDone:                                                                       Boolean = false
      override def resize(width: Pixels, height:      Pixels):                                   Unit    = ()
    }

    sm.pushScreen(s1, Nullable(t1))
    sm.render(Seconds(1f))
    sm.pushScreen(s2, Nullable(t2))

    // Dispose everything
    sm.close()

    assertEquals(disposeCount, 4)
  }

  // -- Tests from ScreenManagerTest2.java -----------------------------------

  /** Tests whether the screens are shown and hidden correctly while a transition is rendering as well as whether the input handlers are unregistered while transitioning.
    */
  test("testScreenLifecycleWhileTransition") {
    val mult = InputMultiplexer()
    val sm   = createTestScreenManager()
    sm.initialize(mult, Pixels(5), Pixels(5), false)

    var i                         = 0
    var k                         = 0
    var z                         = 0
    var firstRenderPassTransition = true
    var firstRenderPassScreen2    = true

    val testScreen = new ManagedScreen {
      addInputProcessor(new InputProcessor {})

      override def show(): Unit = {
        assertEquals(i, 0)
        i = 1
      }

      override def render(delta: Seconds): Unit = {
        z += 1
        z match {
          case 2 => // second render pass; while the transition is rendered
            assertEquals(i, 2)
            i = 3
          case 3 =>
            assertEquals(delta.toFloat, 15f)
            assertEquals(i, 5)
            i = 6
          case 4 =>
            fail("testScreen should not be rendered a 4th time")
          case _ => // ignore first render pass
        }
      }

      override def hide(): Unit = {
        assertEquals(i, 8)
        i = 9
      }

      override def close():                               Unit    = ()
      override def resize(width: Pixels, height: Pixels): Unit    = ()
      override def equals(obj:   Any):                    Boolean = this eq obj.asInstanceOf[AnyRef]
    }

    val test2Screen = new ManagedScreen {
      addInputProcessor(new InputProcessor {})
      addInputProcessor(new InputProcessor {})
      addInputProcessor(new InputProcessor {})

      override def show(): Unit = {
        assertEquals(i, 1)
        i = 2
      }

      override def render(delta: Seconds): Unit =
        if (firstRenderPassScreen2) {
          firstRenderPassScreen2 = false
          assertEquals(i, 3)
          i = 4
        } else {
          if (delta.toFloat == 15f) {
            assertEquals(i, 6)
            i = 7
          } else {
            assertEquals(i, 9)
            i = 10
          }
        }

      override def hide():                                Unit    = ()
      override def close():                               Unit    = ()
      override def resize(width: Pixels, height: Pixels): Unit    = ()
      override def equals(obj:   Any):                    Boolean = this eq obj.asInstanceOf[AnyRef]
    }

    val transition = new ScreenTransition {
      {
        assertEquals(k, 0)
        k = 1
      }

      override def show(): Unit = {
        assertEquals(k, 1)
        k = 2
      }

      override def render(delta: Seconds, lastScr: TextureRegion, currScr: TextureRegion): Unit =
        if (firstRenderPassTransition) {
          firstRenderPassTransition = false
          assertEquals(i, 4)
          i = 5
        } else {
          assertEquals(i, 7)
          assertEquals(k, 2)
          k = delta.toFloat.toInt
        }

      override def isDone: Boolean = k == 15

      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def close():                               Unit = ()
    }

    // Push the first screen
    sm.pushScreen(testScreen, Nullable.empty)
    assert(sm.currentScreen.isEmpty)
    sm.render(Seconds(1f))
    assertEquals(i, 1)

    assertEquals(mult.size(), 1)
    assert(sm.currentScreen.isDefined)
    assert(sm.currentScreen.get eq testScreen)
    assert(sm.lastScreenOption.isEmpty)

    // Push the second screen using a transition
    sm.pushScreen(test2Screen, Nullable(transition))
    sm.render(Seconds(1f))
    assertEquals(i, 5)

    assertEquals(mult.size(), 0)
    assert(sm.currentScreen.isDefined)
    assert(sm.currentScreen.get eq test2Screen)
    assert(sm.lastScreenOption.isDefined)
    assert(sm.lastScreenOption.get eq testScreen)
    assert(!transition.isDone)

    // Let a few seconds pass, so the transition finishes
    sm.render(Seconds(15f))

    assertEquals(mult.size(), 0)
    assert(sm.currentScreen.get eq test2Screen) // didn't change
    assert(sm.lastScreenOption.get eq testScreen) // didn't change
    assert(transition.isDone)

    assertEquals(i, 7)
    i = 8

    // In the next render pass the transition is finished
    sm.render(Seconds(1f))
    assertEquals(i, 10)
    i = 11 // end

    assertEquals(mult.size(), 3)
    assert(sm.lastScreenOption.isEmpty)
  }

  /** Tests that pushing the same screen twice in succession is ignored. */
  test("testIdenticalDoublePush") {
    val mult = InputMultiplexer()
    val sm   = createTestScreenManager()
    sm.initialize(mult, Pixels(5), Pixels(5), false)

    val firstScreen = new TestManagedScreenAdapter

    var isShown    = false
    val mainScreen = new TestManagedScreenAdapter {
      override def show(): Unit = {
        assert(!isShown, "show() called when already shown")
        isShown = true
      }
      override def hide(): Unit = {
        assert(isShown, "hide() called when not shown")
        isShown = false
      }
    }

    sm.pushScreen(firstScreen, Nullable.empty)
    sm.render(Seconds(1f))

    sm.pushScreen(mainScreen, Nullable.empty)
    sm.render(Seconds(1f))

    sm.pushScreen(mainScreen, Nullable.empty)
    sm.render(Seconds(1f))
  }

  /** Tests whether a screen's input processor is removed from the game, even though it was deleted from the screen's list of processors beforehand.
    */
  test("testRemovingProcessor") {
    val mult = InputMultiplexer()
    val sm   = createTestScreenManager()
    sm.initialize(mult, Pixels(5), Pixels(5), false)

    var doneOnce   = false
    val mainScreen = new TestManagedScreenAdapter {
      override def show(): Unit = {
        addInputProcessor(new InputProcessor {})
        addInputProcessor(new InputProcessor {})
      }
      override def render(delta: Seconds): Unit =
        if (!doneOnce) {
          doneOnce = true
          this.inputProcessors.remove(0)
        }
    }

    val secondScreen = new TestManagedScreenAdapter

    sm.pushScreen(mainScreen, Nullable.empty)
    sm.render(Seconds(1f))
    assertEquals(mult.size(), 2)

    sm.pushScreen(secondScreen, Nullable.empty)
    sm.render(Seconds(1f))
    assertEquals(mult.size(), 0)
  }
}
