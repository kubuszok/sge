/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/loader/G3dModelLoader.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package loader

import scala.collection.mutable.ArrayBuffer

import sge.assets.loaders.{ FileHandleResolver, ModelLoader }
import sge.files.FileHandle
import sge.graphics.{ Color, GL20, VertexAttribute }
import sge.graphics.g3d.model.data._
import sge.math.{ Matrix4, Quaternion, Vector2, Vector3 }
import sge.utils.{ ArrayMap, BaseJsonReader, JsonValue, Nullable, SgeError }

/** Loads G3D models from `.g3dj` (JSON text) files. Binary `.g3db` format (UBJson) is not yet supported. */
class G3dModelLoader(val reader: BaseJsonReader, resolver: FileHandleResolver)(using sge: Sge) extends ModelLoader[ModelLoader.ModelParameters](resolver) {

  override def loadModelData(fileHandle: FileHandle, parameters: ModelLoader.ModelParameters): Nullable[ModelData] =
    Nullable(parseModel(fileHandle))

  def parseModel(handle: FileHandle): ModelData = {
    val json    = reader.parse(handle)
    val model   = new ModelData()
    val version = json.require("version")
    model.version(0) = version.getShort(0)
    model.version(1) = version.getShort(1)
    if (model.version(0) != G3dModelLoader.VERSION_HI || model.version(1) != G3dModelLoader.VERSION_LO)
      throw SgeError.InvalidInput("Model version not supported")

    model.id = json.getString("id", Nullable("")).orNull
    parseMeshes(model, json)
    parseMaterials(model, json, handle.parent().path())
    parseNodes(model, json)
    parseAnimations(model, json)
    model
  }

