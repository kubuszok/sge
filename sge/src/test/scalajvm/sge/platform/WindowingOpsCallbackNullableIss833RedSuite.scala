/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package platform

import lowlevel.Nullable

// RED (type-level) pin for ISS-833 (reproducer; wave 2026-07-18-J, territory J1), auditor V finding 4.
//
// The remaining WindowingOps callback setters (WindowingOps.scala:308-382) still document the callback
// as "`... => Unit`, or null to remove" and their JVM impl clears the slot with a raw `callback == null`
// check (WindowingOpsJvm.scala:774,796,818,840,862,891,913,945,967,990,1013,1036). That is the same
// null-sentinel family ISS-808 already migrated for setErrorCallback, which now takes
// `Nullable[(Int, String) => Unit]` (WindowingOps.scala:63) — the SGE null idiom (CLAUDE.md "No null":
// absence is modelled with the `Nullable[A]` opaque type, never a bare `null`). The remaining setters
// must migrate the same way.
//
// This suite pins the Nullable form at the TYPE LEVEL, mirroring ISS-808's
// WindowingOpsErrorCallbackIss808RedSuite: each eta-expansion below type-checks only once the setter's
// callback parameter is `Nullable[<fn>]`. Passing a `Nullable[...]` where a bare function is expected
// (the current signatures) is a type mismatch (Nullable is an opaque type), so every pin is COMPILE-RED
// against the current `(...) => Unit` signatures until the contract is migrated. Per ISS-851 these are
// real compiler-resolved references (assignments the zinc incremental compiler sees), NOT a
// typeCheckErrors string.
//
// WindowingOps is COVENANTED — this reproducer does NOT edit it; the contract change (and covenant
// re-baseline) is the implementer's.
class WindowingOpsCallbackNullableIss833RedSuite extends munit.FunSuite {

  test(
    "the remaining WindowingOps callback setters accept Nullable[...] per the SGE null idiom (ISS-833, mirroring ISS-808)"
  ) {
    val framebufferSize: WindowingOps => Nullable[(Long, Int, Int) => Unit] => Unit =
      ops => cb => ops.setFramebufferSizeCallback(0L, cb)
    val focus: WindowingOps => Nullable[(Long, Boolean) => Unit] => Unit =
      ops => cb => ops.setWindowFocusCallback(0L, cb)
    val iconify: WindowingOps => Nullable[(Long, Boolean) => Unit] => Unit =
      ops => cb => ops.setWindowIconifyCallback(0L, cb)
    val maximize: WindowingOps => Nullable[(Long, Boolean) => Unit] => Unit =
      ops => cb => ops.setWindowMaximizeCallback(0L, cb)
    val close: WindowingOps => Nullable[Long => Unit] => Unit =
      ops => cb => ops.setWindowCloseCallback(0L, cb)
    val drop: WindowingOps => Nullable[(Long, Array[String]) => Unit] => Unit =
      ops => cb => ops.setDropCallback(0L, cb)
    val refresh: WindowingOps => Nullable[Long => Unit] => Unit =
      ops => cb => ops.setWindowRefreshCallback(0L, cb)
    val key: WindowingOps => Nullable[(Long, Int, Int, Int, Int) => Unit] => Unit =
      ops => cb => ops.setKeyCallback(0L, cb)
    val char: WindowingOps => Nullable[(Long, Int) => Unit] => Unit =
      ops => cb => ops.setCharCallback(0L, cb)
    val scroll: WindowingOps => Nullable[(Long, Double, Double) => Unit] => Unit =
      ops => cb => ops.setScrollCallback(0L, cb)
    val cursorPos: WindowingOps => Nullable[(Long, Double, Double) => Unit] => Unit =
      ops => cb => ops.setCursorPosCallback(0L, cb)
    val mouseButton: WindowingOps => Nullable[(Long, Int, Int, Int) => Unit] => Unit =
      ops => cb => ops.setMouseButtonCallback(0L, cb)

    assert(
      (framebufferSize ne null) && (focus ne null) && (iconify ne null) && (maximize ne null) &&
        (close ne null) && (drop ne null) && (refresh ne null) && (key ne null) &&
        (char ne null) && (scroll ne null) && (cursorPos ne null) && (mouseButton ne null)
    )
  }
}
