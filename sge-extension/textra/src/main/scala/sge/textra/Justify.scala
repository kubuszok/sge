/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/Justify.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra

/** Determines line justification behavior in a Layout. Besides [[NONE]], which makes no changes, each constant applies justification to some glyphs on a typical [[Line]].
  */
enum Justify(val ignoreLastLine: Boolean, val affectSpaces: Boolean, val affectAllGlyphs: Boolean) extends java.lang.Enum[Justify] {

  /** No justification will be applied; the x-advances of glyphs will not be changed. */
  case NONE extends Justify(false, false, false)

  /** Adds extra x-advance to every space so the text fills all the way to the right edge. */
  case SPACES_ON_ALL_LINES extends Justify(false, true, false)

  /** Adds extra x-advance to every glyph so the text fills all the way to the right edge. */
  case FULL_ON_ALL_LINES extends Justify(false, false, true)

  /** Adds extra x-advance to every space except for the last Line of a paragraph. */
  case SPACES_ON_PARAGRAPH extends Justify(true, true, false)

  /** Adds extra space to every glyph except for the last Line of a paragraph. */
  case FULL_ON_PARAGRAPH extends Justify(true, false, true)
}
