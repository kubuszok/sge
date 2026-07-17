/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package platform

// RED (type-level) pin for ISS-808 (reproducer; wave 2026-07-17-E, territory V).
//
// WindowingOps.setErrorCallback (WindowingOps.scala:53-61) is new shared trait API
// whose scaladoc says the callback param `null` "clears it" — a null-sentinel on a
// plain Scala function type `(Int, String) => Unit`. That contradicts the SGE
// project idiom: absence of a reference is modelled with the `Nullable[A]` opaque
// type, never a bare `null` (CLAUDE.md "No null"). Every other nullable seam in the
// codebase already uses it (e.g. the recording ops store the callback as
// `Nullable[(Int, String) => Unit]`, DesktopRecordingOpsIss762Iss764.scala:95).
//
// ISS-808 adjudication: the shared-trait contract must be
// `Nullable[(Int, String) => Unit]` (or an explicit clearErrorCallback). This suite
// pins the Nullable form at the TYPE LEVEL: the assignment below type-checks only
// once setErrorCallback accepts a `Nullable[(Int, String) => Unit]`.
//
// WindowingOps is COVENANTED — this reproducer does NOT edit it; the type pin is
// COMPILE-RED against the current `(Int, String) => Unit` signature until the
// implementer changes the contract (and re-baselines the covenant).

class WindowingOpsErrorCallbackIss808RedSuite extends munit.FunSuite {

  test("setErrorCallback accepts Nullable[(Int, String) => Unit] per the SGE null idiom (ISS-808)") {
    // Type-level pin: this eta-expansion compiles only if the parameter type is
    // Nullable[(Int, String) => Unit]. Passing a Nullable where a bare function is
    // expected (the current signature) is a type mismatch (opaque type), so this is
    // COMPILE-RED until the contract is fixed.
    val pin: WindowingOps => lowlevel.Nullable[(Int, String) => Unit] => Unit =
      ops => cb => ops.setErrorCallback(cb)
    assert(pin ne null)
  }
}
