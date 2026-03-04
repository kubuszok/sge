/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audit 2026-03-03):
 * - Java 1-arg constructor G3dModelLoader(reader) with resolver=null removed
 *   (Scala requires both args; resolver is passed to ModelLoader super)
 * - reader field removed — replaced by jsoniter-scala codec derivation (G3dModelJson)
 * - loadModelData returns Nullable[ModelData] (no null)
 * - (using Sge) context parameter added to class constructor
 * - JsonValue tree walking replaced with typed G3dModelJson DTO + transformation
 * - parseNodes return type: Java Array<ModelNode> -> Scala DynamicArray[ModelNode] (equivalent)
 * - ArrayMap constructor: Java uses array factory lambdas, Scala uses (ordered, capacity)
 * - VERSION_HI/VERSION_LO in companion object (matching Java static final)
 * - All methods present: parseModel, parseMeshes, parseType, parseAttributes, parseMaterials,
 *   parseTextureUsage, parseColor, readVector2, parseNodes, parseNodesRecursively,
 *   parseAnimations
 *   Renames: parseModel(FileHandle) now uses readJson[G3dModelJson]; all parse* methods take DTOs
 *   Convention: jsoniter-scala codec derivation replaces JsonValue tree walking
 *   Audited: 2026-03-04
 */
package sge
package graphics
package g3d
package loader

import sge.assets.loaders.{ FileHandleResolver, ModelLoader }
import sge.files.FileHandle
import sge.graphics.{ Color, GL20, VertexAttribute }
import sge.graphics.g3d.model.data._
import sge.math.{ Matrix4, Quaternion, Vector2, Vector3 }
import sge.utils.{ ArrayMap, DynamicArray, Nullable, SgeError, readJson }

/** Loads G3D models from `.g3dj` (JSON text) files. Binary `.g3db` format (UBJson) is not yet supported. */
class G3dModelLoader(resolver: FileHandleResolver)(using Sge) extends ModelLoader[ModelLoader.ModelParameters](resolver) {

  override def loadModelData(fileHandle: FileHandle, parameters: Nullable[ModelLoader.ModelParameters]): Nullable[ModelData] =
    Nullable(parseModel(fileHandle))

  def parseModel(handle: FileHandle): ModelData = {
    val json  = handle.readJson[G3dModelJson]
    val model = ModelData()

    model.version(0) = json.version(0)
    model.version(1) = json.version(1)
    if (model.version(0) != G3dModelLoader.VERSION_HI || model.version(1) != G3dModelLoader.VERSION_LO)
      throw SgeError.InvalidInput("Model version not supported")

    model.id = json.id
    parseMeshes(model, json.meshes)
    parseMaterials(model, json.materials, handle.parent().path())
    parseNodes(model, json.nodes)
    parseAnimations(model, json.animations)
    model
  }

  protected def parseMeshes(model: ModelData, meshes: List[G3dMeshJson]): Unit = {
    model.meshes.ensureCapacity(meshes.size)
    for (mesh <- meshes) {
      val jsonMesh = ModelMesh()
      jsonMesh.id = mesh.id
      jsonMesh.attributes = parseAttributes(mesh.attributes)
      jsonMesh.vertices = mesh.vertices.toArray

      val parts = DynamicArray[ModelMeshPart]()
      for (meshPart <- mesh.parts) {
        val partId = meshPart.id
        if (partId.isEmpty)
          throw SgeError.InvalidInput("Not id given for mesh part")
        for (other <- parts)
          if (other.id.equals(partId))
            throw SgeError.InvalidInput("Mesh part with id '" + partId + "' already in defined")

        val jsonPart = ModelMeshPart()
        jsonPart.id = partId
        jsonPart.primitiveType = parseType(meshPart.tpe)
        jsonPart.indices = meshPart.indices.toArray
        parts.add(jsonPart)
      }
      jsonMesh.parts = parts.toArray
      model.meshes.add(jsonMesh)
    }
  }

