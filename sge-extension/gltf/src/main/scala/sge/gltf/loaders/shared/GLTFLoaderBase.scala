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

import scala.collection.mutable.{ ArrayBuffer, HashMap, HashSet }
import sge.Sge
import sge.utils.{ DynamicArray, Log }
import sge.graphics.{ Camera, Mesh, Pixmap, Texture }
import sge.graphics.g3d.{ Material, Model }
import sge.graphics.g3d.environment.BaseLight
import sge.graphics.g3d.model.{ MeshPart, Node, NodePart }
import sge.gltf.data.GLTF
import sge.gltf.data.camera.GLTFCamera
import sge.gltf.data.extensions.KHRLightsPunctual
import sge.gltf.data.scene.{ GLTFNode, GLTFScene }
import sge.gltf.loaders.exceptions.GLTFUnsupportedException
import sge.gltf.loaders.shared.animation.AnimationLoader
import sge.gltf.loaders.shared.data.{ DataFileResolver, DataResolver }
import sge.gltf.loaders.shared.geometry.MeshLoader
import sge.gltf.loaders.shared.material.{ MaterialLoader, PBRMaterialLoader }
import sge.gltf.loaders.shared.scene.{ NodeResolver, SkinLoader }
import sge.gltf.loaders.shared.texture.{ ImageResolver, TextureResolver }
import sge.gltf.scene3d.model.NodePlus
import sge.gltf.scene3d.scene.{ SceneAsset, SceneModel }
import sge.math.Matrix4
import sge.utils.Nullable

class GLTFLoaderBase(private var textureResolver: Nullable[TextureResolver] = Nullable.empty) extends AutoCloseable {

  protected var glModel:          Nullable[GLTF]             = Nullable.empty
  protected var dataFileResolver: Nullable[DataFileResolver] = Nullable.empty
  protected var materialLoader:   Nullable[MaterialLoader]   = Nullable.empty
  protected var dataResolver:     Nullable[DataResolver]     = Nullable.empty
  protected var imageResolver:    Nullable[ImageResolver]    = Nullable.empty

  private val animationLoader: AnimationLoader = new AnimationLoader()
  private val nodeResolver:    NodeResolver    = new NodeResolver()
  private val meshLoader:      MeshLoader      = new MeshLoader()
  private val skinLoader:      SkinLoader      = new SkinLoader()

  private val cameras:   ArrayBuffer[Camera]       = ArrayBuffer.empty
  private val lights:    ArrayBuffer[BaseLight[?]] = ArrayBuffer.empty
  private val cameraMap: HashMap[String, Int]      = HashMap.empty

  /** node name to light index */
  private val lightMap:     HashMap[String, Int]     = HashMap.empty
  private val loadedMeshes: HashSet[Mesh]            = HashSet.empty
  private val scenes:       DynamicArray[SceneModel] = DynamicArray[SceneModel]()

  def load(dataFileResolver: DataFileResolver, withData: Boolean)(using sge: Sge): SceneAsset =
    try {
      this.dataFileResolver = Nullable(dataFileResolver)

      val model = dataFileResolver.getRoot
      glModel = Nullable(model)

      // prerequisites (mandatory)
      model.extensionsRequired.foreach { required =>
        for (extension <- required)
          if (!GLTFLoaderBase.supportedExtensions.contains(extension)) {
            throw new GLTFUnsupportedException("Extension " + extension + " required but not supported")
          }
      }
      // prerequisites (optional)
      model.extensionsUsed.foreach { used =>
        for (extension <- used)
          if (!GLTFLoaderBase.supportedExtensions.contains(extension)) {
            Log.error(GLTFLoaderBase.TAG + ": " + "Extension " + extension + " used but not supported")
          }
      }

      // load deps from lower to higher

      // images (pixmaps)
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
      loadLights()
      loadScenes()

      animationLoader.load(model.animations, nodeResolver, dataResolver.get)
      skinLoader.load(model.skins, model.nodes, nodeResolver, dataResolver.get)

      // create scene asset
      val sceneAsset = new SceneAsset()
      if (withData) sceneAsset.data = model
      sceneAsset.scenes = Nullable(scenes)
      sceneAsset.scene = Nullable(scenes(model.scene))
      sceneAsset.maxBones = skinLoader.getMaxBones
      val texBuf = textureResolver.get.getTextures(ArrayBuffer[Texture]())
      val texArr = DynamicArray[Texture]()
      for (t <- texBuf) texArr.add(t)
      sceneAsset.textures = Nullable(texArr)
      imageResolver.foreach { ir =>
        val pixBuf = ir.getPixmaps(ArrayBuffer[Pixmap]())
        val pixArr = DynamicArray[Pixmap]()
        for (p <- pixBuf) pixArr.add(p)
        sceneAsset.pixmaps = Nullable(pixArr)
        ir.clear()
      }
      sceneAsset.animations = Nullable(animationLoader.animations)
      // XXX don't know where the animation are ...
      var si = 0
      while (si < scenes.size) {
        scenes(si).model.animations.addAll(animationLoader.animations)
        si += 1
      }

      val meshArray = DynamicArray[Mesh]()
      for (mesh <- loadedMeshes) meshArray.add(mesh)
      loadedMeshes.clear()
      sceneAsset.meshes = Nullable(meshArray)

      sceneAsset
    } catch {
      case e: RuntimeException =>
        close()
        throw e
    }