  protected def parseMeshes(model: ModelData, json: JsonValue): Unit = {
    val meshes = json.get("meshes").orNull
    if (meshes != null) {

      model.meshes.sizeHint(model.meshes.size + meshes.size)
      var meshN = meshes.child
      while (meshN.isDefined) {
        val mesh     = meshN.orNull
        val jsonMesh = new ModelMesh()

        val id = mesh.getString("id", Nullable("")).orNull
        jsonMesh.id = id

        val attributes = mesh.require("attributes")
        jsonMesh.attributes = parseAttributes(attributes)
        jsonMesh.vertices = mesh.require("vertices").asFloatArray()

        val meshParts = mesh.require("parts")
        val parts     = ArrayBuffer[ModelMeshPart]()
        var meshPartN = meshParts.child
        while (meshPartN.isDefined) {
          val meshPart = meshPartN.orNull
          val jsonPart = new ModelMeshPart()
          val partId   = mesh.getString("id", Nullable.empty).orNull
          if (partId == null) {
            throw SgeError.InvalidInput("Not id given for mesh part")
          }
          for (other <- parts)
            if (other.id.equals(partId)) {
              throw SgeError.InvalidInput("Mesh part with id '" + partId + "' already in defined")
            }
          jsonPart.id = partId

          val tpe = meshPart.getString("type", Nullable.empty).orNull
          if (tpe == null) {
            throw SgeError.InvalidInput("No primitive type given for mesh part '" + partId + "'")
          }
          jsonPart.primitiveType = parseType(tpe)

          jsonPart.indices = meshPart.require("indices").asShortArray()
          parts += jsonPart
          meshPartN = meshPart.next
        }
        jsonMesh.parts = parts.toArray
        model.meshes += jsonMesh
        meshN = mesh.next
      }
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

  protected def parseAttributes(attributes: JsonValue): Array[VertexAttribute] = {
    val vertexAttributes = ArrayBuffer[VertexAttribute]()
    var unit             = 0
    var blendWeightCount = 0
    var valueN           = attributes.child
    while (valueN.isDefined) {
      val value = valueN.orNull
      val attr  = value.asString().orNull
      if (attr.equals("POSITION")) {
        vertexAttributes += VertexAttribute.Position()
      } else if (attr.equals("NORMAL")) {
        vertexAttributes += VertexAttribute.Normal()
      } else if (attr.equals("COLOR")) {
        vertexAttributes += VertexAttribute.ColorUnpacked()
      } else if (attr.equals("COLORPACKED")) {
        vertexAttributes += VertexAttribute.ColorPacked()
      } else if (attr.equals("TANGENT")) {
        vertexAttributes += VertexAttribute.Tangent()
      } else if (attr.equals("BINORMAL")) {
        vertexAttributes += VertexAttribute.Binormal()
      } else if (attr.startsWith("TEXCOORD")) {
        vertexAttributes += VertexAttribute.TexCoords(unit)
        unit += 1
      } else if (attr.startsWith("BLENDWEIGHT")) {
        vertexAttributes += VertexAttribute.BoneWeight(blendWeightCount)
        blendWeightCount += 1
      } else {
        throw SgeError.InvalidInput(
          "Unknown vertex attribute '" + attr + "', should be one of position, normal, uv, tangent or binormal"
        )
      }
      valueN = value.next
    }
    vertexAttributes.toArray
  }

  protected def parseMaterials(model: ModelData, json: JsonValue, materialDir: String): Unit = {
    val materials = json.get("materials").orNull
    if (materials == null) {
      // we should probably create some default material in this case
    } else {
      model.materials.sizeHint(model.materials.size + materials.size)
      var materialN = materials.child
      while (materialN.isDefined) {
        val material     = materialN.orNull
        val jsonMaterial = new ModelMaterial()

        val id = material.getString("id", Nullable.empty).orNull
        if (id == null) throw SgeError.InvalidInput("Material needs an id.")

        jsonMaterial.id = id

        // Read material colors
        val diffuse = material.get("diffuse").orNull
        if (diffuse != null) jsonMaterial.diffuse = parseColor(diffuse)
        val ambient = material.get("ambient").orNull
        if (ambient != null) jsonMaterial.ambient = parseColor(ambient)
        val emissive = material.get("emissive").orNull
        if (emissive != null) jsonMaterial.emissive = parseColor(emissive)
        val specular = material.get("specular").orNull
        if (specular != null) jsonMaterial.specular = parseColor(specular)
        val reflection = material.get("reflection").orNull
        if (reflection != null) jsonMaterial.reflection = parseColor(reflection)
        // Read shininess
        jsonMaterial.shininess = material.getFloat("shininess", 0.0f)
        // Read opacity
        jsonMaterial.opacity = material.getFloat("opacity", 1.0f)

        // Read textures
        val textures = material.get("textures").orNull
        if (textures != null) {
          var textureN = textures.child
          while (textureN.isDefined) {
            val texture     = textureN.orNull
            val jsonTexture = new ModelTexture()

            val textureId = texture.getString("id", Nullable.empty).orNull
            if (textureId == null) throw SgeError.InvalidInput("Texture has no id.")
            jsonTexture.id = textureId

            val fileName = texture.getString("filename", Nullable.empty).orNull
            if (fileName == null) throw SgeError.InvalidInput("Texture needs filename.")
            jsonTexture.fileName = materialDir + (if (materialDir.isEmpty || materialDir.endsWith("/")) "" else "/") +
              fileName

            jsonTexture.uvTranslation = readVector2(texture.get("uvTranslation").orNull, 0f, 0f)
            jsonTexture.uvScaling = readVector2(texture.get("uvScaling").orNull, 1f, 1f)

            val textureType = texture.getString("type", Nullable.empty).orNull
            if (textureType == null) throw SgeError.InvalidInput("Texture needs type.")

            jsonTexture.usage = parseTextureUsage(textureType)

            if (jsonMaterial.textures == null) jsonMaterial.textures = ArrayBuffer[ModelTexture]()
            jsonMaterial.textures += jsonTexture
            textureN = texture.next
          }
        }

        model.materials += jsonMaterial
        materialN = material.next
      }
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

  protected def parseColor(colorArray: JsonValue): Color =
    if (colorArray.size >= 3)
      new Color(colorArray.getFloat(0), colorArray.getFloat(1), colorArray.getFloat(2), 1.0f)
    else
      throw SgeError.InvalidInput("Expected Color values <> than three.")

  protected def readVector2(vectorArray: JsonValue, x: Float, y: Float): Vector2 =
    if (vectorArray == null)
      new Vector2(x, y)
    else if (vectorArray.size == 2)
      new Vector2(vectorArray.getFloat(0), vectorArray.getFloat(1))
    else
      throw SgeError.InvalidInput("Expected Vector2 values <> than two.")

  protected def parseNodes(model: ModelData, json: JsonValue): ArrayBuffer[ModelNode] = {
    val nodes = json.get("nodes").orNull
    if (nodes != null) {
      model.nodes.sizeHint(model.nodes.size + nodes.size)
      var nodeN = nodes.child
      while (nodeN.isDefined) {
        model.nodes += parseNodesRecursively(nodeN.orNull)
        nodeN = nodeN.orNull.next
      }
    }

    model.nodes
  }

  protected val tempQ: Quaternion = new Quaternion()

  protected def parseNodesRecursively(json: JsonValue): ModelNode = {
    val jsonNode = new ModelNode()

    val id = json.getString("id", Nullable.empty).orNull
    if (id == null) throw SgeError.InvalidInput("Node id missing.")
    jsonNode.id = id

    val translation = json.get("translation").orNull
    if (translation != null && translation.size != 3) throw SgeError.InvalidInput("Node translation incomplete")
    jsonNode.translation =
      if (translation == null) null
      else new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))

    val rotation = json.get("rotation").orNull
    if (rotation != null && rotation.size != 4) throw SgeError.InvalidInput("Node rotation incomplete")
    jsonNode.rotation =
      if (rotation == null) null
      else new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3))

    val scale = json.get("scale").orNull
    if (scale != null && scale.size != 3) throw SgeError.InvalidInput("Node scale incomplete")
    jsonNode.scale =
      if (scale == null) null
      else new Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2))

    val meshId = json.getString("mesh", Nullable.empty).orNull
    if (meshId != null) jsonNode.meshId = meshId

    val materials = json.get("parts").orNull
    if (materials != null) {
      jsonNode.parts = new Array[ModelNodePart](materials.size)
      var i         = 0
      var materialN = materials.child
      while (materialN.isDefined) {
        val material = materialN.orNull
        val nodePart = new ModelNodePart()

        val meshPartId = material.getString("meshpartid", Nullable.empty).orNull
        val materialId = material.getString("materialid", Nullable.empty).orNull
        if (meshPartId == null || materialId == null) {
          throw SgeError.InvalidInput("Node " + id + " part is missing meshPartId or materialId")
        }
        nodePart.materialId = materialId
        nodePart.meshPartId = meshPartId

        val bones = material.get("bones").orNull
        if (bones != null) {
          nodePart.bones = ArrayMap[String, Matrix4](true, bones.size)
          var boneN = bones.child
          while (boneN.isDefined) {
            val bone   = boneN.orNull
            val nodeId = bone.getString("node", Nullable.empty).orNull
            if (nodeId == null) throw SgeError.InvalidInput("Bone node ID missing")

            val transform = new Matrix4()

            var v = bone.get("translation").orNull
            if (v != null && v.size >= 3) transform.translate(v.getFloat(0), v.getFloat(1), v.getFloat(2))

            v = bone.get("rotation").orNull
            if (v != null && v.size >= 4)
              transform.rotate(tempQ.set(v.getFloat(0), v.getFloat(1), v.getFloat(2), v.getFloat(3)))

            v = bone.get("scale").orNull
            if (v != null && v.size >= 3) transform.scale(v.getFloat(0), v.getFloat(1), v.getFloat(2))

            nodePart.bones.put(nodeId, transform)
            boneN = bone.next
          }
        }

        jsonNode.parts(i) = nodePart
        i += 1
        materialN = material.next
      }
    }

    val children = json.get("children").orNull
    if (children != null) {
      jsonNode.children = new Array[ModelNode](children.size)

      var i      = 0
      var childN = children.child
      while (childN.isDefined) {
        jsonNode.children(i) = parseNodesRecursively(childN.orNull)
        i += 1
        childN = childN.orNull.next
      }
    }

    jsonNode
  }

  protected def parseAnimations(model: ModelData, json: JsonValue): Unit = {
    val animations = json.get("animations").orNull
    if (animations == null) return // scalastyle:ignore

    model.animations.sizeHint(model.animations.size + animations.size)

    var animN = animations.child
    while (animN.isDefined) {
      val anim  = animN.orNull
      val nodes = anim.get("bones").orNull
      if (nodes != null) {
        val animation = new ModelAnimation()
        model.animations += animation
        animation.nodeAnimations.sizeHint(animation.nodeAnimations.size + nodes.size)
        animation.id = anim.getString("id").orNull
        var nodeN = nodes.child
        while (nodeN.isDefined) {
          val node     = nodeN.orNull
          val nodeAnim = new ModelNodeAnimation()
          animation.nodeAnimations += nodeAnim
          nodeAnim.nodeId = node.getString("boneId").orNull

          // For backwards compatibility (version 0.1):
          val keyframes = node.get("keyframes").orNull
          if (keyframes != null && keyframes.isArray) {
            var keyframeN = keyframes.child
            while (keyframeN.isDefined) {
              val keyframe    = keyframeN.orNull
              val keytime     = keyframe.getFloat("keytime", 0f) / 1000.0f
              val translation = keyframe.get("translation").orNull
              if (translation != null && translation.size == 3) {
                if (nodeAnim.translation == null) nodeAnim.translation = ArrayBuffer[ModelNodeKeyframe[Vector3]]()
                val tkf = new ModelNodeKeyframe[Vector3]()
                tkf.keytime = keytime
                tkf.value = Nullable(
                  new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))
                )
                nodeAnim.translation += tkf
              }
              val rotation = keyframe.get("rotation").orNull
              if (rotation != null && rotation.size == 4) {
                if (nodeAnim.rotation == null) nodeAnim.rotation = ArrayBuffer[ModelNodeKeyframe[Quaternion]]()
                val rkf = new ModelNodeKeyframe[Quaternion]()
                rkf.keytime = keytime
                rkf.value = Nullable(
                  new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3))
                )
                nodeAnim.rotation += rkf
              }
              val scale = keyframe.get("scale").orNull
              if (scale != null && scale.size == 3) {
                if (nodeAnim.scaling == null) nodeAnim.scaling = ArrayBuffer[ModelNodeKeyframe[Vector3]]()
                val skf = new ModelNodeKeyframe[Vector3]()
                skf.keytime = keytime
                skf.value = Nullable(new Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2)))
                nodeAnim.scaling += skf
              }
              keyframeN = keyframe.next
            }
          } else { // Version 0.2:
            val translationKF = node.get("translation").orNull
            if (translationKF != null && translationKF.isArray) {
              nodeAnim.translation = ArrayBuffer[ModelNodeKeyframe[Vector3]]()
              nodeAnim.translation.sizeHint(translationKF.size)
              var kfN = translationKF.child
              while (kfN.isDefined) {
                val keyframe = kfN.orNull
                val kf       = new ModelNodeKeyframe[Vector3]()
                nodeAnim.translation += kf
                kf.keytime = keyframe.getFloat("keytime", 0f) / 1000.0f
                val translation = keyframe.get("value").orNull
                if (translation != null && translation.size >= 3)
                  kf.value = Nullable(
                    new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))
                  )
                kfN = keyframe.next
              }
            }

            val rotationKF = node.get("rotation").orNull
            if (rotationKF != null && rotationKF.isArray) {
              nodeAnim.rotation = ArrayBuffer[ModelNodeKeyframe[Quaternion]]()
              nodeAnim.rotation.sizeHint(rotationKF.size)
              var kfN = rotationKF.child
              while (kfN.isDefined) {
                val keyframe = kfN.orNull
                val kf       = new ModelNodeKeyframe[Quaternion]()
                nodeAnim.rotation += kf
                kf.keytime = keyframe.getFloat("keytime", 0f) / 1000.0f
                val rotation = keyframe.get("value").orNull
                if (rotation != null && rotation.size >= 4)
                  kf.value = Nullable(
                    new Quaternion(
                      rotation.getFloat(0),
                      rotation.getFloat(1),
                      rotation.getFloat(2),
                      rotation.getFloat(3)
                    )
                  )
                kfN = keyframe.next
              }
            }

            val scalingKF = node.get("scaling").orNull
            if (scalingKF != null && scalingKF.isArray) {
              nodeAnim.scaling = ArrayBuffer[ModelNodeKeyframe[Vector3]]()
              nodeAnim.scaling.sizeHint(scalingKF.size)
              var kfN = scalingKF.child
              while (kfN.isDefined) {
                val keyframe = kfN.orNull
                val kf       = new ModelNodeKeyframe[Vector3]()
                nodeAnim.scaling += kf
                kf.keytime = keyframe.getFloat("keytime", 0f) / 1000.0f
                val scaling = keyframe.get("value").orNull
                if (scaling != null && scaling.size >= 3)
                  kf.value = Nullable(new Vector3(scaling.getFloat(0), scaling.getFloat(1), scaling.getFloat(2)))
                kfN = keyframe.next
              }
            }
          }
          nodeN = node.next
        }
      }
      animN = anim.next
    }
  }
}

object G3dModelLoader {
  val VERSION_HI: Short = 0
  val VERSION_LO: Short = 1
}