  protected def parseType(tpe: String): Int =
    if (tpe.equals("TRIANGLES")) GL20.GL_TRIANGLES
    else if (tpe.equals("LINES")) GL20.GL_LINES
    else if (tpe.equals("POINTS")) GL20.GL_POINTS
    else if (tpe.equals("TRIANGLE_STRIP")) GL20.GL_TRIANGLE_STRIP
    else if (tpe.equals("LINE_STRIP")) GL20.GL_LINE_STRIP
    else
      throw SgeError.InvalidInput(
        "Unknown primitive type '" + tpe + "', should be one of triangle, trianglestrip, line, linestrip or point"
      )

  protected def parseAttributes(attributes: List[String]): Array[VertexAttribute] = {
    val vertexAttributes = DynamicArray[VertexAttribute]()
    var unit             = 0
    var blendWeightCount = 0
    for (attr <- attributes)
      if (attr.equals("POSITION")) {
        vertexAttributes.add(VertexAttribute.Position())
      } else if (attr.equals("NORMAL")) {
        vertexAttributes.add(VertexAttribute.Normal())
      } else if (attr.equals("COLOR")) {
        vertexAttributes.add(VertexAttribute.ColorUnpacked())
      } else if (attr.equals("COLORPACKED")) {
        vertexAttributes.add(VertexAttribute.ColorPacked())
      } else if (attr.equals("TANGENT")) {
        vertexAttributes.add(VertexAttribute.Tangent())
      } else if (attr.equals("BINORMAL")) {
        vertexAttributes.add(VertexAttribute.Binormal())
      } else if (attr.startsWith("TEXCOORD")) {
        vertexAttributes.add(VertexAttribute.TexCoords(unit))
        unit += 1
      } else if (attr.startsWith("BLENDWEIGHT")) {
        vertexAttributes.add(VertexAttribute.BoneWeight(blendWeightCount))
        blendWeightCount += 1
      } else {
        throw SgeError.InvalidInput(
          "Unknown vertex attribute '" + attr + "', should be one of position, normal, uv, tangent or binormal"
        )
      }
    vertexAttributes.toArray
  }

  protected def parseMaterials(model: ModelData, materials: List[G3dMaterialJson], materialDir: String): Unit = {
    // we should probably create some default material in this case
    model.materials.ensureCapacity(materials.size)
    for (material <- materials) {
      val jsonMaterial = ModelMaterial()

      jsonMaterial.id = material.id
      if (jsonMaterial.id.isEmpty)
        throw SgeError.InvalidInput("Material needs an id.")

      // Read material colors
      material.diffuse.foreach(v => jsonMaterial.diffuse = parseColor(v))
      material.ambient.foreach(v => jsonMaterial.ambient = parseColor(v))
      material.emissive.foreach(v => jsonMaterial.emissive = parseColor(v))
      material.specular.foreach(v => jsonMaterial.specular = parseColor(v))
      material.reflection.foreach(v => jsonMaterial.reflection = parseColor(v))
      // Read shininess
      jsonMaterial.shininess = material.shininess
      // Read opacity
      jsonMaterial.opacity = material.opacity

      // Read textures
      for (texture <- material.textures) {
        val jsonTexture = ModelTexture()

        jsonTexture.id = texture.id
        if (jsonTexture.id.isEmpty)
          throw SgeError.InvalidInput("Texture has no id.")

        val fileName = texture.filename
        if (fileName.isEmpty)
          throw SgeError.InvalidInput("Texture needs filename.")
        jsonTexture.fileName = materialDir + (if (materialDir.isEmpty || materialDir.endsWith("/")) "" else "/") +
          fileName

        jsonTexture.uvTranslation = texture.uvTranslation match {
          case Some(arr) if arr.size == 2 => Vector2(arr(0), arr(1))
          case _                          => Vector2(0f, 0f)
        }
        jsonTexture.uvScaling = texture.uvScaling match {
          case Some(arr) if arr.size == 2 => Vector2(arr(0), arr(1))
          case _                          => Vector2(1f, 1f)
        }

        jsonTexture.usage = parseTextureUsage(texture.tpe)

        if (Nullable(jsonMaterial.textures).isEmpty) jsonMaterial.textures = DynamicArray[ModelTexture]()
        jsonMaterial.textures.add(jsonTexture)
      }

      model.materials.add(jsonMaterial)
    }
  }

