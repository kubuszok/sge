/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package shared
package geometry

import java.nio.{ByteBuffer, FloatBuffer, ShortBuffer}
import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}
import sge.Sge
import sge.utils.Log
import sge.graphics.{DataType, GL20, Mesh, PrimitiveMode, VertexAttribute, VertexAttributes}
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.Material
import sge.graphics.g3d.model.{MeshPart, Node, NodePart}
import sge.graphics.glutils.ShaderProgram
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.data.geometry.{GLTFMesh, GLTFPrimitive}
import sge.gltf.loaders.exceptions.{GLTFIllegalException, GLTFUnsupportedException}
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.loaders.shared.data.{AccessorBuffer, DataResolver}
import sge.gltf.loaders.shared.material.MaterialLoader
import sge.utils.Nullable

class MeshLoader {

  private val meshMap: HashMap[GLTFMesh, ArrayBuffer[NodePart]] = HashMap.empty
  private val meshes: ArrayBuffer[Mesh] = ArrayBuffer.empty

  def load(node: Node, glMesh: GLTFMesh, dataResolver: DataResolver, materialLoader: MaterialLoader)(using sge: Sge): Unit = {
    var parts = meshMap.getOrElse(glMesh, null) // @nowarn — checking for presence
    if (parts == null) { // @nowarn — checking for presence
      parts = ArrayBuffer[NodePart]()

      glMesh.primitives.foreach { primitives =>
        for (primitive <- primitives) {
          val glPrimitiveType = GLTFTypes.mapPrimitiveMode(primitive.mode)

          // material
          val material: Material = primitive.material.fold(materialLoader.getDefaultMaterial) { matIdx =>
            materialLoader.get(matIdx)
          }

          // vertices
          val vertexAttributes = ArrayBuffer[VertexAttribute]()
          val glAccessors = ArrayBuffer[GLTFAccessor]()
          val rgbOddAttributes = HashSet[VertexAttribute]()

          val bonesIndices = ArrayBuffer[Array[Int]]()
          val bonesWeights = ArrayBuffer[Array[Float]]()

          var hasNormals = false
          var hasTangent = false

          primitive.attributes.foreach { attributes =>
            for ((attributeName, accessorId) <- attributes) {
              val accessor = dataResolver.getAccessor(accessorId)
              var rawAttribute = true

              if (attributeName == "POSITION") {
                if (!(GLTFTypes.TYPE_VEC3 == accessor.`type`.get && accessor.componentType == GLTFTypes.C_FLOAT))
                  throw new GLTFIllegalException("illegal position attribute format")
                vertexAttributes += VertexAttribute.Position()
              } else if (attributeName == "NORMAL") {
                if (!(GLTFTypes.TYPE_VEC3 == accessor.`type`.get && accessor.componentType == GLTFTypes.C_FLOAT))
                  throw new GLTFIllegalException("illegal normal attribute format")
                vertexAttributes += VertexAttribute.Normal()
                hasNormals = true
              } else if (attributeName == "TANGENT") {
                if (!(GLTFTypes.TYPE_VEC4 == accessor.`type`.get && accessor.componentType == GLTFTypes.C_FLOAT))
                  throw new GLTFIllegalException("illegal tangent attribute format")
                vertexAttributes += new VertexAttribute(Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE)
                hasTangent = true
              } else if (attributeName.startsWith("TEXCOORD_")) {
                if (GLTFTypes.TYPE_VEC2 != accessor.`type`.get)
                  throw new GLTFIllegalException("illegal texture coordinate attribute type : " + accessor.`type`.get)
                if (accessor.componentType != GLTFTypes.C_FLOAT)
                  throw new GLTFIllegalException("illegal texture coordinate component type : " + accessor.componentType)
                val unit = parseAttributeUnit(attributeName)
                vertexAttributes += VertexAttribute.TexCoords(unit)
              } else if (attributeName.startsWith("COLOR_")) {
                val unit = parseAttributeUnit(attributeName)
                val alias = if (unit > 0) ShaderProgram.COLOR_ATTRIBUTE + unit else ShaderProgram.COLOR_ATTRIBUTE
                if (GLTFTypes.TYPE_VEC4 == accessor.`type`.get) {
                  if (GLTFTypes.C_FLOAT == accessor.componentType) {
                    vertexAttributes += new VertexAttribute(Usage.ColorUnpacked, 4, DataType.Float, false, alias)
                  } else if (GLTFTypes.C_USHORT == accessor.componentType) {
                    vertexAttributes += new VertexAttribute(Usage.ColorUnpacked, 4, DataType.UnsignedShort, true, alias)
                  } else if (GLTFTypes.C_UBYTE == accessor.componentType) {
                    vertexAttributes += new VertexAttribute(Usage.ColorUnpacked, 4, DataType.UnsignedByte, true, alias)
                  } else {
                    throw new GLTFIllegalException("illegal color attribute component type: " + accessor.`type`.get)
                  }
                } else if (GLTFTypes.TYPE_VEC3 == accessor.`type`.get) {
                  if (GLTFTypes.C_FLOAT == accessor.componentType) {
                    vertexAttributes += new VertexAttribute(Usage.ColorUnpacked, 3, DataType.Float, false, alias)
                  } else {
                    throw new GLTFIllegalException("illegal color attribute component type: " + accessor.`type`.get)
                  }
                } else {
                  throw new GLTFIllegalException("illegal color attribute type: " + accessor.`type`.get)
                }
              } else if (attributeName.startsWith("WEIGHTS_")) {
                rawAttribute = false
                if (GLTFTypes.TYPE_VEC4 != accessor.`type`.get) {
                  throw new GLTFIllegalException("illegal weight attribute type: " + accessor.`type`.get)
                }
                val unit = parseAttributeUnit(attributeName)
                while (bonesWeights.size <= unit) bonesWeights += null // @nowarn — padding
                if (accessor.componentType == GLTFTypes.C_FLOAT) {
                  bonesWeights(unit) = dataResolver.readBufferFloat(accessorId)
                } else if (accessor.componentType == GLTFTypes.C_USHORT) {
                  bonesWeights(unit) = dataResolver.readBufferUShortAsFloat(accessorId)
                } else if (accessor.componentType == GLTFTypes.C_UBYTE) {
                  bonesWeights(unit) = dataResolver.readBufferUByteAsFloat(accessorId)
                } else {
                  throw new GLTFIllegalException("illegal weight attribute type: " + accessor.componentType)
                }
              } else if (attributeName.startsWith("JOINTS_")) {
                rawAttribute = false
                if (GLTFTypes.TYPE_VEC4 != accessor.`type`.get) {
                  throw new GLTFIllegalException("illegal joints attribute type: " + accessor.`type`.get)
                }
                val unit = parseAttributeUnit(attributeName)
                while (bonesIndices.size <= unit) bonesIndices += null // @nowarn — padding
                if (accessor.componentType == GLTFTypes.C_UBYTE) {
                  bonesIndices(unit) = dataResolver.readBufferUByte(accessorId)
                } else if (accessor.componentType == GLTFTypes.C_USHORT) {
                  bonesIndices(unit) = dataResolver.readBufferUShort(accessorId)
                } else {
                  throw new GLTFIllegalException("illegal type for joints: " + accessor.componentType)
                }
              } else if (attributeName.startsWith("_")) {
                Log.error("GLTF: " + "skip unsupported custom attribute: " + attributeName)
                rawAttribute = false
              } else {
                throw new GLTFIllegalException("illegal attribute type " + attributeName)
              }

              if (rawAttribute) {
                glAccessors += accessor
              }
            }
          }

          val bSize = bonesIndices.size * 4
          val bonesAttributes = ArrayBuffer[VertexAttribute]()
          var b = 0
          while (b < bSize) {
            val boneAttribute = VertexAttribute.BoneWeight(b)
            vertexAttributes += boneAttribute
            bonesAttributes += boneAttribute
            b += 1
          }

          // add missing normals
          var computeNormals = false
          var computeTangents = false
          var normalMapUVs: VertexAttribute = null // @nowarn — may remain null if tangent not needed
          if (glPrimitiveType == GL20.GL_TRIANGLES) {
            if (!hasNormals) {
              vertexAttributes += VertexAttribute.Normal()
              glAccessors += null // @nowarn — placeholder
              computeNormals = true
            }
            if (!hasTangent) {
              // tangent computation requires PBR texture attributes not yet ported
              // skip for now
            }
          }

          val attributesGroup = new VertexAttributes(vertexAttributes.toArray*)

          val vertexFloats = attributesGroup.vertexSize / 4
          val maxVertices = if (glAccessors.nonEmpty && glAccessors.head != null) glAccessors.head.count else 0

          val vertices = new Array[Float](maxVertices * vertexFloats)

          b = 0
          while (b < bSize) {
            val boneAttribute = bonesAttributes(b)
            var i = 0
            while (i < maxVertices) {
              vertices(i * vertexFloats + boneAttribute.offset / 4) = bonesIndices(b / 4)(i * 4 + b % 4).toFloat
              vertices(i * vertexFloats + boneAttribute.offset / 4 + 1) = bonesWeights(b / 4)(i * 4 + b % 4)
              i += 1
            }
            b += 1
          }

          var ai = 0
          while (ai < glAccessors.size) {
            val glAccessor = glAccessors(ai)
            val attribute = vertexAttributes(ai)

            if (glAccessor != null) { // @nowarn — null used as placeholder
              val buffer = dataResolver.getAccessorBuffer(glAccessor)
              val data = buffer.prepareForReading()
              val byteStride = buffer.getByteStride
              val floatStride = byteStride / 4
              val attributeFloats = attribute.sizeInBytes / 4

              if (byteStride % 4 != 0) {
                var j = 0
                while (j < glAccessor.count) {
                  val vIndex = j * vertexFloats + attribute.offset / 4
                  val dIndex = j * byteStride
                  var k = 0
                  while (k < attributeFloats) {
                    vertices(vIndex + k) = data.getFloat(dIndex + k * 4)
                    k += 1
                  }
                  j += 1
                }
              } else {
                val floatBuffer = data.asFloatBuffer()
                var j = 0
                while (j < glAccessor.count) {
                  floatBuffer.position(j * floatStride)
                  val vIndex = j * vertexFloats + attribute.offset / 4
                  floatBuffer.get(vertices, vIndex, attributeFloats)
                  j += 1
                }
              }
            }
            ai += 1
          }

          // indices
          primitive.indices.fold {
            // non indexed mesh
            generateParts(node, parts, material, glMesh.name, vertices, maxVertices, null, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs) // @nowarn — null indices for non-indexed
          } { indicesIdx =>
            val indicesAccessor = dataResolver.getAccessor(indicesIdx)

            if (indicesAccessor.`type`.get != GLTFTypes.TYPE_SCALAR) {
              throw new GLTFIllegalException("indices accessor must be SCALAR but was " + indicesAccessor.`type`.get)
            }

            val maxIndices = indicesAccessor.count

            indicesAccessor.componentType match {
              case GLTFTypes.C_UINT =>
                Log.error("GLTF: " + "integer indices partially supported, mesh will be split")
                val verticesPerPrimitive =
                  if (glPrimitiveType == GL20.GL_TRIANGLES) 3
                  else if (glPrimitiveType == GL20.GL_LINES) 2
                  else throw new GLTFUnsupportedException("integer indices only supported for triangles or lines")

                val intIndices = new Array[Int](maxIndices)
                dataResolver.getBufferInt(indicesAccessor).get(intIndices)

                val splitVerts = ArrayBuffer[Array[Float]]()
                val splitInds = ArrayBuffer[Array[Short]]()

                MeshSpliter.split(splitVerts, splitInds, vertices, attributesGroup, intIndices, verticesPerPrimitive)

                val stride = attributesGroup.vertexSize / 4
                var gi = 0
                while (gi < splitInds.size) {
                  val groupVertices = splitVerts(gi)
                  val groupIndices = splitInds(gi)
                  val groupVertexCount = groupVertices.length / stride
                  generateParts(node, parts, material, glMesh.name, groupVertices, groupVertexCount, groupIndices, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs)
                  gi += 1
                }

              case GLTFTypes.C_USHORT | GLTFTypes.C_SHORT =>
                val indices = new Array[Short](maxIndices)
                dataResolver.getBufferShort(indicesAccessor).get(indices)
                generateParts(node, parts, material, glMesh.name, vertices, maxVertices, indices, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs)

              case GLTFTypes.C_UBYTE =>
                val indices = new Array[Short](maxIndices)
                val byteBuffer = dataResolver.getBufferByte(indicesAccessor)
                var idx = 0
                while (idx < maxIndices) {
                  indices(idx) = (byteBuffer.get() & 0xFF).toShort
                  idx += 1
                }
                generateParts(node, parts, material, glMesh.name, vertices, maxVertices, indices, attributesGroup, glPrimitiveType, computeNormals, computeTangents, normalMapUVs)

              case _ =>
                throw new GLTFIllegalException("illegal componentType " + indicesAccessor.componentType)
            }
          }
        }
      }
      meshMap.put(glMesh, parts)
    }
    node.parts.addAll(parts)
  }

