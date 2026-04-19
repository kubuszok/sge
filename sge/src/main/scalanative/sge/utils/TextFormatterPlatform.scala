/*
 * Scala Native TextFormatter platform — no java.text.MessageFormat available.
 * Falls back to simpleFormat (matches GWT emulation behavior).
 */
package sge
package utils

import java.util.Locale

private[utils] object TextFormatterPlatform {

  trait AdvancedFormatter {
    def format(pattern: String, args: Seq[AnyRef]): String
  }

  def createAdvancedFormatter(locale: Locale): Nullable[AdvancedFormatter] =
    Nullable.empty
}
