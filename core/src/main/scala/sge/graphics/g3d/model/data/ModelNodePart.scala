/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelNodePart.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package model
package data

import sge.math.Matrix4
import sge.utils.ArrayMap

class ModelNodePart {
  var materialId: String                    = scala.compiletime.uninitialized
  var meshPartId: String                    = scala.compiletime.uninitialized
  var bones:      ArrayMap[String, Matrix4] = scala.compiletime.uninitialized
  var uvMapping:  Array[Array[Int]]         = scala.compiletime.uninitialized
}
