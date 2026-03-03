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

import scala.language.implicitConversions

import sge.assets.loaders.{ FileHandleResolver, ModelLoader }
import sge.files.FileHandle
import sge.graphics.{ Color, GL20, VertexAttribute }
import sge.graphics.g3d.model.data._
import sge.math.{ Matrix4, Quaternion, Vector2, Vector3 }
import sge.utils.{ ArrayMap, BaseJsonReader, DynamicArray, JsonValue, Nullable, SgeError }

/** Loads G3D models from `.g3dj` (JSON text) files. Binary `.g3db` format (UBJson) is not yet supported. */
class G3dModelLoader(val reader: BaseJsonReader, resolver: FileHandleResolver)(using Sge) extends ModelLoader[ModelLoader.ModelParameters](resolver) {

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

    model.id = json.getString("id", Nullable("")).getOrElse("")
    parseMeshes(model, json)
    parseMaterials(model, json, handle.parent().path())
    parseNodes(model, json)
    parseAnimations(model, json)
    model
  }

  protected def parseMeshes(model: ModelData, json: JsonValue): Unit =
    json.get("meshes").foreach { meshes =>
      model.meshes.ensureCapacity(meshes.size)
      var meshN = meshes.child
      while (meshN.isDefined)
        meshN.foreach { mesh =>
          val jsonMesh = new ModelMesh()

          jsonMesh.id = mesh.getString("id", Nullable("")).getOrElse("")

          val attributes = mesh.require("attributes")
          jsonMesh.attributes = parseAttributes(attributes)
          jsonMesh.vertices = mesh.require("vertices").asFloatArray()

          val meshParts = mesh.require("parts")
          val parts     = DynamicArray[ModelMeshPart]()
          var meshPartN = meshParts.child
          while (meshPartN.isDefined)
            meshPartN.foreach { meshPart =>
              val jsonPart = new ModelMeshPart()
              val partId   = mesh.getString("id", Nullable.empty).getOrElse {
                throw SgeError.InvalidInput("Not id given for mesh part")
              }
              for (other <- parts)
                if (other.id.equals(partId)) {
                  throw SgeError.InvalidInput("Mesh part with id '" + partId + "' already in defined")
                }
              jsonPart.id = partId

              val tpe = meshPart.getString("type", Nullable.empty).getOrElse {
                throw SgeError.InvalidInput("No primitive type given for mesh part '" + partId + "'")
              }
              jsonPart.primitiveType = parseType(tpe)

              jsonPart.indices = meshPart.require("indices").asShortArray()
              parts.add(jsonPart)
              meshPartN = meshPart.next
            }
          jsonMesh.parts = parts.toArray
          model.meshes.add(jsonMesh)
          meshN = mesh.next
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
    val vertexAttributes = DynamicArray[VertexAttribute]()
    var unit             = 0
    var blendWeightCount = 0
    var valueN           = attributes.child
    while (valueN.isDefined) {
      val value = valueN.getOrElse(throw SgeError.InvalidInput("Unexpected empty attribute"))
      val attr  = value.asString().getOrElse(throw SgeError.InvalidInput("Attribute must be a string"))
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
      valueN = value.next
    }
    vertexAttributes.toArray
  }

  protected def parseMaterials(model: ModelData, json: JsonValue, materialDir: String): Unit =
    // we should probably create some default material in this case
    json.get("materials").foreach { materials =>
      model.materials.ensureCapacity(materials.size)
      var materialN = materials.child
      while (materialN.isDefined)
        materialN.foreach { material =>
          val jsonMaterial = new ModelMaterial()

          jsonMaterial.id = material.getString("id", Nullable.empty).getOrElse {
            throw SgeError.InvalidInput("Material needs an id.")
          }

          // Read material colors
          material.get("diffuse").foreach(v => jsonMaterial.diffuse = parseColor(v))
          material.get("ambient").foreach(v => jsonMaterial.ambient = parseColor(v))
          material.get("emissive").foreach(v => jsonMaterial.emissive = parseColor(v))
          material.get("specular").foreach(v => jsonMaterial.specular = parseColor(v))
          material.get("reflection").foreach(v => jsonMaterial.reflection = parseColor(v))
          // Read shininess
          jsonMaterial.shininess = material.getFloat("shininess", 0.0f)
          // Read opacity
          jsonMaterial.opacity = material.getFloat("opacity", 1.0f)

          // Read textures
          material.get("textures").foreach { textures =>
            var textureN = textures.child
            while (textureN.isDefined)
              textureN.foreach { texture =>
                val jsonTexture = new ModelTexture()

                jsonTexture.id = texture.getString("id", Nullable.empty).getOrElse {
                  throw SgeError.InvalidInput("Texture has no id.")
                }

                val fileName = texture.getString("filename", Nullable.empty).getOrElse {
                  throw SgeError.InvalidInput("Texture needs filename.")
                }
                jsonTexture.fileName = materialDir + (if (materialDir.isEmpty || materialDir.endsWith("/")) "" else "/") +
                  fileName

                jsonTexture.uvTranslation = readVector2(texture.get("uvTranslation"), 0f, 0f)
                jsonTexture.uvScaling = readVector2(texture.get("uvScaling"), 1f, 1f)

                val textureType = texture.getString("type", Nullable.empty).getOrElse {
                  throw SgeError.InvalidInput("Texture needs type.")
                }

                jsonTexture.usage = parseTextureUsage(textureType)

                if (Nullable(jsonMaterial.textures).isEmpty) jsonMaterial.textures = DynamicArray[ModelTexture]()
                jsonMaterial.textures.add(jsonTexture)
                textureN = texture.next
              }
          }

          model.materials.add(jsonMaterial)
          materialN = material.next
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

  protected def readVector2(vectorArray: Nullable[JsonValue], x: Float, y: Float): Vector2 =
    vectorArray.fold(new Vector2(x, y)) { v =>
      if (v.size == 2)
        new Vector2(v.getFloat(0), v.getFloat(1))
      else
        throw SgeError.InvalidInput("Expected Vector2 values <> than two.")
    }

  protected def parseNodes(model: ModelData, json: JsonValue): DynamicArray[ModelNode] = {
    json.get("nodes").foreach { nodes =>
      model.nodes.ensureCapacity(nodes.size)
      var nodeN = nodes.child
      while (nodeN.isDefined)
        nodeN.foreach { n =>
          model.nodes.add(parseNodesRecursively(n))
          nodeN = n.next
        }
    }

    model.nodes
  }

  protected val tempQ: Quaternion = new Quaternion()

  protected def parseNodesRecursively(json: JsonValue): ModelNode = {
    val jsonNode = new ModelNode()

    jsonNode.id = json.getString("id", Nullable.empty).getOrElse {
      throw SgeError.InvalidInput("Node id missing.")
    }

    json.get("translation").foreach { t =>
      if (t.size != 3) throw SgeError.InvalidInput("Node translation incomplete")
      jsonNode.translation = new Vector3(t.getFloat(0), t.getFloat(1), t.getFloat(2))
    }

    json.get("rotation").foreach { r =>
      if (r.size != 4) throw SgeError.InvalidInput("Node rotation incomplete")
      jsonNode.rotation = new Quaternion(r.getFloat(0), r.getFloat(1), r.getFloat(2), r.getFloat(3))
    }

    json.get("scale").foreach { s =>
      if (s.size != 3) throw SgeError.InvalidInput("Node scale incomplete")
      jsonNode.scale = new Vector3(s.getFloat(0), s.getFloat(1), s.getFloat(2))
    }

    json.getString("mesh", Nullable.empty).foreach(m => jsonNode.meshId = m)

    json.get("parts").foreach { materials =>
      jsonNode.parts = new Array[ModelNodePart](materials.size)
      var i         = 0
      var materialN = materials.child
      while (materialN.isDefined)
        materialN.foreach { material =>
          val nodePart = new ModelNodePart()

          val meshPartId = material.getString("meshpartid", Nullable.empty)
          val materialId = material.getString("materialid", Nullable.empty)
          if (meshPartId.isEmpty || materialId.isEmpty) {
            throw SgeError.InvalidInput("Node " + jsonNode.id + " part is missing meshPartId or materialId")
          }
          nodePart.materialId = materialId.getOrElse("")
          nodePart.meshPartId = meshPartId.getOrElse("")

          material.get("bones").foreach { bones =>
            nodePart.bones = ArrayMap[String, Matrix4](true, bones.size)
            var boneN = bones.child
            while (boneN.isDefined)
              boneN.foreach { bone =>
                val nodeId = bone.getString("node", Nullable.empty).getOrElse {
                  throw SgeError.InvalidInput("Bone node ID missing")
                }

                val transform = new Matrix4()

                bone.get("translation").foreach { v =>
                  if (v.size >= 3) transform.translate(v.getFloat(0), v.getFloat(1), v.getFloat(2))
                }
                bone.get("rotation").foreach { v =>
                  if (v.size >= 4)
                    transform.rotate(tempQ.set(v.getFloat(0), v.getFloat(1), v.getFloat(2), v.getFloat(3)))
                }
                bone.get("scale").foreach { v =>
                  if (v.size >= 3) transform.scale(v.getFloat(0), v.getFloat(1), v.getFloat(2))
                }

                nodePart.bones.put(nodeId, transform)
                boneN = bone.next
              }
          }

          jsonNode.parts(i) = nodePart
          i += 1
          materialN = material.next
        }
    }

    json.get("children").foreach { children =>
      jsonNode.children = new Array[ModelNode](children.size)

      var i      = 0
      var childN = children.child
      while (childN.isDefined)
        childN.foreach { c =>
          jsonNode.children(i) = parseNodesRecursively(c)
          i += 1
          childN = c.next
        }
    }

    jsonNode
  }

  protected def parseAnimations(model: ModelData, json: JsonValue): Unit = {
    json.get("animations").foreach { animations =>
      model.animations.ensureCapacity(animations.size)

      var animN = animations.child
      while (animN.isDefined) {
        animN.foreach { anim =>
          anim.get("bones").foreach { nodes =>
            val animation = new ModelAnimation()
            model.animations.add(animation)
            animation.nodeAnimations.ensureCapacity(nodes.size)
            animation.id = anim.getString("id").getOrElse("")
            var nodeN = nodes.child
            while (nodeN.isDefined) {
              nodeN.foreach { node =>
                val nodeAnim = new ModelNodeAnimation()
                animation.nodeAnimations.add(nodeAnim)
                nodeAnim.nodeId = node.getString("boneId").getOrElse("")

                // For backwards compatibility (version 0.1):
                val keyframes = node.get("keyframes")
                if (keyframes.fold(false)(_.isArray)) {
                  var keyframeN = keyframes.fold(Nullable.empty[JsonValue])(_.child)
                  while (keyframeN.isDefined)
                    keyframeN.foreach { keyframe =>
                      val keytime = keyframe.getFloat("keytime", 0f) / 1000.0f
                      keyframe.get("translation").foreach { translation =>
                        if (translation.size == 3) {
                          if (Nullable(nodeAnim.translation).isEmpty) nodeAnim.translation = DynamicArray[ModelNodeKeyframe[Vector3]]()
                          val tkf = new ModelNodeKeyframe[Vector3]()
                          tkf.keytime = keytime
                          tkf.value = Nullable(
                            new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))
                          )
                          nodeAnim.translation.add(tkf)
                        }
                      }
                      keyframe.get("rotation").foreach { rotation =>
                        if (rotation.size == 4) {
                          if (Nullable(nodeAnim.rotation).isEmpty) nodeAnim.rotation = DynamicArray[ModelNodeKeyframe[Quaternion]]()
                          val rkf = new ModelNodeKeyframe[Quaternion]()
                          rkf.keytime = keytime
                          rkf.value = Nullable(
                            new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3))
                          )
                          nodeAnim.rotation.add(rkf)
                        }
                      }
                      keyframe.get("scale").foreach { scale =>
                        if (scale.size == 3) {
                          if (Nullable(nodeAnim.scaling).isEmpty) nodeAnim.scaling = DynamicArray[ModelNodeKeyframe[Vector3]]()
                          val skf = new ModelNodeKeyframe[Vector3]()
                          skf.keytime = keytime
                          skf.value = Nullable(new Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2)))
                          nodeAnim.scaling.add(skf)
                        }
                      }
                      keyframeN = keyframe.next
                    }
                } else { // Version 0.2:
                  node.get("translation").foreach { translationKF =>
                    if (translationKF.isArray) {
                      nodeAnim.translation = DynamicArray[ModelNodeKeyframe[Vector3]]()
                      nodeAnim.translation.ensureCapacity(translationKF.size)
                      var kfN = translationKF.child
                      while (kfN.isDefined)
                        kfN.foreach { kfVal =>
                          val kf = new ModelNodeKeyframe[Vector3]()
                          nodeAnim.translation.add(kf)
                          kf.keytime = kfVal.getFloat("keytime", 0f) / 1000.0f
                          kfVal.get("value").foreach { translation =>
                            if (translation.size >= 3)
                              kf.value = Nullable(
                                new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2))
                              )
                          }
                          kfN = kfVal.next
                        }
                    }
                  }

                  node.get("rotation").foreach { rotationKF =>
                    if (rotationKF.isArray) {
                      nodeAnim.rotation = DynamicArray[ModelNodeKeyframe[Quaternion]]()
                      nodeAnim.rotation.ensureCapacity(rotationKF.size)
                      var kfN = rotationKF.child
                      while (kfN.isDefined)
                        kfN.foreach { kfVal =>
                          val kf = new ModelNodeKeyframe[Quaternion]()
                          nodeAnim.rotation.add(kf)
                          kf.keytime = kfVal.getFloat("keytime", 0f) / 1000.0f
                          kfVal.get("value").foreach { rotation =>
                            if (rotation.size >= 4)
                              kf.value = Nullable(
                                new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3))
                              )
                          }
                          kfN = kfVal.next
                        }
                    }
                  }

                  node.get("scaling").foreach { scalingKF =>
                    if (scalingKF.isArray) {
                      nodeAnim.scaling = DynamicArray[ModelNodeKeyframe[Vector3]]()
                      nodeAnim.scaling.ensureCapacity(scalingKF.size)
                      var kfN = scalingKF.child
                      while (kfN.isDefined)
                        kfN.foreach { kfVal =>
                          val kf = new ModelNodeKeyframe[Vector3]()
                          nodeAnim.scaling.add(kf)
                          kf.keytime = kfVal.getFloat("keytime", 0f) / 1000.0f
                          kfVal.get("value").foreach { scaling =>
                            if (scaling.size >= 3)
                              kf.value = Nullable(new Vector3(scaling.getFloat(0), scaling.getFloat(1), scaling.getFloat(2)))
                          }
                          kfN = kfVal.next
                        }
                    }
                  }
                }
                nodeN = node.next
              }
            }
          }
          animN = anim.next
        }
      }
    }
  }
}

object G3dModelLoader {
  val VERSION_HI: Short = 0
  val VERSION_LO: Short = 1
}
