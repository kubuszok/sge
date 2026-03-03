/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RemoveActorAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Idiom: target.remove() -> target.foreach(_.remove())
 *   Audited: 2026-03-03
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
  private var removed: Boolean = false

  def act(delta: Float): Boolean = {
    if (!removed) {
      removed = true
      target.foreach(_.remove())
    }
    true
  }

  override def restart(): Unit = removed = false
}