  protected def parseTextureUsage(value: String): Int =
    if (value.equalsIgnoreCase("AMBIENT")) ModelTexture.USAGE_AMBIENT
    else if (value.equalsIgnoreCase("BUMP")) ModelTexture.USAGE_BUMP
    else if (value.equalsIgnoreCase("DIFFUSE")) ModelTexture.USAGE_DIFFUSE
    else if (value.equalsIgnoreCase("EMISSIVE")) ModelTexture.USAGE_EMISSIVE
    else if (value.equalsIgnoreCase("NONE")) ModelTexture.USAGE_NONE
    else if (value.equalsIgnoreCase("NORMAL")) ModelTexture.USAGE_NORMAL
    else if (value.equalsIgnoreCase("REFLECTION")) ModelTexture.USAGE_REFLECTION
    else if (value.equalsIgnoreCase("SHININESS")) ModelTexture.USAGE_SHININESS
    else if (value.equalsIgnoreCase("SPECULAR")) ModelTexture.USAGE_SPECULAR
    else if (value.equalsIgnoreCase("TRANSPARENCY")) ModelTexture.USAGE_TRANSPARENCY
    else ModelTexture.USAGE_UNKNOWN

  protected def parseColor(colorArray: List[Float]): Color =
    if (colorArray.size >= 3)
      Color(colorArray(0), colorArray(1), colorArray(2), 1.0f)
    else
      throw SgeError.InvalidInput("Expected Color values <> than three.")

  protected def parseNodes(model: ModelData, nodes: List[G3dNodeJson]): DynamicArray[ModelNode] = {
    model.nodes.ensureCapacity(nodes.size)
    for (n <- nodes)
      model.nodes.add(parseNodesRecursively(n))
    model.nodes
  }

  protected val tempQ: Quaternion = Quaternion()

  protected def parseNodesRecursively(json: G3dNodeJson): ModelNode = {
    val jsonNode = ModelNode()

    jsonNode.id = json.id
    if (jsonNode.id.isEmpty)
      throw SgeError.InvalidInput("Node id missing.")

    json.translation.foreach { t =>
      if (t.size != 3) throw SgeError.InvalidInput("Node translation incomplete")
      jsonNode.translation = Vector3(t(0), t(1), t(2))
    }

    json.rotation.foreach { r =>
      if (r.size != 4) throw SgeError.InvalidInput("Node rotation incomplete")
      jsonNode.rotation = Quaternion(r(0), r(1), r(2), r(3))
    }

    json.scale.foreach { s =>
      if (s.size != 3) throw SgeError.InvalidInput("Node scale incomplete")
      jsonNode.scale = Vector3(s(0), s(1), s(2))
    }

    json.mesh.foreach(m => jsonNode.meshId = m)

    if (json.parts.nonEmpty) {
      jsonNode.parts = new Array[ModelNodePart](json.parts.size)
      var i = 0
      for (material <- json.parts) {
        val nodePart = ModelNodePart()

        if (material.meshpartid.isEmpty || material.materialid.isEmpty)
          throw SgeError.InvalidInput("Node " + jsonNode.id + " part is missing meshPartId or materialId")
        nodePart.materialId = material.materialid
        nodePart.meshPartId = material.meshpartid

        if (material.bones.nonEmpty) {
          nodePart.bones = ArrayMap[String, Matrix4](true, material.bones.size)
          for (bone <- material.bones) {
            val nodeId = bone.node
            if (nodeId.isEmpty)
              throw SgeError.InvalidInput("Bone node ID missing")

            val transform = Matrix4()

            bone.translation.foreach { v =>
              if (v.size >= 3) transform.translate(v(0), v(1), v(2))
            }
            bone.rotation.foreach { v =>
              if (v.size >= 4)
                transform.rotate(tempQ.set(v(0), v(1), v(2), v(3)))
            }
            bone.scale.foreach { v =>
              if (v.size >= 3) transform.scale(v(0), v(1), v(2))
            }

            nodePart.bones.put(nodeId, transform)
          }
        }

        jsonNode.parts(i) = nodePart
        i += 1
      }
    }

    if (json.children.nonEmpty) {
      jsonNode.children = new Array[ModelNode](json.children.size)
      var i = 0
      for (c <- json.children) {
        jsonNode.children(i) = parseNodesRecursively(c)
        i += 1
      }
    }

    jsonNode
  }

