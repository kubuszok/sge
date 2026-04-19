/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/data/ModelData.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields and methods match Java source
 * - Array -> DynamicArray for collections (standard SGE collection mapping)
 * - GdxRuntimeException -> SgeError.InvalidInput in addMesh()
 * - String concatenation -> string interpolation in error message
 * - No API differences from Java source
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: ModelData,addMesh,animations,id,materials,meshes,nodes,version
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/data/ModelData.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model
package data

import sge.utils.DynamicArray

/** Returned by a ModelLoader, contains meshes, materials, nodes and animations. OpenGL resources like textures or vertex buffer objects are not stored. Instead, a ModelData instance needs to be
  * converted to a Model first.
  * @author
  *   badlogic
  */
class ModelData {
  var id:         String                       = scala.compiletime.uninitialized
  val version:    Array[Short]                 = new Array[Short](2)
  val meshes:     DynamicArray[ModelMesh]      = DynamicArray[ModelMesh]()
  val materials:  DynamicArray[ModelMaterial]  = DynamicArray[ModelMaterial]()
  val nodes:      DynamicArray[ModelNode]      = DynamicArray[ModelNode]()
  val animations: DynamicArray[ModelAnimation] = DynamicArray[ModelAnimation]()

  def addMesh(mesh: ModelMesh): Unit = {
    meshes.foreach { other =>
      if (other.id.equals(mesh.id)) {
        throw sge.utils.SgeError.InvalidInput(s"Mesh with id '${other.id}' already in model")
      }
    }
    meshes.add(mesh)
  }
}
