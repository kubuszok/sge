package sge.utils

import java.util.Locale

final class TextFormatter(locale: Locale, useAdvanced: Boolean) {
  private val buffer = new StringBuilder()

  private val advancedFormatter: lowlevel.Nullable[TextFormatterPlatform.AdvancedFormatter] =
    if (useAdvanced) TextFormatterPlatform.createAdvancedFormatter(locale)
    else lowlevel.Nullable.empty

  def this(locale: Locale) = this(locale, false)

  def format(pattern: String, args: Array[java.lang.Object]): String = {
    val safeArgs = if (args == null) Array[java.lang.Object](null.asInstanceOf[java.lang.Object]) else args
    if (!advancedFormatter.isEmpty) {
      advancedFormatter.get.format(replaceEscapeChars(pattern), safeArgs.toSeq.asInstanceOf[Seq[AnyRef]])
    } else {
      simpleFormat(pattern, safeArgs)
    }
  }

  def format(pattern: String, args: java.lang.Object*): String =
    format(pattern, if (args == null) Array[java.lang.Object](null.asInstanceOf[java.lang.Object]) else args.toArray)

  private def replaceEscapeChars(pattern: String): String = {
    buffer.setLength(0)
    var changed = false
    var len     = pattern.length
    var i       = 0
    while (i < len) {
      val ch = pattern.charAt(i)
      if (ch == '\'') {
        changed = true
        buffer.append("''")
      } else if (ch == '{' && i + 1 < len && pattern.charAt(i + 1) == '{') {
        changed = true
        buffer.append('{')
        i += 1
      } else {
        buffer.append(ch)
      }
      i += 1
    }
    if (changed) buffer.toString else pattern
  }

  private def simpleFormat(pattern: String, args: Array[java.lang.Object]): String = {
    buffer.setLength(0)
    var changed = false
    var len     = pattern.length
    var i       = 0
    while (i < len) {
      val ch = pattern.charAt(i)
      if (ch == '{') {
        if (i + 1 < len && pattern.charAt(i + 1) == '{') {
          buffer.append('{')
          i += 1
          changed = true
        } else {
          val end = pattern.indexOf('}', i + 1)
          if (end < 0) throw new IllegalArgumentException("Unmatched '{' in pattern: " + pattern)
          val placeholder = pattern.substring(i + 1, end)
          if (placeholder.isEmpty) throw new IllegalArgumentException("Empty placeholder in pattern: " + pattern)
          val idx = try { placeholder.toInt } catch {
            case _: NumberFormatException => throw new IllegalArgumentException("Non-numeric placeholder '{" + placeholder + "}' in pattern: " + pattern)
          }
          if (idx < 0 || idx >= args.length) throw new IllegalArgumentException("Placeholder index " + idx + " out of range [0, " + args.length + ") in pattern: " + pattern)
          buffer.append(if (args(idx) == null) "null" else args(idx).toString)
          i = end
          changed = true
        }
      } else {
        buffer.append(ch)
      }
      i += 1
    }
    if (changed) buffer.toString else pattern
  }
}
