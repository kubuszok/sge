/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/collision/Segment.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: dst -> distance, dst2 -> distanceSq
 *   Convention: Serializable dropped; serialVersionUID dropped;
 *     primary constructor takes Vector3 refs with defaults (Java has no no-arg ctor);
 *     Java 2-arg (Vector3,Vector3) ctor copies via set(); Scala stores refs directly
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 63
 * Covenant-baseline-methods: Segment,equals,hashCode,len,len2,prime,result,this
 * Covenant-source-reference: com/badlogic/gdx/math/collision/Segment.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math
package collision

import sge.math.Vector3

/** A Segment is a line in 3-space having a starting and an ending position.
  *
  * @author
  *   mzechner (original implementation)
  */
class Segment(val a: Vector3 = Vector3(), val b: Vector3 = Vector3()) {

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
  def this(aX: Float, aY: Float, aZ: Float, bX: Float, bY: Float, bZ: Float) =
    this(Vector3(aX, aY, aZ), Vector3(bX, bY, bZ))

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
