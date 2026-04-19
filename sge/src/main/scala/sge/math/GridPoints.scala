/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/GridPoint2.java
 *                  com/badlogic/gdx/math/GridPoint3.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: GridPoint2 and GridPoint3 as final case class (not plain class); combines
 *     GridPoint2.java + GridPoint3.java into single file
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — GridPoint2: set, dst2, dst, add, sub all ported. cpy() provided by
 * case class copy(). equals/hashCode/toString provided by case class.
 * GridPoint3: same — all methods ported. INTENTIONAL: final case class (not plain class).
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 325
 * Covenant-baseline-methods: GridPoint2,GridPoint3,add,dst,dst2,set,sub,xd,yd,zd
 * Covenant-source-reference: com/badlogic/gdx/math/GridPoint2.java
 * Covenant-verified: 2026-04-19
 */
package sge
package math

/** A point in a 2D grid, with integer x and y coordinates.
  *
  * @author
  *   badlogic (original implementation)
  */
final case class GridPoint2(var x: Int = 0, var y: Int = 0) {

  /** Sets the coordinates of this 2D grid point to that of another.
    *
    * @param point
    *   The 2D grid point to copy the coordinates of.
    *
    * @return
    *   this 2D grid point for chaining.
    */
  def set(point: GridPoint2): GridPoint2 = {
    this.x = point.x
    this.y = point.y
    this
  }

  /** Sets the coordinates of this 2D grid point.
    *
    * @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    *
    * @return
    *   this 2D grid point for chaining.
    */
  def set(x: Int, y: Int): GridPoint2 = {
    this.x = x
    this.y = y
    this
  }

  /** @param other
    *   The other point
    * @return
    *   the squared distance between this point and the other point.
    */
  def dst2(other: GridPoint2): Float = {
    val xd = other.x - x
    val yd = other.y - y
    xd.toFloat * xd + yd.toFloat * yd
  }

  /** @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @return
    *   the squared distance between this point and the other point.
    */
  def dst2(x: Int, y: Int): Float = {
    val xd = x - this.x
    val yd = y - this.y
    xd.toFloat * xd + yd.toFloat * yd
  }

  /** @param other
    *   The other point
    * @return
    *   the distance between this point and the other vector.
    */
  def dst(other: GridPoint2): Float = {
    val xd = other.x - x
    val yd = other.y - y
    scala.math.sqrt(xd.toFloat * xd + yd.toFloat * yd).toFloat
  }

  /** @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @return
    *   the distance between this point and the other point.
    */
  def dst(x: Int, y: Int): Float = {
    val xd = x - this.x
    val yd = y - this.y
    scala.math.sqrt(xd.toFloat * xd + yd.toFloat * yd).toFloat
  }

  /** Adds another 2D grid point to this point.
    *
    * @param other
    *   The other point
    * @return
    *   this 2d grid point for chaining.
    */
  def add(other: GridPoint2): GridPoint2 = {
    x += other.x
    y += other.y
    this
  }

  /** Adds another 2D grid point to this point.
    *
    * @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @return
    *   this 2d grid point for chaining.
    */
  def add(x: Int, y: Int): GridPoint2 = {
    this.x += x
    this.y += y
    this
  }

  /** Subtracts another 2D grid point from this point.
    *
    * @param other
    *   The other point
    * @return
    *   this 2d grid point for chaining.
    */
  def sub(other: GridPoint2): GridPoint2 = {
    x -= other.x
    y -= other.y
    this
  }

  /** Subtracts another 2D grid point from this point.
    *
    * @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @return
    *   this 2d grid point for chaining.
    */
  def sub(x: Int, y: Int): GridPoint2 = {
    this.x -= x
    this.y -= y
    this
  }
}

/** A point in a 3D grid, with integer x, y and z coordinates.
  *
  * @author
  *   badlogic (original implementation)
  */
final case class GridPoint3(var x: Int = 0, var y: Int = 0, var z: Int = 0) {

  /** Sets the coordinates of this 3D grid point to that of another.
    *
    * @param point
    *   The 3D grid point to copy coordinates of.
    *
    * @return
    *   this GridPoint3 for chaining.
    */
  def set(point: GridPoint3): GridPoint3 = {
    this.x = point.x
    this.y = point.y
    this.z = point.z
    this
  }

  /** Sets the coordinates of this GridPoint3D.
    *
    * @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    * @param z
    *   Z coordinate
    *
    * @return
    *   this GridPoint3D for chaining.
    */
  def set(x: Int, y: Int, z: Int): GridPoint3 = {
    this.x = x
    this.y = y
    this.z = z
    this
  }

  /** @param other
    *   The other point
    * @return
    *   the squared distance between this point and the other point.
    */
  def dst2(other: GridPoint3): Float = {
    val xd = other.x - x
    val yd = other.y - y
    val zd = other.z - z
    xd.toFloat * xd + yd.toFloat * yd + zd.toFloat * zd
  }

  /** @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @param z
    *   The z-coordinate of the other point
    * @return
    *   the squared distance between this point and the other point.
    */
  def dst2(x: Int, y: Int, z: Int): Float = {
    val xd = x - this.x
    val yd = y - this.y
    val zd = z - this.z
    xd.toFloat * xd + yd.toFloat * yd + zd.toFloat * zd
  }

  /** @param other
    *   The other point
    * @return
    *   the distance between this point and the other vector.
    */
  def dst(other: GridPoint3): Float = {
    val xd = other.x - x
    val yd = other.y - y
    val zd = other.z - z
    scala.math.sqrt(xd.toFloat * xd + yd.toFloat * yd + zd.toFloat * zd).toFloat
  }

  /** @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @param z
    *   The z-coordinate of the other point
    * @return
    *   the distance between this point and the other point.
    */
  def dst(x: Int, y: Int, z: Int): Float = {
    val xd = x - this.x
    val yd = y - this.y
    val zd = z - this.z
    scala.math.sqrt(xd.toFloat * xd + yd.toFloat * yd + zd.toFloat * zd).toFloat
  }

  /** Adds another 3D grid point to this point.
    *
    * @param other
    *   The other point
    * @return
    *   this 3d grid point for chaining.
    */
  def add(other: GridPoint3): GridPoint3 = {
    x += other.x
    y += other.y
    z += other.z
    this
  }

  /** Adds another 3D grid point to this point.
    *
    * @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @param z
    *   The z-coordinate of the other point
    * @return
    *   this 3d grid point for chaining.
    */
  def add(x: Int, y: Int, z: Int): GridPoint3 = {
    this.x += x
    this.y += y
    this.z += z
    this
  }

  /** Subtracts another 3D grid point from this point.
    *
    * @param other
    *   The other point
    * @return
    *   this 3d grid point for chaining.
    */
  def sub(other: GridPoint3): GridPoint3 = {
    x -= other.x
    y -= other.y
    z -= other.z
    this
  }

  /** Subtracts another 3D grid point from this point.
    *
    * @param x
    *   The x-coordinate of the other point
    * @param y
    *   The y-coordinate of the other point
    * @param z
    *   The z-coordinate of the other point
    * @return
    *   this 3d grid point for chaining.
    */
  def sub(x: Int, y: Int, z: Int): GridPoint3 = {
    this.x -= x
    this.y -= y
    this.z -= z
    this
  }
}
