/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelMaterial.java
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
