/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package oklab

import sge.colorful.FloatColors

import scala.collection.mutable

/** A palette of predefined colors as packed Oklab floats, plus a way to describe colors by combinations and adjustments. The description code revolves around [[parseDescription]], which takes a color
  * description String and returns a packed float color. The color descriptions look like "darker rich mint yellow", where the order of the words doesn't matter. They can include lightness changes
  * (light/dark), saturation changes (rich/dull), and must include one or more color names that will be mixed together.
  */
object SimplePalette {

  /** Maps lowercase color names to packed float Oklab colors. */
  val NAMED: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Stores alternative names for colors in NAMED. */
  val ALIASES: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Lists packed float color values, in declaration order. */
  val LIST: mutable.ArrayBuffer[Float] = mutable.ArrayBuffer.empty[Float]

  val TRANSPARENT: Float = java.lang.Float.intBitsToFloat(0x7f7f00)
  val BLACK:       Float = java.lang.Float.intBitsToFloat(0xfe7f7f00)
  val GRAY:        Float = java.lang.Float.intBitsToFloat(0xfe7f7f80)
  val SILVER:      Float = java.lang.Float.intBitsToFloat(0xfe7f7fb6)
  val WHITE:       Float = java.lang.Float.intBitsToFloat(0xfe7f7fff)
  val RED:         Float = java.lang.Float.intBitsToFloat(0xfe8f9c7f)
  val ORANGE:      Float = java.lang.Float.intBitsToFloat(0xfe928ca4)
  val YELLOW:      Float = java.lang.Float.intBitsToFloat(0xfe9876f3)
  val GREEN:       Float = java.lang.Float.intBitsToFloat(0xfe9661ce)
  val BLUE:        Float = java.lang.Float.intBitsToFloat(0xfe577b4d)
  val INDIGO:      Float = java.lang.Float.intBitsToFloat(0xfe5e8851)
  val VIOLET:      Float = java.lang.Float.intBitsToFloat(0xfe658f73)
  val PURPLE:      Float = java.lang.Float.intBitsToFloat(0xfe639a7c)
  val BROWN:       Float = java.lang.Float.intBitsToFloat(0xfe878666)
  val PINK:        Float = java.lang.Float.intBitsToFloat(0xfe798ec3)
  val MAGENTA:     Float = java.lang.Float.intBitsToFloat(0xfe6aa190)
  val BRICK:       Float = java.lang.Float.intBitsToFloat(0xfe889180)
  val EMBER:       Float = java.lang.Float.intBitsToFloat(0xfe8e928f)
  val SALMON:      Float = java.lang.Float.intBitsToFloat(0xfe889499)
  val CHOCOLATE:   Float = java.lang.Float.intBitsToFloat(0xfe888645)
  val TAN:         Float = java.lang.Float.intBitsToFloat(0xfe8681b9)
  val BRONZE:      Float = java.lang.Float.intBitsToFloat(0xfe8f839b)
  val CINNAMON:    Float = java.lang.Float.intBitsToFloat(0xfe8f8a88)
  val APRICOT:     Float = java.lang.Float.intBitsToFloat(0xfe9385bb)
  val PEACH:       Float = java.lang.Float.intBitsToFloat(0xfe8b84cd)
  val PEAR:        Float = java.lang.Float.intBitsToFloat(0xfe9575d5)
  val SAFFRON:     Float = java.lang.Float.intBitsToFloat(0xfe967cd7)
  val BUTTER:      Float = java.lang.Float.intBitsToFloat(0xfe8e7cee)
  val CHARTREUSE:  Float = java.lang.Float.intBitsToFloat(0xfe9570e7)
  val CACTUS:      Float = java.lang.Float.intBitsToFloat(0xfe906b83)
  val LIME:        Float = java.lang.Float.intBitsToFloat(0xfe946fba)
  val OLIVE:       Float = java.lang.Float.intBitsToFloat(0xfe8f797a)
  val FERN:        Float = java.lang.Float.intBitsToFloat(0xfe87766b)
  val MOSS:        Float = java.lang.Float.intBitsToFloat(0xfe89753b)
  val CELERY:      Float = java.lang.Float.intBitsToFloat(0xfe8f6bda)
  val SAGE:        Float = java.lang.Float.intBitsToFloat(0xfe8277d4)
  val JADE:        Float = java.lang.Float.intBitsToFloat(0xfe8e6a9f)
  val CYAN:        Float = java.lang.Float.intBitsToFloat(0xfe7a6cdc)
  val MINT:        Float = java.lang.Float.intBitsToFloat(0xfe8270e1)
  val TEAL:        Float = java.lang.Float.intBitsToFloat(0xfe7c736d)
  val TURQUOISE:   Float = java.lang.Float.intBitsToFloat(0xfe7d6eb9)
  val SKY:         Float = java.lang.Float.intBitsToFloat(0xfe7671a9)
  val COBALT:      Float = java.lang.Float.intBitsToFloat(0xfe697b4d)
  val DENIM:       Float = java.lang.Float.intBitsToFloat(0xfe74777e)
  val NAVY:        Float = java.lang.Float.intBitsToFloat(0xfe667c27)
  val LAVENDER:    Float = java.lang.Float.intBitsToFloat(0xfe6f88a9)
  val PLUM:        Float = java.lang.Float.intBitsToFloat(0xfe6c9b71)
  val MAUVE:       Float = java.lang.Float.intBitsToFloat(0xfe788a89)
  val ROSE:        Float = java.lang.Float.intBitsToFloat(0xfe7e9d7b)
  val RASPBERRY:   Float = java.lang.Float.intBitsToFloat(0xfe83944c)

