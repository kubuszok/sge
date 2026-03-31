/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/CubicVector3.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * In Java, CubicVector3 extends Vector3 to add tangent data for cubic spline interpolation.
 * In SGE, Vector3 is sealed, so we use a wrapper class that holds the tangent data.
 * Animation code casts keyframe values to CubicVector3 to access tangents.
 *
 * NOTE: This class wraps a Vector3 value. Callers must cast the Vector3 keyframe value
 * to access tangent data through this type.
 */
package sge
package gltf
package scene3d
package model

import sge.math.Vector3

/** Wrapper holding cubic spline tangent data alongside a Vector3 value. Used as keyframe values in animations with CUBICSPLINE interpolation.
  */
final case class CubicVector3(
  value:      Vector3 = Vector3(),
  tangentIn:  Vector3 = Vector3(),
  tangentOut: Vector3 = Vector3()
)
