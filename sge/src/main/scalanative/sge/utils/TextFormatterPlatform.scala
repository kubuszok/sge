/*
 * Scala Native TextFormatter platform — no java.text.MessageFormat on this platform.
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

  /** Scala Native ships no `java.text.MessageFormat`: none, so `TextFormatter` always formats simply, as libGDX's GWT emulation of it does whatever `useMessageFormat` says.
    */
  def messageFormat(pattern: String, locale: Locale): Nullable[MessageFormat] = Nullable.empty
}
