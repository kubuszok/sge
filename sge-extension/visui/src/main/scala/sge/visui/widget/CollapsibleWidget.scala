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

import sge.graphics.g2d.Batch
import sge.math.Interpolation
import sge.scenes.scene2d.Touchable
import sge.scenes.scene2d.actions.FloatAction
import sge.scenes.scene2d.ui.{ Table, WidgetGroup }
import sge.utils.{ Nullable, Seconds }

/** Widget containing table that can be vertically collapsed.
  * @author
  *   Kotcrab
  * @see
  *   [[HorizontalCollapsibleWidget]]
  * @since 0.3.1
  */
class CollapsibleWidget(using Sge) extends WidgetGroup {
  private var _table: Nullable[Table] = Nullable.empty

  private val collapseAction:        CollapsibleWidget.CollapseAction = new CollapsibleWidget.CollapseAction(this)
  private var collapseDuration:      Float                            = 0.3f
  private var collapseInterpolation: Interpolation                    = Interpolation.pow3Out

  private var _collapsed:    Boolean = false
  private var actionRunning: Boolean = false
  private var currentHeight: Float   = 0f

  def this(table: Table)(using Sge) = {
    this()
    setTable(table)
  }

  def this(table: Table, collapsed: Boolean)(using Sge) = {
    this()
    _collapsed = collapsed
    setTable(table)
    updateTouchable()
  }

  def setCollapsed(collapse: Boolean, withAnimation: Boolean): Unit = {
    _collapsed = collapse
    updateTouchable()

    if (_table.isEmpty) return

    actionRunning = true

    if (withAnimation) {
      collapseAction.restart()
      collapseAction.start = currentHeight
      collapseAction.setEnd(if (collapse) 0f else _table.get.prefHeight)
      collapseAction.duration = Seconds(collapseDuration)
      collapseAction.interpolation = Nullable(collapseInterpolation)
      addAction(collapseAction)
    } else {
      if (collapse) {
        currentHeight = 0
        _collapsed = true
      } else {
        currentHeight = _table.get.prefHeight
        _collapsed = false
      }
      actionRunning = false
      invalidateHierarchy()
    }
  }

  def collapsed_=(collapse: Boolean): Unit    = setCollapsed(collapse, withAnimation = true)
  def collapsed:                      Boolean = _collapsed

  private def updateTouchable(): Unit =
    if (_collapsed) touchable = Touchable.disabled
    else touchable = Touchable.enabled

  def collapseDuration_=(value:      Float):         Unit = collapseDuration = value
  def collapseInterpolation_=(value: Interpolation): Unit = collapseInterpolation = value

  override def draw(batch: Batch, parentAlpha: Float): Unit =
    if (currentHeight > 1 && y + currentHeight > 1) {
      if (actionRunning) {
        batch.flush()
        val clipEnabled = clipBegin(x, y, width, currentHeight)
        super.draw(batch, parentAlpha)
        batch.flush()
        if (clipEnabled) clipEnd()
      } else {
        super.draw(batch, parentAlpha)
      }
    }

  override def layout(): Unit = {
    if (_table.isEmpty) return
    _table.get.setBounds(0, 0, _table.get.prefWidth, _table.get.prefHeight)
    if (!actionRunning) {
      if (_collapsed) currentHeight = 0
      else currentHeight = _table.get.prefHeight
    }
  }

  override def prefWidth: Float = if (_table.isEmpty) 0 else _table.get.prefWidth

  override def prefHeight: Float = {
    if (_table.isEmpty) return 0
    if (!actionRunning) {
      if (_collapsed) 0
      else _table.get.prefHeight
    } else {
      currentHeight
    }
  }

  def setTable(table: Table): Unit = {
    _table = Nullable(table)
    clearChildren()
    addActor(table)
  }

  override protected def childrenChanged(): Unit = {
    super.childrenChanged()
    if (children.size > 1) throw sge.utils.SgeError.InvalidInput("Only one actor can be added to CollapsibleWidget")
  }
}

private[widget] object CollapsibleWidget {
  private class CollapseAction(widget: CollapsibleWidget) extends FloatAction {
    override protected def update(percent: Float): Unit = {
      super.update(percent)
      widget.currentHeight = value
      if (percent == 1) {
        widget.actionRunning = false
        widget._collapsed = widget.currentHeight == 0
      }
      widget.invalidateHierarchy()
    }
  }
}
