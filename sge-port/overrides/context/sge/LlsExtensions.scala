package sge

extension [A](da: lowlevel.util.DynamicArray[A]) {
  def length: Int = da.size
  /** sge keeps `first` on `DynamicArray`; the lls port renamed it to `head` (Scala convention) */
  inline def first: A = da.head
}
extension [A <: AnyRef](os: lowlevel.util.OrderedSet[A]) {
  /** sge keeps `first` on `OrderedSet`; the lls port renamed it to `head` */
  inline def first: A = os.head
}
extension (a: sge.scenes.scene2d.Actor) {
  def right: Float = a.x + a.width
  def top: Float = a.y + a.height
}
