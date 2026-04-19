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
 */
package sge

import sge.Graphics

final case class Sge private[sge] (
  application: Application,
  graphics:    Graphics,
  audio:       Audio,
  files:       Files,
  input:       Input,
  net:         Net
)
object Sge {

  inline def apply()(using Sge): Sge = summon[Sge]
}
