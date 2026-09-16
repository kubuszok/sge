/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/ImageTextraButton.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: ImageTextraButton extends sge.scenes.scene2d.ui.Button
 *     (Button extends Table extends WidgetGroup extends Actor) — faithful to
 *     upstream ImageTextraButton.java:36 `extends Button`. The button manages
 *     its OWN scene2d Image (the icon) and a textra TextraLabel (the text) as
 *     cells in its Table; it does NOT inherit ImageTextButton's image/label.
 *     Button state (isChecked/isPressed/isOver/disabled/hasKeyboardFocus/
 *     toggle/ButtonGroup membership) and the Table/Cell backing are inherited.
 *     The textra Styles.ImageTextButtonStyle is flattened (NOT a scene2d
 *     Button.ButtonStyle, and its image drawables are Nullable[AnyRef]), so the
 *     inherited Button carries an empty scene2d Button.ButtonStyle for its own
 *     background/offset machinery while the textra style drives the image
 *     drawable and the label font/color. The image drawables are bridged from
 *     Nullable[AnyRef] to the scene2d Image's Nullable[Drawable] the same way
 *     the rest of the textra package does (pattern-match `case d: Drawable`).
 *   Convention: Image text button state and rendering preserved in API.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 270
 * Covenant-baseline-methods: ImageTextraButton,_style,asDrawable,c,draw,focused,getFontColor,getImage,getImageCell,getImageDrawable,getLabel,getLabelCell,getStyle,getText,image,isDisabled,isDisabled_,label,newImage,newLabel,setLabel,setStyle,setText,skipToTheEnd,this,toString,updateImage
 * Covenant-source-reference: com/github/tommyettinger/textra/ImageTextraButton.java
 * Covenant-verified: 2026-06-15
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.graphics.g2d.Batch
import lowlevel.Nullable
import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.{ Button, Cell, Image, Skin }
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Scaling }

/** A button with a child {@link Image} and {@link TextraLabel}.
  * @author
  *   Nathan Sweet
  * @see
  *   ImageButton
  * @see
  *   TextraButton
  * @see
  *   Button
  */
