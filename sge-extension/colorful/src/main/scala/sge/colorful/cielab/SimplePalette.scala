/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package cielab

import sge.colorful.FloatColors

import scala.collection.mutable

/** A palette of predefined colors as packed CIELAB floats, plus a way to describe colors by combinations and adjustments. The description code revolves around [[parseDescription]], which takes a
  * color description String and returns a packed float color. The color descriptions look like "darker rich mint yellow", where the order of the words doesn't matter. They can include lightness
  * changes (light/dark), saturation changes (rich/dull), and must include one or more color names that will be mixed together.
  */
object SimplePalette {

  /** Maps lowercase color names to packed float colors. */
  val NAMED: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Stores alternative names for colors in NAMED. */
  val ALIASES: mutable.HashMap[String, Float] = mutable.HashMap.empty[String, Float]

  /** Lists packed float color values, in declaration order. */
  val LIST: mutable.ArrayBuffer[Float] = mutable.ArrayBuffer.empty[Float]

  val TRANSPARENT: Float = java.lang.Float.intBitsToFloat(0x7f7f00)
  val BLACK:       Float = java.lang.Float.intBitsToFloat(0xfe7f7f00)
  val GRAY:        Float = java.lang.Float.intBitsToFloat(0xfe7b7989)
  val SILVER:      Float = java.lang.Float.intBitsToFloat(0xfe7977bd)
  val WHITE:       Float = java.lang.Float.intBitsToFloat(0xfe7874ff)
  val RED:         Float = java.lang.Float.intBitsToFloat(0xfed3dd88)
  val ORANGE:      Float = java.lang.Float.intBitsToFloat(0xfedbaeab)
  val YELLOW:      Float = java.lang.Float.intBitsToFloat(0xfef459f8)
  val GREEN:       Float = java.lang.Float.intBitsToFloat(0xfee609e0)
  val BLUE:        Float = java.lang.Float.intBitsToFloat(0xfe00de52)
  val INDIGO:      Float = java.lang.Float.intBitsToFloat(0xfe07d454)
  val VIOLET:      Float = java.lang.Float.intBitsToFloat(0xfe19cc77)
  val PURPLE:      Float = java.lang.Float.intBitsToFloat(0xfe14ea80)
  val BROWN:       Float = java.lang.Float.intBitsToFloat(0xfe9d936d)
  val PINK:        Float = java.lang.Float.intBitsToFloat(0xfe62aec5)
  val MAGENTA:     Float = java.lang.Float.intBitsToFloat(0xfe2df094)
  val BRICK:       Float = java.lang.Float.intBitsToFloat(0xfea5b986)
  val EMBER:       Float = java.lang.Float.intBitsToFloat(0xfec0c096)
  val SALMON:      Float = java.lang.Float.intBitsToFloat(0xfea4c39f)
  val CHOCOLATE:   Float = java.lang.Float.intBitsToFloat(0xfea2924a)
  val TAN:         Float = java.lang.Float.intBitsToFloat(0xfe997dbf)
  val BRONZE:      Float = java.lang.Float.intBitsToFloat(0xfec48ca3)
  val CINNAMON:    Float = java.lang.Float.intBitsToFloat(0xfec6a78f)
  val APRICOT:     Float = java.lang.Float.intBitsToFloat(0xfed893c1)
  val PEACH:       Float = java.lang.Float.intBitsToFloat(0xfeae8ad1)
  val PEAR:        Float = java.lang.Float.intBitsToFloat(0xfedf55dd)
  val SAFFRON:     Float = java.lang.Float.intBitsToFloat(0xfee975dd)
  val BUTTER:      Float = java.lang.Float.intBitsToFloat(0xfebd69f2)
  val CHARTREUSE:  Float = java.lang.Float.intBitsToFloat(0xfee040ef)
  val CACTUS:      Float = java.lang.Float.intBitsToFloat(0xfec93294)
  val LIME:        Float = java.lang.Float.intBitsToFloat(0xfede3ec7)
  val OLIVE:       Float = java.lang.Float.intBitsToFloat(0xfec56985)
  val FERN:        Float = java.lang.Float.intBitsToFloat(0xfe9d5877)
  val MOSS:        Float = java.lang.Float.intBitsToFloat(0xfea45b42)
  val CELERY:      Float = java.lang.Float.intBitsToFloat(0xfec127e7)
  val SAGE:        Float = java.lang.Float.intBitsToFloat(0xfe8457db)
  val JADE:        Float = java.lang.Float.intBitsToFloat(0xfebf2daf)
  val CYAN:        Float = java.lang.Float.intBitsToFloat(0xfe6639e9)
  val MINT:        Float = java.lang.Float.intBitsToFloat(0xfe853ceb)
  val TEAL:        Float = java.lang.Float.intBitsToFloat(0xfe70557a)
  val TURQUOISE:   Float = java.lang.Float.intBitsToFloat(0xfe723fc7)
  val SKY:         Float = java.lang.Float.intBitsToFloat(0xfe5653b7)
  val COBALT:      Float = java.lang.Float.intBitsToFloat(0xfe2e9853)
  val DENIM:       Float = java.lang.Float.intBitsToFloat(0xfe506c89)
  val NAVY:        Float = java.lang.Float.intBitsToFloat(0xfe28b821)
  val LAVENDER:    Float = java.lang.Float.intBitsToFloat(0xfe39a6ad)
  val PLUM:        Float = java.lang.Float.intBitsToFloat(0xfe37dc75)
  val MAUVE:       Float = java.lang.Float.intBitsToFloat(0xfe5fa08f)
  val ROSE:        Float = java.lang.Float.intBitsToFloat(0xfe7ed882)
  val RASPBERRY:   Float = java.lang.Float.intBitsToFloat(0xfe8ebb50)

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

    if (mixing.size < 2) { return 0f } // scalastyle:ignore return

    var result = FloatColors.unevenMix(mixing.toArray, 0, mixing.size)
    if (result == 0f) { return result } // scalastyle:ignore return

    if (lightness > 0) result = FloatColors.lerpFloatColorsBlended(result, WHITE, lightness)
    else if (lightness < 0) result = FloatColors.lerpFloatColorsBlended(result, BLACK, -lightness)

    if (saturation > 0) result = ColorTools.enrich(result, saturation)
    else if (saturation < 0) result = ColorTools.dullen(result, -saturation)

    result
  }
}
