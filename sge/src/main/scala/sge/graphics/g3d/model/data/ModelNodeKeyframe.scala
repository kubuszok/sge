/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNodeKeyframe.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source
 * - Java `value = null` -> Nullable.empty (correct)
 * - keytime initialized to 0f (matches Java default)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 31
 * Covenant-baseline-methods: ModelNodeKeyframe,keytime,value
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelNodeKeyframe.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package model
package data

import sge.utils.Nullable

class ModelNodeKeyframe[T] {

  /** the timestamp of the keyframe in seconds * */
  var keytime: Float = 0f

  /** the value of the keyframe */
  var value: Nullable[T] = Nullable.empty
}
