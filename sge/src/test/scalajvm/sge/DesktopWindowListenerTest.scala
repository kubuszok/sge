/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

class DesktopWindowListenerTest extends munit.FunSuite {

  // The listener methods are all no-ops that ignore their arguments,
  // so a null window is safe here.
  @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
  private val dummyWindow: DesktopWindow = null.asInstanceOf[DesktopWindow]

  test("default implementations do not throw") {
    val listener = new DesktopWindowListener {}
    listener.created(dummyWindow)
    listener.iconified(true)
    listener.iconified(false)
    listener.maximized(true)
    listener.maximized(false)
    listener.focusLost()
    listener.focusGained()
    listener.filesDropped(Array("file1.txt", "file2.png"))
    listener.refreshRequested()
  }

  test("closeRequested defaults to true") {
    val listener = new DesktopWindowListener {}
    assert(listener.closeRequested())
  }

  test("closeRequested can be overridden to return false") {
    val listener = new DesktopWindowListener {
      override def closeRequested(): Boolean = false
    }
    assert(!listener.closeRequested())
  }

  test("listener methods can be overridden") {
    var iconifiedCalled = false
    val listener        = new DesktopWindowListener {
      override def iconified(isIconified: Boolean): Unit =
        iconifiedCalled = true
    }
    listener.iconified(true)
    assert(iconifiedCalled)
  }

  test("DesktopWindowListener is a trait") {
    val listener: DesktopWindowListener = new DesktopWindowListener {}
    assert(listener != null)
  }
}
