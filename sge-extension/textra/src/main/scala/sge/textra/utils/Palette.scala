/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/Palette.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 228
 * Covenant-baseline-methods: ALIASES,BLACK,BLUE,BROWN,CHARTREUSE,CLEAR,CORAL,CYAN,DARK_GRAY,FIREBRICK,FOREST,GOLD,GOLDENROD,GRAY,GREEN,LIGHT_GRAY,LIME,LIST,MAGENTA,MAROON,NAMED,NAMES,NAVY,OLIVE,ORANGE,PINK,PURPLE,Palette,RED,ROYAL,SALMON,SCARLET,SKY,SLATE,TAN,TEAL,VIOLET,WHITE,YELLOW,addColor,apricot,black,blue,brown,butter,cactus,celeste,charcoal,chartreuse,chocolate,cobalt,coral,crimson,cyan,denim,ember,entries,fern,gray,green,indigo,jade,lavender,lime,magenta,mauve,moss,navy,orange,peach,pine,plum,puce,purple,raspberry,red,rose,saffron,sage,salmon,sand,sepia,silver,sky,tan,teal,transparent,violet,wheat,white,yellow
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/Palette.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package utils

import scala.collection.mutable.{ ArrayBuffer, HashMap => MutableMap }

/** Combines 50 colors chosen to be generally distinct with the 34 colors libGDX defines in Colors. */
object Palette {

  val ALIASES: MutableMap[String, Int] = MutableMap.empty
  val NAMED:   MutableMap[String, Int] = MutableMap.empty
  val LIST:    ArrayBuffer[Int]        = ArrayBuffer.empty
  val NAMES:   ArrayBuffer[String]     = ArrayBuffer.empty

  // Color constants
  val transparent: Int = 0x00000000
  val black:       Int = 0x000000ff
  val gray:        Int = 0x808080ff
  val silver:      Int = 0xb6b6b6ff
  val white:       Int = 0xffffffff
  val red:         Int = 0xff0000ff
  val orange:      Int = 0xff7f00ff
  val yellow:      Int = 0xffff00ff
  val green:       Int = 0x00ff00ff
  val blue:        Int = 0x0000ffff
  val indigo:      Int = 0x520fe0ff
  val violet:      Int = 0x9040efff
  val purple:      Int = 0xc000ffff
  val brown:       Int = 0x8b4513ff
  val salmon:      Int = 0xff6347ff
  val chocolate:   Int = 0xd2691eff
  val tan:         Int = 0xd2b48cff.toInt
  val saffron:     Int = 0xf4c430ff.toInt
  val butter:      Int = 0xffd580ff.toInt
  val chartreuse:  Int = 0xc8ff00ff.toInt
  val cactus:      Int = 0x6b8e23ff
  val lime:        Int = 0x32cd32ff
  val jade:        Int = 0x00a86bff
  val cyan:        Int = 0x00ffffff
  val teal:        Int = 0x007f7fff
  val sky:         Int = 0x87ceebff.toInt
  val cobalt:      Int = 0x0047abff
  val denim:       Int = 0x1560bdff
  val navy:        Int = 0x000080ff
  val lavender:    Int = 0xb57edfff.toInt
  val plum:        Int = 0x701f6eff
  val magenta:     Int = 0xff00ffff.toInt
  val puce:        Int = 0xcc8899ff.toInt
  val rose:        Int = 0xe8909cff.toInt
  val raspberry:   Int = 0x911437ff.toInt
  val peach:       Int = 0xffcba4ff.toInt
  val mauve:       Int = 0xe0b0ffff.toInt
  val ember:       Int = 0xf05030ff.toInt
  val crimson:     Int = 0xdc143cff.toInt
  val fern:        Int = 0x4f7942ff
  val moss:        Int = 0x8b8b00ff.toInt
  val celeste:     Int = 0xb2ffff.toInt
  val sage:        Int = 0xb2ac88ff.toInt
  val pine:        Int = 0x01796fff
  val coral:       Int = 0xff7f50ff.toInt
  val apricot:     Int = 0xfbceb1ff.toInt
  val sand:        Int = 0xc2b280ff.toInt
  val wheat:       Int = 0xf5deb3ff.toInt
  val sepia:       Int = 0x704214ff
  val charcoal:    Int = 0x36454fff

