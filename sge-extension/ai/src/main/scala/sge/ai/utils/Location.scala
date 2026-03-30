/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/Location.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `Vector` -> `sge.math.Vector`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package utils

import sge.math.Vector

/** Represents any game object having a position and an orientation.
  *
  * @tparam T
  *   Type of vector, either 2D or 3D, implementing the [[Vector]] trait
  *
  * @author
  *   davebaol (original implementation)
  */
trait Location[T <: Vector[T]] {

  /** Returns the vector indicating the position of this location. */
  def position: T

  /** Returns the float value indicating the orientation of this location. The orientation is the angle in radians representing the direction that this location is facing.
    */
  def orientation: Float

  /** Sets the orientation of this location, i.e. the angle in radians representing the direction that this location is facing.
    * @param orientation
    *   the orientation in radians
    */
  def orientation_=(orientation: Float): Unit

  /** Returns the angle in radians pointing along the specified vector.
    * @param vector
    *   the vector
    */
  def vectorToAngle(vector: T): Float

  /** Returns the unit vector in the direction of the specified angle expressed in radians.
    * @param outVector
    *   the output vector.
    * @param angle
    *   the angle in radians.
    * @return
    *   the output vector for chaining.
    */
  def angleToVector(outVector: T, angle: Float): T

  /** Creates a new location.
    *
    * This method is used internally to instantiate locations of the correct type parameter `T`. This technique keeps the API simple and makes the API easier to use with the GWT backend because it
    * avoids the use of reflection.
    * @return
    *   the newly created location.
    */
  def newLocation(): Location[T]
}