  protected def createMaterialLoader(textureResolver: TextureResolver): MaterialLoader =
    new PBRMaterialLoader(textureResolver)

  private def loadLights(): Unit = {
    val model = glModel.get
    model.extensions.foreach { exts =>
      val lightExt = exts.get(classOf[KHRLightsPunctual.GLTFLights], KHRLightsPunctual.EXT)
      lightExt.foreach { le =>
        le.lights.foreach { lts =>
          for (light <- lts)
            lights += KHRLightsPunctual.map(light)
        }
      }
    }
  }

  private def loadCameras(model: GLTF)(using sge: Sge): Unit =
    model.cameras.foreach { cams =>
      for (glCamera <- cams)
        cameras += GLTFTypes.map(glCamera)
    }

  private def loadScenes()(using sge: Sge): Unit = {
    val model = glModel.get
    model.scenes.foreach { scns =>
      var i = 0
      while (i < scns.size) {
        scenes.add(loadScene(scns(i)))
        i += 1
      }
    }
  }

  private def loadScene(gltfScene: GLTFScene)(using sge: Sge): SceneModel = {
    val sceneModel = new SceneModel()
    sceneModel.name = gltfScene.name
    sceneModel.model = new Model()

    // add root nodes
    gltfScene.nodes.foreach { nodeIndices =>
      for (id <- nodeIndices)
        sceneModel.model.nodes.add(getNode(id))
    }
    // add scene cameras (filter from all scenes cameras)
    for ((name, camIdx) <- cameraMap) {
      val node = sceneModel.model.getNode(name, true)
      node.foreach { n =>
        sceneModel.cameras.put(n, cameras(camIdx))
      }
    }
    // add scene lights (filter from all scenes lights)
    for ((name, lightIdx) <- lightMap) {
      val node = sceneModel.model.getNode(name, true)
      node.foreach { n =>
        sceneModel.lights.put(n, lights(lightIdx))
      }
    }

    // collect data references to store in model
    collectData(sceneModel.model, sceneModel.model.nodes)

    for (mesh <- GLTFLoaderBase.meshSet) loadedMeshes.add(mesh)

    copySetToArray(GLTFLoaderBase.meshSet, sceneModel.model.meshes)
    copySetToArray(GLTFLoaderBase.meshPartSet, sceneModel.model.meshParts)
    copySetToArray(GLTFLoaderBase.materialSet, sceneModel.model.materials)

    GLTFLoaderBase.meshSet.clear()
    GLTFLoaderBase.meshPartSet.clear()
    GLTFLoaderBase.materialSet.clear()

    sceneModel
  }

  private def collectData(model: Model, nodes: DynamicArray[Node]): Unit =
    for (node <- nodes) {
      for (part <- node.parts) {
        GLTFLoaderBase.meshSet.add(part.meshPart.mesh)
        GLTFLoaderBase.meshPartSet.add(part.meshPart)
        GLTFLoaderBase.materialSet.add(part.material)
      }
      collectData(model, node.children)
    }

  private def copySetToArray[T](src: HashSet[T], dst: DynamicArray[T]): Unit =
    for (e <- src) dst.add(e)

  private def getNode(id: Int)(using sge: Sge): Node =
    nodeResolver
      .get(id)
      .fold {
        val node = new NodePlus()
        nodeResolver.put(id, node)

        val model  = glModel.get
        val glNode = model.nodes.get(id)

        glNode.matrix.foreach { mat =>
          val matrix = new Matrix4(mat)
          matrix.translation(node.translation)
          matrix.getScale(node.scale)
          matrix.rotation(node.rotation, true)
        }
        if (glNode.matrix.isEmpty) {
          glNode.translation.foreach(t => GLTFTypes.map(node.translation, t))
          glNode.rotation.foreach(r => GLTFTypes.map(node.rotation, r))
          glNode.scale.foreach(s => GLTFTypes.map(node.scale, s))
        }

        node.id = glNode.name.getOrElse("glNode " + id)

        glNode.children.foreach { children =>
          for (childId <- children)
            node.addChild(getNode(childId))
        }

        glNode.mesh.foreach { meshIdx =>
          model.meshes.foreach { meshes =>
            meshLoader.load(node, meshes(meshIdx), dataResolver.get, materialLoader.get)
          }
        }

        glNode.camera.foreach { camIdx =>
          cameraMap.put(node.id, camIdx)
        }

        // node extensions
        glNode.extensions.foreach { exts =>
          val nodeLight = exts.get(classOf[KHRLightsPunctual.GLTFLightNode], KHRLightsPunctual.EXT)
          nodeLight.foreach { nl =>
            nl.light.foreach { lightIdx =>
              lightMap.put(node.id, lightIdx)
            }
          }
        }

        node
      }(identity)

  override def close(): Unit = {
    imageResolver.foreach(_.close())
    textureResolver.foreach(_.close())
    var i = 0
    while (i < scenes.size) {
      scenes(i).close()
      i += 1
    }
    for (mesh <- loadedMeshes)
      mesh.close()
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

  private val materialSet: HashSet[Material] = HashSet.empty
  private val meshPartSet: HashSet[MeshPart] = HashSet.empty
  private val meshSet:     HashSet[Mesh]     = HashSet.empty
}
