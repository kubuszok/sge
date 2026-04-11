/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import java.nio.{ FloatBuffer, ShortBuffer }

import scala.collection.mutable.{ ArrayBuffer, HashMap }

import sge.graphics.{ GL20, Mesh, VertexAttribute }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.model.MeshPart
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.data.geometry.{ GLTFMorphTarget, GLTFPrimitive }
import sge.gltf.loaders.exceptions.GLTFUnsupportedException
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.scene3d.attributes.PBRVertexAttributes
import sge.utils.Nullable

private[exporters] class GLTFMeshExporter(private val base: GLTFExporter) {

  private val layouts: HashMap[Mesh, GLTFPrimitive] = HashMap.empty

  def exportMeshPart(meshPart: MeshPart): GLTFPrimitive = {
    val mesh = meshPart.mesh
    val primitive = new GLTFPrimitive()
    primitive.attributes = Nullable(HashMap[String, Int]())
    primitive.mode = GLTFMeshExporter.mapPrimitiveMode(meshPart.primitiveType.toInt)

    layouts.get(mesh) match {
      case Some(layout) =>
        copyLayout(primitive, layout)
      case scala.None =>
        layouts.put(mesh, primitive)
        exportMesh(primitive, mesh)
    }

    // mesh may not have indices
    if (mesh.numIndices > 0) {
      val outBuffer = base.binManager.beginShorts(meshPart.size)
      val inBuffer = mesh.getIndicesBuffer(false)
      if (meshPart.offset == 0 && meshPart.size == mesh.numIndices) {
        outBuffer.put(mesh.getIndicesBuffer(false))
      } else {
        val localIndices = new Array[Short](meshPart.size)
        inBuffer.position(meshPart.offset)
        inBuffer.get(localIndices)
        outBuffer.put(localIndices)
      }
      inBuffer.rewind()

      val accessor = base.obtainAccessor()
      accessor.`type` = Nullable(GLTFTypes.TYPE_SCALAR)
      accessor.componentType = GLTFTypes.C_USHORT
      accessor.count = meshPart.size
      accessor.bufferView = Nullable(base.binManager.end())

      primitive.indices = Nullable(base.root.accessors.get.size - 1)
    }

    primitive
  }

  private def copyLayout(primitive: GLTFPrimitive, layout: GLTFPrimitive): Unit = {
    layout.attributes.foreach { la =>
      primitive.attributes.foreach { pa =>
        pa ++= la
      }
    }
    layout.targets.foreach { lt =>
      val newTargets = ArrayBuffer[GLTFMorphTarget]()
      newTargets ++= lt
      primitive.targets = Nullable(newTargets)
    }
  }

  private def exportMesh(primitive: GLTFPrimitive, mesh: Mesh): Unit = {
    val vertices = mesh.getVerticesBuffer(false)

    val boneWeightsBuffers = ArrayBuffer[Nullable[FloatBuffer]]()
    val boneJointsBuffers = ArrayBuffer[Nullable[ShortBuffer]]() // TODO short or byte (for small amount of bones)

    // split vertices individual attributes
    val stride = mesh.vertexAttributes.vertexSize / 4
    val numVertices = mesh.numVertices
    val vertexAttrs = mesh.vertexAttributes
    var attrIdx = 0
    while (attrIdx < vertexAttrs.size) {
      val a = vertexAttrs.get(attrIdx)
      var accessorType: String = ""
      var accessorComponentType: Int = GLTFTypes.C_FLOAT
      var useTargets: Boolean = false
      var shouldComputeBounds: Boolean = false
      var attributeKey: String = ""
      var skipAttribute: Boolean = false

      if (a.usage == Usage.Position) {
        attributeKey = "POSITION"
        accessorType = GLTFTypes.TYPE_VEC3
        shouldComputeBounds = true
      } else if (a.usage == Usage.Normal) {
        attributeKey = "NORMAL"
        accessorType = GLTFTypes.TYPE_VEC3
      } else if (a.usage == Usage.Tangent) {
        attributeKey = "TANGENT"
        accessorType = GLTFTypes.TYPE_VEC4
      } else if (a.usage == Usage.ColorUnpacked) {
        attributeKey = "COLOR_" + a.unit
        accessorType = GLTFTypes.TYPE_VEC4
        if (a.`type`.toInt == GL20.GL_FLOAT) {
          accessorComponentType = GLTFTypes.C_FLOAT
        } else if (a.`type`.toInt == GL20.GL_UNSIGNED_SHORT) {
          accessorComponentType = GLTFTypes.C_USHORT
        } else if (a.`type`.toInt == GL20.GL_UNSIGNED_BYTE) {
          accessorComponentType = GLTFTypes.C_UBYTE
        } else {
          throw new GLTFUnsupportedException("color attribute format not supported")
        }
      } else if (a.usage == Usage.TextureCoordinates) {
        attributeKey = "TEXCOORD_" + a.unit
        accessorType = GLTFTypes.TYPE_VEC2
      } else if (a.usage == PBRVertexAttributes.Usage.PositionTarget) {
        attributeKey = "POSITION"
        accessorType = GLTFTypes.TYPE_VEC3
        useTargets = true
      } else if (a.usage == PBRVertexAttributes.Usage.NormalTarget) {
        attributeKey = "NORMAL"
        accessorType = GLTFTypes.TYPE_VEC3
        useTargets = true
      } else if (a.usage == PBRVertexAttributes.Usage.TangentTarget) {
        attributeKey = "TANGENT"
        accessorType = GLTFTypes.TYPE_VEC4
        useTargets = true
      } else if (a.usage == Usage.BoneWeight) {
        while (a.unit >= boneWeightsBuffers.size) boneWeightsBuffers += Nullable.empty
        while (a.unit >= boneJointsBuffers.size) boneJointsBuffers += Nullable.empty

        val boneWeightsBuffer = FloatBuffer.allocate(numVertices)
        boneWeightsBuffers(a.unit) = Nullable(boneWeightsBuffer)

        val boneJointsBuffer = ShortBuffer.allocate(numVertices)
        boneJointsBuffers(a.unit) = Nullable(boneJointsBuffer)

        var i = 0
        while (i < numVertices) {
          vertices.position(i * stride + a.offset / 4)

          val boneID = vertices.get().toInt
          val shortID = (boneID & 0xffff).toShort

          val boneWeight = vertices.get()

          boneWeightsBuffer.put(boneWeight)
          boneJointsBuffer.put(shortID)
          i += 1
        }

        // skip this attribute because will be output later
        skipAttribute = true
      } else {
        throw new GLTFUnsupportedException("unsupported vertex attribute " + a.alias)
      }

      if (!skipAttribute) {
        val accessor = base.obtainAccessor()
        accessor.`type` = Nullable(accessorType)
        accessor.componentType = accessorComponentType
        accessor.count = numVertices
        if (shouldComputeBounds) {
          computeBounds(accessor, vertices, a, numVertices, stride)
        }

        if (useTargets) {
          if (primitive.targets.isEmpty) primitive.targets = Nullable(ArrayBuffer[GLTFMorphTarget]())
          val targets = primitive.targets.get
          while (targets.size <= a.unit) targets += new GLTFMorphTarget()
          targets(a.unit).put(attributeKey, base.root.accessors.get.size - 1)
        } else {
          primitive.attributes.get.put(attributeKey, base.root.accessors.get.size - 1)
        }

        val attributeFloats = GLTFTypes.accessorStrideSize(accessor) / 4

        val outBuffer = base.binManager.beginFloats(numVertices * attributeFloats)

        var i = 0
        while (i < numVertices) {
          vertices.position(i * stride + a.offset / 4)
          var j = 0
          while (j < attributeFloats) {
            outBuffer.put(vertices.get())
            j += 1
          }
          i += 1
        }
        accessor.bufferView = Nullable(base.binManager.end())
      }

      attrIdx += 1
    }

    // export bones
    if (boneWeightsBuffers.nonEmpty) {
      val numGroup = if (boneWeightsBuffers.size > 4) 2 else 1

      // export weights
      var g = 0
      while (g < numGroup) {
        val accessor = base.obtainAccessor()
        val outBuffer = base.binManager.beginFloats(numVertices * 4)
        var i = 0
        while (i < numVertices) {
          // first is bone, second is weight
          var j = 0
          while (j < 4) {
            val bwb = boneWeightsBuffers(g * 4 + j)
            bwb.fold {
              // fill zeros
              outBuffer.put(0f)
            } { buf =>
              outBuffer.put(buf.get(i))
            }
            j += 1
          }
          i += 1
        }
        accessor.`type` = Nullable(GLTFTypes.TYPE_VEC4)
        accessor.componentType = GLTFTypes.C_FLOAT
        accessor.count = numVertices
        accessor.bufferView = Nullable(base.binManager.end())
        primitive.attributes.get.put("WEIGHTS_" + g, base.root.accessors.get.size - 1)
        g += 1
      }

      // export joints
      g = 0
      while (g < numGroup) {
        val accessor = base.obtainAccessor()
        val soutBuffer = base.binManager.beginShorts(numVertices * 4)
        var i = 0
        while (i < numVertices) {
          // first is bone, second is weight
          var j = 0
          while (j < 4) {
            val bjb = boneJointsBuffers(g * 4 + j)
            bjb.fold {
              // fill zeros
              soutBuffer.put(0.toShort)
            } { buf =>
              soutBuffer.put(buf.get(i))
            }
            j += 1
          }
          i += 1
        }
        accessor.`type` = Nullable(GLTFTypes.TYPE_VEC4)
        accessor.componentType = GLTFTypes.C_USHORT
        accessor.count = numVertices
        accessor.bufferView = Nullable(base.binManager.end())
        primitive.attributes.get.put("JOINTS_" + g, base.root.accessors.get.size - 1)
        g += 1
      }
    }
  }

  private def computeBounds(
    accessor: GLTFAccessor,
    vertices: FloatBuffer,
    attribute: VertexAttribute,
    numVertices: Int,
    stride: Int
  ): Unit = {
    accessor.min = Nullable(new Array[Float](attribute.numComponents))
    accessor.max = Nullable(new Array[Float](attribute.numComponents))
    val offset = attribute.offset / 4
    var i = 0
    while (i < numVertices) {
      var j = 0
      while (j < attribute.numComponents) {
        val index = i * stride + offset + j
        val value = vertices.get(index)
        if (i == 0) {
          accessor.min.get(j) = value
          accessor.max.get(j) = value
        } else {
          accessor.min.get(j) = Math.min(accessor.min.get(j), value)
          accessor.max.get(j) = Math.max(accessor.max.get(j), value)
        }
        j += 1
      }
      i += 1
    }
  }
}

private[exporters] object GLTFMeshExporter {

  def mapPrimitiveMode(primitiveType: Int): Nullable[Int] = {
    if (primitiveType == GL20.GL_POINTS) Nullable(0)
    else if (primitiveType == GL20.GL_LINES) Nullable(1)
    else if (primitiveType == GL20.GL_LINE_LOOP) Nullable(2)
    else if (primitiveType == GL20.GL_LINE_STRIP) Nullable(3)
    else if (primitiveType == GL20.GL_TRIANGLES) Nullable.empty // default not need to be set
    else if (primitiveType == GL20.GL_TRIANGLE_STRIP) Nullable(5)
    else if (primitiveType == GL20.GL_TRIANGLE_FAN) Nullable(6)
    else throw new GLTFUnsupportedException("unsupported primitive type " + primitiveType)
  }
}
