/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 84
 * Covenant-baseline-methods: FloatingGroup,_prefHeight,_prefWidth,_useChildrenPreferredSize,childArr,i,layout,prefHeight,prefWidth,setPrefHeight,setPrefWidth,this,useChildrenPreferredSize,useChildrenPreferredSize_
 * Covenant-source-reference: com/kotcrab/vis/ui/layout/FloatingGroup.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package layout

import sge.scenes.scene2d.Touchable
import sge.scenes.scene2d.ui.WidgetGroup
import sge.scenes.scene2d.utils.Layout

/** Does not arrange actors in specific layout, instead it uses their position and preferred sizes to place them inside group. Does not check position values and allows widgets to be placed outside
  * group. If combined with [[sge.visui.widget.VisWindow.keepWithinParent]] allows to create windows that can only moved within this group area.
  * @author
  *   Kotcrab
  * @since 1.0.2
  */
class FloatingGroup()(using Sge) extends WidgetGroup() {
  private var _useChildrenPreferredSize: Boolean = false
  private var _prefWidth:                Float   = 0f
  private var _prefHeight:               Float   = 0f

  touchable = Touchable.childrenOnly

  /** Creates floating group with fixed area size.
    * @param prefWidth
    *   preferred width of group. If set to lower than 0 then [[height]] is used as preferred height.
    * @param prefHeight
    *   preferred height of group. If set to lower than 0 then [[width]] is used as preferred width.
    */
  def this(prefWidth: Float, prefHeight: Float)(using Sge) = {
    this()
    _prefWidth = prefWidth
    _prefHeight = prefHeight
  }

  override def layout(): Unit = {
    if (!_useChildrenPreferredSize) return
    val childArr = children

    var i = 0
    while (i < childArr.size) {
      val child = childArr(i)
      var w     = child.width
      var h     = child.height
      child match {
        case layout: Layout =>
          w = layout.prefWidth
          h = layout.prefHeight
        case _ =>
      }
      child.setBounds(child.x, child.y, w, h)
      i += 1
    }
  }

  def useChildrenPreferredSize: Boolean = _useChildrenPreferredSize

  /** If true then children preferred size will be used whenever possible when doing group layout. If set to true changing widget width and height using [[sge.scenes.scene2d.Actor.width]] and
    * [[sge.scenes.scene2d.Actor.height]] may not have effect for instances of [[Layout]] (depending on implementation). Default is false.
    */
  def useChildrenPreferredSize_=(value: Boolean): Unit = {
    _useChildrenPreferredSize = value
    invalidate()
  }

  override def prefWidth: Float = if (_prefWidth < 0f) width else _prefWidth

  override def prefHeight: Float = if (_prefHeight < 0f) height else _prefHeight

  def setPrefWidth(prefWidth: Float): Unit = {
    _prefWidth = prefWidth
    invalidate()
  }

  def setPrefHeight(prefHeight: Float): Unit = {
    _prefHeight = prefHeight
    invalidate()
  }
}
