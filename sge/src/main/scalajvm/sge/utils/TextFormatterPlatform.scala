/*
 * JVM TextFormatter platform — provides java.text.MessageFormat integration.
 */
package sge
package utils

import java.text.MessageFormat
import java.util.Locale

private[utils] object TextFormatterPlatform {

  trait AdvancedFormatter {
    def format(pattern: String, args: Seq[AnyRef]): String
  }

  def createAdvancedFormatter(locale: Locale): Nullable[AdvancedFormatter] =
    Nullable(
      new AdvancedFormatter {
        private val mf = new MessageFormat("", locale)

        def format(pattern: String, args: Seq[AnyRef]): String = {
          mf.applyPattern(pattern)
          mf.format(args.toArray)
        }
      }
    )
}
