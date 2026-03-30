/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraArea.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: TextraField → extends TextraField
 *   Convention: Multi-line text input; scene2d integration deferred.
 *   Note: This widget is noted as "not ready for production yet" in the original.
 */
package sge
package textra

import sge.utils.Nullable

/** A multiple-line TextraField using a Font; not ready for production yet.
  *
  * If you have to use Font but don't need multiple lines, TextraField should work. If you do need multiple-line input, you can use a libGDX BitmapFont with a scene2d.ui TextField. If you don't need
  * input, just selectable text, you can make a TypingLabel selectable with TypingLabel.setSelectable(boolean).
  */
class TextraArea(text: Nullable[String], style: Styles.TextFieldStyle) extends TextraField {

  writeEnters = true

  def this(text: Nullable[String], style: Styles.TextFieldStyle, replacementFont: Font) = {
    this(text, style)
    val rf = new Font(replacementFont)
    rf.enableSquareBrackets = false
    rf.omitCurlyBraces = false
    label = new TypingLabel("", new Styles.LabelStyle(rf, style.fontColor))
    setText(text)
  }

  {
    val s = new Styles.TextFieldStyle(style)
    Nullable.foreach(s.font) { f =>
      f.enableSquareBrackets = false
      f.omitCurlyBraces = false
    }
    this.style = Nullable(s)
    Nullable.foreach(s.font) { f =>
      label = new TypingLabel("", new Styles.LabelStyle(f, style.fontColor))
    }
    label.setWrap(true)
    setText(text)
  }
}
