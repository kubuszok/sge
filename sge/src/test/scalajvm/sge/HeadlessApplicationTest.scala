/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import munit.FunSuite
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger

class HeadlessApplicationTest extends FunSuite {

  // ---- lifecycle ----

  test("HeadlessApplication calls create and render") {
    val createCount = AtomicInteger(0)
    val renderCount = AtomicInteger(0)
    val latch       = CountDownLatch(3) // wait for at least 3 render calls

    val listener = new ApplicationListener {
      override def create():                              Unit = createCount.incrementAndGet()
      def render():                                       Unit = { renderCount.incrementAndGet(); latch.countDown() }
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 1000))
    try {
      assert(latch.await(5, TimeUnit.SECONDS), "render was not called 3 times within 5s")
      assert(createCount.get() >= 1, "create should be called at least once")
      assert(renderCount.get() >= 3, "render should be called at least 3 times")
    } finally {
      app.exit()
      Thread.sleep(200) // allow shutdown
    }
  }

  // ---- exit ----

  test("exit stops the main loop") {
    val disposeLatch = CountDownLatch(1)

    val listener = new ApplicationListener {
      override def create():                              Unit = ()
      def render():                                       Unit = ()
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = disposeLatch.countDown()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 1000))
    Thread.sleep(50) // let it start
    app.exit()
    assert(disposeLatch.await(5, TimeUnit.SECONDS), "dispose should be called after exit")
  }

  // ---- postRunnable ----

  test("postRunnable executes on main loop thread") {
    val latch          = CountDownLatch(1)
    val runnableThread = new Array[Thread](1)

    val listener = new ApplicationListener {
      override def create():                              Unit = ()
      def render():                                       Unit = ()
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 1000))
    try {
      app.postRunnable { () =>
        runnableThread(0) = Thread.currentThread()
        latch.countDown()
      }
      assert(latch.await(5, TimeUnit.SECONDS), "runnable should execute")
      assert(runnableThread(0).getName() == "HeadlessApplication")
    } finally {
      app.exit()
      Thread.sleep(200)
    }
  }

  // ---- Application trait methods ----

  test("getType returns HeadlessDesktop") {
    val listener = new ApplicationListener {
      override def create():                              Unit = ()
      def render():                                       Unit = ()
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 0))
    try {
      assertEquals(app.applicationType, Application.ApplicationType.HeadlessDesktop)
      assertEquals(app.version, 0)
      assert(app.javaHeap > 0)
      assert(app.nativeHeap > 0)
    } finally {
      app.exit()
      Thread.sleep(200)
    }
  }

  // ---- sgeContext ----

  test("sgeContext provides valid Sge") {
    val listener = new ApplicationListener {
      override def create():                              Unit = ()
      def render():                                       Unit = ()
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 0))
    try {
      val sge = app.sgeContext
      assert(sge.application eq app)
      // Behavioral probes: exercise one wired call per subsystem (each NPEs if its Sge
      // field were uninitialized) and check the value the headless implementation actually
      // produces — instead of a vacuous not-null assert on a non-nullable field.
      // graphics: NoopGraphics is wired at its 640x480 defaults (HeadlessApplication.scala:43).
      assertEquals(sge.graphics.width.toInt, 640)
      assertEquals(sge.graphics.height.toInt, 480)
      // audio: NoopAudio reports no output devices (NoopAudio.availableOutputDevices == empty).
      assertEquals(sge.audio.availableOutputDevices.length, 0)
      // files: DesktopFiles builds a well-formed internal handle for the requested path.
      assertEquals(sge.files.internal("sge-headless-probe.txt").path, "sge-headless-probe.txt")
      // input: NoopInput polls report no touch (justTouched == false).
      assert(!sge.input.justTouched())
      // net: DesktopNet exposes httpClient as a wired `val` — same instance across accesses.
      val http = sge.net.httpClient
      assert(http eq sge.net.httpClient)
    } finally {
      app.exit()
      Thread.sleep(200)
    }
  }

  // ---- lifecycle listeners ----

  test("lifecycle listeners receive pause and dispose on exit") {
    val pauseCount   = AtomicInteger(0)
    val disposeCount = AtomicInteger(0)
    val latch        = CountDownLatch(1)

    val listener = new ApplicationListener {
      override def create():                              Unit = ()
      def render():                                       Unit = ()
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = latch.countDown()
    }

    val lifecycleListener = new LifecycleListener {
      def pause():   Unit = pauseCount.incrementAndGet()
      def resume():  Unit = ()
      def dispose(): Unit = disposeCount.incrementAndGet()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 1000))
    app.addLifecycleListener(lifecycleListener)
    Thread.sleep(50)
    app.exit()
    assert(latch.await(5, TimeUnit.SECONDS))
    Thread.sleep(100)
    assert(pauseCount.get() >= 1, "lifecycle listener should receive pause")
    assert(disposeCount.get() >= 1, "lifecycle listener should receive dispose")
  }

  // ---- preferences ----

  test("getPreferences returns same instance for same name") {
    val listener = new ApplicationListener {
      override def create():                              Unit = ()
      def render():                                       Unit = ()
      override def resize(width: Pixels, height: Pixels): Unit = ()
      override def pause():                               Unit = ()
      override def resume():                              Unit = ()
      override def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 0))
    try {
      val prefs1 = app.getPreferences("test-prefs")
      val prefs2 = app.getPreferences("test-prefs")
      assert(prefs1 eq prefs2)
    } finally {
      app.exit()
      Thread.sleep(200)
    }
  }
}
