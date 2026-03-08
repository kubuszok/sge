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
      def create():                              Unit = createCount.incrementAndGet()
      def render():                              Unit = { renderCount.incrementAndGet(); latch.countDown() }
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = ()
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
      def create():                              Unit = ()
      def render():                              Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = disposeLatch.countDown()
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
      def create():                              Unit = ()
      def render():                              Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = ()
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
      def create():                              Unit = ()
      def render():                              Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 0))
    try {
      assertEquals(app.getType(), Application.ApplicationType.HeadlessDesktop)
      assertEquals(app.getVersion(), 0)
      assert(app.getJavaHeap() > 0)
      assert(app.getNativeHeap() > 0)
    } finally {
      app.exit()
      Thread.sleep(200)
    }
  }

  // ---- sgeContext ----

  test("sgeContext provides valid Sge") {
    val listener = new ApplicationListener {
      def create():                              Unit = ()
      def render():                              Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = ()
    }

    val app = HeadlessApplication(listener, HeadlessApplicationConfig(updatesPerSecond = 0))
    try {
      val sge = app.sgeContext
      assert(sge.application eq app)
      assert(sge.graphics != null)
      assert(sge.audio != null)
      assert(sge.files != null)
      assert(sge.input != null)
      assert(sge.net != null)
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
      def create():                              Unit = ()
      def render():                              Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = latch.countDown()
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
      def create():                              Unit = ()
      def render():                              Unit = ()
      def resize(width: Pixels, height: Pixels): Unit = ()
      def pause():                               Unit = ()
      def resume():                              Unit = ()
      def dispose():                             Unit = ()
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
