/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Line.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: Line,appendTo,glyphs,height,i,reset,size,toString,width
 * Covenant-source-reference: com/github/tommyettinger/textra/Line.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

/** One line of possibly-colorful, possibly-styled text, with a width and height set by Font on Lines in a Layout. This stores each (colorful, styled) char as a `long` in an ArrayBuffer.
  */
class Line(capacity: Int = 16) {

  val glyphs: ArrayBuffer[Long] = new ArrayBuffer[Long](capacity)
  var width:  Float             = 0f
  var height: Float             = 0f

  def size(width: Float, height: Float): Line = {
    this.width = width
    this.height = height
    this
  }

  /** Resets the object for reuse. This clears glyphs. The sizes are set to 0. */
  def reset(): Unit = {
    glyphs.clear()
    width = 0
    height = 0
  }

  def appendTo(sb: StringBuilder): StringBuilder = {
    sb.append("(\"")
    var i = 0
    while (i < glyphs.size) {
      sb.append(glyphs(i).toChar)
      i += 1
    }
    sb.append("\" w=").append(width).append(" h=").append(height).append(')')
    sb
  }

  override def toString: String = appendTo(new StringBuilder(glyphs.size + 20)).toString
}
