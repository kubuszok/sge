/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelMeshPart.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source exactly
 * - id/indices use scala.compiletime.uninitialized (Java null defaults)
 * - primitiveType initialized to 0 (matches Java default)
 * - No methods in Java source, none in Scala — pure data class
 * - Status: pass
 */
package sge
package graphics
package g3d
package model
package data

class ModelMeshPart {
  var id:            String       = scala.compiletime.uninitialized
  var indices:       Array[Short] = scala.compiletime.uninitialized
  var primitiveType: Int          = 0
}
