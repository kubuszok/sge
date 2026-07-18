/*
 * SGE — Browser IT console-error classification unit guard (ISS-726 c2).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge.browser

import munit.FunSuite

/** Pure (no-browser) mutation guard for the ISS-726 c2 console-error allow-list.
  *
  * This pins the de-theatered behavior so it cannot silently rot back to the old blanket 404 excusal: a KNOWN-benign request (favicon) stays excused, but a missing SGE ASSET (a real regression) is
  * reported. Under the pre-c2 logic the "missing texture" case below was excused (any 404 was) — this suite is red against that logic and green against [[BrowserConsole]], which is exactly the
  * mutation the reviewer asked to verify.
  */
class BrowserConsoleClassificationTest extends FunSuite {

  // Chromium emits an identical generic text for every failed resource; only the
  // location URL distinguishes them.
  private val resourceText =
    "Failed to load resource: the server responded with a status of 404 (Not Found)"

  test("favicon.ico 404 is excused as a known-benign, non-SGE request") {
    assert(
      BrowserConsole.isBenignResourceError(resourceText, "http://localhost:54321/favicon.ico"),
      "favicon.ico 404 must be treated as benign"
    )
  }

  test("a missing SGE texture 404 is NOT excused — it must fail the suite (c2 mutation guard)") {
    assert(
      !BrowserConsole.isBenignResourceError(resourceText, "http://localhost:54321/regression/test-texture.png"),
      "a missing SGE asset 404 must be reported, not blanket-excused"
    )
  }

  test("a missing embedded JS chunk 404 is NOT excused") {
    assert(
      !BrowserConsole.isBenignResourceError(resourceText, "http://localhost:54321/main.js"),
      "a missing app JS chunk 404 must be reported"
    )
  }

  test("a non-resource console.error (thrown TypeError) is never excused") {
    assert(
      !BrowserConsole.isBenignResourceError(
        "Uncaught TypeError: undefined is not a function",
        "http://localhost:54321/main.js"
      ),
      "a genuine runtime error must never be excused as a benign 404"
    )
  }

  test("the benign allow-list stays short and favicon-only (widening it hides asset regressions)") {
    assertEquals(
      BrowserConsole.BenignMissingResourceUrls,
      List("/favicon.ico"),
      "adding entries widens the asset-regression blind spot — justify any change here and in ISS-726 c2"
    )
  }
}
