/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/Disableable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface -> Scala trait
 * - Faithful port, no API changes
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 22
 * Covenant-baseline-methods: Disableable,disabled,disabled_
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/Disableable.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package utils

trait Disableable {
  def disabled_=(value: Boolean): Unit

  def disabled: Boolean
}
