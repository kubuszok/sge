/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelMaterial.java
 * Original authors: Mario Zechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java source
 * - Java inner enum MaterialType -> Scala 3 enum in companion object (correct)
 * - Field renamed: Java `type` (reserved in Scala) -> `materialType`
 * - Java Array<ModelTexture> -> DynamicArray[ModelTexture] for textures
 * - Color fields use scala.compiletime.uninitialized (Java null defaults)
 * - opacity initialized to 1.0f (matches Java)
 * - Status: minor_issues (field rename: type -> materialType)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 47
 * Covenant-baseline-methods: MaterialType,ModelMaterial,ambient,diffuse,emissive,id,materialType,opacity,reflection,shininess,specular,textures
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelMaterial.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model
package data

import sge.utils.DynamicArray

class ModelMaterial {
  var id: String = scala.compiletime.uninitialized

  var materialType: ModelMaterial.MaterialType = scala.compiletime.uninitialized

  var ambient:    Color = scala.compiletime.uninitialized
  var diffuse:    Color = scala.compiletime.uninitialized
  var specular:   Color = scala.compiletime.uninitialized
  var emissive:   Color = scala.compiletime.uninitialized
  var reflection: Color = scala.compiletime.uninitialized

  var shininess: Float = 0f
  var opacity:   Float = 1.0f

  var textures: DynamicArray[ModelTexture] = scala.compiletime.uninitialized
}

object ModelMaterial {
  enum MaterialType {
    case Lambert, Phong
  }
}
