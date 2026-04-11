/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import java.nio.FloatBuffer

import scala.collection.mutable.ArrayBuffer

import sge.graphics.g3d.model.{ Node, NodePart }
import sge.math.Matrix4
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.data.scene.{ GLTFNode, GLTFSkin }
import sge.gltf.loaders.shared.GLTFTypes
import sge.utils.Nullable

private[exporters] class GLTFSkinExporter(private val base: GLTFExporter) {

  def exportSkins(): Unit = {
    // note that node.skeleton is not mandatory, it's set on root node of armature

    var i = 0
    while (i < base.nodeMapping.size) {
      val node = base.nodeMapping(i)
      val glNode = base.root.nodes.get(i)

      // skip already exported skins (in case of multiple scene)
      if (glNode.skin.isDefined) {
        // already exported, skip
      } else {
        if (node.parts != null) { // @nowarn — DynamicArray field may be null from model loader
          var pIdx = 0
          while (pIdx < node.parts.size) {
            val part = node.parts(pIdx)
            part.invBoneBindTransforms.foreach { invBoneBinds =>
              // here we can create a new skin
              val skin = new GLTFSkin()
              if (base.root.skins.isEmpty) base.root.skins = Nullable(ArrayBuffer[GLTFSkin]())
              base.root.skins.get += skin
              glNode.skin = Nullable(base.root.skins.get.size - 1)

              skin.joints = Nullable(ArrayBuffer[Int]())

              val matrixBuffer = base.binManager.beginFloats(invBoneBinds.size * 16)

              invBoneBinds.foreachEntry { (boneNode, matrix) =>
                val boneID = base.nodeMapping.indexOfByRef(boneNode)
                skin.joints.get += boneID
                matrixBuffer.put(matrix.values)
              }
              val accessor = base.obtainAccessor()
              accessor.bufferView = Nullable(base.binManager.end())
              accessor.`type` = Nullable(GLTFTypes.TYPE_MAT4)
              accessor.componentType = GLTFTypes.C_FLOAT
              accessor.count = invBoneBinds.size

              skin.inverseBindMatrices = Nullable(base.root.accessors.get.size - 1)
            }
            pIdx += 1
          }
        }
      }
      i += 1
    }
  }
}
