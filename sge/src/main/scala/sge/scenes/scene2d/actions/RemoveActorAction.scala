/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RemoveActorAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.remove() -> target.foreach(_.remove())
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: RemoveActorAction,act,removed,restart
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/RemoveActorAction.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package actions

/** Removes an actor from the stage.
  * @author
  *   Nathan Sweet
  */
class RemoveActorAction extends Action {
  import sge.utils.Seconds

  private var removed: Boolean = false

  def act(delta: Seconds): Boolean = {
    if (!removed) {
      removed = true
      target.foreach(_.remove())
    }
    true
  }

  override def restart(): Unit = removed = false
}
