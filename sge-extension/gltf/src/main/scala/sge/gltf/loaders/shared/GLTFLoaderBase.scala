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

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}
import sge.Sge
import sge.utils.Log
import sge.graphics.{Camera, Mesh, Pixmap, Texture}
import sge.graphics.g3d.{Material, Model}
import sge.graphics.g3d.model.{MeshPart, Node, NodePart}
import sge.gltf.data.GLTF
import sge.gltf.data.camera.GLTFCamera
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.data.scene.{GLTFNode, GLTFScene}
import sge.gltf.loaders.exceptions.GLTFUnsupportedException
import sge.gltf.loaders.shared.animation.AnimationLoader
import sge.gltf.loaders.shared.data.{DataFileResolver, DataResolver}
import sge.gltf.loaders.shared.geometry.MeshLoader
import sge.gltf.loaders.shared.material.{MaterialLoader, PBRMaterialLoader}
import sge.gltf.loaders.shared.scene.{NodeResolver, SkinLoader}
import sge.gltf.loaders.shared.texture.{ImageResolver, TextureResolver}
import sge.math.Matrix4
import sge.utils.Nullable

class GLTFLoaderBase(private var textureResolver: Nullable[TextureResolver] = Nullable.empty) extends AutoCloseable {

  protected var glModel: Nullable[GLTF] = Nullable.empty
  protected var dataFileResolver: Nullable[DataFileResolver] = Nullable.empty
  protected var materialLoader: Nullable[MaterialLoader] = Nullable.empty
  protected var dataResolver: Nullable[DataResolver] = Nullable.empty
  protected var imageResolver: Nullable[ImageResolver] = Nullable.empty

  private val animationLoader: AnimationLoader = new AnimationLoader()
  private val nodeResolver: NodeResolver = new NodeResolver()
  private val meshLoader: MeshLoader = new MeshLoader()
  private val skinLoader: SkinLoader = new SkinLoader()

  private val cameras: ArrayBuffer[Camera] = ArrayBuffer.empty
  private val cameraMap: HashMap[String, Int] = HashMap.empty
  private val loadedMeshes: HashSet[Mesh] = HashSet.empty

  def load(dataFileResolver: DataFileResolver, withData: Boolean)(using sge: Sge): Model = {
    try {
      this.dataFileResolver = Nullable(dataFileResolver)

      val model = dataFileResolver.getRoot
      glModel = Nullable(model)

      // prerequisites (mandatory)
      model.extensionsRequired.foreach { required =>
        for (extension <- required) {
          if (!GLTFLoaderBase.supportedExtensions.contains(extension)) {
            throw new GLTFUnsupportedException("Extension " + extension + " required but not supported")
          }
        }
      }
      // prerequisites (optional)
      model.extensionsUsed.foreach { used =>
        for (extension <- used) {
          if (!GLTFLoaderBase.supportedExtensions.contains(extension)) {
            Log.error(GLTFLoaderBase.TAG + ": " + "Extension " + extension + " used but not supported")
          }
        }
      }

      // load deps from lower to higher
      dataResolver = Nullable(new DataResolver(model, dataFileResolver))

      if (textureResolver.isEmpty) {
        val imgResolver = new ImageResolver(dataFileResolver)
        imageResolver = Nullable(imgResolver)
        imgResolver.load(model.images)
        val texResolver = new TextureResolver()
        textureResolver = Nullable(texResolver)
        texResolver.loadTextures(model.textures, model.samplers, imgResolver)
      }

      val matLoader = createMaterialLoader(textureResolver.get)
      materialLoader = Nullable(matLoader)
      matLoader.loadMaterials(model.materials)

      loadCameras(model)

      // Build a combined model from all scenes
      val resultModel = new Model()

      model.scenes.foreach { scenes =>
        var i = 0
        while (i < scenes.size) {
          loadScene(scenes(i), resultModel)
          i += 1
        }
      }

      animationLoader.load(model.animations, nodeResolver, dataResolver.get)
      skinLoader.load(model.skins, model.nodes, nodeResolver, dataResolver.get)

      // add animations to the model
      resultModel.animations.addAll(animationLoader.animations)

      resultModel
    } catch {
      case e: RuntimeException =>
        close()
        throw e
    }
  }

