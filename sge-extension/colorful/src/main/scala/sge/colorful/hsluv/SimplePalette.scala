/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package hsluv

import sge.colorful.FloatColors

import scala.collection.mutable

/** A palette of predefined colors as packed HSLUV floats, plus a way to describe colors by combinations and adjustments. The description code revolves around [[parseDescription]], which takes a color
  * description String and returns a packed float color. The color descriptions look like "darker rich mint yellow", where the order of the words doesn't matter. They can include lightness changes
  * (light/dark), saturation changes (rich/dull), and must include one or more color names that will be mixed together.
  */
object SimplePalette {

  /** Maps lowercase color names to packed float colors. */
  val NAMED: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Stores alternative names for colors in NAMED. */
  val ALIASES: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Lists packed float color values, in declaration order. */
  val LIST: mutable.ArrayBuffer[Float] = mutable.ArrayBuffer.empty[Float]

  val TRANSPARENT: Float = java.lang.Float.intBitsToFloat(0x0)
  val BLACK:       Float = java.lang.Float.intBitsToFloat(0xfe000000)
  val GRAY:        Float = java.lang.Float.intBitsToFloat(0xfe800029)
  val SILVER:      Float = java.lang.Float.intBitsToFloat(0xfeb50029)
  val WHITE:       Float = java.lang.Float.intBitsToFloat(0xfeff0029)
  val RED:         Float = java.lang.Float.intBitsToFloat(0xfe7fff08)
  val ORANGE:      Float = java.lang.Float.intBitsToFloat(0xfea2ff15)
  val YELLOW:      Float = java.lang.Float.intBitsToFloat(0xfef7ff3d)
  val GREEN:       Float = java.lang.Float.intBitsToFloat(0xfedcff5a)
  val BLUE:        Float = java.lang.Float.intBitsToFloat(0xfe4cffbd)
  val INDIGO:      Float = java.lang.Float.intBitsToFloat(0xfe4dffc0)
  val VIOLET:      Float = java.lang.Float.intBitsToFloat(0xfe6ee1c5)
  val PURPLE:      Float = java.lang.Float.intBitsToFloat(0xfe77ffcd)
  val BROWN:       Float = java.lang.Float.intBitsToFloat(0xfe65ae15)
  val PINK:        Float = java.lang.Float.intBitsToFloat(0xfebee0e8)
  val MAGENTA:     Float = java.lang.Float.intBitsToFloat(0xfe8bffda)
  val BRICK:       Float = java.lang.Float.intBitsToFloat(0xfe7daf0a)
  val EMBER:       Float = java.lang.Float.intBitsToFloat(0xfe8dec0d)
  val SALMON:      Float = java.lang.Float.intBitsToFloat(0xfe96dc08)
  val CHOCOLATE:   Float = java.lang.Float.intBitsToFloat(0xfe44e416)
  val TAN:         Float = java.lang.Float.intBitsToFloat(0xfeb86f27)
  val BRONZE:      Float = java.lang.Float.intBitsToFloat(0xfe9af121)
  val CINNAMON:    Float = java.lang.Float.intBitsToFloat(0xfe86ff14)
  val APRICOT:     Float = java.lang.Float.intBitsToFloat(0xfebafd1f)
  val PEACH:       Float = java.lang.Float.intBitsToFloat(0xfecbd820)
  val PEAR:        Float = java.lang.Float.intBitsToFloat(0xfed8f841)
  val SAFFRON:     Float = java.lang.Float.intBitsToFloat(0xfed8ff30)
  val BUTTER:      Float = java.lang.Float.intBitsToFloat(0xfeefd438)
  val CHARTREUSE:  Float = java.lang.Float.intBitsToFloat(0xfeecef4a)
  val CACTUS:      Float = java.lang.Float.intBitsToFloat(0xfe8aff58)
  val LIME:        Float = java.lang.Float.intBitsToFloat(0xfebfff4e)
  val OLIVE:       Float = java.lang.Float.intBitsToFloat(0xfe7bff3c)
  val FERN:        Float = java.lang.Float.intBitsToFloat(0xfe6ea856)
  val MOSS:        Float = java.lang.Float.intBitsToFloat(0xfe3dff55)
  val CELERY:      Float = java.lang.Float.intBitsToFloat(0xfee3d559)
  val SAGE:        Float = java.lang.Float.intBitsToFloat(0xfed66369)
  val JADE:        Float = java.lang.Float.intBitsToFloat(0xfea6e45a)
  val CYAN:        Float = java.lang.Float.intBitsToFloat(0xfee5ff88)
  val MINT:        Float = java.lang.Float.intBitsToFloat(0xfee8d970)
  val TEAL:        Float = java.lang.Float.intBitsToFloat(0xfe71ff88)
  val TURQUOISE:   Float = java.lang.Float.intBitsToFloat(0xfec0fb81)
  val SKY:         Float = java.lang.Float.intBitsToFloat(0xfeafff9a)
  val COBALT:      Float = java.lang.Float.intBitsToFloat(0xfe4cffb8)
  val DENIM:       Float = java.lang.Float.intBitsToFloat(0xfe80f2a8)
  val NAVY:        Float = java.lang.Float.intBitsToFloat(0xfe20ffbd)
  val LAVENDER:    Float = java.lang.Float.intBitsToFloat(0xfea5e8c5)
  val PLUM:        Float = java.lang.Float.intBitsToFloat(0xfe6dffd8)
  val MAUVE:       Float = java.lang.Float.intBitsToFloat(0xfe8661da)
  val ROSE:        Float = java.lang.Float.intBitsToFloat(0xfe79fffc)
  val RASPBERRY:   Float = java.lang.Float.intBitsToFloat(0xfe49f802)

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

  /** Parses a color description string and returns a packed float color. The description can include lightness modifiers (light, lighter, lightest, dark, darker, darkest), saturation modifiers (rich,
    * richer, richest, dull, duller, dullest), and color names to mix. Combined modifiers: bright (light+rich), pale (light+dull), deep (dark+rich), weak (dark+dull).
    *
    * @param description
    *   a color description, such as "darker rich mint yellow"
    * @return
    *   a packed float color as described, or 0f if parsing fails
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
