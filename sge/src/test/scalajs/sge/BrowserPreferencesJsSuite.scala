/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
// SGE — real JS coverage for BrowserPreferences (ISS-860).
//
// The wave-I I5 browser de-theater deleted proxy suites that never actually exercised the SGE
// path, leaving BrowserPreferences with only browser-IT smoke markers. This suite pins the
// put/get/remove/clear/flush roundtrip of the localStorage-backed implementation with concrete,
// exact-value assertions — not tautologies. It runs under the jsdom jsEnv (build.sbt), which
// provides window.localStorage, so flush()/reload persistence is a genuine localStorage roundtrip.
package sge

import munit.FunSuite

class BrowserPreferencesJsSuite extends FunSuite {

  // Each test uses a distinct preference set name and wipes any persisted state first so runs are
  // deterministic regardless of leftover localStorage entries from earlier runs.
  private def cleanPrefs(name: String): BrowserPreferences = {
    val prefs = new BrowserPreferences(name)
    prefs.clear()
    prefs.flush() // wipes every key with this prefix out of localStorage
    prefs
  }

  test("putString / getString roundtrips the exact value") {
    val prefs = cleanPrefs("iss860-string")
    prefs.putString("player", "Zelda")
    assertEquals(prefs.getString("player"), "Zelda")
  }

  test("putInteger / getInteger roundtrips the exact value and type") {
    val prefs = cleanPrefs("iss860-int")
    prefs.putInteger("score", 4200)
    assertEquals(prefs.getInteger("score"), 4200)
  }

  test("putBoolean / getBoolean roundtrips the exact value") {
    val prefs = cleanPrefs("iss860-bool")
    prefs.putBoolean("muted", true)
    assertEquals(prefs.getBoolean("muted"), true)
    prefs.putBoolean("muted", false)
    assertEquals(prefs.getBoolean("muted"), false)
  }

  test("putLong / getLong roundtrips a value beyond Int range") {
    val prefs = cleanPrefs("iss860-long")
    prefs.putLong("seed", 9007199254740000L)
    assertEquals(prefs.getLong("seed"), 9007199254740000L)
  }

  test("putFloat / getFloat roundtrips the exact value") {
    val prefs = cleanPrefs("iss860-float")
    prefs.putFloat("volume", 2.5f)
    assertEquals(prefs.getFloat("volume"), 2.5f)
  }

  test("getters return zero/empty defaults for an absent key") {
    val prefs = cleanPrefs("iss860-defaults")
    assertEquals(prefs.getString("nope"), "")
    assertEquals(prefs.getInteger("nope"), 0)
    assertEquals(prefs.getLong("nope"), 0L)
    assertEquals(prefs.getFloat("nope"), 0f)
    assertEquals(prefs.getBoolean("nope"), false)
  }

  test("getters with an explicit default return that default for an absent key") {
    val prefs = cleanPrefs("iss860-defvalue")
    assertEquals(prefs.getString("nope", "fallback"), "fallback")
    assertEquals(prefs.getInteger("nope", 7), 7)
    assertEquals(prefs.getLong("nope", 11L), 11L)
    assertEquals(prefs.getFloat("nope", 1.5f), 1.5f)
    assertEquals(prefs.getBoolean("nope", true), true)
  }

  test("a stored value wins over an explicit default") {
    val prefs = cleanPrefs("iss860-stored-wins")
    prefs.putInteger("lives", 3)
    assertEquals(prefs.getInteger("lives", 99), 3)
  }

  test("type-mismatched get falls back to the default across the JS-distinguishable types") {
    val prefs = cleanPrefs("iss860-typetag")
    prefs.putInteger("count", 5)
    // Stored as Int → Boolean/String/Long are distinct runtime kinds on Scala.js, so the
    // type-tagged pattern match falls through to the defaults for them.
    assertEquals(prefs.getBoolean("count"), false)
    assertEquals(prefs.getString("count"), "")
    assertEquals(prefs.getLong("count", -1L), -1L)
    // the correctly-typed accessor still sees the exact value
    assertEquals(prefs.getInteger("count"), 5)
    // NOTE (platform semantics): Int and Float share one runtime representation on Scala.js
    // (both are JS `number`), so getFloat("count") here observes 5f rather than the default —
    // the `case v: Float` test cannot distinguish a whole-number Int from a Float at runtime.
    // This is a Scala.js number-representation artifact, not a port logic error, and differs
    // from JVM/Native where getFloat on an Int-stored key returns the default.
    assertEquals(prefs.getFloat("count", -1f), 5f)
  }