  protected def createMaterialLoader(textureResolver: TextureResolver): MaterialLoader = {
    new PBRMaterialLoader(textureResolver)
  }

  private def loadCameras(model: GLTF)(using sge: Sge): Unit = {
    model.cameras.foreach { cams =>
      for (glCamera <- cams) {
        cameras += GLTFTypes.map(glCamera)
      }
    }
  }

  private def loadScene(gltfScene: GLTFScene, resultModel: Model)(using sge: Sge): Unit = {
    // add root nodes
    gltfScene.nodes.foreach { nodeIndices =>
      for (id <- nodeIndices) {
        resultModel.nodes += getNode(id)
      }
    }

    // collect meshes, mesh parts, and materials from nodes
    collectData(resultModel, resultModel.nodes)
  }

  private def collectData(model: Model, nodes: sge.utils.DynamicArray[Node]): Unit = {
    for (node <- nodes) {
      for (part <- node.parts) {
        if (!model.meshes.contains(part.meshPart.mesh)) {
          model.meshes += part.meshPart.mesh
        }
        model.meshParts += part.meshPart
        if (!model.materials.contains(part.material)) {
          model.materials += part.material
        }
      }
      collectData(model, node.children)
    }
  }

  private def getNode(id: Int)(using sge: Sge): Node = {
    nodeResolver.get(id).fold {
      val node = new Node()
      nodeResolver.put(id, node)

      val model = glModel.get
      val glNode = model.nodes.get(id)

      glNode.matrix.foreach { mat =>
        val matrix = new Matrix4(mat)
        matrix.translation(node.translation)
        matrix.getScale(node.scale)
        matrix.rotation(node.rotation, true)
      }
      if (glNode.matrix.isEmpty) {
        glNode.translation.foreach { t => GLTFTypes.map(node.translation, t) }
        glNode.rotation.foreach { r => GLTFTypes.map(node.rotation, r) }
        glNode.scale.foreach { s => GLTFTypes.map(node.scale, s) }
      }

      node.id = glNode.name.getOrElse("glNode " + id)

      glNode.children.foreach { children =>
        for (childId <- children) {
          node.addChild(getNode(childId))
        }
      }

      glNode.mesh.foreach { meshIdx =>
        model.meshes.foreach { meshes =>
          meshLoader.load(node, meshes(meshIdx), dataResolver.get, materialLoader.get)
        }
      }

      glNode.camera.foreach { camIdx =>
        cameraMap.put(node.id, camIdx)
      }

      node
    }(identity)
  }

  override def close(): Unit = {
    imageResolver.foreach(_.close())
    textureResolver.foreach(_.close())
    for (mesh <- loadedMeshes) {
      mesh.close()
    }
    loadedMeshes.clear()
  }
}

object GLTFLoaderBase {
  val TAG: String = "GLTF"

  val supportedExtensions: HashSet[String] = HashSet(
    sge.gltf.data.extensions.KHRMaterialsPBRSpecularGlossiness.EXT,
    sge.gltf.data.extensions.KHRTextureTransform.EXT,
    sge.gltf.data.extensions.KHRLightsPunctual.EXT,
    sge.gltf.data.extensions.KHRMaterialsUnlit.EXT,
    sge.gltf.data.extensions.KHRMaterialsTransmission.EXT,
    sge.gltf.data.extensions.KHRMaterialsVolume.EXT,
    sge.gltf.data.extensions.KHRMaterialsIOR.EXT,
    sge.gltf.data.extensions.KHRMaterialsSpecular.EXT,
    sge.gltf.data.extensions.KHRMaterialsIridescence.EXT,
    sge.gltf.data.extensions.KHRMaterialsEmissiveStrength.EXT
  )
}
