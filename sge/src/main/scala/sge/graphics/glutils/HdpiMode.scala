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