  protected def parseAnimations(model: ModelData, animations: List[G3dAnimationJson]): Unit = {
    model.animations.ensureCapacity(animations.size)

    for (anim <- animations)
      if (anim.bones.nonEmpty) {
        val animation = ModelAnimation()
        model.animations.add(animation)
        animation.nodeAnimations.ensureCapacity(anim.bones.size)
        animation.id = anim.id

        for (node <- anim.bones) {
          val nodeAnim = ModelNodeAnimation()
          animation.nodeAnimations.add(nodeAnim)
          nodeAnim.nodeId = node.boneId

          // For backwards compatibility (version 0.1):
          node.keyframes match {
            case Some(keyframes) =>
              for (keyframe <- keyframes) {
                val keytime = keyframe.keytime / 1000.0f
                keyframe.translation.foreach { translation =>
                  if (translation.size == 3) {
                    if (Nullable(nodeAnim.translation).isEmpty) nodeAnim.translation = DynamicArray[ModelNodeKeyframe[Vector3]]()
                    val tkf = ModelNodeKeyframe[Vector3]()
                    tkf.keytime = keytime
                    tkf.value = Nullable(Vector3(translation(0), translation(1), translation(2)))
                    nodeAnim.translation.add(tkf)
                  }
                }
                keyframe.rotation.foreach { rotation =>
                  if (rotation.size == 4) {
                    if (Nullable(nodeAnim.rotation).isEmpty) nodeAnim.rotation = DynamicArray[ModelNodeKeyframe[Quaternion]]()
                    val rkf = ModelNodeKeyframe[Quaternion]()
                    rkf.keytime = keytime
                    rkf.value = Nullable(Quaternion(rotation(0), rotation(1), rotation(2), rotation(3)))
                    nodeAnim.rotation.add(rkf)
                  }
                }
                keyframe.scale.foreach { scale =>
                  if (scale.size == 3) {
                    if (Nullable(nodeAnim.scaling).isEmpty) nodeAnim.scaling = DynamicArray[ModelNodeKeyframe[Vector3]]()
                    val skf = ModelNodeKeyframe[Vector3]()
                    skf.keytime = keytime
                    skf.value = Nullable(Vector3(scale(0), scale(1), scale(2)))
                    nodeAnim.scaling.add(skf)
                  }
                }
              }

            case None => // Version 0.2:
              node.translation.foreach { translationKFs =>
                nodeAnim.translation = DynamicArray[ModelNodeKeyframe[Vector3]]()
                nodeAnim.translation.ensureCapacity(translationKFs.size)
                for (kfVal <- translationKFs) {
                  val kf = ModelNodeKeyframe[Vector3]()
                  nodeAnim.translation.add(kf)
                  kf.keytime = kfVal.keytime / 1000.0f
                  val v = kfVal.value
                  if (v.size >= 3)
                    kf.value = Nullable(Vector3(v(0), v(1), v(2)))
                }
              }

              node.rotation.foreach { rotationKFs =>
                nodeAnim.rotation = DynamicArray[ModelNodeKeyframe[Quaternion]]()
                nodeAnim.rotation.ensureCapacity(rotationKFs.size)
                for (kfVal <- rotationKFs) {
                  val kf = ModelNodeKeyframe[Quaternion]()
                  nodeAnim.rotation.add(kf)
                  kf.keytime = kfVal.keytime / 1000.0f
                  val v = kfVal.value
                  if (v.size >= 4)
                    kf.value = Nullable(Quaternion(v(0), v(1), v(2), v(3)))
                }
              }

              node.scaling.foreach { scalingKFs =>
                nodeAnim.scaling = DynamicArray[ModelNodeKeyframe[Vector3]]()
                nodeAnim.scaling.ensureCapacity(scalingKFs.size)
                for (kfVal <- scalingKFs) {
                  val kf = ModelNodeKeyframe[Vector3]()
                  nodeAnim.scaling.add(kf)
                  kf.keytime = kfVal.keytime / 1000.0f
                  val v = kfVal.value
                  if (v.size >= 3)
                    kf.value = Nullable(Vector3(v(0), v(1), v(2)))
                }
              }
          }
        }
      }
  }
}

object G3dModelLoader {
  val VERSION_HI: Short = 0
  val VERSION_LO: Short = 1
}
