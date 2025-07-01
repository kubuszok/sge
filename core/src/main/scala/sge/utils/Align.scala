package sge
package utils

/** Provides bit flag constants for alignment.
  *
  * @author
  *   Nathan Sweet (original implementation)
  */
opaque type Align = Int
object Align {

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
    def |(b: Align): Align = a | b
    def &(b: Align): Align = a & b

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
