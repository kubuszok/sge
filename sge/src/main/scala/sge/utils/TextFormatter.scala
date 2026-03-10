/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TextFormatter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `useMessageFormat` -> `useAdvanced`
 *   Idiom: split packages
 *   Convention: `MessageFormat` not available cross-platform (JS/Native); both modes use `simpleFormat`
 *     (matches GWT behavior where simpleFormatter is always assumed true)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.util.Locale

/** TextFormatter is used by I18NBundle to perform argument replacement.
  *
  * Supports placeholder syntax `{0}`, `{1}`, etc. Doubled left curly brackets `{{` are escaped to a literal `{`. Single quotes do not need escaping (unlike java.text.MessageFormat).
  */
class TextFormatter(locale: Locale, useAdvanced: Boolean) {
  private val buffer = new StringBuilder()

  /** Formats the given pattern replacing its placeholders with the actual arguments specified by args.
    *
    * Placeholders are in the simplified form {0}, {1}, {2} and so on. They will be replaced with the corresponding object from args converted to a string with toString(), so without taking into
    * account the locale.
    *
    * The only escaping rule is: a left curly bracket must be doubled if you want it to be part of your string.
    *
    * @param pattern
    *   the pattern
    * @param args
    *   the arguments
    * @return
    *   the formatted pattern
    * @throws IllegalArgumentException
    *   if the pattern is invalid
    */
  def format(pattern: String, args: AnyRef*): String =
    simpleFormat(pattern, args)

  /** Formats the given pattern replacing any placeholder of the form {0}, {1}, {2} and so on with the corresponding object from args converted to a string with toString().
    *
    * This method only implements a small subset of the grammar supported by java.text.MessageFormat. Especially, placeholders are only made up of an index; neither the type nor the style are
    * supported.
    *
    * If nothing has been replaced this implementation returns the pattern itself.
    *
    * @param pattern
    *   the pattern
    * @param args
    *   the arguments
    * @return
    *   the formatted pattern
    * @throws IllegalArgumentException
    *   if the pattern is invalid
    */
  private def simpleFormat(pattern: String, args: Seq[AnyRef]): String = {
    buffer.setLength(0)
    var changed       = false
    var placeholder   = -1
    val patternLength = pattern.length
    var i             = 0
    while (i < patternLength) {
      val ch = pattern.charAt(i)
      if (placeholder < 0) {
        // processing constant part
        if (ch == '{') {
          changed = true
          if (i + 1 < patternLength && pattern.charAt(i + 1) == '{') {
            buffer.append(ch) // handle escaped '{'
            i += 1
          } else {
            placeholder = 0 // switch to placeholder part
          }
        } else {
          buffer.append(ch)
        }
      } else {
        // processing placeholder part
        if (ch == '}') {
          if (placeholder >= args.length) {
            throw IllegalArgumentException("Argument index out of bounds: " + placeholder)
          }
          if (pattern.charAt(i - 1) == '{') {
            throw IllegalArgumentException("Missing argument index after a left curly brace")
          }
          val arg = args(placeholder)
          if (arg == null) { // @nowarn — Java interop: args may contain null from Java callers
            buffer.append("null")
          } else {
            buffer.append(arg.toString)
          }
          placeholder = -1 // switch to constant part
        } else {
          if (ch < '0' || ch > '9') {
            throw IllegalArgumentException("Unexpected '" + ch + "' while parsing argument index")
          }
          placeholder = placeholder * 10 + (ch - '0')
        }
      }
      i += 1
    }
    if (placeholder >= 0) {
      throw IllegalArgumentException("Unmatched braces in the pattern.")
    }

    if (changed) buffer.toString else pattern
  }
}
