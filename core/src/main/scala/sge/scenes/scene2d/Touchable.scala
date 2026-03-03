/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/Touchable.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Java enum -> Scala 3 enum
 *   Convention: Split packages; braces on enum body
 *   Idiom: Exact 1:1 port, no behavioral changes
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d

/** Determines how touch input events are distributed to an actor and any children.
  * @author
  *   Nathan Sweet
  */
enum Touchable {

  /** All touch input events will be received by the actor and any children. */
  case enabled

  /** No touch input events will be received by the actor or any children. */
  case disabled

  /** No touch input events will be received by the actor, but children will still receive events. Note that events on the children will still bubble to the parent.
    */
  case childrenOnly
}
