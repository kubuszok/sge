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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 56
 * Covenant-baseline-methods: AfterAction,delegate,restart,setTarget,waitForActions
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/actions/AfterAction.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package actions

import sge.utils.{ DynamicArray, Nullable, Seconds }

/** Executes an action only after all other actions on the actor at the time this action's target was set have finished.
  * @author
  *   Nathan Sweet
  */
class AfterAction extends DelegateAction {
  private val waitForActions: DynamicArray[Action] = DynamicArray[Action]()

  override def setTarget(newTarget: Nullable[Actor]): Unit = {
    newTarget.foreach { t =>
      waitForActions.addAll(t.actions)
    }
    super.setTarget(newTarget)
  }

  override def restart(): Unit = {
    super.restart()
    waitForActions.clear()
  }

  override protected def delegate(delta: Seconds): Boolean =
    target.forall { t =>
      val currentActions = t.actions
      if (currentActions.size == 1) waitForActions.clear()
      var i = waitForActions.size - 1
      while (i >= 0) {
        val a = waitForActions(i)
        if (!currentActions.contains(a)) waitForActions.removeIndex(i)
        i -= 1
      }
      if (waitForActions.nonEmpty) false
      else action.forall(_.act(delta))
    }
}
