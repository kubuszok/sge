/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingTooltip.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Tooltip<TypingLabel> -> standalone class (scene2d base not inherited;
 *     tooltip positioning handled by scene2d Tooltip when integrated),
 *     Container -> deferred, TooltipManager -> deferred, Skin -> removed
 *   Convention: TypingTooltip extends TextraTooltip, overrides newLabel methods to
 *     produce TypingLabel. The enter method restarts the typing animation.
 *   Idiom: Nullable[A] for nullable fields.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 91
 * Covenant-baseline-methods: TypingTooltip,container,enter,getTypingLabel,newLabel,setStyle,this,wrap
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingTooltip.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A tooltip that shows a TypingLabel.
  *
  * @author
  *   Nathan Sweet
  */
class TypingTooltip(
  text:            Nullable[String],
  style:           Styles.TextTooltipStyle,
  replacementFont: Font
) extends TextraTooltip(text, style, replacementFont) {

  // Restart the typing animation after construction (matches original constructor behavior)
  getTypingLabel.restart()

  def this(text: Nullable[String], style: Styles.TextTooltipStyle) =
    this(
      text,
      style,
      Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity))
    )

  /** Returns the actor cast to TypingLabel. The label is always a TypingLabel because newLabel is overridden. */
  def getTypingLabel: TypingLabel = getActor.asInstanceOf[TypingLabel]

  override protected def newLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  override protected def newLabel(text: String, style: Styles.LabelStyle, font: Font): TypingLabel =
    new TypingLabel(text, style, font)

  override protected def newLabel(text: String, font: Font): TypingLabel =
    new TypingLabel(text, font)

  override protected def newLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)

  override def setStyle(style: Styles.TextTooltipStyle): Unit = {
    require(style != null, "style cannot be null")
    require(Nullable.isDefined(style.label), "style.label cannot be null")
    Nullable.foreach(style.label) { ls =>
      require(Nullable.isDefined(ls.font), "style.label.font cannot be null")
    }
    setStyle(style, Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity)))
  }

  override def setStyle(style: Styles.TextTooltipStyle, font: Font): Unit = {
    require(style != null, "style cannot be null")
    val container = getContainer
    container.setBackground(style.background)
    container.maxWidth(style.wrapWidth)

    @scala.annotation.unused
    val wrap = style.wrapWidth != 0
    // container.fill(wrap) -- not available in standalone ContainerProxy

    getActor.setFont(font, false)
    getActor.layout.setTargetWidth(style.wrapWidth)
    getActor.wrap = true
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.fontColor)(c => getActor.setColor(c))
    }
    font.regenerateLayout(getActor.layout)
//    font.calculateSize(container.getActor.layout)
    getActor.setSize(getActor.layout.getWidth, getActor.layout.getHeight)
  }

  /** Restarts the typing animation when the tooltip is entered. */
  def enter(): Unit =
    getTypingLabel.restart()
}
