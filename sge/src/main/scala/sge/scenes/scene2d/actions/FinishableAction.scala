/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/FinishableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Java interface -> Scala trait
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: FinishableAction,finish
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/FinishableAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 59d660057ddc8f835b38c4dd66ed8954259edac7
 */
package sge
package scenes
package scene2d
package actions

/** An interface for actions that can be finished manually. */
trait FinishableAction {

  /** Manually finishes the action, performing necessary finalization steps. */
  def finish(): Unit
}
