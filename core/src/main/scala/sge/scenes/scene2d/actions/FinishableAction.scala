/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/FinishableAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Java interface -> Scala trait
 *   Audited: 2026-03-03
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
