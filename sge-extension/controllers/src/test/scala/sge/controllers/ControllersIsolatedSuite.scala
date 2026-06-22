/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Test-isolation seam for the process-global Controllers singleton.
 *
 * The Controllers facade is an init-once global singleton (manager + frame hooks
 * + installed app hook). munit runs distinct suites in PARALLEL on the JVM while
 * running the tests within a single suite sequentially. Several suites mutate the
 * shared Controllers singleton (initialize / dispose / installAutoPollInto /
 * registeredFrameHooks), so without isolation two suites racing on the singleton
 * corrupt each other's state — manifesting as doubled poll counts and the
 * "throws when not initialized" contract failing because another suite left the
 * singleton initialized.
 *
 * This base suite isolates them by wrapping each Controllers-touching test body in
 * `serialized { ... }`, which atomically, while holding `ControllersGlobalLock`:
 *   1. resets the singleton to its pristine un-initialized state, then
 *   2. runs the test body, then
 *   3. resets it again.
 * Because the reset AND the body are inside the same lock acquisition, no other
 * suite's reset or body can interleave with this test — the whole reset→body→reset
 * sequence is atomic relative to every other Controllers-touching test. This makes
 * the suites pass regardless of execution order or parallel scheduling.
 *
 * On Scala.js / Scala Native (single-threaded, no parallel suite execution) the lock
 * is an uncontended no-op; the per-test reset still guarantees order-independence.
 */
package sge
package controllers

/** Shared lock guarding the process-global [[Controllers]] singleton so that, on the JVM, Controllers-touching test suites running in parallel never mutate it concurrently. */
object ControllersGlobalLock

/** Base suite for every test that touches the process-global [[Controllers]] singleton. Wrap each Controllers-touching test body in [[serialized]] to run it under [[ControllersGlobalLock]] with a
  * pristine-state reset on both sides, making the suites pass regardless of execution order or parallelism. See the file header for the full rationale.
  */
abstract class ControllersIsolatedSuite extends munit.FunSuite {

  /** Runs the given test body atomically with respect to every other Controllers-touching test: while holding [[ControllersGlobalLock]], it resets the [[Controllers]] singleton to its pristine
    * un-initialized state, runs the body, then resets it again. Holding the lock across the whole reset→body→reset sequence guarantees no concurrent suite can observe or mutate intermediate state.
    */
  protected def serialized[A](body: => A): A =
    ControllersGlobalLock.synchronized {
      Controllers.resetForTest()
      try body
      finally Controllers.resetForTest()
    }
}