class ImageTextraButton(
  text:            Nullable[String],
  style:           Styles.ImageTextButtonStyle,
  replacementFont: Font
)(using Sge)
    extends Button() {

  private val image: Image = newImage()

  private var label: TextraLabel = newLabel(
    Nullable.fold(text)("")(identity),
    replacementFont,
    Nullable.fold(style.fontColor)(Color.WHITE)(identity)
  )
  label.setAlignment(Align.center)

  private var _style: Styles.ImageTextButtonStyle = style

  // Upstream `super(style); defaults().space(3); image = newImage();
  // label = newLabel(...); add(image); add(label); setStyle(style, font);
  // setSize(getPrefWidth(), getPrefHeight())` (ImageTextraButton.java:65-82).
  // The inherited Button carries an empty scene2d ButtonStyle for its own
  // background/offset machinery; the textra Styles.ImageTextButtonStyle drives
  // the image drawable and the label font/color. The image and the textra
  // TextraLabel are added to the Table backing this Button and so get real
  // Cells (ImageTextraButton.java:76-77 `add(image); add(label);`).
  defaults().space(3)
  add(Nullable[Actor](image))
  add(Nullable[Actor](label))
  setStyle(style, replacementFont)
  setSize(prefWidth, prefHeight)

  def this(text: Nullable[String], style: Styles.ImageTextButtonStyle)(using Sge) =
    this(text, style, Nullable.fold(style.font)(new Font())(f => new Font(f)))

  def this(text: Nullable[String], skin: Skin)(using Sge) = {
    this(text, skin.get[Styles.ImageTextButtonStyle])
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], skin: Skin, styleName: String)(using Sge) = {
    this(text, skin.get[Styles.ImageTextButtonStyle](styleName))
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], skin: Skin, replacementFont: Font)(using Sge) = {
    this(text, skin.get[Styles.ImageTextButtonStyle], replacementFont)
    setSkin(Nullable(skin))
  }

  def this(text: Nullable[String], skin: Skin, styleName: String, replacementFont: Font)(using Sge) = {
    this(text, skin.get[Styles.ImageTextButtonStyle](styleName), replacementFont)
    setSkin(Nullable(skin))
  }

  protected def newImage(): Image =
    new Image(Nullable.empty, Scaling.fit)

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TextraLabel(text, font, color)

  def setStyle(style: Styles.ImageTextButtonStyle): Unit = {
    this._style = style
    // The inherited Button carries an empty scene2d ButtonStyle for its own
    // background/offset machinery; the textra style (flattened, not a scene2d
    // Button.ButtonStyle) drives only the image drawable and label font/color.
    super.setStyle(new Button.ButtonStyle())

    Nullable(image).foreach(_ => updateImage())

    Nullable(label).foreach { l =>
      Nullable.foreach(style.font)(f => l.setFont(new Font(f)))
      val c = getFontColor
      Nullable.foreach(c)(l.setColor)
    }
  }

  def setStyle(style: Styles.ImageTextButtonStyle, makeGridGlyphs: Boolean): Unit = {
    this._style = style
    super.setStyle(new Button.ButtonStyle())

    Nullable(image).foreach(_ => updateImage())

    Nullable(label).foreach { l =>
      Nullable.foreach(style.font)(f => l.setFont(f))
      val c = getFontColor
      Nullable.foreach(c)(l.setColor)
    }
  }

  def setStyle(style: Styles.ImageTextButtonStyle, font: Font): Unit = {
    this._style = style
    super.setStyle(new Button.ButtonStyle())

    Nullable(image).foreach(_ => updateImage())

    Nullable(label).foreach { l =>
      l.setFont(font)
      val c = getFontColor
      Nullable.foreach(c)(l.setColor)
    }
  }

  def getStyle: Styles.ImageTextButtonStyle = _style

  /** Returns the appropriate image drawable from the style based on the current button state. The textra style image fields are Nullable[AnyRef]; they are bridged to the scene2d Image's
    * Nullable[Drawable] the same way the rest of the textra package does (pattern-match `case d: Drawable`).
    */
  protected def getImageDrawable: Nullable[Drawable] = boundary {
    if (isDisabled && _style.imageDisabled.isDefined) break(asDrawable(_style.imageDisabled))
    if (isPressed) {
      if (isChecked && _style.imageCheckedDown.isDefined) break(asDrawable(_style.imageCheckedDown))
      if (_style.imageDown.isDefined) break(asDrawable(_style.imageDown))
    }
    if (isOver) {
      if (isChecked) {
        if (_style.imageCheckedOver.isDefined) break(asDrawable(_style.imageCheckedOver))
      } else {
        if (_style.imageOver.isDefined) break(asDrawable(_style.imageOver))
      }
    }
    if (isChecked) {
      if (_style.imageChecked.isDefined) break(asDrawable(_style.imageChecked))
      if (isOver && _style.imageOver.isDefined) break(asDrawable(_style.imageOver))
    }
    asDrawable(_style.imageUp)
  }

  /** Bridges a textra-style Nullable[AnyRef] image field to the scene2d Image's Nullable[Drawable], mirroring the rest of the textra package (e.g. TextraField.getBackgroundDrawable). */
  private def asDrawable(value: Nullable[AnyRef]): Nullable[Drawable] =
    Nullable.fold(value)(Nullable.empty[Drawable]) {
      case d: Drawable => Nullable(d)
      case _ => Nullable.empty
    }

  /** Sets the image drawable based on the current button state. The default implementation sets the image drawable using {@link #getImageDrawable()}. Guards on `image`/`_style`: the (text, style,
    * font) canonical ctor calls setStyle during super-construction, before this class's own fields finish initializing (they can still hold null then); mirroring upstream's `if (image != null)`
    * guards keeps that early call a no-op.
    */
  protected def updateImage(): Unit =
    Nullable(image).foreach(img => Nullable(_style).foreach(_ => img.drawable = getImageDrawable))

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor: Nullable[Color] = boundary {
    if (isDisabled && _style.disabledFontColor.isDefined) break(_style.disabledFontColor)
    if (isPressed) {
      if (isChecked && _style.checkedDownFontColor.isDefined) break(_style.checkedDownFontColor)
      if (_style.downFontColor.isDefined) break(_style.downFontColor)
    }
    if (isOver) {
      if (isChecked) {
        if (_style.checkedOverFontColor.isDefined) break(_style.checkedOverFontColor)
      } else {
        if (_style.overFontColor.isDefined) break(_style.overFontColor)
      }
    }
    val focused = hasKeyboardFocus
    if (isChecked) {
      if (focused && _style.checkedFocusedFontColor.isDefined) break(_style.checkedFocusedFontColor)
      if (_style.checkedFontColor.isDefined) break(_style.checkedFontColor)
      if (isOver && _style.overFontColor.isDefined) break(_style.overFontColor)
    }
    if (focused && _style.focusedFontColor.isDefined) break(_style.focusedFontColor)
    _style.fontColor
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    updateImage()
    val c = getFontColor
    Nullable.foreach(c)(label.setColor)
    super.draw(batch, parentAlpha)
  }

  def getImage: Image = image

  def getImageCell: Cell[Image] = getCell(image).get

  def setLabel(newLabel: TextraLabel): Unit = {
    getLabelCell.setActor(Nullable[Actor](newLabel))
    this.label = newLabel
  }

  def getLabel: TextraLabel = label

  def getLabelCell: Cell[TextraLabel] = getCell(label).get

  def setText(text: CharSequence): Unit =
    label.setText(text.toString)

  def getText: String = label.toString

  /** Does nothing unless the label used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    label.skipToTheEnd()

  /** Upstream reads `isDisabled()` (inherited from Button); SGE renamed it to the `disabled` property, exposed here under the Java name this button's own state-driven image/font logic is written
    * against. `isChecked`/`isPressed`/`isOver`/`hasKeyboardFocus`/`setChecked`/`toggle` come from the inherited Button.
    */
  def isDisabled:                   Boolean = disabled
  def isDisabled_=(value: Boolean): Unit    = disabled = value

  override def toString: String =
    name.getOrElse {
      val className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      val cn        = if (dotIndex != -1) className.substring(dotIndex + 1) else className
      (if (cn.indexOf('$') != -1) "ImageTextraButton " else "") + cn + ": " + image.drawable + " " + label.toString
    }
}
