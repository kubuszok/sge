/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import sge.scenes.scene2d.{ InputEvent, InputListener }
import sge.scenes.scene2d.ui.SgeList
import sge.visui.{ FocusManager, VisUI }

/** Compatible with [[SgeList]]. Does not provide additional features however for proper VisUI focus management VisList should be always preferred.
  * @author
  *   Kotcrab
  * @see
  *   [[SgeList]]
  */
class VisList[T](listStyle: SgeList.ListStyle)(using Sge) extends SgeList[T](listStyle) {

  init()

  def this()(using Sge) = this(VisUI.getSkin.get[SgeList.ListStyle])
  def this(styleName: String)(using Sge) = this(VisUI.getSkin.get[SgeList.ListStyle](styleName))

  private def init(): Unit =
    addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          FocusManager.resetFocus(stage)
          false
        }
      }
    )
}
