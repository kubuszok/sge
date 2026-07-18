/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
// SGE — cross-platform type-coercion contract for BrowserPreferences (ISS-866).
//
// BrowserPreferences (JS) historically pattern-matched the stored value's *runtime* type in its
// get* accessors (`case Some(v: T)`), which made cross-type reads diverge from the reference
// DesktopPreferences (JVM/Native) — the LibGDX-faithful backend that stores every value as a
// String (java.util.Properties) and PARSES it on read. This suite pins the string-parse-LENIENT
// contract that BrowserPreferences must honour so a preference roundtrip behaves identically on
// every platform. These assertions FAIL on the runtime-type-strict implementation and pass only
// once BrowserPreferences parses value.toString on read (matching DesktopPreferences).
package sge

import munit.FunSuite

class BrowserPreferencesLenientJsSuite extends FunSuite {

  private def cleanPrefs(name: String): BrowserPreferences = {
    val prefs = new BrowserPreferences(name)
    prefs.clear()
    prefs.flush()
    prefs
  }

  test("getLong on an Int-stored key widens to the numeric value (JVM parseLong parity)") {
    val prefs = cleanPrefs("iss866-long-on-int")
    prefs.putInteger("k", 5)
    // JVM: Long.parseLong("5") == 5L. JS strict impl returned the default because a JS number
    // never matches Scala.js RuntimeLong. The lenient contract requires 5L here.
    assertEquals(prefs.getLong("k"), 5L)
    assertEquals(prefs.getLong("k", -1L), 5L)
  }

  test("getInteger on a Long-stored key narrows to the numeric value (JVM parseInt parity)") {
    val prefs = cleanPrefs("iss866-int-on-long")
    prefs.putLong("k", 5L)
    // JVM: Integer.parseInt("5") == 5. JS strict impl returned the default (RuntimeLong is not Int).
    assertEquals(prefs.getInteger("k"), 5)
  }

  test("getFloat on a Long-stored key widens to the numeric value (JVM parseFloat parity)") {
    val prefs = cleanPrefs("iss866-float-on-long")
    prefs.putLong("k", 5L)
    // JVM: Float.parseFloat("5") == 5f. JS strict impl returned the default (RuntimeLong is not Float).
    assertEquals(prefs.getFloat("k"), 5f)
  }

  test("getString on a numeric key returns the string form (JVM getProperty parity)") {
    val prefs = cleanPrefs("iss866-string-on-int")
    prefs.putInteger("k", 5)
    // JVM: getString returns the raw stored string "5". JS strict impl returned "" (Int is not String).
    assertEquals(prefs.getString("k"), "5")
  }

  test("getInteger on a numeric String key parses it (JVM parseInt parity)") {
    val prefs = cleanPrefs("iss866-int-on-string")
    prefs.putString("k", "42")
    // JVM: Integer.parseInt("42") == 42. JS strict impl returned the default (String is not Int).
    assertEquals(prefs.getInteger("k"), 42)
  }

  test("getInteger on a non-whole Float key throws NumberFormatException (JVM parity)") {
    val prefs = cleanPrefs("iss866-int-on-float")
    prefs.putFloat("k", 2.5f)
    // JVM: Integer.parseInt("2.5") throws. JS strict impl silently returned the default (no throw).
    intercept[NumberFormatException] {
      prefs.getInteger("k")
    }
  }
}
