/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Origin: SGE-original (UBJSON variant of G3dModelLoader for .g3db binary format)
 *   Convention: extends G3dModelLoader, overrides parseModel to read UBJSON via kindlings
 *   Idiom: split packages, (using Sge) context
 */
package sge
package graphics
package g3d
package loader

import sge.assets.loaders.FileHandleResolver
import sge.files.FileHandle
import sge.graphics.g3d.model.data.ModelData
import sge.utils.readUBJson

/** Loads G3D models from `.g3db` (Universal Binary JSON) files.
  *
  * Reuses all parsing logic from [[G3dModelLoader]] — only the deserialization format differs.
  */
class G3dBinaryModelLoader(resolver: FileHandleResolver)(using Sge) extends G3dModelLoader(resolver) {

  override def parseModel(handle: FileHandle): ModelData = {
    val json  = handle.readUBJson[G3dModelJson]
    val model = sge.graphics.g3d.model.data.ModelData()

    model.version(0) = json.version(0)
    model.version(1) = json.version(1)
    if (model.version(0) != G3dModelLoader.VERSION_HI || model.version(1) != G3dModelLoader.VERSION_LO)
      throw sge.utils.SgeError.InvalidInput("Model version not supported")

    model.id = json.id
    parseMeshes(model, json.meshes)
    parseMaterials(model, json.materials, handle.parent().path)
    parseNodes(model, json.nodes)
    parseAnimations(model, json.animations)
    model
  }
}
