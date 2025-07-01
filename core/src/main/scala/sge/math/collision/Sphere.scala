package sge
package math
package collision

/** Encapsulates a 3D sphere with a center and a radius
  *
  * @author
  *   badlogicgames@gmail.com (original implementation)
  */
class Sphere(val center: Vector3, var radius: Float) {

  def this(centerX: Float, centerY: Float, centerZ: Float, radius: Float) = {
    this(new Vector3(centerX, centerY, centerZ), radius)
  }

  /** @param sphere
    *   the other sphere
    * @return
    *   whether this and the other sphere overlap
    */
  def overlaps(sphere: Sphere): Boolean =
    center.distanceSq(sphere.center) < (radius + sphere.radius) * (radius + sphere.radius)

  override def hashCode(): Int = {
    val prime  = 71;
    var result = 1;
    result = prime * result + center.hashCode();
    result = prime * result + java.lang.Float.floatToRawIntBits(radius);
    result;
  }

  override def equals(obj: Any): Boolean = obj match {
    case s: Sphere => this.radius == s.radius && this.center.equals(s.center);
    case _ => false;
  }

  def volume(): Float =
    Sphere.PI_4_3 * radius * radius * radius;

  def surfaceArea(): Float =
    4 * MathUtils.PI * radius * radius;
}

object Sphere {
  private val PI_4_3 = MathUtils.PI * 4f / 3f;
}
