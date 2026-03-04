/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TextFormatter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `useMessageFormat` -> `useAdvanced`
 *   Idiom: split packages
 *   Issues: implementation is a simplistic placeholder using basic `String.replace`; missing `MessageFormat`-based locale formatting, brace escaping (`{{`), and `simpleFormat` error checking from Java original
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.util.Locale

class TextFormatter(locale: Locale, useAdvanced: Boolean) {
  def format(pattern: String, args: AnyRef*): String = {
    // Simple placeholder implementation - should use proper MessageFormat
    var result = pattern
    args.zipWithIndex.foreach { case (arg, index) =>
      result = result.replace(s"{$index}", Nullable(arg).fold("null")(_.toString))
    }
    result
  }
}
