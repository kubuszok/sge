/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 105
 * Covenant-baseline-methods: SkinLoader,bonesCount,floatBuffer,getMaxBones,i,ibms,joints,load,maxBones
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package shared
package scene

import java.nio.FloatBuffer
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import sge.graphics.g3d.model.{ Node, NodePart }
import sge.gltf.data.scene.{ GLTFNode, GLTFSkin }
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.data.DataResolver
import sge.math.Matrix4
import sge.utils.{ ArrayMap, DynamicArray, Nullable }

class SkinLoader {

  private var maxBones: Int = 0

  def load(
    glSkins:      Nullable[ArrayBuffer[GLTFSkin]],
    glNodes:      Nullable[ArrayBuffer[GLTFNode]],
    nodeResolver: NodeResolver,
    dataResolver: DataResolver
  ): Unit =
    glNodes.foreach { nodes =>
      var i = 0
      while (i < nodes.size) {
        val glNode = nodes(i)
        glNode.skin.foreach { skinIdx =>
          glSkins.foreach { skins =>
            val glSkin = skins(skinIdx)
            nodeResolver.get(i).foreach { node =>
              load(glSkin, glNode, node, nodeResolver, dataResolver)
            }
          }
        }
        i += 1
      }
    }

  private def load(
    glSkin:       GLTFSkin,
    glNode:       GLTFNode,
    node:         Node,
    nodeResolver: NodeResolver,
    dataResolver: DataResolver
  ): Unit = {
    val ibms   = ArrayBuffer[Matrix4]()
    val joints = ArrayBuffer[Int]()

    val bonesCount = glSkin.joints.get.size
    maxBones = scala.math.max(maxBones, bonesCount)

    val floatBuffer: FloatBuffer = dataResolver.getBufferFloat(glSkin.inverseBindMatrices.get)

    var i = 0
    while (i < bonesCount) {
      val matrixData = new Array[Float](16)
      floatBuffer.get(matrixData)
      ibms += new Matrix4(matrixData)
      i += 1
    }
    joints ++= glSkin.joints.get

    if (ibms.nonEmpty) {
      i = 0
      while (i < node.parts.size) {
        var nodePart = node.parts(i)
        if (nodePart.bones.isDefined) {
          // special case when the same mesh is used by several skins.
          // in this case, we need to clone the node part
          val newNodePart = new NodePart()
          newNodePart.material = nodePart.material
          newNodePart.meshPart = nodePart.meshPart
          node.parts(i) = newNodePart
          nodePart = newNodePart
        }
        val bonesArray   = new Array[Matrix4](ibms.size)
        val invBoneBinds = ArrayMap[Node, Matrix4]()
        var n            = 0
        while (n < joints.size) {
          bonesArray(n) = new Matrix4().idt()
          val nodeIndex = joints(n)
          val key       = nodeResolver.get(nodeIndex)
          if (key.isEmpty) throw new GLTFIllegalException("node not found for bone: " + nodeIndex)
          invBoneBinds.put(key.get, ibms(n))
          n += 1
        }
        nodePart.bones = bonesArray
        nodePart.invBoneBindTransforms = invBoneBinds
        i += 1
      }
    }
  }

  def getMaxBones: Int = maxBones
}
