/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/WidgetGroup.java
 * Original authors: Nathan Sweet
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
import sge.utils.Nullable

/** A {@link Group} that participates in layout and provides a minimum, preferred, and maximum size.
  *
  * The default preferred size of a widget group is 0 and this is almost always overridden by a subclass. The default minimum size returns the preferred size, so a subclass may choose to return 0 for
  * minimum size if it wants to allow itself to be sized smaller than the preferred size. The default maximum size is 0, which means no maximum size.
  *
  * See {@link Layout} for details on how a widget group should participate in layout. A widget group's mutator methods should call {@link #invalidate()} or {@link #invalidateHierarchy()} as needed.
  * By default, invalidateHierarchy is called when child widgets are added and removed.
  * @author
  *   Nathan Sweet
  */
class WidgetGroup()(using Sge) extends Group() with Layout {

  private var _needsLayout:  Boolean = true
  private var fillParent:    Boolean = false
  private var layoutEnabled: Boolean = true

  /** Creates a new widget group containing the specified actors. */
  def this(actors: Actor*)(using Sge) = {
    this()
    actors.foreach(addActor)
  }

  def getMinWidth: Float = getPrefWidth

  def getMinHeight: Float = getPrefHeight

  def getPrefWidth: Float = 0

  def getPrefHeight: Float = 0

  def getMaxWidth: Float = 0

  def getMaxHeight: Float = 0

  def setLayoutEnabled(enabled: Boolean): Unit = {
    layoutEnabled = enabled
    setLayoutEnabled(this, enabled)
  }

  private def setLayoutEnabled(parent: Group, enabled: Boolean): Unit = {
    val children = parent.getChildren
    var i        = 0
    while (i < children.size) {
      children(i) match {
        case l: Layout => l.setLayoutEnabled(enabled)
        case g: Group  => setLayoutEnabled(g, enabled) //
        case _ =>
      }
      i += 1
    }
  }

  def validate(): Unit =
    if (layoutEnabled) {
      getParent.foreach { parent =>
        if (fillParent) {
          getStage.fold(setSize(parent.getWidth, parent.getHeight)) { stage =>
            if (parent eq stage.getRoot) setSize(stage.getWidth, stage.getHeight)
            else setSize(parent.getWidth, parent.getHeight)
          }
        }
      }

      if (_needsLayout) {
        _needsLayout = false
        layout()

        // Widgets may call invalidateHierarchy during layout (eg, a wrapped label). The root-most widget group retries layout a
        // reasonable number of times.
        if (_needsLayout) {
          if (!getParent.exists(_.isInstanceOf[WidgetGroup])) { // The parent widget will layout again.
            var i = 0
            scala.util.boundary {
              while (i < 5) {
                _needsLayout = false
                layout()
                if (!_needsLayout) scala.util.boundary.break()
                i += 1
              }
            }
          }
        }
      }
    }

  /** Returns true if the widget's layout has been {@link #invalidate() invalidated}. */
  def needsLayout: Boolean = _needsLayout

  def invalidate(): Unit =
    _needsLayout = true

  def invalidateHierarchy(): Unit = {
    invalidate()
    getParent.foreach {
      case l: Layout => l.invalidateHierarchy()
      case _ =>
    }
  }

  override protected def childrenChanged(): Unit =
    invalidateHierarchy()

  override protected def sizeChanged(): Unit =
    invalidate()

  def pack(): Unit = {
    setSize(getPrefWidth, getPrefHeight)
    validate()
    // Validating the layout may change the pref size. Eg, a wrapped label doesn't know its pref height until it knows its
    // width, so it calls invalidateHierarchy() in layout() if its pref height has changed.
    setSize(getPrefWidth, getPrefHeight)
    validate()
  }

  def setFillParent(fillParent: Boolean): Unit =
    this.fillParent = fillParent

  def layout(): Unit = {}

  /** If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget group is laid out.
    */
  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = {
    validate()
    super.hit(x, y, touchable)
  }

  /** If this method is overridden, the super method or {@link #validate()} should be called to ensure the widget group is laid out.
    */
  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()
    super.draw(batch, parentAlpha)
  }
}
