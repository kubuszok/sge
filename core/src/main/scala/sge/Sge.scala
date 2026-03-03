/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Gdx.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
