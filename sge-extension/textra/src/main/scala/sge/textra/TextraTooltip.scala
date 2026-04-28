/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraTooltip.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Tooltip<TextraLabel> → standalone class (scene2d base not inherited;
 *     tooltip positioning handled by scene2d Tooltip when integrated),
 *     Container → deferred, TooltipManager → deferred, Skin → removed
 *   Convention: Tooltip label and style management preserved in API.
 *   Idiom: Nullable[A] for nullable fields.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 128
 * Covenant-baseline-methods: ContainerProxy,TextraTooltip,_label,background,getActor,getContainer,l,ls,maxWidth,newLabel,setBackground,setStyle,skipToTheEnd,this,width,wrapWidth
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraTooltip.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A tooltip that shows a TextraLabel. */
class TextraTooltip(
  text:            Nullable[String],
  style:           Styles.TextTooltipStyle,
  replacementFont: Font
) {

  private val _label: TextraLabel = {
    val ls = Nullable.fold(style.label)(new Styles.LabelStyle())(identity)
    val l  = newLabel(Nullable.fold(text)("")(identity), ls, replacementFont)
    l.setAlignment(sge.utils.Align.center)
    l.setWrap(true)
    l
  }

  /** The background drawable for this tooltip, from the style. */
  var background: Nullable[AnyRef] = style.background

  /** The wrap width from the style, controlling the container width. */
  var wrapWidth: Float = style.wrapWidth

  def this(text: Nullable[String], style: Styles.TextTooltipStyle) =
    this(
      text,
      style,
      Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity))
    )

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, style: Styles.LabelStyle, font: Font): TextraLabel =
    new TextraLabel(text, style, font)

  protected def newLabel(text: String, font: Font): TextraLabel =
    new TextraLabel(text, font)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  def getActor: TextraLabel = _label

  /** Returns this tooltip's container-like state holder. In the original TextraTypist, this returns the Container from the Tooltip superclass. Since TextraTooltip is standalone in SGE, this returns a
    * lightweight wrapper providing the same fluent API for background and width configuration.
    */
  def getContainer: TextraTooltip.ContainerProxy = new TextraTooltip.ContainerProxy(this)

  def setStyle(style: Styles.TextTooltipStyle): Unit = {
    Nullable.foreach(style.label) { ls =>
      // We don't want to regenerate the layout yet, so the last parameter is false.
      Nullable.foreach(ls.font)(f => _label.setFont(f, false))
      Nullable.foreach(ls.fontColor)(c => _label.setColor(c))
    }
    // Then we can regenerate the layout.
    _label.getFont.regenerateLayout(_label.layout)
    _label.setSize(_label.layout.getWidth, _label.layout.getHeight)
    this.background = style.background
    this.wrapWidth = style.wrapWidth
  }

  def setStyle(style: Styles.TextTooltipStyle, font: Font): Unit = {
    _label.setFont(font, false)
    _label.layout.setTargetWidth(style.wrapWidth)
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.fontColor)(c => _label.setColor(c))
    }
    font.regenerateLayout(_label.layout)
    _label.setSize(_label.layout.getWidth, _label.layout.getHeight)
    this.background = style.background
    this.wrapWidth = style.wrapWidth
  }

  /** Does nothing unless the label used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    _label.skipToTheEnd()
}

object TextraTooltip {

  /** A lightweight wrapper that mimics Container's fluent API for setting background and width on the tooltip. */
  final class ContainerProxy(private val tooltip: TextraTooltip) {

    /** Sets the background drawable for this container (tooltip). Returns this for chaining. */
    def background(bg: Nullable[AnyRef]): ContainerProxy = {
      tooltip.background = bg
      this
    }

    /** Sets the container width (wrapWidth). Returns this for chaining. */
    def width(w: Float): ContainerProxy = {
      tooltip.wrapWidth = w
      this
    }

    /** Returns the actor (TextraLabel) in this container. */
    def getActor: TextraLabel = tooltip.getActor

    /** Sets the background drawable. */
    def setBackground(bg: Nullable[AnyRef]): Unit =
      tooltip.background = bg

    /** Sets the maximum width. Returns this for chaining. */
    def maxWidth(w: Float): ContainerProxy = {
      tooltip.wrapWidth = w
      this
    }
  }
}
