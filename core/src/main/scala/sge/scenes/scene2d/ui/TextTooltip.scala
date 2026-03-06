/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/TextTooltip.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; (using Sge) context; Skin constructors present
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.scenes.scene2d.ui.Label.LabelStyle
import sge.scenes.scene2d.utils.Drawable
import sge.utils.Nullable

/** A tooltip that shows a label.
  * @author
  *   Nathan Sweet
  */
class TextTooltip(text: Nullable[String], manager: TooltipManager, style: TextTooltip.TextTooltipStyle)(using Sge)
    extends Tooltip[Label](Nullable.empty, manager)
    with Styleable[TextTooltip.TextTooltipStyle] {

  container.setActor(Nullable(newLabel(text, style.label)))

  setStyle(style)

  def this(text: Nullable[String], skin: Skin)(using Sge) = {
    this(text, TooltipManager.getInstance(), skin.get(classOf[TextTooltip.TextTooltipStyle]))
  }

  def this(text: Nullable[String], skin: Skin, styleName: String)(using Sge) = {
    this(text, TooltipManager.getInstance(), skin.get(styleName, classOf[TextTooltip.TextTooltipStyle]))
  }

  def this(text: Nullable[String], style: TextTooltip.TextTooltipStyle)(using Sge) = {
    this(text, TooltipManager.getInstance(), style)
  }

  def this(text: Nullable[String], manager: TooltipManager, skin: Skin)(using Sge) = {
    this(text, manager, skin.get(classOf[TextTooltip.TextTooltipStyle]))
  }

  def this(text: Nullable[String], manager: TooltipManager, skin: Skin, styleName: String)(using Sge) = {
    this(text, manager, skin.get(styleName, classOf[TextTooltip.TextTooltipStyle]))
  }

  protected def newLabel(text: Nullable[String], style: LabelStyle): Label =
    Label(text.map(t => t: CharSequence), style)

  private var _style: TextTooltip.TextTooltipStyle = scala.compiletime.uninitialized

  def setStyle(style: TextTooltip.TextTooltipStyle): Unit = {
    this._style = style
    container.setBackground(style.background)
    container.maxWidth(style.wrapWidth)

    val wrap = style.wrapWidth != 0
    container.fill(wrap)

    val label = container.getActor
    label.foreach { l =>
      l.setStyle(style.label)
      l.setWrap(wrap)
    }
  }

  def getStyle: TextTooltip.TextTooltipStyle = _style
}

object TextTooltip {

  /** The style for a text tooltip, see {@link TextTooltip}.
    * @author
    *   Nathan Sweet
    */
  class TextTooltipStyle() {
    var label: LabelStyle = scala.compiletime.uninitialized

    /** 0 means don't wrap. */
    var wrapWidth:  Float              = 0
    var background: Nullable[Drawable] = Nullable.empty

    def this(label: LabelStyle, background: Nullable[Drawable]) = {
      this()
      this.label = label
      this.background = background
    }

    def this(style: TextTooltipStyle) = {
      this()
      label = LabelStyle(style.label)
      background = style.background
      wrapWidth = style.wrapWidth
    }
  }
}