  private def generateParts(
      node: Node,
      parts: ArrayBuffer[NodePart],
      material: Material,
      id: Nullable[String],
      vertices: Array[Float],
      vertexCount: Int,
      indices: Array[Short],
      attributesGroup: VertexAttributes,
      glPrimitiveType: Int,
      computeNormals: Boolean,
      computeTangents: Boolean,
      normalMapUVs: VertexAttribute
  )(using sge: Sge): Unit = {
    // skip empty meshes
    if (vertices.isEmpty || (indices != null && indices.isEmpty)) { // @nowarn — indices can be null
      return // handled by caller
    }

    if (computeNormals || computeTangents) {
      if (indices != null) { // @nowarn — indices can be null for non-indexed meshes
        MeshTangentSpaceGenerator.computeTangentSpace(vertices, indices, attributesGroup, computeNormals, computeTangents, normalMapUVs)
      }
    }

    val mesh = new Mesh(true, vertexCount, if (indices == null) 0 else indices.length, attributesGroup) // @nowarn — indices can be null
    meshes += mesh
    mesh.setVertices(vertices)

    if (indices != null) { // @nowarn — indices can be null
      mesh.setIndices(indices)
    }

    val len = if (indices == null) vertexCount else indices.length // @nowarn — indices can be null

    val idStr = id.getOrElse("mesh")
    val meshPart = new MeshPart(idStr, mesh, 0, len, PrimitiveMode(glPrimitiveType))

    val nodePart = new NodePart()
    nodePart.meshPart = meshPart
    nodePart.material = material
    parts += nodePart
  }

  private def parseAttributeUnit(attributeName: String): Int = {
    val lastUnderscoreIndex = attributeName.lastIndexOf('_')
    try {
      Integer.parseInt(attributeName.substring(lastUnderscoreIndex + 1))
    } catch {
      case _: NumberFormatException =>
        throw new GLTFIllegalException("illegal attribute name " + attributeName)
    }
  }

  def getMeshes: ArrayBuffer[Mesh] = meshes
}
