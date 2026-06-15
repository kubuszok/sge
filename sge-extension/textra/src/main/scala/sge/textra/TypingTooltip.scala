/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TypingTooltip.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Tooltip<TypingLabel> -> sge.scenes.scene2d.ui.Tooltip[TypingLabel]
 *     (which extends InputListener). Since Tooltip[T] is invariant, TypingTooltip
 *     extends Tooltip[TypingLabel] directly (mirroring upstream, which extends
 *     Tooltip<TypingLabel>, NOT TextraTooltip). The inherited Container[TypingLabel]
 *     holds the label and the inherited TooltipManager `manager` drives showing.
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
 * Covenant-baseline-loc: 182
 * Covenant-baseline-methods: TypingTooltip,container,enter,getActor,getContainer,getTypingLabel,ls,newLabel,setBackgroundOn,setContainerBackground,setStyle,this,wrap
 * Covenant-source-reference: com/github/tommyettinger/textra/TypingTooltip.java
 * Covenant-verified: 2026-06-15
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Color
import sge.scenes.scene2d.{ Actor, InputEvent }
import sge.scenes.scene2d.ui.{ Container, Skin, Tooltip, TooltipManager }
import sge.scenes.scene2d.utils.Drawable
import lowlevel.Nullable

/** A tooltip that shows a TypingLabel.
  *
  * @author
  *   Nathan Sweet
  */
class TypingTooltip(
  text:            Nullable[String],
  manager:         TooltipManager,
  style:           Styles.TextTooltipStyle,
  replacementFont: Font
)(using Sge)
    extends Tooltip[TypingLabel](Nullable.empty, manager) {

  // super(null, manager); setActor(newLabel(...)); ...; getActor().restart()
  // upstream Tooltip.setActor(T) delegates to container.setActor(T) (Tooltip.java:64-66)
  locally {
    val ls = Nullable.fold(style.label)(new Styles.LabelStyle())(identity)
    container.setActor(Nullable(newLabel(Nullable.fold(text)("")(identity), ls, replacementFont)))
    getActor.setAlignment(sge.utils.Align.center)
    getActor.setWrap(true)
    getContainer.width(style.wrapWidth)
    setContainerBackground(style.background)
    getActor.restart()
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

  /** Returns the contained TypingLabel (upstream getActor() == getContainer().getActor()). The actor is installed in the ctor and is never null. */
  def getActor: TypingLabel = getContainer.actor.get

  /** Returns the inherited Container[TypingLabel] (upstream getContainer()). */
  def getContainer: Container[TypingLabel] = container

  /** Returns the contained TypingLabel. */
  def getTypingLabel: TypingLabel = getActor

  protected def newLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  protected def newLabel(text: String, style: Styles.LabelStyle, font: Font): TypingLabel =
    new TypingLabel(text, style, font)

  protected def newLabel(text: String, font: Font): TypingLabel =
    new TypingLabel(text, font)

  protected def newLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)

  def setStyle(style: Styles.TextTooltipStyle): Unit = {
    require(style != null, "style cannot be null")
    require(Nullable.isDefined(style.label), "style.label cannot be null")
    Nullable.foreach(style.label) { ls =>
      require(Nullable.isDefined(ls.font), "style.label.font cannot be null")
    }
    setStyle(style, Nullable.fold(style.label)(new Font())(ls => Nullable.fold(ls.font)(new Font())(identity)))
  }

  def setStyle(style: Styles.TextTooltipStyle, font: Font): Unit = {
    require(style != null, "style cannot be null")
    val container = getContainer
    setBackgroundOn(container, style.background)
    container.maxWidth(style.wrapWidth)

    val wrap = style.wrapWidth != 0
    container.fill(wrap)

    getActor.setFont(font, false)
    getActor.baseLayout.setTargetWidth(style.wrapWidth)
    getActor.wrap = true
    Nullable.foreach(style.label) { ls =>
      Nullable.foreach(ls.fontColor)(c => getActor.setColor(c))
    }
    font.regenerateLayout(getActor.baseLayout)
    // font.calculateSize(container.getActor.baseLayout)
    getActor.setSize(getActor.baseLayout.getWidth, getActor.baseLayout.getHeight)
  }

  override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit = {
    super.enter(event, x, y, pointer, fromActor)
    getContainer.actor.foreach(_.restart())
    // System.out.println("TypingTooltip has size " + getActor.getWidth + "," + getActor.getHeight)
    // System.out.println("Container has size " + getContainer.getWidth + "," + getContainer.getHeight)
  }

  /** Bridges the textra Styles.TextTooltipStyle background (Nullable[AnyRef] holding a Drawable) to the inherited Container.background. */
  private def setContainerBackground(bg: Nullable[AnyRef]): Unit =
    setBackgroundOn(getContainer, bg)

  private def setBackgroundOn(container: Container[TypingLabel], bg: Nullable[AnyRef]): Unit =
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
