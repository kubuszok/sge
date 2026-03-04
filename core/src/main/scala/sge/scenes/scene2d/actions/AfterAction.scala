/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/AfterAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: null -> Nullable[A]; no return; split packages; braces on class
 *   Renames: Array<Action> -> DynamicArray[Action]
 *   Idiom: target null-check -> newTarget.foreach; target null-access -> target.fold(true);
 *          for loop -> while; waitForActions.size > 0 -> waitForActions.nonEmpty;
 *          indexOf(action, true) == -1 -> !contains(action)
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ DynamicArray, Nullable }

/** Executes an action only after all other actions on the actor at the time this action's target was set have finished.
  * @author
  *   Nathan Sweet
  */
class AfterAction extends DelegateAction {
  private val waitForActions: DynamicArray[Action] = DynamicArray[Action]()

  override def setTarget(newTarget: Nullable[Actor]): Unit = {
    newTarget.foreach { t =>
      waitForActions.addAll(t.getActions)
    }
    super.setTarget(newTarget)
  }

  override def restart(): Unit = {
    super.restart()
    waitForActions.clear()
  }

  override protected def delegate(delta: Float): Boolean =
    target.fold(true) { t =>
      val currentActions = t.getActions
      if (currentActions.size == 1) waitForActions.clear()
      var i = waitForActions.size - 1
      while (i >= 0) {
        val a = waitForActions(i)
        if (!currentActions.contains(a)) waitForActions.removeIndex(i)
        i -= 1
      }
      if (waitForActions.nonEmpty) false
      else action.fold(true)(_.act(delta))
    }
}
