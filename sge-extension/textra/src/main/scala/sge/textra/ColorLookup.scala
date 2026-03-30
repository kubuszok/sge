/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/ColorLookup.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra

import sge.textra.utils.ColorUtils

/** Allows looking up an RGBA8888 int color given a String key, returning either the color or 256 if none was found. This is an extension point for games and libraries that may want their own way of
  * looking up colors.
  */
trait ColorLookup {

  /** Looks up `key` to get an RGBA8888 color, and returns that color as an int if one was found, or returns 256 if none was found.
    */
  def getRgba(key: String): Int = getRgba(key, 0, key.length)

  /** Looks up `key` to get an RGBA8888 color, and returns that color as an int if one was found, or returns 256 if none was found.
    */
  def getRgba(key: String, beginIndex: Int, endIndex: Int): Int
}

object ColorLookup {

  /** Simply looks up `key` in Colors. Returns 256 if no Color exists by that exact name. */
  val INSTANCE: ColorLookup = new ColorLookup {
    override def getRgba(key: String, beginIndex: Int, endIndex: Int): Int =
      ColorUtils.lookupInColors(key, beginIndex, endIndex)
  }

  /** The default ColorLookup, parses a description such as "peach red" or "DARK DULLEST GREEN" using [[ColorUtils.describe]].
    */
  val DESCRIPTIVE: ColorLookup = new ColorLookup {
    override def getRgba(key: String, beginIndex: Int, endIndex: Int): Int =
      ColorUtils.describe(key, beginIndex, endIndex)
  }
}
