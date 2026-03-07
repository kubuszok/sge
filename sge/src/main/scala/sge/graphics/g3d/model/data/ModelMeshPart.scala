/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelMeshPart.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source exactly
 * - id/indices use scala.compiletime.uninitialized (Java null defaults)
 * - primitiveType initialized to PrimitiveMode(0) (matches Java default)
 * - Convention: typed GL enums (PrimitiveMode)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 */
package sge
package graphics
package g3d
package model
package data

import sge.graphics.PrimitiveMode

class ModelMeshPart {
  var id:            String        = scala.compiletime.uninitialized
  var indices:       Array[Short]  = scala.compiletime.uninitialized
  var primitiveType: PrimitiveMode = PrimitiveMode(0)
}
