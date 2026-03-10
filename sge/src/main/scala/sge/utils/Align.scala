/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Align.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `int` constants -> opaque type `Align`; static methods -> extension methods
 *   Convention: Java class with static int fields -> opaque type with extension methods; `toString(int)` -> `Show[Align]` type class; added bitwise operators and `isCenter`
 *   Idiom: split packages
 *   TODO: replace given Show[Align] with derived FastShowPretty[Align]
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Provides bit flag constants for alignment.
  *
  * @author
  *   Nathan Sweet (original implementation)
  */
opaque type Align = Int
object Align {

  given MkArray[Align] = MkArray.mkInt.asInstanceOf[MkArray[Align]]

  val center: Align = 1 << 0
  val top:    Align = 1 << 1
  val bottom: Align = 1 << 2
  val left:   Align = 1 << 3
  val right:  Align = 1 << 4

  val topLeft:     Align = top | left
  val topRight:    Align = top | right
  val bottomLeft:  Align = bottom | left
  val bottomRight: Align = bottom | right

  extension (a: Align) {
    inline def toInt: Int   = a
    def unary_~     : Align = ~a
    def |(b: Align):  Align = a | b
    def &(b: Align):  Align = a & b

    inline def isLeft:   Boolean = (a & left) != 0
    inline def isRight:  Boolean = (a & right) != 0
    inline def isTop:    Boolean = (a & top) != 0
    inline def isBottom: Boolean = (a & bottom) != 0

    inline def isCenterVertical:   Boolean = (a & top) == 0 && (a & bottom) == 0
    inline def isCenterHorizontal: Boolean = (a & left) == 0 && (a & right) == 0
    inline def isCenter:           Boolean = isCenterVertical && isCenterHorizontal
  }

  given Show[Align] with {
    extension (align: Align) {
      def show: String = {
        val buffer = new StringBuilder
        if (isTop(align)) buffer.append("top,")
        else if (isBottom(align)) buffer.append("bottom,")
        else buffer.append("center,")
        if (isLeft(align)) buffer.append("left")
        else if (isRight(align)) buffer.append("right")
        else buffer.append("center")
        buffer.toString()
      }
    }
  }
}
