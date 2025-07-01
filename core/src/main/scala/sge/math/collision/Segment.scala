package sge
package math
package collision

import sge.math.Vector3

/** A Segment is a line in 3-space having a starting and an ending position.
  *
  * @author
  *   mzechner (original implementation)
  */
class Segment(val a: Vector3 = new Vector3(), val b: Vector3 = new Vector3()) {

  /** Constructs a new Segment from the two points given.
    * @param aX
    *   the x-coordinate of the first point
    * @param aY
    *   the y-coordinate of the first point
    * @param aZ
    *   the z-coordinate of the first point
    * @param bX
    *   the x-coordinate of the second point
    * @param bY
    *   the y-coordinate of the second point
    * @param bZ
    *   the z-coordinate of the second point
    */
  def this(aX: Float, aY: Float, aZ: Float, bX: Float, bY: Float, bZ: Float) = {
    this(new Vector3(aX, aY, aZ), new Vector3(bX, bY, bZ))
  }

  def len(): Float = a.distance(b)

  def len2(): Float = a.distanceSq(b)

  override def equals(o: Any): Boolean = o match {
    case s: Segment => this.a.equals(s.a) && this.b.equals(s.b)
    case _ => false
  }

  override def hashCode(): Int = {
    val prime  = 71
    var result = 1
    result = prime * result + this.a.hashCode()
    result = prime * result + this.b.hashCode()
    result
  }
}
