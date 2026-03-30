/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TokenCategory.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra

enum TokenCategory extends java.lang.Enum[TokenCategory] {
  case WAIT, SPEED, COLOR, VARIABLE, IF, EVENT, RESET, SKIP, EFFECT_START, EFFECT_END, UNDO
}

object TokenCategory {
  val ALL: Array[TokenCategory] = values
}
