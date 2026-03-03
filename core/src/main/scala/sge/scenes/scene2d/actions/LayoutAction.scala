/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/LayoutAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package actions

/** Sets an actor's layout to enabled or disabled. The actor must implement Layout.
  * @author
  *   Nathan Sweet
  */
class LayoutAction extends Action {
  private var enabled: Boolean = false

  // TODO: uncomment when Layout is ported
  // override def setTarget(actor: Nullable[Actor]): Unit = {
  //   actor.foreach { a =>
  //     if (!a.isInstanceOf[Layout]) throw SgeError.InvalidInput("Actor must implement layout: " + a)
  //   }
  //   super.setTarget(actor)
  // }

  def act(delta: Float): Boolean =
    // TODO: uncomment when Layout is ported
    // target.foreach {
    //   case l: Layout => l.setLayoutEnabled(enabled)
    //   case _ =>
    // }
    true

  def isEnabled: Boolean = enabled

  def setLayoutEnabled(enabled: Boolean): Unit = this.enabled = enabled
}
