/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 117
 * Covenant-baseline-methods: IconStack,_maxHeight,_maxWidth,_minHeight,_minWidth,_prefHeight,_prefWidth,add,ch,checkHeight,checkStyle,computeSize,h,i,invalidate,layout,maxHeight,maxWidth,minHeight,minWidth,n,prefHeight,prefWidth,sizeInvalid,w
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/IconStack.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file
package internal

import sge.scenes.scene2d.{ Actor, Touchable }
import sge.scenes.scene2d.ui.WidgetGroup
import sge.scenes.scene2d.utils.Layout
import sge.visui.widget.{ VisCheckBox, VisImage }

/** @author Kotcrab */
class IconStack(icon: VisImage, checkBox: VisCheckBox)(using Sge) extends WidgetGroup {
  private var _prefWidth:  Float   = 0f
  private var _prefHeight: Float   = 0f
  private var _minWidth:   Float   = 0f
  private var _minHeight:  Float   = 0f
  private var _maxWidth:   Float   = 0f
  private var _maxHeight:  Float   = 0f
  private var sizeInvalid: Boolean = true

  transform = false
  touchable = Touchable.childrenOnly
  addActor(icon)
  addActor(checkBox)

  override def invalidate(): Unit = {
    super.invalidate()
    sizeInvalid = true
  }

  private def computeSize(): Unit = {
    sizeInvalid = false
    _prefWidth = 0
    _prefHeight = 0
    _minWidth = 0
    _minHeight = 0
    _maxWidth = 0
    _maxHeight = 0
    val ch = children
    var i  = 0
    val n  = ch.size
    while (i < n) {
      val child          = ch(i)
      var childMaxWidth  = 0f
      var childMaxHeight = 0f
      child match {
        case l: Layout =>
          _prefWidth = Math.max(_prefWidth, l.prefWidth)
          _prefHeight = Math.max(_prefHeight, l.prefHeight)
          _minWidth = Math.max(_minWidth, l.minWidth)
          _minHeight = Math.max(_minHeight, l.minHeight)
          childMaxWidth = l.maxWidth
          childMaxHeight = l.maxHeight
        case _ =>
          _prefWidth = Math.max(_prefWidth, child.width)
          _prefHeight = Math.max(_prefHeight, child.height)
          _minWidth = Math.max(_minWidth, child.width)
          _minHeight = Math.max(_minHeight, child.height)
      }
      if (childMaxWidth > 0) _maxWidth = if (_maxWidth == 0) childMaxWidth else Math.min(_maxWidth, childMaxWidth)
      if (childMaxHeight > 0) _maxHeight = if (_maxHeight == 0) childMaxHeight else Math.min(_maxHeight, childMaxHeight)
      i += 1
    }
  }

  def add(actor: Actor): Unit = addActor(actor)

  override def layout(): Unit = {
    if (sizeInvalid) computeSize()
    val w = this.width
    val h = this.height
    icon.setBounds(0, 0, w, h)
    icon.validate()
    val checkStyle  = checkBox.style.asInstanceOf[VisCheckBox.VisCheckBoxStyle]
    val checkHeight = checkStyle.checkBackground.map(_.minHeight).getOrElse(0f)
    checkBox.setBounds(3, h - checkHeight - 3, checkBox.prefWidth, checkBox.prefHeight)
    checkBox.validate()
  }

  override def prefWidth: Float = {
    if (sizeInvalid) computeSize()
    _prefWidth
  }

  override def prefHeight: Float = {
    if (sizeInvalid) computeSize()
    _prefHeight
  }

  override def minWidth: Float = {
    if (sizeInvalid) computeSize()
    _minWidth
  }

  override def minHeight: Float = {
    if (sizeInvalid) computeSize()
    _minHeight
  }

  override def maxWidth: Float = {
    if (sizeInvalid) computeSize()
    _maxWidth
  }

  override def maxHeight: Float = {
    if (sizeInvalid) computeSize()
    _maxHeight
  }
}
