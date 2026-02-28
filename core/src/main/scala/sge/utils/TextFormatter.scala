/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TextFormatter.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
