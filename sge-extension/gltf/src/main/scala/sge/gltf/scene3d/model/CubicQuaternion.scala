/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/CubicQuaternion.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * In Java, CubicQuaternion extends Quaternion. In SGE, Quaternion is sealed,
 * so we use a wrapper case class.
 */
package sge
package gltf
package scene3d
package model

import sge.math.Quaternion

/** Wrapper holding cubic spline tangent data alongside a Quaternion value. Used as keyframe values in animations with CUBICSPLINE interpolation.
  */
final case class CubicQuaternion(
  value:      Quaternion = Quaternion(),
  tangentIn:  Quaternion = Quaternion(),
  tangentOut: Quaternion = Quaternion()
)
