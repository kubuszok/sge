/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/TextMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getRectangle() -> val rectangle; 16 getter/setter pairs -> public vars
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.math.Rectangle

/** @brief Represents a text map object */
class TextMapObject(x: Float = 0.0f, y: Float = 0.0f, width: Float = 1.0f, height: Float = 1.0f, initialText: String = "") extends MapObject {

  // defaults set based on tiled docs
  var rotation:   Float   = 0.0f
  var text:       String  = initialText
  var pixelSize:  Int     = 16
  var fontFamily: String  = ""
  var bold:       Boolean = false
  var italic:     Boolean = false
  var underline:  Boolean = false
  var strikeout:  Boolean = false
  var kerning:    Boolean = true
  var wrap:       Boolean = true

  // possible values: "left", "center", "right", "justify" (default: "left")
  var horizontalAlign: String = "left"
  // possible values: "top", "center", "bottom" (default: "top")
  var verticalAlign: String = "top"

  // Rectangle shape representing the object's bounds
  val rectangle: Rectangle = Rectangle(x, y, width, height)
}
