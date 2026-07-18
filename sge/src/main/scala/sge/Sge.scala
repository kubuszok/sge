/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Gdx.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Gdx -> Sge
 *   Convention: static fields -> final case class with (using Sge) context; Sge() summons implicit
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: Sge,apply
 * Covenant-source-reference: com/badlogic/gdx/Gdx.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 5f2dae5f6879556f663774ba9d9ff439b5bae822
 */
package sge

import sge.Graphics
import scala.annotation.implicitNotFound

/** The per-application context bundling the engine's core services: [[Application]], [[Graphics]], [[Audio]], [[Files]], [[Input]] and [[Net]].
  *
  * `Sge` is the entry point for reaching any engine service. Where LibGDX exposes global static fields (`Gdx.graphics`, `Gdx.input`, …), SGE passes a single `Sge` value explicitly through
  * `(using Sge)` context parameters. Your [[Game]] / [[ApplicationListener]] receives it and propagates it to the classes it constructs; from anywhere it is in scope, summon it with `Sge()` and reach
  * a service as `Sge().graphics`, `Sge().input`, and so on.
  *
  * @note
  *   LibGDX: `com.badlogic.gdx.Gdx` — the global static holder became this explicitly-passed context.
  */
@implicitNotFound(
  "No given `Sge` is in scope. `Sge` is this application's context — graphics, audio, input, files, net — passed explicitly via `(using Sge)` (it replaces LibGDX's global `Gdx.*`). Add a `(using Sge)` parameter to the enclosing class constructor or method, propagating the `Sge` your `Game`/`ApplicationListener` already receives."
)
final case class Sge private[sge] (
  application: Application,
  graphics:    Graphics,
  audio:       Audio,
  files:       Files,
  input:       Input,
  net:         Net
)
object Sge {

  /** Summons the `Sge` context that is in scope. Sugar for `summon[Sge]`; use it to reach a service, e.g. `Sge().graphics`, wherever a `(using Sge)` parameter is available.
    */
  inline def apply()(using Sge): Sge = summon[Sge]
}
