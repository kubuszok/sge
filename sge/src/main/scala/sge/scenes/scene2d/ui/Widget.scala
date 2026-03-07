/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Widget.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.Layout

/** An {@link Actor} that participates in layout and provides a minimum, preferred, and maximum size.
  *
  * The default preferred size of a widget is 0 and this is almost always overridden by a subclass. The default minimum size returns the preferred size, so a subclass may choose to return 0 if it
  * wants to allow itself to be sized smaller. The default maximum size is 0, which means no maximum size.
  *
  * See {@link Layout} for details on how a widget should participate in layout. A widget's mutator methods should call {@link #invalidate()} or {@link #invalidateHierarchy()} as needed.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class Widget()(using Sge) extends Actor() with Layout {

  private var _needsLayout:  Boolean = true
  private var fillParent:    Boolean = false
  private var layoutEnabled: Boolean = true

  def getMinWidth: Float = getPrefWidth

  def getMinHeight: Float = getPrefHeight

  def getPrefWidth: Float = 0

  def getPrefHeight: Float = 0

  def getMaxWidth: Float = 0

  def getMaxHeight: Float = 0

  def setLayoutEnabled(enabled: Boolean): Unit = {
    layoutEnabled = enabled
    if (enabled) invalidateHierarchy()
  }

  def validate(): Unit =
    if (layoutEnabled) {
      getParent.foreach { parent =>
        if (fillParent) {
          val (parentWidth, parentHeight) = stage.fold((parent.width, parent.height)) { stage =>
            if (parent eq stage.getRoot) (stage.getWidth, stage.getHeight)
            else (parent.width, parent.height)
          }
          setSize(parentWidth, parentHeight)
        }
      }

      if (_needsLayout) {
        _needsLayout = false
        layout()
      }
    }

  /** Returns true if the widget's layout has been {@link #invalidate() invalidated}. */
  def needsLayout: Boolean = _needsLayout

  def invalidate(): Unit =
    _needsLayout = true

  def invalidateHierarchy(): Unit =
    if (layoutEnabled) {
      invalidate()
      getParent.foreach {
        case l: Layout => l.invalidateHierarchy()
        case _ =>
      }
    }

  override protected def sizeChanged(): Unit =
    invalidate()

  def pack(): Unit = {
    setSize(getPrefWidth, getPrefHeight)
    validate()
  }

  def setFillParent(fillParent: Boolean): Unit =
    this.fillParent = fillParent

  /** If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget is laid out. */
  override def draw(batch: Batch, parentAlpha: Float): Unit =
    validate()

  def layout(): Unit = {}
}
