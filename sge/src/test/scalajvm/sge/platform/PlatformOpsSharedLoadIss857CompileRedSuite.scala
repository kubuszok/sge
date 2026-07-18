/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Compile-shape red — ISS-857 (major), wave 2026-07-18-I, territory I1.
 * Reproducer-authored: MUST NOT be modified by the fixer.
 *
 * This is the DETERMINISTIC red for ISS-857 (red on EVERY platform axis until the
 * fix lands, regardless of whether a native provider is present). It pins the
 * fix's design seam as a COMPILE-LEVEL contract.
 *
 * DEFECT. ETC1OpsPanama and BufferOpsPanama each independently call
 * multiarch.core.NativeLibLoader.load("sge_native_ops") (ETC1OpsPanama.scala:26,
 * BufferOpsPanama.scala:29), so a JVM app touching both PlatformOps.etc1 and
 * PlatformOps.buffer issues the SAME native-lib load TWICE in one process. SGE's
 * idempotency therefore hinges on a third-party implementation detail (multiarch's
 * per-name loader cache), not on anything SGE controls. See the sibling
 * PlatformOpsSharedLoadIss857RedSuite header for why the RUNTIME crash is
 * order-dependent: multiarch 0.4.0's per-name cache masks it only under
 * SEQUENTIAL access; a CONCURRENT first-time double load still races (non-atomic
 * check-then-act) and throws FileAlreadyExistsException despite REPLACE_EXISTING
 * — the real, reproduced train-19 crash.
 *
 * PRESCRIBED FIX (ISS-857): memoize the load exactly once in a single seam that
 * both ops consume — no constructor signature changes:
 *
 *   package sge
 *   package platform
 *   private[platform] object SgeNativeOpsLib {
 *     // one load per JVM, shared by ETC1OpsPanama and BufferOpsPanama
 *     lazy val path: java.nio.file.Path =
 *       multiarch.core.NativeLibLoader.load("sge_native_ops")
 *   }
 *
 * and both ops replace
 *   val found = multiarch.core.NativeLibLoader.load("sge_native_ops")
 * with
 *   val found = SgeNativeOpsLib.path
 *
 * THE RED. This suite hard-references `sge.platform.SgeNativeOpsLib.path` at the
 * TYPE level. Until the object exists the reference does not resolve, so this
 * compilation unit FAILS TO COMPILE — the module goes red on every axis
 * ("value path is not a member of ... SgeNativeOpsLib" / "SgeNativeOpsLib is not
 * a member of sge.platform"). Once the fix introduces the object with a
 * `path: java.nio.file.Path` member, this compiles and the test passes.
 *
 * WHY A HARD REFERENCE, NOT typeCheckErrors (ISS-851 anchor discipline). A
 * compile-shape red that references the target only inside a typeCheckErrors
 * string literal is invisible to zinc's incremental compiler: after the symbol
 * exists and the suite goes green, a REGRESSION that deletes SgeNativeOpsLib (or
 * reverts both ops to call NativeLibLoader.load directly) would NOT recompile the
 * suite, leaving a stale pass — and for a symbol that does not exist YET, no
 * real-code zinc anchor can be written pre-fix either. A direct hard reference
 * sidesteps both problems: it IS a real zinc dependency (any change to
 * SgeNativeOpsLib.path recompiles this suite), and its absence is itself the red
 * (the module cannot compile), so this suite is self-anchoring by construction.
 *
 * WHY TYPE-LEVEL, NEVER EXECUTED. Evaluating `SgeNativeOpsLib.path` would force
 * the native-lib load, which is legitimately absent on native-less JVM legs.
 * The reference lives in an uncalled method and is only ever wrapped in an
 * un-invoked thunk, so post-fix this suite is GREEN on every axis WITHOUT needing
 * the native library present.
 */
package sge
package platform

class PlatformOpsSharedLoadIss857CompileRedSuite extends munit.FunSuite {

  // Self-anchoring hard reference: type-level only, never evaluated. Compiles
  // ONLY once `private[platform] object SgeNativeOpsLib { lazy val path }` exists.
  // Returning the seam (not forcing it) keeps the native load out of this method.
  private def sharedNativeOpsLibPathSeam: java.nio.file.Path = SgeNativeOpsLib.path

  test("ISS-857 (compile-shape): both ops must share one memoized native-lib load via SgeNativeOpsLib.path") {
    // The seam is referenced through an un-invoked thunk so `.path` is never
    // forced (no native load required for this test to pass post-fix). The
    // load-bearing assertion is that this file COMPILES — i.e. that the
    // SgeNativeOpsLib.path memoization seam exists at all.
    val seamThunk: () => java.nio.file.Path = () => sharedNativeOpsLibPathSeam
    assert(seamThunk ne null, "SgeNativeOpsLib.path must exist so ETC1OpsPanama and BufferOpsPanama share one load")
  }
}
