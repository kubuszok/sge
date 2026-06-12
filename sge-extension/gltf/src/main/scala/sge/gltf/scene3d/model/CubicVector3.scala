/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/CubicVector3.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * In Java, CubicVector3 extends Vector3 (CubicVector3.java:6) adding
 * tangentIn/tangentOut Vector3 fields (CubicVector3.java:8-9), and the cubic
 * keyframe value IS the CubicVector3 (its own x/y/z are the value).
 *
 * In SGE, Vector3 is a `final case class` in sge.math (Vectors.scala:605) and
 * its `Vector` super-trait is `sealed` (Vectors.scala:44), so — unlike the
 * Quaternion/CubicQuaternion and WeightVector/CubicWeightVector pairs, whose
 * bases are plain classes — CubicVector3 cannot subtype Vector3. We therefore
 * keep the original Java semantics through composition: a CubicVector3 holds
 * the keyframe `value` Vector3 plus the two tangents, and NodeAnimationHack
 * stores these cubic objects in a parallel array (translationCubic /
 * scalingCubic) index-aligned with the NodeKeyframe[Vector3] arrays. The
 * keyframe slot still carries the plain `value` Vector3 (so STEP/LINEAR and
 * t==anchor evaluation read it directly, matching Java's `(Vector3)value`
 * reads at AnimationControllerHack.java:184/188/207), while CUBICSPLINE
 * evaluation recovers `value`/`tangentOut`/`tangentIn` from the parallel
 * array, reproducing the Java downcast at AnimationControllerHack.java:202-205.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: CubicVector3
 * Covenant-source-reference: net/mgsx/gltf/scene3d/model/CubicVector3.java
 * Covenant-verified: 2026-06-12
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
