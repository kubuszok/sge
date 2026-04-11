/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/geometry/MeshLoader.java
 */
package sge
package gltf
package loaders
package shared
package geometry

import java.nio.{ ByteBuffer, FloatBuffer, ShortBuffer }
import scala.collection.mutable.{ ArrayBuffer, HashMap, HashSet }
import sge.Sge
import sge.utils.Log
import sge.graphics.{ DataType, GL20, Mesh, PrimitiveMode, VertexAttribute, VertexAttributes }
import sge.graphics.VertexAttributes.Usage
import sge.graphics.g3d.Material
import sge.graphics.g3d.model.{ MeshPart, Node, NodePart }
import sge.graphics.glutils.ShaderProgram
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.data.geometry.{ GLTFMesh, GLTFPrimitive }
import sge.gltf.loaders.blender.BlenderShapeKeys
import sge.gltf.loaders.exceptions.{ GLTFIllegalException, GLTFUnsupportedException }
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.loaders.shared.data.{ AccessorBuffer, DataResolver }
import sge.gltf.loaders.shared.material.MaterialLoader
import sge.gltf.scene3d.attributes.{ PBRTextureAttribute, PBRVertexAttributes }
import sge.gltf.scene3d.model.{ NodePartPlus, NodePlus, WeightVector }
import sge.utils.Nullable

class MeshLoader {

  private val meshMap: HashMap[GLTFMesh, ArrayBuffer[NodePart]] = HashMap.empty
  private val meshes:  ArrayBuffer[Mesh]                        = ArrayBuffer.empty