  // libGDX-compatible ALL_CAPS colors
  val WHITE:      Int = 0xffffffff
  val LIGHT_GRAY: Int = 0xbfbfbfff.toInt
  val GRAY:       Int = 0x7f7f7fff
  val DARK_GRAY:  Int = 0x3f3f3fff
  val BLACK:      Int = 0x000000ff
  val CLEAR:      Int = 0x00000000
  val BLUE:       Int = 0x0000ffff
  val NAVY:       Int = 0x000080ff
  val ROYAL:      Int = 0x4169e1ff
  val SLATE:      Int = 0x708090ff
  val SKY:        Int = 0x87ceebff.toInt
  val CYAN:       Int = 0x00ffffff
  val TEAL:       Int = 0x007f7fff
  val GREEN:      Int = 0x00ff00ff
  val CHARTREUSE: Int = 0x7fff00ff
  val LIME:       Int = 0x32cd32ff
  val FOREST:     Int = 0x228b22ff
  val OLIVE:      Int = 0x6b8e23ff
  val YELLOW:     Int = 0xffff00ff.toInt
  val GOLD:       Int = 0xffd700ff.toInt
  val GOLDENROD:  Int = 0xdaa520ff.toInt
  val ORANGE:     Int = 0xffa500ff.toInt
  val BROWN:      Int = 0x8b4513ff.toInt
  val TAN:        Int = 0xd2b48cff.toInt
  val FIREBRICK:  Int = 0xb22222ff.toInt
  val RED:        Int = 0xff0000ff.toInt
  val SCARLET:    Int = 0xff341cff.toInt
  val CORAL:      Int = 0xff7f50ff.toInt
  val SALMON:     Int = 0xfa8072ff.toInt
  val PINK:       Int = 0xff69b4ff.toInt
  val MAGENTA:    Int = 0xff00ffff.toInt
  val PURPLE:     Int = 0xa020f0ff.toInt
  val VIOLET:     Int = 0xee82eeff.toInt
  val MAROON:     Int = 0xb03060ff.toInt

  // Initialize color maps
  locally {
    val entries: Seq[(String, Int)] = Seq(
      "transparent" -> transparent,
      "black" -> black,
      "gray" -> gray,
      "silver" -> silver,
      "white" -> white,
      "red" -> red,
      "orange" -> orange,
      "yellow" -> yellow,
      "green" -> green,
      "blue" -> blue,
      "indigo" -> indigo,
      "violet" -> violet,
      "purple" -> purple,
      "brown" -> brown,
      "salmon" -> salmon,
      "chocolate" -> chocolate,
      "tan" -> tan,
      "saffron" -> saffron,
      "butter" -> butter,
      "chartreuse" -> chartreuse,
      "cactus" -> cactus,
      "lime" -> lime,
      "jade" -> jade,
      "cyan" -> cyan,
      "teal" -> teal,
      "sky" -> sky,
      "cobalt" -> cobalt,
      "denim" -> denim,
      "navy" -> navy,
      "lavender" -> lavender,
      "plum" -> plum,
      "magenta" -> magenta,
      "puce" -> puce,
      "rose" -> rose,
      "raspberry" -> raspberry,
      "peach" -> peach,
      "mauve" -> mauve,
      "ember" -> ember,
      "crimson" -> crimson,
      "fern" -> fern,
      "moss" -> moss,
      "celeste" -> celeste,
      "sage" -> sage,
      "pine" -> pine,
      "coral" -> coral,
      "apricot" -> apricot,
      "sand" -> sand,
      "wheat" -> wheat,
      "sepia" -> sepia,
      "charcoal" -> charcoal,
      "WHITE" -> WHITE,
      "LIGHT_GRAY" -> LIGHT_GRAY,
      "GRAY" -> GRAY,
      "DARK_GRAY" -> DARK_GRAY,
      "BLACK" -> BLACK,
      "CLEAR" -> CLEAR,
      "BLUE" -> BLUE,
      "NAVY" -> NAVY,
      "ROYAL" -> ROYAL,
      "SLATE" -> SLATE,
      "SKY" -> SKY,
      "CYAN" -> CYAN,
      "TEAL" -> TEAL,
      "GREEN" -> GREEN,
      "CHARTREUSE" -> CHARTREUSE,
      "LIME" -> LIME,
      "FOREST" -> FOREST,
      "OLIVE" -> OLIVE,
      "YELLOW" -> YELLOW,
      "GOLD" -> GOLD,
      "GOLDENROD" -> GOLDENROD,
      "ORANGE" -> ORANGE,
      "BROWN" -> BROWN,
      "TAN" -> TAN,
      "FIREBRICK" -> FIREBRICK,
      "RED" -> RED,
      "SCARLET" -> SCARLET,
      "CORAL" -> CORAL,
      "SALMON" -> SALMON,
      "PINK" -> PINK,
      "MAGENTA" -> MAGENTA,
      "PURPLE" -> PURPLE,
      "VIOLET" -> VIOLET,
      "MAROON" -> MAROON
    )
    for ((name, color) <- entries) {
      NAMED.put(name, color)
      LIST += color
    }

    ALIASES.put("grey", gray)
    ALIASES.put("gold", saffron)
    ALIASES.put("puce", mauve)
    ALIASES.put("sand", tan)
    ALIASES.put("skin", peach)
    ALIASES.put("coral", salmon)
    ALIASES.put("azure", sky)
    ALIASES.put("ocean", teal)
    ALIASES.put("sapphire", cobalt)
    NAMED ++= ALIASES

    NAMES ++= NAMED.keys
    NAMES.sortInPlace()
  }

  /** Modifies the Palette by adding a color with its name. */
  def addColor(name: String, rgba8888: Int): Boolean =
    if (NAMED.contains(name)) false
    else {
      NAMED.put(name, rgba8888)
      LIST += rgba8888
      NAMES += name
      NAMES.sortInPlace()
      true
    }
}
