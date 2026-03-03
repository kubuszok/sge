/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/TextMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (16 pairs → vars)
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Rectangle

/** @brief Represents a text map object */
class TextMapObject(x: Float, y: Float, width: Float, height: Float, text: String) extends MapObject {

  // defaults set based on tiled docs
  private var rotation:   Float   = 0.0f
  private var _text:      String  = text
  private var pixelSize:  Int     = 16
  private var fontFamily: String  = ""
  private var bold:       Boolean = false
  private var italic:     Boolean = false
  private var underline:  Boolean = false
  private var strikeout:  Boolean = false
  private var kerning:    Boolean = true
  private var wrap:       Boolean = true

  // possible values: "left", "center", "right", "justify" (default: "left")
  private var horizontalAlign: String = "left"
  // possible values: "top", "center", "bottom" (default: "top")
  private var verticalAlign: String = "top"

  // Rectangle shape representing the object's bounds
  private val rectangle: Rectangle = new Rectangle(x, y, width, height)

  /** Creates an empty text object with bounds starting in the lower left corner at (0, 0) with width=1 and height=1 */
  def this() = this(0.0f, 0.0f, 1.0f, 1.0f, "")

  /** @return rectangle representing object bounds */
  def getRectangle: Rectangle = rectangle

  /** @return object's X coordinate */
  def getX: Float = rectangle.getX()

  /** @return object's Y coordinate */
  def getY: Float = rectangle.getY()

  /** @return object's bounds height */
  def getWidth: Float = rectangle.getWidth()

  /** @return object's bounds height */
  def getHeight: Float = rectangle.getHeight()

  /** @return object's rotation */
  def getRotation: Float = rotation

  /** @param rotation new rotation value for the object */
  def setRotation(rotation: Float): Unit =
    this.rotation = rotation

  /** @return object's text */
  def getText: String = _text

  /** @param text new text to display */
  def setText(text: String): Unit =
    this._text = text

  /** @return A String describing object's horizontal alignment */
  def getHorizontalAlign: String = horizontalAlign

  /** @param horizontalAlign the horizontal alignment string from Tiled */
  def setHorizontalAlign(horizontalAlign: String): Unit =
    this.horizontalAlign = horizontalAlign

  /** @return String describing object's vertical alignment */
  def getVerticalAlign: String = verticalAlign

  /** @param verticalAlign the vertical alignment string from Tiled */
  def setVerticalAlign(verticalAlign: String): Unit =
    this.verticalAlign = verticalAlign

  /** @return font pixel size */
  def getPixelSize: Int = pixelSize

  /** @param pixelSize the pixel size for the font */
  def setPixelSize(pixelSize: Int): Unit =
    this.pixelSize = pixelSize

  /** @return font family */
  def getFontFamily: String = fontFamily

  /** @param fontFamily new font family */
  def setFontFamily(fontFamily: String): Unit =
    this.fontFamily = fontFamily

  /** @return true if the font is bold */
  def isBold: Boolean = bold

  /** @param bold set font to bold or not */
  def setBold(bold: Boolean): Unit =
    this.bold = bold

  /** @return true if the font is italic */
  def isItalic: Boolean = italic

  /** @param italic set font to italic or not */
  def setItalic(italic: Boolean): Unit =
    this.italic = italic

  /** @return true if the font is underlined */
  def isUnderline: Boolean = underline

  /** @param underline set font to underline or not */
  def setUnderline(underline: Boolean): Unit =
    this.underline = underline

  /** @return true if the font is strikeout */
  def isStrikeout: Boolean = strikeout

  /** @param strikeout set font to strikeout or not */
  def setStrikeout(strikeout: Boolean): Unit =
    this.strikeout = strikeout

  /** @return true if kerning is enabled */
  def isKerning: Boolean = kerning

  /** @param kerning enable or disable kerning */
  def setKerning(kerning: Boolean): Unit =
    this.kerning = kerning

  /** @return true if text wrapping is enabled */
  def isWrap: Boolean = wrap

  /** @param wrap enable or disable text wrapping */
  def setWrap(wrap: Boolean): Unit =
    this.wrap = wrap
}
