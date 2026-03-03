/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNodeKeyframe.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source
 * - Java `value = null` -> Nullable.empty (correct)
 * - keytime initialized to 0f (matches Java default)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
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
