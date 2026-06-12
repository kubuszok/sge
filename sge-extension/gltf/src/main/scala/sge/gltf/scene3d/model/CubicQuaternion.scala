/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/CubicQuaternion.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * In Java, CubicQuaternion extends Quaternion (CubicQuaternion.java:6) adding
 * tangentIn/tangentOut Quaternion fields (CubicQuaternion.java:8-9). In SGE,
 * Quaternion is a plain (non-final) class — exactly like libGDX — so we
 * restore the original subtype relationship here, matching the
 * CubicWeightVector extends WeightVector precedent (CubicWeightVector.scala:20).
 * This makes the NodeKeyframe[Quaternion] keyframe value genuinely a
 * Quaternion (so AnimationLoader stores it with no cast) and lets
 * AnimationControllerHack downcast it back to CubicQuaternion to read the
 * tangents during cubic-spline evaluation, just as the Java does
 * (AnimationControllerHack.java:287-290).
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 42
 * Covenant-baseline-methods: CubicQuaternion,tangentIn,tangentOut
 * Covenant-source-reference: net/mgsx/gltf/scene3d/model/CubicQuaternion.java
 * Covenant-verified: 2026-06-12
 */
package sge
package gltf
package scene3d
package model

import sge.math.Quaternion

/** A Quaternion carrying cubic spline tangent data. Used as keyframe values in animations with CUBICSPLINE interpolation.
  *
  * Mirrors net.mgsx.gltf.scene3d.model.CubicQuaternion: `extends Quaternion` with `final` tangentIn/tangentOut Quaternion fields (CubicQuaternion.java:6-9). The quaternion's own (x, y, z, w) hold the
  * keyframe value, so a CubicQuaternion can be stored directly in a NodeKeyframe[Quaternion].
  */
class CubicQuaternion extends Quaternion {

  val tangentIn:  Quaternion = Quaternion()
  val tangentOut: Quaternion = Quaternion()
}
