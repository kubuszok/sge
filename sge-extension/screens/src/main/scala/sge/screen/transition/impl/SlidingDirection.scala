/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 30
 * Covenant-baseline-methods: SlidingDirection
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package screen
package transition
package impl

/** An enum denoting the slide direction for the respective transitions.
  *
  * @author
  *   damios
  *
  * @see
  *   SlidingInTransition
  * @see
  *   SlidingOutTransition
  * @see
  *   PushTransition
  */
enum SlidingDirection(val xPosFactor: Int, val yPosFactor: Int) {
  case UP extends SlidingDirection(0, 1)
  case DOWN extends SlidingDirection(0, -1)
  case LEFT extends SlidingDirection(-1, 0)
  case RIGHT extends SlidingDirection(1, 0)
}
