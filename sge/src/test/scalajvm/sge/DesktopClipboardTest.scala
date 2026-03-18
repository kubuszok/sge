/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.utils.Nullable

class DesktopClipboardTest extends munit.FunSuite {

  test("hasContents returns false when clipboard is empty") {
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable.empty,
      setClipboardString = _ => ()
    )
    assert(!clipboard.hasContents)
  }

  test("hasContents returns false when clipboard has empty string") {
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable(""),
      setClipboardString = _ => ()
    )
    assert(!clipboard.hasContents)
  }

  test("hasContents returns true when clipboard has text") {
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable("hello"),
      setClipboardString = _ => ()
    )
    assert(clipboard.hasContents)
  }

  test("contents returns clipboard text") {
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable("test data"),
      setClipboardString = _ => ()
    )
    assertEquals(clipboard.contents.getOrElse(""), "test data")
  }

  test("contents returns empty when nothing on clipboard") {
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable.empty,
      setClipboardString = _ => ()
    )
    assert(clipboard.contents.isEmpty)
  }

  test("contents_= calls the set function") {
    var captured: String = ""
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable.empty,
      setClipboardString = s => captured = s
    )
    clipboard.contents = Nullable("new text")
    assertEquals(captured, "new text")
  }

  test("contents_= does nothing when given empty Nullable") {
    var called    = false
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable.empty,
      setClipboardString = _ => called = true
    )
    clipboard.contents = Nullable.empty
    assert(!called)
  }

  test("implements Clipboard trait") {
    val clipboard = DesktopClipboard(
      clipboardString = () => Nullable.empty,
      setClipboardString = _ => ()
    )
    assert(clipboard.isInstanceOf[utils.Clipboard])
  }

  test("round-trip: set then get") {
    var stored: Nullable[String] = Nullable.empty
    val clipboard = DesktopClipboard(
      clipboardString = () => stored,
      setClipboardString = s => stored = Nullable(s)
    )
    clipboard.contents = Nullable("round trip")
    assertEquals(clipboard.contents.getOrElse(""), "round trip")
    assert(clipboard.hasContents)
  }
}