  test("contains reflects presence and absence") {
    val prefs = cleanPrefs("iss860-contains")
    assertEquals(prefs.contains("k"), false)
    prefs.putString("k", "v")
    assertEquals(prefs.contains("k"), true)
  }

  test("remove drops a single key, leaving the rest") {
    val prefs = cleanPrefs("iss860-remove")
    prefs.putString("keep", "yes")
    prefs.putString("drop", "no")
    prefs.remove("drop")
    assertEquals(prefs.contains("drop"), false)
    assertEquals(prefs.contains("keep"), true)
    assertEquals(prefs.getString("keep"), "yes")
  }

  test("clear empties every key") {
    val prefs = cleanPrefs("iss860-clear")
    prefs.putString("a", "1")
    prefs.putInteger("b", 2)
    prefs.clear()
    assertEquals(prefs.contains("a"), false)
    assertEquals(prefs.contains("b"), false)
    assertEquals(prefs.get().size, 0)
  }

  test("put(map) bulk-sets values preserving each declared type") {
    val prefs = cleanPrefs("iss860-putmap")
    prefs.put(
      Map[String, Boolean | Int | Long | Float | String](
        "s" -> "text",
        "i" -> 42,
        "b" -> true,
        "l" -> 123456789012L,
        "f" -> 3.25f
      )
    )
    assertEquals(prefs.getString("s"), "text")
    assertEquals(prefs.getInteger("i"), 42)
    assertEquals(prefs.getBoolean("b"), true)
    assertEquals(prefs.getLong("l"), 123456789012L)
    assertEquals(prefs.getFloat("f"), 3.25f)
  }

  test("get() returns the full map view with exact values") {
    val prefs = cleanPrefs("iss860-getall")
    prefs.putString("name", "Link")
    prefs.putInteger("hp", 20)
    val all = prefs.get()
    assertEquals(all.size, 2)
    assertEquals(all("name"), "Link": Boolean | Int | Long | Float | String)
    assertEquals(all("hp"), 20:       Boolean | Int | Long | Float | String)
  }

  test("flush persists to localStorage; a fresh instance reloads exact values and types") {
    val prefs = cleanPrefs("iss860-flush")
    prefs.putString("world", "hyrule")
    prefs.putInteger("rupees", 255)
    prefs.putBoolean("hardMode", true)
    prefs.putLong("playedMs", 3600000L)
    prefs.putFloat("brightness", 0.75f)
    prefs.flush()

    // A brand-new instance loads its state from localStorage in its constructor.
    val reloaded = new BrowserPreferences("iss860-flush")
    assertEquals(reloaded.getString("world"), "hyrule")
    assertEquals(reloaded.getInteger("rupees"), 255)
    assertEquals(reloaded.getBoolean("hardMode"), true)
    assertEquals(reloaded.getLong("playedMs"), 3600000L)
    assertEquals(reloaded.getFloat("brightness"), 0.75f)
  }

  test("in-memory mutations are NOT visible to a fresh instance until flush") {
    val prefs = cleanPrefs("iss860-noflush")
    prefs.putString("draft", "unsaved")
    // no flush()
    val reloaded = new BrowserPreferences("iss860-noflush")
    assertEquals(reloaded.contains("draft"), false)
    assertEquals(reloaded.getString("draft"), "")
  }

  test("flush after clear wipes the persisted state for a fresh instance") {
    val prefs = cleanPrefs("iss860-clearflush")
    prefs.putString("temp", "value")
    prefs.flush()
    assertEquals(new BrowserPreferences("iss860-clearflush").getString("temp"), "value")

    prefs.clear()
    prefs.flush()
    val afterClear = new BrowserPreferences("iss860-clearflush")
    assertEquals(afterClear.contains("temp"), false)
    assertEquals(afterClear.get().size, 0)
  }

  test("two preference sets with different names do not collide") {
    val a = cleanPrefs("iss860-nsA")
    val b = cleanPrefs("iss860-nsB")
    a.putInteger("shared", 1)
    b.putInteger("shared", 2)
    a.flush()
    b.flush()
    assertEquals(new BrowserPreferences("iss860-nsA").getInteger("shared"), 1)
    assertEquals(new BrowserPreferences("iss860-nsB").getInteger("shared"), 2)
  }
}
