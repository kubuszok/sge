/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package net

// RED regression suite for ISS-834 (reproducer; wave 2026-07-18-J, territory J2).
// Residual openURI fidelity gaps carried over from the wave-E auditor-V review of
// DesktopNet (findings 2 + 3).
//
// ── (a) invalid-URI construction sits OUTSIDE the try (DesktopNet.scala:47) ─────
// Upstream Lwjgl3Net.openURI (Lwjgl3Net.java:74-98) constructs `new URI(uri)`
// INSIDE every try/catch(Throwable) branch:
//     mac:   try { new ProcessBuilder("open", new URI(uri).toString()).start(); ... }
//            catch (Throwable t) { return false; }              // Lwjgl3Net.java:76-81
//     awt:   try { Desktop.getDesktop().browse(new URI(uri)); ... }
//            catch (Throwable t) { return false; }              // Lwjgl3Net.java:83-88
//     linux: try { new ProcessBuilder("xdg-open", new URI(uri).toString()).start(); ... }
//            catch (Throwable t) { return false; }              // Lwjgl3Net.java:90-95
// so an invalid URI is swallowed and the method returns false. DesktopNet instead
// parses `java.net.URI.create(URI)` at line 47, ABOVE the `try` that opens at line 48,
// so a malformed URI throws IllegalArgumentException straight out of openURI rather
// than returning false. This test drives the real openURI and asserts the upstream
// return-false behavior; it is RED until URI parsing moves inside the try.
//
// ── (b) rundll32 url.dll,FileProtocolHandler fallback (DesktopNet.scala:97) ──────
// The Windows fallback launches `rundll32 url.dll,FileProtocolHandler <uri>`, whose
// ANSI FileProtocolHandler entry truncates URLs past INTERNET_MAX_URL_LENGTH (2083)
// and can mangle non-ASCII IRIs. This is Windows-only AND fallback-only
// (java.awt.Desktop.browse covers real desktops), so the *runtime* truncation has NO
// cross-platform, pure-Scala seam to reproduce — it is OS behavior inside rundll32.
// FIX (ISS-834b, length guard): openUriCommand now refuses over-2083-char URLs on the
// Windows branch (returns Nil, so openURI logs + returns false) rather than handing
// rundll32 a URL it would silently truncate/ANSI-mangle. The pins below assert the
// guarded behavior at the observable argv seam: > 2083 => Nil, <= 2083 => full command.

class DesktopNetOpenUriIss834RedSuite extends munit.FunSuite {

  // openURI never dereferences its `app` argument (see the DesktopNet migration note:
  // the Application reference is the explicit-context stand-in for Gdx.app and is
  // otherwise unused). A cast placeholder is sufficient to exercise openURI in a test,
  // matching the existing `null.asInstanceOf[...]` idiom in DesktopInterfacesTest.
  private def net: DesktopNet = new DesktopNet(null.asInstanceOf[Application])

  test("openURI returns false for a malformed URI instead of throwing (ISS-834a)") {
    // A space is an illegal character in the scheme component, so java.net.URI.create
    // throws IllegalArgumentException (wrapping URISyntaxException). Upstream's
    // `new URI(uri)` throws the checked URISyntaxException, which the try/catch(Throwable)
    // turns into a `return false`. The port must match that: catch the invalid URI and
    // return false rather than letting the exception escape openURI.
    val malformed = "ht tp://exa mple.com"
    val result    =
      try net.openURI(malformed)
      catch {
        case e: IllegalArgumentException =>
          fail(
            "openURI must catch an invalid URI and return false like upstream " +
              s"Lwjgl3Net.java:74-98, but threw: $e"
          )
      }
    assertEquals(result, false, "openURI(malformed) must return false (upstream fidelity, ISS-834a)")
  }

  test("Windows rundll32 fallback refuses a URL longer than INTERNET_MAX_URL_LENGTH (ISS-834b guard)") {
    // The length guard: rundll32's ANSI FileProtocolHandler silently truncates URLs past
    // INTERNET_MAX_URL_LENGTH (2083) and can mangle non-ASCII IRIs, so openUriCommand must
    // NOT build the rundll32 command for an over-long URL — it returns Nil, making openURI
    // log and return false rather than opening a corrupted URL.
    val longUri = "http://example.com/?q=" + ("a" * 2100)
    assert(longUri.length > 2083, s"fixture must exceed INTERNET_MAX_URL_LENGTH; got ${longUri.length}")
    val cmd = DesktopNet.openUriCommand("Windows 10", longUri)
    assertEquals(
      cmd,
      Nil,
      s"an over-2083-char URL must not be handed to rundll32 (ISS-834b length guard); got: $cmd"
    )
  }

  test("Windows rundll32 fallback still builds the full command for a URL within INTERNET_MAX_URL_LENGTH (ISS-834b)") {
    // Below the guard threshold the Windows fallback is unchanged: the URI reaches rundll32
    // intact as a single argument (query ampersands included, per ISS-773).
    val okUri = "http://example.com/?a=1&b=2&c=3"
    assert(okUri.length <= 2083, s"fixture must stay within INTERNET_MAX_URL_LENGTH; got ${okUri.length}")
    val cmd = DesktopNet.openUriCommand("Windows 10", okUri)
    assertEquals(
      cmd,
      List("rundll32", "url.dll,FileProtocolHandler", okUri),
      s"a within-limit URL must still be launched via rundll32 with the full URI intact (ISS-834b); got: $cmd"
    )
  }
}
