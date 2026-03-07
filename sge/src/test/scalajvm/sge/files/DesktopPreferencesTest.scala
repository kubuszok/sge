/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package files

import java.io.File
import munit.FunSuite

class DesktopPreferencesTest extends FunSuite {

  private def freshPrefs(): DesktopPreferences = {
    val dir  = System.getProperty("java.io.tmpdir")
    val name = s"sge-prefs-test-${System.nanoTime()}.xml"
    val fh   = DesktopFileHandle(new File(dir, name), FileType.Absolute, DesktopFiles.externalPath)
    DesktopPreferences(fh)
  }

  // ---- put/get round-trips ----

  test("putBoolean and getBoolean round-trip") {
    val prefs = freshPrefs()
    prefs.putBoolean("flag", true)
    assert(prefs.getBoolean("flag"))
    assertEquals(prefs.getBoolean("flag", false), true)
  }

  test("putInteger and getInteger round-trip") {
    val prefs = freshPrefs()
    prefs.putInteger("count", 1234)
    assertEquals(prefs.getInteger("count"), 1234)
  }

  test("putLong and getLong round-trip") {
    val prefs = freshPrefs()
    prefs.putLong("big", Long.MaxValue)
    assertEquals(prefs.getLong("big"), Long.MaxValue)
  }

  test("putFloat and getFloat round-trip") {
    val prefs = freshPrefs()
    prefs.putFloat("ratio", 1.2345f)
    assertEquals(prefs.getFloat("ratio"), 1.2345f)
  }

  test("putString and getString round-trip") {
    val prefs = freshPrefs()
    prefs.putString("name", "test!")
    assertEquals(prefs.getString("name"), "test!")
  }

  // ---- defaults ----

  test("getBoolean returns default for missing key") {
    val prefs = freshPrefs()
    assertEquals(prefs.getBoolean("missing"), false)
    assertEquals(prefs.getBoolean("missing", true), true)
  }

  test("getInteger returns default for missing key") {
    val prefs = freshPrefs()
    assertEquals(prefs.getInteger("missing"), 0)
    assertEquals(prefs.getInteger("missing", 42), 42)
  }

  test("getString returns default for missing key") {
    val prefs = freshPrefs()
    assertEquals(prefs.getString("missing"), "")
    assertEquals(prefs.getString("missing", "fallback"), "fallback")
  }

  // ---- contains / remove / clear ----

  test("contains detects existing keys") {
    val prefs = freshPrefs()
    assert(!prefs.contains("x"))
    prefs.putString("x", "y")
    assert(prefs.contains("x"))
  }

  test("remove deletes a key") {
    val prefs = freshPrefs()
    prefs.putString("x", "y")
    prefs.remove("x")
    assert(!prefs.contains("x"))
  }

  test("clear removes all keys") {
    val prefs = freshPrefs()
    prefs.putString("a", "1")
    prefs.putString("b", "2")
    prefs.clear()
    assert(!prefs.contains("a"))
    assert(!prefs.contains("b"))
  }

  // ---- put(Map) ----

  test("put(Map) stores multiple values") {
    val prefs = freshPrefs()
    prefs.put(Map("b" -> true, "i" -> 42, "s" -> "hello"))
    assert(prefs.getBoolean("b"))
    assertEquals(prefs.getInteger("i"), 42)
    assertEquals(prefs.getString("s"), "hello")
  }

  // ---- get() returns all entries ----

  test("get() returns all stored values as strings") {
    val prefs = freshPrefs()
    prefs.putString("a", "1")
    prefs.putString("b", "2")
    val all = prefs.get()
    assertEquals(all.size, 2)
    assertEquals(all("a"), "1")
    assertEquals(all("b"), "2")
  }

  // ---- flush and reload ----

  test("flush persists and reload reads back") {
    val dir  = System.getProperty("java.io.tmpdir")
    val name = s"sge-prefs-test-${System.nanoTime()}.xml"
    val fh   = DesktopFileHandle(new File(dir, name), FileType.Absolute, DesktopFiles.externalPath)

    try {
      // Write
      val prefs1 = DesktopPreferences(fh)
      prefs1.putBoolean("bool", true)
      prefs1.putInteger("int", 1234)
      prefs1.putLong("long", Long.MaxValue)
      prefs1.putFloat("float", 1.2345f)
      prefs1.putString("string", "test!")
      prefs1.flush()

      // Reload from same file
      val prefs2 = DesktopPreferences(fh)
      assert(prefs2.getBoolean("bool"))
      assertEquals(prefs2.getInteger("int"), 1234)
      assertEquals(prefs2.getLong("long"), Long.MaxValue)
      assertEquals(prefs2.getFloat("float"), 1.2345f)
      assertEquals(prefs2.getString("string"), "test!")
    } finally
      fh.delete()
  }

  // ---- fluent API ----

  test("put methods return this for chaining") {
    val prefs  = freshPrefs()
    val result = prefs.putBoolean("a", true).putInteger("b", 1).putLong("c", 2L).putFloat("d", 3.0f).putString("e", "f")
    assert(result eq prefs)
  }
}
