/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import scala.compiletime.testing.*

/** ISS-555 (part A): `final case class Sge` must carry a `@scala.annotation.implicitNotFound(...)` annotation so that a missing `(using Sge)` produces a helpful, domain-specific compiler message
  * instead of the generic "no given instance of type sge.Sge".
  *
  * The canonical custom message (verbatim, baked into the annotation) is:
  *
  * "No given `Sge` is in scope. `Sge` is this application's context — graphics, audio, input, files, net — passed explicitly via `(using Sge)` (it replaces LibGDX's global `Gdx.*`). Add a
  * `(using Sge)` parameter to the enclosing class constructor or method, propagating the `Sge` your `Game`/`ApplicationListener` already receives."
  *
  * This suite is RED today: with no `@implicitNotFound` annotation, the compiler emits its generic "no given instance" diagnostic, which does NOT contain the distinctive phrases "passed explicitly
  * via" and "replaces LibGDX's global". It turns GREEN once the annotation is added to `sge.Sge`.
  */
class SgeImplicitNotFoundIss555RedSuite extends munit.FunSuite {

  test("ISS-555 missing (using Sge) yields the custom @implicitNotFound message") {
    // Requires a `using Sge` with none in scope — must fail to type-check.
    val errors: List[Error] = typeCheckErrors("summon[sge.Sge]")

    assert(
      errors.nonEmpty,
      "expected summon[sge.Sge] to fail type-checking (no given Sge in scope)"
    )

    val messages = errors.map(_.message)
    assert(
      messages.exists(_.contains("passed explicitly via")),
      s"expected the custom @implicitNotFound message containing \"passed explicitly via\"; got: $messages"
    )
    assert(
      messages.exists(_.contains("replaces LibGDX's global")),
      s"expected the custom @implicitNotFound message containing \"replaces LibGDX's global\"; got: $messages"
    )
  }

  // ISS-851: zinc-visible dependency anchor. The compile-shape assertion above
  // exercises `summon[sge.Sge]` (and its @implicitNotFound diagnostic) ONLY inside
  // a typeCheckErrors string literal, which zinc's incremental compiler cannot see
  // — so a change to Sge's @implicitNotFound annotation would NOT recompile this
  // suite, leaving a stale pass (the wave-G false-green trap). Unlike the other
  // ISS-851 anchors, no positive real-code mirror is possible here: the assertion
  // pins a MISSING-given diagnostic, and introducing a `given Sge` to reference
  // would itself make `summon[sge.Sge]` succeed and invert the test. The guarded
  // regression, however, is a CLASS-LEVEL change (the @implicitNotFound annotation
  // sits on `Sge` itself, not on a member), so a class-name reference is the
  // correctly-scoped anchor: `classOf[Sge]` records a dependency on the `Sge`
  // name, whose api hash includes its class annotations, so re-editing
  // @implicitNotFound recompiles this suite. Type-level reference in an uncalled
  // method: never executed, no side effects.
  def zincAnchor: Class[Sge] = classOf[Sge]
}
