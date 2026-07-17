/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package net

// RED regression test for ISS-773 (reproducer; wave 2026-07-17-E, territory V).
//
// DesktopNet.openURI (DesktopNet.scala:41-63) opens a URI on Windows with
//
//     new ProcessBuilder("cmd", "/c", "start", uri).start()
//
// cmd.exe's `start` builtin RE-PARSES its command line, and `&` (and `^`) are
// cmd metacharacters: `start http://x/?a=1&b=2` runs `start http://x/?a=1` and
// then tries to run `b=2` as a SECOND command. So a perfectly valid URI whose
// query contains `&` is silently truncated / shell-interpreted — the browser
// receives only `http://x/?a=1`. LibGDX opened URIs via `java.awt.Desktop.browse`
// (a java.net.URI, never a shell), which is immune to this.
//
// The Scala-side string already keeps `&` intact (URI.create(...).toString), so
// the bug is NOT observable in the string — it is in the *launch mechanism*
// (routing the URI through cmd's `start`). Pinning it therefore requires a
// seam that exposes the command construction WITHOUT launching a process.
//
// ── Implementer NOTE (anticipated-new-method pattern, cf. the createCursor note
//    in DesktopRecordingOpsIss762Iss764) ───────────────────────────────────────
// DesktopNet has no command-construction seam yet — openURI builds and launches
// the ProcessBuilder inline. The faithful fix (LibGDX Desktop.browse) must factor
// the per-OS decision out into a pure, launch-free helper on the DesktopNet
// companion, e.g.
//
//     private[sge] def openUriCommand(osName: String, uri: String): List[String]
//
// returning the process argv that WOULD be launched for that os.name (or Nil when
// a non-shell mechanism such as java.awt.Desktop.browse is used instead). openURI
// then calls it with the real System.getProperty("os.name"). This suite asserts on
// that observable command, not on any one fixed FFI signature: a `&`-bearing URI on
// Windows must NOT be routed through cmd's `start` builtin (which splits on `&`).
//
// This suite is COMPILE-RED until that seam lands.

class DesktopNetOpenUriIss773RedSuite extends munit.FunSuite {

  private val UriWithAmpersand = "http://example.com/?a=1&b=2&c=3"

  test("Windows: a URI containing '&' is not routed through cmd's start builtin (ISS-773)") {
    val cmd = DesktopNet.openUriCommand("Windows 10", UriWithAmpersand)
    // `cmd /c start <uri>` lets cmd re-parse `&` as a command separator, truncating
    // the URI. A safe launcher (Desktop.browse -> Nil, or a non-shell process) must
    // not begin with `cmd`.
    assert(
      !cmd.headOption.contains("cmd"),
      s"Windows openURI must not route a '&'-bearing URI through cmd's start builtin (ISS-773); got: $cmd"
    )
    // If a process command IS produced (non-Desktop.browse fallback), the full URI —
    // ampersands included — must reach the launched program as a single, intact token.
    if (cmd.nonEmpty) {
      assert(
        cmd.contains(UriWithAmpersand),
        s"the full URI (with '&') must survive intact as one argument (ISS-773); got: $cmd"
      )
    }
  }

  test("the '&' in the URI query survives command construction on every desktop OS (ISS-773)") {
    // The safe OSes (mac 'open', linux 'xdg-open') already pass the URI as a single
    // ProcessBuilder argument; pin that they keep doing so and that Windows joins them.
    for (osName <- List("Mac OS X", "Linux", "Windows 10")) {
      val cmd = DesktopNet.openUriCommand(osName, UriWithAmpersand)
      if (cmd.nonEmpty) {
        assert(
          cmd.contains(UriWithAmpersand),
          s"[$osName] URI with '&' must survive intact as one command argument (ISS-773); got: $cmd"
        )
        assert(
          !cmd.headOption.contains("cmd"),
          s"[$osName] must not shell out to cmd (ISS-773); got: $cmd"
        )
      }
    }
  }
}
