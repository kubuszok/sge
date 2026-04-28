/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/HdpiMode.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Scala 3 enum instead of Java enum
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 28
 * Covenant-baseline-methods: HdpiMode
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/HdpiMode.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package glutils

enum HdpiMode {

  /** mouse coordinates, {@link Graphics#getWidth()} and {@link Graphics#getHeight()} will return logical coordinates according to the system defined HDPI scaling. Rendering will be performed to a
    * backbuffer at raw resolution. Use {@link HdpiUtils} when calling {@link GL20#glScissor} or {@link GL20#glViewport} which expect raw coordinates.
    */
  case Logical

  /** Mouse coordinates, {@link Graphics#getWidth()} and {@link Graphics#getHeight()} will return raw pixel coordinates irrespective of the system defined HDPI scaling.
    */
  case Pixels
}
