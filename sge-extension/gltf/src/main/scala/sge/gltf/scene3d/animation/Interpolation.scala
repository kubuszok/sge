/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/loaders/shared/animation/Interpolation.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 15
 * Covenant-baseline-methods: Interpolation
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package animation

enum Interpolation extends java.lang.Enum[Interpolation] {
  case LINEAR, STEP, CUBICSPLINE
}
