/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraWindow.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Table → standalone class (scene2d base deferred),
 *     Skin → removed, InputListener → deferred, Vector2 → deferred,
 *     Batch → deferred, Camera/OrthographicCamera → deferred
 *   Convention: Window dragging/resizing/modal behavior preserved in API,
 *     but actual scene2d integration deferred.
 */
package sge
package textra

import sge.graphics.Color
import sge.utils.Nullable

/** A table that can be dragged and act as a modal window. The top padding is used as the window's title height. */
class TextraWindow(title: String, style: Styles.WindowStyle, replacementFont: Font, scaleTitleFont: Boolean) {

  require(title != null, "title cannot be null.")
  require(replacementFont != null, "replacementFont cannot be null.")

  private var _style:     Styles.WindowStyle = style
  var isMovable:          Boolean            = true
  private var isModal:    Boolean            = false
  var isResizable:        Boolean            = false
  var resizeBorder:       Int                = 8
  var keepWithinStage:    Boolean            = true
  var titleLabel:         TextraLabel        = newLabel(title, replacementFont, Nullable.fold(style.titleFontColor)(null: Color)(identity))
  var drawTitleTable:     Boolean            = false
  protected var edge:     Int                = 0
  protected var dragging: Boolean            = false
  protected var font:     Font               = replacementFont

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), false)

  def this(title: String, style: Styles.WindowStyle, scaleTitleFont: Boolean) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), scaleTitleFont)

  def this(title: String, style: Styles.WindowStyle, replacementFont: Font) =
    this(title, style, replacementFont, false)

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  def setStyle(style: Styles.WindowStyle): Unit = {
    this._style = style
    titleLabel.setFont(this.font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
  }

  def setStyle(style: Styles.WindowStyle, font: Font): Unit = {
    this._style = style
    this.font = font
    titleLabel.setFont(font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
  }

  def getStyle: Styles.WindowStyle = _style

  def setModal(modal: Boolean): Unit    = isModal = modal
  def getModal:                 Boolean = isModal

  def getTitleLabel: TextraLabel = titleLabel

  /** Does nothing unless the titleLabel used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    titleLabel.skipToTheEnd()

  def isDragging: Boolean = dragging
}
