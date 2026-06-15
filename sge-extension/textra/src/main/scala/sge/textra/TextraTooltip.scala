/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraTooltip.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Tooltip<TextraLabel> -> sge.scenes.scene2d.ui.Tooltip[TextraLabel]
 *     (which extends InputListener); the inherited Container[TextraLabel] holds
 *     the label and the inherited TooltipManager `manager` drives showing.
 *     getContainer() -> the inherited `container` val (SGE rename); getActor()
 *     unwraps container.actor. The 8 Skin-based ctors are ported via
 *     sge.scenes.scene2d.ui.Skin.get[Styles.TextTooltipStyle] /
 *     skin.get[Styles.TextTooltipStyle](styleName) (mirroring the sibling
 *     TextraButton/TextraWindow Skin ctors already in this package).
 *   Convention: the textra Styles.TextTooltipStyle background is Nullable[AnyRef]
 *     (a Drawable); it is bridged to the inherited Container.background(Drawable).
 *   Idiom: Nullable[A] for nullable fields.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 176
 * Covenant-baseline-methods: TextraTooltip,container,getActor,getContainer,ls,newLabel,setBackgroundOn,setContainerBackground,setStyle,skipToTheEnd,this
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraTooltip.java
 * Covenant-verified: 2026-06-15
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Color
import sge.scenes.scene2d.ui.{ Container, Skin, Tooltip, TooltipManager }
import sge.scenes.scene2d.utils.Drawable
import lowlevel.Nullable

/** A tooltip that shows a TextraLabel.
  *
  * @author
  *   Nathan Sweet
  */
class TextraTooltip(
  text:            Nullable[String],
  manager:         TooltipManager,
  style:           Styles.TextTooltipStyle,
  replacementFont: Font
)(using Sge)
    extends Tooltip[TextraLabel](Nullable.empty, manager) {

  // super(null, manager); setActor(newLabel(text, style.label, replacementFont))
  // upstream Tooltip.setActor(T) delegates to container.setActor(T) (Tooltip.java:64-66)
  locally {
    val ls = Nullable.fold(style.label)(new Styles.LabelStyle())(identity)
    container.setActor(Nullable(newLabel(Nullable.fold(text)("")(identity), ls, replacementFont)))
    getActor.setAlignment(sge.utils.Align.center)
    getActor.setWrap(true)
    getContainer.width(style.wrapWidth)
    setContainerBackground(style.background)
  }

  def this(text: Nullable[String], skin: Skin)(using Sge) =
    this(text, TooltipManager.instance, skin.get[Styles.TextTooltipStyle])

  def this(text: Nullable[String], skin: Skin, styleName: String)(using Sge) =
    this(text, TooltipManager.instance, skin.get[Styles.TextTooltipStyle](styleName))

  def this(text: Nullable[String], style: Styles.TextTooltipStyle)(using Sge) =
    this(
      text,
      TooltipManager.instance,
      style,
      Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity))
    )

  def this(text: Nullable[String], manager: TooltipManager, skin: Skin)(using Sge) =
    this(text, manager, skin.get[Styles.TextTooltipStyle])

  def this(text: Nullable[String], manager: TooltipManager, skin: Skin, styleName: String)(using Sge) =
    this(text, manager, skin.get[Styles.TextTooltipStyle](styleName))

  def this(text: Nullable[String], manager: TooltipManager, style: Styles.TextTooltipStyle)(using Sge) =
    this(
      text,
      manager,
      style,
      Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity))
    )

  def this(text: Nullable[String], skin: Skin, replacementFont: Font)(using Sge) =
    this(text, TooltipManager.instance, skin.get[Styles.TextTooltipStyle], replacementFont)

  def this(text: Nullable[String], skin: Skin, styleName: String, replacementFont: Font)(using Sge) =
    this(text, TooltipManager.instance, skin.get[Styles.TextTooltipStyle](styleName), replacementFont)

  def this(text: Nullable[String], style: Styles.TextTooltipStyle, replacementFont: Font)(using Sge) =
    this(text, TooltipManager.instance, style, replacementFont)

  def this(text: Nullable[String], manager: TooltipManager, skin: Skin, replacementFont: Font)(using Sge) =
    this(text, manager, skin.get[Styles.TextTooltipStyle], replacementFont)

  def this(text: Nullable[String], manager: TooltipManager, skin: Skin, styleName: String, replacementFont: Font)(using
    Sge
  ) =
    this(text, manager, skin.get[Styles.TextTooltipStyle](styleName), replacementFont)

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, style: Styles.LabelStyle, font: Font): TextraLabel =
    new TextraLabel(text, style, font)

  protected def newLabel(text: String, font: Font): TextraLabel =
    new TextraLabel(text, font)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  /** Returns the contained TextraLabel (upstream getActor() == getContainer().getActor()). The actor is installed in the ctor and is never null. */
  def getActor: TextraLabel = getContainer.actor.get

  /** Returns the inherited Container[TextraLabel] (upstream getContainer()). */
  def getContainer: Container[TextraLabel] = container

  def setStyle(style: Styles.TextTooltipStyle): Unit = {
    require(style != null, "style cannot be null")
    val container = getContainer
    // we don't want to regenerate the layout yet, so the last parameter is false.
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.font)(f => getActor.setFont(f, false))
      // we set the target width first.
      // getActor.baseLayout.targetWidth = style.wrapWidth
      Nullable.foreach(ls.fontColor)(c => getActor.setColor(c))
    }
    // and then we can regenerate the layout.
    getActor.getFont.regenerateLayout(getActor.baseLayout)
    // getActor.getFont.calculateSize(getActor.baseLayout)
    getActor.setSize(getActor.baseLayout.getWidth, getActor.baseLayout.getHeight)
    setBackgroundOn(container, style.background)
    container.width(style.wrapWidth)
  }

  def setStyle(style: Styles.TextTooltipStyle, font: Font): Unit = {
    require(style != null, "style cannot be null")
    val container = getContainer
    getActor.setFont(font, false)
    getActor.baseLayout.setTargetWidth(style.wrapWidth)
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.fontColor)(c => getActor.setColor(c))
    }
    font.regenerateLayout(getActor.baseLayout)
    // font.calculateSize(getActor.baseLayout)
    getActor.setSize(getActor.baseLayout.getWidth, getActor.baseLayout.getHeight)
    setBackgroundOn(container, style.background)
    container.maxWidth(style.wrapWidth)
  }

  /** Does nothing unless the label used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    getContainer.actor.foreach(_.skipToTheEnd())

  /** Bridges the textra Styles.TextTooltipStyle background (Nullable[AnyRef] holding a Drawable) to the inherited Container.background (upstream getContainer().background(style.background)). */
  private def setContainerBackground(bg: Nullable[AnyRef]): Unit =
    setBackgroundOn(getContainer, bg)

  private def setBackgroundOn(container: Container[TextraLabel], bg: Nullable[AnyRef]): Unit =
    Nullable.fold(bg) {
      container.background(Nullable.empty[Drawable])
      ()
    } {
      case drawable: Drawable =>
        container.background(Nullable(drawable))
        ()
      case _ => ()
    }
}
