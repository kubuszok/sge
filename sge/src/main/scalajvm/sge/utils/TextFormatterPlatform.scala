/*
 * JVM TextFormatter platform — provides java.text.MessageFormat integration.
 */
package sge
package utils

import lowlevel.Nullable
import java.util.Locale

/** The `java.text.MessageFormat` members the generated `TextFormatter` calls, per platform row. */
private[utils] object TextFormatterPlatform {

  /** `java.text.MessageFormat`'s shape as `TextFormatter` uses it: set the pattern, then format. */
  trait MessageFormat {
    def applyPattern(pattern: String): Unit
    def format(arguments:     Object): String
  }

  /** The JVM has `java.text.MessageFormat`: java's own formatter for `pattern` in `locale`. */
  def messageFormat(pattern: String, locale: Locale): Nullable[MessageFormat] =
    Nullable(
      new MessageFormat {
        private val mf = new java.text.MessageFormat(pattern, locale)

        def applyPattern(pattern: String): Unit   = mf.applyPattern(pattern)
        def format(arguments:     Object): String = mf.format(arguments)
      }
    )
}