  def load(node: Node, glMesh: GLTFMesh, dataResolver: DataResolver, materialLoader: MaterialLoader)(using sge: Sge): Unit = {
    node.asInstanceOf[NodePlus].morphTargetNames = BlenderShapeKeys.parse(glMesh)

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
          val glAccessors      = ArrayBuffer[GLTFAccessor]()
          val rgbOddAttributes = HashSet[VertexAttribute]()

          val bonesIndices = ArrayBuffer[Array[Int]]()
          val bonesWeights = ArrayBuffer[Array[Float]]()

          var hasNormals = false
          var hasTangent = false

          primitive.attributes.foreach { attributes =>
            for ((attributeName, accessorId) <- attributes) {
              val accessor     = dataResolver.getAccessor(accessorId)
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
                if (accessor.componentType == GLTFTypes.C_UBYTE)
                  throw new GLTFUnsupportedException("unsigned byte texture coordinate attribute not supported")
                if (accessor.componentType == GLTFTypes.C_USHORT)
                  throw new GLTFUnsupportedException("unsigned short texture coordinate attribute not supported")
                if (accessor.componentType != GLTFTypes.C_FLOAT)
                  throw new GLTFIllegalException("illegal texture coordinate component type : " + accessor.componentType)
                val unit = parseAttributeUnit(attributeName)
                vertexAttributes += VertexAttribute.TexCoords(unit)
              } else if (attributeName.startsWith("COLOR_")) {
                val unit  = parseAttributeUnit(attributeName)
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
                  }
                  // LibGDX requires attribute to be multiple of 4 so RGB short (6 bytes) and RGB bytes (3 bytes) data needs to be converted to RGBA.
                  else if (GLTFTypes.C_USHORT == accessor.componentType) {
                    val a = new VertexAttribute(Usage.ColorUnpacked, 4, DataType.UnsignedShort, true, alias)
                    rgbOddAttributes += a
                    vertexAttributes += a
                  } else if (GLTFTypes.C_UBYTE == accessor.componentType) {
                    val a = new VertexAttribute(Usage.ColorUnpacked, 4, DataType.UnsignedByte, true, alias)
                    rgbOddAttributes += a
                    vertexAttributes += a
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

          // morph targets
          primitive.targets.foreach { targets =>
            val morphTargetCount = targets.size
            node.asInstanceOf[NodePlus].weights = Nullable(new WeightVector(morphTargetCount))

            var t = 0
            while (t < targets.size) {
              val unit = t
              for ((attributeName, accessorIdBoxed) <- targets(t)) {
                val accessorId = accessorIdBoxed.intValue
                val accessor   = dataResolver.getAccessor(accessorId)
                glAccessors += accessor

                if (attributeName == "POSITION") {
                  if (!(GLTFTypes.TYPE_VEC3 == accessor.`type`.get && accessor.componentType == GLTFTypes.C_FLOAT))
                    throw new GLTFIllegalException("illegal morph target position attribute format")
                  vertexAttributes += new VertexAttribute(PBRVertexAttributes.Usage.PositionTarget, 3, ShaderProgram.POSITION_ATTRIBUTE + unit, unit)
                } else if (attributeName == "NORMAL") {
                  if (!(GLTFTypes.TYPE_VEC3 == accessor.`type`.get && accessor.componentType == GLTFTypes.C_FLOAT))
                    throw new GLTFIllegalException("illegal morph target normal attribute format")
                  vertexAttributes += new VertexAttribute(PBRVertexAttributes.Usage.NormalTarget, 3, ShaderProgram.NORMAL_ATTRIBUTE + unit, unit)
                } else if (attributeName == "TANGENT") {
                  if (!(GLTFTypes.TYPE_VEC3 == accessor.`type`.get && accessor.componentType == GLTFTypes.C_FLOAT))
                    throw new GLTFIllegalException("illegal morph target tangent attribute format")
                  vertexAttributes += new VertexAttribute(PBRVertexAttributes.Usage.TangentTarget, 3, ShaderProgram.TANGENT_ATTRIBUTE + unit, unit)
                } else {
                  throw new GLTFIllegalException("illegal morph target attribute type " + attributeName)
                }
              }
              t += 1
            }
          }

          val bSize           = bonesIndices.size * 4
          val bonesAttributes = ArrayBuffer[VertexAttribute]()
          var b               = 0
          while (b < bSize) {
            val boneAttribute = VertexAttribute.BoneWeight(b)
            vertexAttributes += boneAttribute
            bonesAttributes += boneAttribute
            b += 1
          }

          // add missing vertex attributes (normals and tangent)
          var computeNormals  = false
          var computeTangents = false
          var normalMapUVs: VertexAttribute = null // @nowarn — may remain null if tangent not needed
          if (glPrimitiveType == GL20.GL_TRIANGLES) {
            if (!hasNormals) {
              vertexAttributes += VertexAttribute.Normal()
              glAccessors += null // @nowarn — placeholder
              computeNormals = true
            }
            if (!hasTangent) {
              // tangent is only needed when normal map is used
              val normalMap = material.getAs[PBRTextureAttribute](PBRTextureAttribute.NormalTexture)
              normalMap.foreach { nm =>
                vertexAttributes += new VertexAttribute(Usage.Tangent, 4, ShaderProgram.TANGENT_ATTRIBUTE)
                glAccessors += null // @nowarn — placeholder
                computeTangents = true
                var found = false
                for (attribute <- vertexAttributes if !found)
                  if (attribute.usage == Usage.TextureCoordinates && attribute.unit == nm.uvIndex) {
                    normalMapUVs = attribute
                    found = true
                  }
                if (normalMapUVs == null) throw new GLTFIllegalException("UVs not found for normal map") // @nowarn — null check
              }
            }
          }

          val attributesGroup = new VertexAttributes(vertexAttributes.toArray*)

          val vertexFloats = attributesGroup.vertexSize / 4
          val maxVertices  = if (glAccessors.nonEmpty && glAccessors.head != null) glAccessors.head.count else 0 // @nowarn — null check for placeholder

          val vertices = new Array[Float](maxVertices * vertexFloats)

          b = 0
          while (b < bSize) {
            val boneAttribute = bonesAttributes(b)
            var i             = 0
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
            val attribute  = vertexAttributes(ai)

            if (glAccessor != null) { // @nowarn — null used as placeholder
              val buffer          = dataResolver.getAccessorBuffer(glAccessor)
              val data            = buffer.prepareForReading()
              val byteStride      = buffer.getByteStride
              val floatStride     = byteStride / 4
              val attributeFloats = attribute.sizeInBytes / 4

              // libGDX requires attribute size to be multiple of 4 bytes.
              // RGB short and RGB bytes need to be converted to RGBA.
              if (rgbOddAttributes.contains(attribute)) {
                if (attribute.`type` == DataType.UnsignedShort) {
                  val shortBuffer = data.asShortBuffer()
                  var j           = 0
                  while (j < glAccessor.count) {
                    shortBuffer.position(j * 3)
                    val vIndex = j * vertexFloats + attribute.offset / 4
                    val r      = shortBuffer.get() & 0xffff
                    val g      = shortBuffer.get() & 0xffff
                    val bv     = shortBuffer.get() & 0xffff
                    val a      = 0xffff
                    vertices(vIndex) = java.lang.Float.intBitsToFloat((r << 16) | g)
                    vertices(vIndex + 1) = java.lang.Float.intBitsToFloat((bv << 16) | a)
                    j += 1
                  }
                } else if (attribute.`type` == DataType.UnsignedByte) {
                  var j = 0
                  while (j < glAccessor.count) {
                    data.position(j * 3)
                    val vIndex = j * vertexFloats + attribute.offset / 4
                    val r      = data.get() & 0xff
                    val g      = data.get() & 0xff
                    val bv     = data.get() & 0xff
                    val a      = 0xff
                    vertices(vIndex) = java.lang.Float.intBitsToFloat((r << 24) | (g << 16) | (bv << 8) | a)
                    j += 1
                  }
                }
              }
              // if vertex stride is not multiple of 4 bytes, we have to read float from byte buffer
              else if (byteStride % 4 != 0) {
                var j = 0
                while (j < glAccessor.count) {
                  val vIndex = j * vertexFloats + attribute.offset / 4
                  val dIndex = j * byteStride
                  var k      = 0
                  while (k < attributeFloats) {
                    vertices(vIndex + k) = data.getFloat(dIndex + k * 4)
                    k += 1
                  }
                  j += 1
                }
              }
              // optimized copy when vertex stride is multiple of 4 bytes
              else {
                val floatBuffer = data.asFloatBuffer()
                var j           = 0
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
            generateParts(
              node,
              parts,
              material,
              glMesh.name,
              vertices,
              maxVertices,
              null,
              attributesGroup,
              glPrimitiveType,
              computeNormals,
              computeTangents,
              normalMapUVs
            ) // @nowarn — null indices for non-indexed
          } { indicesIdx =>
            val indicesAccessor = dataResolver.getAccessor(indicesIdx)

            if (indicesAccessor.`type`.get != GLTFTypes.TYPE_SCALAR) {
              throw new GLTFIllegalException("indices accessor must be SCALAR but was " + indicesAccessor.`type`.get)
            }

            val maxIndices = indicesAccessor.count

            indicesAccessor.componentType match {
              case GLTFTypes.C_UINT =>
                Log.error("GLTF: " + "integer indices partially supported, mesh will be split")
                Log.error(
                  "GLTF: " + "splitting mesh: '" + glMesh.name.getOrElse("null") + "', " + maxVertices + " vertices, " + maxIndices + " indices."
                )

                val verticesPerPrimitive =
                  if (glPrimitiveType == GL20.GL_TRIANGLES) 3
                  else if (glPrimitiveType == GL20.GL_LINES) 2
                  else throw new GLTFUnsupportedException("integer indices only supported for triangles or lines")

                val intIndices = new Array[Int](maxIndices)
                dataResolver.getBufferInt(indicesAccessor).get(intIndices)

                val splitVerts = ArrayBuffer[Array[Float]]()
                val splitInds  = ArrayBuffer[Array[Short]]()

                MeshSpliter.split(splitVerts, splitInds, vertices, attributesGroup, intIndices, verticesPerPrimitive)

                val stride        = attributesGroup.vertexSize / 4
                var totalVertices = 0
                var totalIndices  = 0
                var gi            = 0
                while (gi < splitInds.size) {
                  val groupVertices    = splitVerts(gi)
                  val groupIndices     = splitInds(gi)
                  val groupVertexCount = groupVertices.length / stride

                  totalVertices += groupVertexCount
                  totalIndices += groupIndices.length

                  Log.error("GLTF: " + "generate mesh: " + groupVertexCount + " vertices, " + groupIndices.length + " indices.")

                  generateParts(
                    node,
                    parts,
                    material,
                    glMesh.name,
                    groupVertices,
                    groupVertexCount,
                    groupIndices,
                    attributesGroup,
                    glPrimitiveType,
                    computeNormals,
                    computeTangents,
                    normalMapUVs
                  )
                  gi += 1
                }
                Log.error("GLTF: " + "mesh split: " + parts.size + " meshes generated: " + totalVertices + " vertices, " + totalIndices + " indices.")

              case GLTFTypes.C_USHORT | GLTFTypes.C_SHORT =>
                val indices = new Array[Short](maxIndices)
                dataResolver.getBufferShort(indicesAccessor).get(indices)
                generateParts(
                  node,
                  parts,
                  material,
                  glMesh.name,
                  vertices,
                  maxVertices,
                  indices,
                  attributesGroup,
                  glPrimitiveType,
                  computeNormals,
                  computeTangents,
                  normalMapUVs
                )

              case GLTFTypes.C_UBYTE =>
                val indices    = new Array[Short](maxIndices)
                val byteBuffer = dataResolver.getBufferByte(indicesAccessor)
                var idx        = 0
                while (idx < maxIndices) {
                  indices(idx) = (byteBuffer.get() & 0xff).toShort
                  idx += 1
                }
                generateParts(
                  node,
                  parts,
                  material,
                  glMesh.name,
                  vertices,
                  maxVertices,
                  indices,
                  attributesGroup,
                  glPrimitiveType,
                  computeNormals,
                  computeTangents,
                  normalMapUVs
                )

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
    node:            Node,
    parts:           ArrayBuffer[NodePart],
    material:        Material,
    id:              Nullable[String],
    vertices:        Array[Float],
    vertexCount:     Int,
    indices:         Array[Short],
    attributesGroup: VertexAttributes,
    glPrimitiveType: Int,
    computeNormals:  Boolean,
    computeTangents: Boolean,
    normalMapUVs:    VertexAttribute
  )(using sge: Sge): Unit =
    // skip empty meshes
    if (vertices.isEmpty || (indices != null && indices.isEmpty)) { // @nowarn — indices can be null
      // handled by caller — nothing to do
    } else {

      if (computeNormals || computeTangents) {
        if (computeNormals && computeTangents) Log.info("GLTF: " + "compute normals and tangents for primitive " + id.getOrElse("mesh"))
        else if (computeTangents) Log.info("GLTF: " + "compute tangents for primitive " + id.getOrElse("mesh"))
        else Log.info("GLTF: " + "compute normals for primitive " + id.getOrElse("mesh"))
        MeshTangentSpaceGenerator.computeTangentSpace(vertices, indices, attributesGroup, computeNormals, computeTangents, normalMapUVs)
      }

      val mesh = new Mesh(true, vertexCount, if (indices == null) 0 else indices.length, attributesGroup) // @nowarn — indices can be null
      meshes += mesh
      mesh.setVertices(vertices)

      if (indices != null) { // @nowarn — indices can be null
        mesh.setIndices(indices)
      }

      val len = if (indices == null) vertexCount else indices.length // @nowarn — indices can be null

      val idStr    = id.getOrElse("mesh")
      val meshPart = new MeshPart(idStr, mesh, 0, len, PrimitiveMode(glPrimitiveType))

      val nodePart = new NodePartPlus()
      nodePart.morphTargets = node.asInstanceOf[NodePlus].weights
      nodePart.meshPart = meshPart
      nodePart.material = material
      parts += nodePart
    }

  private def parseAttributeUnit(attributeName: String): Int = {
    val lastUnderscoreIndex = attributeName.lastIndexOf('_')
    try
      Integer.parseInt(attributeName.substring(lastUnderscoreIndex + 1))
    catch {
      case _: NumberFormatException =>
        throw new GLTFIllegalException("illegal attribute name " + attributeName)
    }
  }

  def getMeshes: ArrayBuffer[Mesh] = meshes
}
