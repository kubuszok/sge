/*
 * Covenant: full-port
 */
package sge

extension [A](da: lowlevel.util.DynamicArray[A]) {
  def length: Int = da.size
}
extension (a: sge.scenes.scene2d.Actor) {
  def right: Float = a.x + a.width
  def top:   Float = a.y + a.height
}
