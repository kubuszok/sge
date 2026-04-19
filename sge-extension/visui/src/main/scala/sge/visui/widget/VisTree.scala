/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: VisTree,init,this,touchDown
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisTree.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.ui.Tree
import sge.utils.Nullable
import sge.visui.{ FocusManager, VisUI }

/** Does not provide additional features over standard [[Tree]], however for proper VisUI focus border management VisTree should be always preferred. Compatible with standard [[Tree]].
  * @author
  *   Kotcrab
  * @see
  *   [[Tree]]
  */
class VisTree[N <: Tree.Node[N, V, ? <: Actor], V](treeStyle: Tree.TreeStyle)(using Sge) extends Tree[N, V](treeStyle) {

  init()

  def this()(using Sge) = this(VisUI.getSkin.get[Tree.TreeStyle])
  def this(styleName: String)(using Sge) = this(VisUI.getSkin.get[Tree.TreeStyle](styleName))

  private def init(): Unit =
    addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          val focusable = FocusManager.getFocusedWidget
          focusable.foreach { f =>
            f match {
              case a: Actor if isAscendantOf(a) => // keep focus
              case _ => FocusManager.resetFocus(stage)
            }
          }
          false
        }
      }
    )
}