  // Register all colors
  locally {
    val colors: Array[(String, Float)] = Array(
      "transparent" -> TRANSPARENT,
      "black" -> BLACK,
      "gray" -> GRAY,
      "silver" -> SILVER,
      "white" -> WHITE,
      "red" -> RED,
      "orange" -> ORANGE,
      "yellow" -> YELLOW,
      "green" -> GREEN,
      "blue" -> BLUE,
      "indigo" -> INDIGO,
      "violet" -> VIOLET,
      "purple" -> PURPLE,
      "brown" -> BROWN,
      "pink" -> PINK,
      "magenta" -> MAGENTA,
      "brick" -> BRICK,
      "ember" -> EMBER,
      "salmon" -> SALMON,
      "chocolate" -> CHOCOLATE,
      "tan" -> TAN,
      "bronze" -> BRONZE,
      "cinnamon" -> CINNAMON,
      "apricot" -> APRICOT,
      "peach" -> PEACH,
      "pear" -> PEAR,
      "saffron" -> SAFFRON,
      "butter" -> BUTTER,
      "chartreuse" -> CHARTREUSE,
      "cactus" -> CACTUS,
      "lime" -> LIME,
      "olive" -> OLIVE,
      "fern" -> FERN,
      "moss" -> MOSS,
      "celery" -> CELERY,
      "sage" -> SAGE,
      "jade" -> JADE,
      "cyan" -> CYAN,
      "mint" -> MINT,
      "teal" -> TEAL,
      "turquoise" -> TURQUOISE,
      "sky" -> SKY,
      "cobalt" -> COBALT,
      "denim" -> DENIM,
      "navy" -> NAVY,
      "lavender" -> LAVENDER,
      "plum" -> PLUM,
      "mauve" -> MAUVE,
      "rose" -> ROSE,
      "raspberry" -> RASPBERRY
    )
    for ((name, color) <- colors) {
      NAMED.put(name, color)
      LIST += color
    }
    ALIASES.put("grey", GRAY)
    ALIASES.put("gold", SAFFRON)
    ALIASES.put("puce", MAUVE)
    ALIASES.put("sand", TAN)
    ALIASES.put("skin", PEACH)
    ALIASES.put("coral", SALMON)
    ALIASES.put("azure", SKY)
    ALIASES.put("ocean", TEAL)
    ALIASES.put("sapphire", COBALT)
    for ((name, color) <- ALIASES)
      NAMED.put(name, color)
  }

  /** Parses a color description string and returns a packed Oklab float color. The description can include lightness modifiers (light, lighter, lightest, dark, darker, darkest), saturation modifiers
    * (rich, richer, richest, dull, duller, dullest), and color names to mix. Combined modifiers: bright (light+rich), pale (light+dull), deep (dark+rich), weak (dark+dull).
    *
    * @param description
    *   a color description, such as "darker rich mint yellow"
    * @return
    *   a packed Oklab float color as described, or 0f if parsing fails
    */
  def parseDescription(description: String): Float =
    parseDescription(description, 0, -1)

