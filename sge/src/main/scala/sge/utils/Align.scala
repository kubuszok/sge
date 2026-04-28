/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Align.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `int` constants -> opaque type `Align`; static methods -> extension methods
 *   Convention: Java class with static int fields -> opaque type with extension methods;
 *     `toString(int)` -> `align.show` extension + `Show[Align]` type class;
 *     added bitwise operators and `isCenter`
 *   Idiom: split packages
 *   TODO: replace given Show[Align] with derived FastShowPretty[Align]
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 84
 * Covenant-baseline-methods: Align,bottom,bottomLeft,bottomRight,buffer,center,isBottom,isCenter,isCenterHorizontal,isCenterVertical,isLeft,isRight,isTop,left,right,show,showImpl,toInt,top,topLeft,topRight,unary_
 * Covenant-source-reference: com/badlogic/gdx/utils/Align.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: aa0cbc52417eb705004c0a94361b6777c1bf8636
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

  /** Formats alignment flags as a human-readable string (e.g. "top,left").
    *
    * This is a private helper; use `align.show` (extension) or the `Show[Align]` type class. Replaces the original `Align.toString(int)` static method.
    */
  private def showImpl(a: Int): String = {
    val buffer = new StringBuilder(13)
    if ((a & top) != 0) buffer.append("top,")
    else if ((a & bottom) != 0) buffer.append("bottom,")
    else buffer.append("center,")
    if ((a & left) != 0) buffer.append("left")
    else if ((a & right) != 0) buffer.append("right")
    else buffer.append("center")
    buffer.toString()
  }

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

    /** Returns a human-readable string for this alignment, e.g. "top,left" or "center,center".
      *
      * Compatibility note: replaces the original `Align.toString(int)` static method.
      */
    def show: String = showImpl(a)
  }

  given Show[Align] with {
    extension (align: Align) {
      def show: String = showImpl(align)
    }
  }
}
