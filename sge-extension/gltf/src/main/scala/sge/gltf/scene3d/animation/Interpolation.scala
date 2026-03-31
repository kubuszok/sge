/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/loaders/shared/animation/Interpolation.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package animation

enum Interpolation extends java.lang.Enum[Interpolation] {
  case LINEAR, STEP, CUBICSPLINE
}