  /** Parses a color description with start index and length.
    */
  def parseDescription(description: String, start: Int, length: Int): Float = {
    var lightness  = 0f
    var saturation = 0f
    val end        = if (length < 0) description.length else Math.min(description.length, start + length)
    val terms      = description.substring(start, end).split("[^a-zA-Z0-9_.]+")
    val mixing     = mutable.ArrayBuffer.empty[Float]

    for (term <- terms if term != null && term.nonEmpty) {
      val len     = term.length
      val ch0     = term.charAt(0)
      var handled = false

      ch0 match {
        case 'L' | 'l' =>
          if (len > 2 && (term.charAt(2) == 'g' || term.charAt(2) == 'G')) { // light
            len match {
              case 9 => lightness += 0.600f; handled = true // lightmost
              case 8 => lightness += 0.450f; handled = true // lightest
              case 7 => lightness += 0.300f; handled = true // lighter
              case 5 => lightness += 0.150f; handled = true // light
              case _ => // not a modifier
            }
          }
        case 'B' | 'b' =>
          if (len > 3 && (term.charAt(3) == 'g' || term.charAt(3) == 'G')) { // bright
            len match {
              case 10 => lightness += 0.600f; saturation += 0.800f; handled = true
              case 9  => lightness += 0.450f; saturation += 0.600f; handled = true
              case 8  => lightness += 0.300f; saturation += 0.400f; handled = true
              case 6  => lightness += 0.150f; saturation += 0.200f; handled = true
              case _  =>
            }
          }
        case 'P' | 'p' =>
          if (len > 2 && (term.charAt(2) == 'l' || term.charAt(2) == 'L')) { // pale
            len match {
              case 8 => lightness += 0.600f; saturation -= 0.800f; handled = true
              case 7 => lightness += 0.450f; saturation -= 0.600f; handled = true
              case 6 => lightness += 0.300f; saturation -= 0.400f; handled = true
              case 5 => lightness += 0.150f; saturation -= 0.200f; handled = true
              case 4 => lightness += 0.150f; saturation -= 0.200f; handled = true
              case _ =>
            }
          }
        case 'W' | 'w' =>
          if (len > 3 && (term.charAt(3) == 'k' || term.charAt(3) == 'K')) { // weak
            len match {
              case 8 => lightness -= 0.600f; saturation -= 0.800f; handled = true
              case 7 => lightness -= 0.450f; saturation -= 0.600f; handled = true
              case 6 => lightness -= 0.300f; saturation -= 0.400f; handled = true
              case 4 => lightness -= 0.150f; saturation -= 0.200f; handled = true
              case _ =>
            }
          }
        case 'R' | 'r' =>
          if (len > 1 && (term.charAt(1) == 'i' || term.charAt(1) == 'I')) { // rich
            len match {
              case 8 => saturation += 0.800f; handled = true
              case 7 => saturation += 0.600f; handled = true
              case 6 => saturation += 0.400f; handled = true
              case 4 => saturation += 0.200f; handled = true
              case _ =>
            }
          }
        case 'D' | 'd' =>
          if (len > 1 && (term.charAt(1) == 'a' || term.charAt(1) == 'A')) { // dark
            len match {
              case 8 => lightness -= 0.600f; handled = true
              case 7 => lightness -= 0.450f; handled = true
              case 6 => lightness -= 0.300f; handled = true
              case 4 => lightness -= 0.150f; handled = true
              case _ =>
            }
          } else if (len > 1 && (term.charAt(1) == 'u' || term.charAt(1) == 'U')) { // dull
            len match {
              case 8 => saturation -= 0.800f; handled = true
              case 7 => saturation -= 0.600f; handled = true
              case 6 => saturation -= 0.400f; handled = true
              case 4 => saturation -= 0.200f; handled = true
              case _ =>
            }
          } else if (len > 3 && (term.charAt(3) == 'p' || term.charAt(3) == 'P')) { // deep
            len match {
              case 8 => lightness -= 0.600f; saturation += 0.800f; handled = true
              case 7 => lightness -= 0.450f; saturation += 0.600f; handled = true
              case 6 => lightness -= 0.300f; saturation += 0.400f; handled = true
              case 4 => lightness -= 0.150f; saturation += 0.200f; handled = true
              case _ =>
            }
          }
        case c if c >= '0' && c <= '9' =>
          if (mixing.size >= 2) {
            try {
              val num = term.toFloat
              mixing(mixing.size - 1) = num
            } catch {
              case _: NumberFormatException => // ignore
            }
          }
          handled = true
        case _ => // fall through to color lookup
      }

      if (!handled) {
        val color = NAMED.getOrElse(term, NAMED.getOrElse(term.toLowerCase, 0f))
        if (color != 0f) {
          mixing += color
          mixing += 1f
        }
      }
    }

    if (mixing.size < 2) 0f
    else {
      var result = FloatColors.unevenMix(mixing.toArray, 0, mixing.size)
      if (result == 0f) result
      else {
        if (lightness > 0) result = FloatColors.lerpFloatColorsBlended(result, WHITE, lightness)
        else if (lightness < 0) result = FloatColors.lerpFloatColorsBlended(result, BLACK, -lightness)

        if (saturation > 0) result = ColorTools.enrich(result, saturation)
        else if (saturation < 0) result = ColorTools.dullen(result, -saturation)

        result
      }
    }
  }
}
