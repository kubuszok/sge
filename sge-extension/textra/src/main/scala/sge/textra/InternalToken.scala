/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/InternalToken.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 56
 * Covenant-baseline-methods: InternalToken,fromName,toString
 * Covenant-source-reference: com/github/tommyettinger/textra/InternalToken.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import sge.utils.Nullable

enum InternalToken(val tokenName: String, val category: TokenCategory) extends java.lang.Enum[InternalToken] {
  case WAIT extends InternalToken("WAIT", TokenCategory.WAIT)
  case SPEED extends InternalToken("SPEED", TokenCategory.SPEED)
  case SLOWER extends InternalToken("SLOWER", TokenCategory.SPEED)
  case SLOW extends InternalToken("SLOW", TokenCategory.SPEED)
  case NORMAL extends InternalToken("NORMAL", TokenCategory.SPEED)
  case FAST extends InternalToken("FAST", TokenCategory.SPEED)
  case FASTER extends InternalToken("FASTER", TokenCategory.SPEED)
  case NATURAL extends InternalToken("NATURAL", TokenCategory.SPEED)
  case COLOR extends InternalToken("COLOR", TokenCategory.COLOR)
  case STYLE extends InternalToken("STYLE", TokenCategory.COLOR)
  case SIZE extends InternalToken("SIZE", TokenCategory.COLOR)
  case FONT extends InternalToken("FONT", TokenCategory.COLOR)
  case CLEARCOLOR extends InternalToken("CLEARCOLOR", TokenCategory.COLOR)
  case CLEARSIZE extends InternalToken("CLEARSIZE", TokenCategory.COLOR)
  case CLEARFONT extends InternalToken("CLEARFONT", TokenCategory.COLOR)
  case ENDCOLOR extends InternalToken("ENDCOLOR", TokenCategory.COLOR)
  case VAR extends InternalToken("VAR", TokenCategory.VARIABLE)
  case IF extends InternalToken("IF", TokenCategory.IF)
  case EVENT extends InternalToken("EVENT", TokenCategory.EVENT)
  case RESET extends InternalToken("RESET", TokenCategory.RESET)
  case SKIP extends InternalToken("SKIP", TokenCategory.SKIP)
  case UNDO extends InternalToken("UNDO", TokenCategory.UNDO)

  override def toString: String = tokenName
}

object InternalToken {
  def fromName(name: String): Nullable[InternalToken] =
    if (name != null) {
      val vals = values
      var i    = 0
      var found: Nullable[InternalToken] = Nullable.empty
      while (i < vals.length && found.isEmpty) {
        if (name.equalsIgnoreCase(vals(i).tokenName)) {
          found = Nullable(vals(i))
        }
        i += 1
      }
      found
    } else {
      Nullable.empty
    }
}
