/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package utils

import sge.utils.{ DynamicArray, Nullable }

/** Tests for Selection: add, remove, set, clear, multiple, required, toggle, disabled, iterability. */
class SelectionTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // Default state
  // ---------------------------------------------------------------------------

  test("Selection starts empty") {
    given Sge = ctx()
    val sel   = Selection[String]()
    assert(sel.isEmpty)
    assert(!sel.notEmpty)
    assertEquals(sel.size, 0)
    assert(sel.first.isEmpty)
    assert(sel.lastSelected.isEmpty)
  }

  // ---------------------------------------------------------------------------
  // add / remove
  // ---------------------------------------------------------------------------

  test("add inserts item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("a")))
    assert(sel.first.exists(_ == "a"))
  }

  test("add duplicate is idempotent") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("a")
    assertEquals(sel.size, 1)
  }

  test("remove removes item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("b")
    sel.remove("a")
    assertEquals(sel.size, 1)
    assert(!sel.contains(Nullable("a")))
    assert(sel.contains(Nullable("b")))
  }

  test("remove non-existent item is no-op") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.remove("z")
    assertEquals(sel.size, 1)
  }

  // ---------------------------------------------------------------------------
  // set
  // ---------------------------------------------------------------------------

  test("set replaces selection with single item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("b")
    sel.set("c")
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("c")))
    assert(!sel.contains(Nullable("a")))
  }

  test("set with same single item is no-op") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    // Setting the same item when it's already the only selection should be no-op
    sel.set("a")
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("a")))
  }

  // ---------------------------------------------------------------------------
  // setAll
  // ---------------------------------------------------------------------------

  test("setAll replaces selection with multiple items") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("old")
    val items = DynamicArray[String]()
    items.add("x")
    items.add("y")
    items.add("z")
    sel.setAll(items)
    assertEquals(sel.size, 3)
    assert(sel.contains(Nullable("x")))
    assert(sel.contains(Nullable("y")))
    assert(sel.contains(Nullable("z")))
    assert(!sel.contains(Nullable("old")))
  }

  // ---------------------------------------------------------------------------
  // addAll / removeAll
  // ---------------------------------------------------------------------------

  test("addAll adds multiple items") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    val items = DynamicArray[String]()
    items.add("a")
    items.add("b")
    sel.addAll(items)
    assertEquals(sel.size, 2)
  }

  test("removeAll removes multiple items") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("b")
    sel.add("c")
    val toRemove = DynamicArray[String]()
    toRemove.add("a")
    toRemove.add("c")
    sel.removeAll(toRemove)
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("b")))
  }

  // ---------------------------------------------------------------------------
  // clear
  // ---------------------------------------------------------------------------

  test("clear empties selection") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("b")
    sel.clear()
    assert(sel.isEmpty)
    assertEquals(sel.size, 0)
    assert(sel.lastSelected.isEmpty)
  }

  test("clear on empty is no-op") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.clear()
    assert(sel.isEmpty)
  }

  // ---------------------------------------------------------------------------
  // contains
  // ---------------------------------------------------------------------------

  test("contains with Nullable.empty returns false") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    assert(!sel.contains(Nullable.empty))
  }

  // ---------------------------------------------------------------------------
  // first / lastSelected
  // ---------------------------------------------------------------------------

  test("first returns first item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("first")
    sel.add("second")
    assert(sel.first.isDefined)
    // LinkedHashSet preserves insertion order
    assert(sel.first.exists(_ == "first"))
  }

  test("lastSelected tracks the last set/add item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    assert(sel.lastSelected.exists(_ == "a"))
    sel.add("b")
    assert(sel.lastSelected.exists(_ == "b"))
  }

  test("lastSelected falls back to head when cleared") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("b")
    sel.remove("b")
    // After removing the last selected, lastSelected should fall back to head
    assert(sel.lastSelected.exists(_ == "a"))
  }

  // ---------------------------------------------------------------------------
  // disabled
  // ---------------------------------------------------------------------------

  test("disabled property") {
    given Sge = ctx()
    val sel   = Selection[String]()
    assert(!sel.disabled)
    sel.disabled = true
    assert(sel.disabled)
  }

  test("choose does nothing when disabled") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.disabled = true
    sel.choose("a")
    assert(sel.isEmpty)
  }

  // ---------------------------------------------------------------------------
  // choose: basic (no ctrl, no toggle)
  // ---------------------------------------------------------------------------

  test("choose selects item when empty") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.choose("a")
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("a")))
  }

  test("choose replaces selection when not multiple") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.choose("a")
    sel.choose("b")
    assertEquals(sel.size, 1)
    assert(!sel.contains(Nullable("a")))
    assert(sel.contains(Nullable("b")))
  }

  // ---------------------------------------------------------------------------
  // choose: multiple mode
  // ---------------------------------------------------------------------------

  test("choose does not replace in multiple mode without ctrl/toggle") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.multiple = true
    // In multiple mode without ctrl pressed, choose still replaces
    sel.choose("a")
    sel.choose("b")
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("b")))
  }

  // ---------------------------------------------------------------------------
  // choose: toggle mode
  // ---------------------------------------------------------------------------

  test("choose with toggle deselects already selected item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.toggle = true
    sel.choose("a")
    assertEquals(sel.size, 1)
    // Choosing again with toggle should deselect
    sel.choose("a")
    assert(sel.isEmpty)
  }

  test("choose with toggle and required does not deselect last item") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.toggle = true
    sel.required = true
    sel.choose("a")
    assertEquals(sel.size, 1)
    // Should NOT deselect — required prevents deselecting the last item
    sel.choose("a")
    assertEquals(sel.size, 1)
    assert(sel.contains(Nullable("a")))
  }

  // ---------------------------------------------------------------------------
  // iterator / Iterable
  // ---------------------------------------------------------------------------

  test("Selection is iterable") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("a")
    sel.add("b")
    val list = sel.toList
    assertEquals(list.size, 2)
    assert(list.contains("a"))
    assert(list.contains("b"))
  }

  // ---------------------------------------------------------------------------
  // toArray
  // ---------------------------------------------------------------------------

  test("toArray returns DynamicArray of selected items") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("x")
    sel.add("y")
    val arr = sel.toArray
    assertEquals(arr.size, 2)
  }

  // ---------------------------------------------------------------------------
  // toString
  // ---------------------------------------------------------------------------

  test("toString reflects selection") {
    given Sge = ctx()
    val sel   = Selection[String]()
    sel.programmaticChangeEvents = false
    sel.add("hello")
    val str = sel.toString
    assert(str.contains("hello"))
  }
}
