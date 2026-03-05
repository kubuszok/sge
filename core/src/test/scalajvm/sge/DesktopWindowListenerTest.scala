/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

class DesktopWindowListenerTest extends munit.FunSuite {

  test("default implementations do not throw") {
    val listener = new DesktopWindowListener {}
    val window   = DesktopWindow()
    listener.created(window)
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
