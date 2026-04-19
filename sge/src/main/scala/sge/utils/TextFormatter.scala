/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TextFormatter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `useMessageFormat` -> `useAdvanced`
 *   Idiom: split packages
 *   Convention: `java.text.MessageFormat` is used via reflection when `useAdvanced=true`
 *     and the class is available (JVM). On JS/Native, where `MessageFormat` is not present,
 *     falls back to `simpleFormat` (matches GWT emulation behavior).
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import sge.utils.Nullable

import java.util.Locale

/** TextFormatter is used by I18NBundle to perform argument replacement.
  *
  * Supports placeholder syntax `{0}`, `{1}`, etc. Doubled left curly brackets `{{` are escaped to a literal `{`. Single quotes do not need escaping (unlike java.text.MessageFormat).
  */
final class TextFormatter(locale: Locale, useAdvanced: Boolean) {
  private val buffer = new StringBuilder()

  // On JVM, java.text.MessageFormat is available and we use it for locale-aware formatting.
  // On Scala.js/Native, where MessageFormat is not present, falls back to simpleFormat
  // (matches GWT emulation behavior). Platform-specific implementation provided by
  // TextFormatterPlatform.createMessageFormat.
  private val messageFormat: Nullable[TextFormatterPlatform.AdvancedFormatter] =
    if (useAdvanced) TextFormatterPlatform.createAdvancedFormatter(locale)
    else Nullable.empty

  /** Formats the given pattern replacing its placeholders with the actual arguments specified by args.
    *
    * If this TextFormatter has been instantiated with `TextFormatter(locale, true)` java.text.MessageFormat is used to process the pattern, meaning that the actual arguments are properly localized
    * with the locale of this TextFormatter.
    *
    * On the contrary, if this TextFormatter has been instantiated with `TextFormatter(locale, false)` pattern's placeholders are expected to be in the simplified form {0}, {1}, {2} and so on and they
    * will be replaced with the corresponding object from args converted to a string with toString(), so without taking into account the locale.
    *
    * In both cases, there's only one simple escaping rule, i.e. a left curly bracket must be doubled if you want it to be part of your string.
    *
    * It's worth noting that the rules for using single quotes within java.text.MessageFormat patterns have shown to be somewhat confusing. In particular, it isn't always obvious to localizers whether
    * single quotes need to be doubled or not. For this very reason we decided to offer the simpler escaping rule above without limiting the expressive power of message format patterns. So, if you're
    * used to MessageFormat's syntax, remember that with TextFormatter single quotes never need to be escaped!
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
    messageFormat.fold(simpleFormat(pattern, args)) { mf =>
      // Use java.text.MessageFormat for locale-aware formatting.
      // replaceEscapeChars preprocesses the pattern: doubles single quotes (MessageFormat escape char)
      // and converts paired {{ to MessageFormat-escaped literal braces.
      try {
        val preprocessed = replaceEscapeChars(pattern)
        mf.format(preprocessed, args)
      } catch {
        case _: Exception => simpleFormat(pattern, args)
      }
    }

  // This code is needed because a simple replacement like
  // pattern.replace("'", "''").replace("{{", "'{'")
  // can't properly manage some special cases.
  // For example, the expected output for {{{{ is {{ but you get {'{ instead.
  // Also this code is optimized since a new string is returned only if something has been replaced.
  private def replaceEscapeChars(pattern: String): String = {
    buffer.setLength(0)
    var changed = false
    val len     = pattern.length
    var i       = 0
    while (i < len) {
      val ch = pattern.charAt(i)
      if (ch == '\'') {
        changed = true
        buffer.append("''")
      } else if (ch == '{') {
        var j = i + 1
        while (j < len && pattern.charAt(j) == '{')
          j += 1
        val escaped = (j - i) / 2
        if (escaped > 0) {
          changed = true
          buffer.append('\'')
          var k = escaped
          while (k > 0) {
            buffer.append('{')
            k -= 1
          }
          buffer.append('\'')
        }
        if ((j - i) % 2 != 0) {
          buffer.append('{')
        }
        i = j - 1
      } else {
        buffer.append(ch)
      }
      i += 1
    }
    if (changed) buffer.toString else pattern
  }

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
