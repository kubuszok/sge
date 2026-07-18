/*
 * SGE — Browser IT console-error classification (ISS-726 c2).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge.browser

import com.microsoft.playwright.ConsoleMessage

/** Classifies browser `console.error` messages for the Playwright IT suites.
  *
  * ISS-726 c2 (de-theater): the suites previously excused EVERY 404 / "Failed to
  * load resource" console error (`!text.contains("404") || !text.contains("Failed
  * to load resource")`), so a genuinely missing SGE asset — a real regression —
  * was silently swallowed. This narrows the excusal to an explicit, documented
  * allow-list of KNOWN-benign, non-SGE resource requests. Any other resource-load
  * failure (a missing texture, audio clip, or JS chunk) is a real error and must
  * fail the suite.
  *
  * Enumerated from actual runs (headless Chromium, 2026-07-18): the SGE demo /
  * regression bundle embeds all of its assets (base64) and performs NO manifest
  * or asset fetch at startup (see `BrowserAssetLoader` — "there is no HTTP fetch,
  * manifest, or async preload"), so it emits ZERO resource-load console errors.
  * The stale "BrowserApplication fetches assets.txt at startup" rationale the old
  * filter cited is obsolete — nothing fetches assets.txt. The only non-SGE request
  * a browser may make against the bare test HTTP server is the automatic
  * `/favicon.ico` fetch: it produced NO console.error in local headless Chromium,
  * but can surface on other Chromium builds/CI, so it is the sole allow-listed
  * entry.
  */
object BrowserConsole {

  /** Failed-resource URL fragments whose 404 is a known browser/runtime
    * artifact, NOT a missing SGE asset. Keep this list SHORT and each entry
    * justified — every addition widens the blind spot for asset regressions.
    */
  val BenignMissingResourceUrls: List[String] = List(
    // Chromium auto-requests /favicon.ico on top-level navigation; the minimal
    // test HTTP server serves none. Never an SGE asset, never game-affecting.
    "/favicon.ico"
  )

  /** True when `text` is a resource-load failure (the browser's generic 404 /
    * "Failed to load resource" console error). Only these are eligible for the
    * benign allow-list; every other console.error (a thrown TypeError,
    * ReferenceError, WebGL failure, …) is always treated as real.
    */
  def isResourceLoadFailure(text: String): Boolean =
    text.contains("Failed to load resource") || text.contains("404 (Not Found)")

  /** True when this is a resource-load failure for a KNOWN-benign, non-SGE URL
    * (see [[BenignMissingResourceUrls]]). Chromium's resource-error `text` is
    * identical for every failed URL, so the discriminating URL is read from the
    * console message `location` (with a `text` fallback). A failure for any URL
    * NOT on the allow-list — i.e. an actual missing SGE asset — returns false
    * and therefore fails the suite.
    */
  def isBenignResourceError(text: String, location: String): Boolean =
    isResourceLoadFailure(text) &&
      BenignMissingResourceUrls.exists(u => location.contains(u) || text.contains(u))

  /** [[ConsoleMessage]] overload used by the suites: an error-type message is
    * benign only when it is an allow-listed resource-load failure.
    */
  def isBenignError(msg: ConsoleMessage): Boolean =
    isBenignResourceError(msg.text(), msg.location())
}
