/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelAnimation.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source
 * - id uses scala.compiletime.uninitialized (Java null default)
 * - Array -> DynamicArray for nodeAnimations (standard SGE collection mapping)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 */
package sge
package graphics
package g3d
package model
package data

import sge.utils.DynamicArray

class ModelAnimation {
  var id:             String                           = scala.compiletime.uninitialized
  var nodeAnimations: DynamicArray[ModelNodeAnimation] = DynamicArray[ModelNodeAnimation]()
}
