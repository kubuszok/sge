/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import scala.collection.mutable.ArrayBuffer

import sge.Sge
import sge.files.FileHandle
import sge.graphics.{ Mesh, PrimitiveMode, Texture }
import sge.graphics.g3d.{ Material, Model }
import sge.graphics.g3d.model.{ MeshPart, Node, NodePart }
import sge.gltf.data.{ GLTF, GLTFAsset }
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.data.geometry.{ GLTFMesh, GLTFPrimitive }
import sge.gltf.data.scene.{ GLTFNode, GLTFScene }
import sge.gltf.loaders.exceptions.{ GLTFIllegalException, GLTFRuntimeException }
import sge.gltf.scene3d.scene.{ Scene, SceneAsset, SceneModel }
import sge.utils.{ DynamicArray, Nullable }

class GLTFExporter(private val config: GLTFExporterConfig)(using Sge) {

  private val GeneratorInfo: String = "SGE glTF exporter 1.0"

  var root:                 GLTF               = scala.compiletime.uninitialized // @nowarn — reset before each export
  var binManager:           GLTFBinaryExporter = scala.compiletime.uninitialized // @nowarn — reset before each export
  private val meshExporter: GLTFMeshExporter   = new GLTFMeshExporter(this)

  val nodeMapping:     DynamicArray[Node]     = DynamicArray[Node]()
  val materialMapping: DynamicArray[Material] = DynamicArray[Material]()
  val textureMapping:  DynamicArray[Texture]  = DynamicArray[Texture]()

  /** current texture file index */
  protected var textureFileIndex: Int = 0

  /** current file handle name without extension */
  protected var fileHandleName: String = ""

  /** create with default config.
    */
  def this()(using Sge) =
    this(new GLTFExporterConfig())

  /** sub class may override this method in order to implement some custom name mapping.
    * @return
    *   a unique name for the texture
    */
  private[exporters] def getImageName(texture: Texture): String = {
    val name = fileHandleName + "texture" + textureFileIndex
    textureFileIndex += 1
    name
  }

  private def reset(): Unit = {
    root = null.asInstanceOf[GLTF] // @nowarn — reset between exports
    binManager.reset()
    nodeMapping.clear()
    materialMapping.clear()
    textureMapping.clear()
    textureFileIndex = 0
  }

  /** convenient method to export a single mesh primitiveType can be any of OpenGL primitive: GL_POINTS, GL_LINES, GL_LINE_STRIP, GL_TRIANGLES, GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN, etc.
    */
  def exportMesh(mesh: Mesh, primitiveType: PrimitiveMode, file: FileHandle): Unit = {
    val scene = beginSingleScene(file)

    val glNode = obtainNode()
    scene.nodes = Nullable(ArrayBuffer[Int]())
    scene.nodes.get += root.nodes.get.size - 1

    val gltfMesh = obtainMesh()
    glNode.mesh = Nullable(root.meshes.get.size - 1)

    val meshPart = new MeshPart()
    meshPart.mesh = mesh
    meshPart.offset = 0
    meshPart.primitiveType = primitiveType
    meshPart.size = mesh.numIndices
    if (meshPart.size == 0) meshPart.size = mesh.numVertices

    gltfMesh.primitives = Nullable(ArrayBuffer[GLTFPrimitive]())
    val primitive = meshExporter.exportMeshPart(meshPart)
    gltfMesh.primitives.get += primitive

    end(file)
  }

  /** convenient method to export a single model */
  def exportModel(model: Model, file: FileHandle): Unit = {
    val scene = beginSingleScene(file)

    new GLTFMaterialExporter(this).exportMaterials(model.nodes)

    scene.nodes = exportNodes(scene, model.nodes)

    new GLTFSkinExporter(this).exportSkins()
    new GLTFAnimationExporter(this).exportAnimations(model.animations)

    end(file)
  }

  /** convenient method to export a single scene */
  def exportScene(scene: Scene, file: FileHandle): Unit = {
    val glScene = beginSingleScene(file)

    doExportScene(glScene, scene)

    end(file)
  }

  /** convenient method to export a single scene */
  def exportSceneModel(scene: SceneModel, file: FileHandle): Unit = {
    val glScene = beginSingleScene(file)

    doExportScene(glScene, scene)

    end(file)
  }

  private def doExportScene(glScene: GLTFScene, scene: Scene): Unit = {
    new GLTFMaterialExporter(this).exportMaterials(scene.modelInstance.nodes)

    glScene.nodes = exportNodes(glScene, scene.modelInstance.nodes)

    if (config.exportCameras) {
      new GLTFCameraExporter(this).exportCameras(scene.cameras)
    }
    if (config.exportLights) {
      new GLTFLightExporter(this).exportLights(scene.lights)
    }

    new GLTFSkinExporter(this).exportSkins()
    new GLTFAnimationExporter(this).exportAnimations(scene.modelInstance.animations)
  }

  private def doExportScene(glScene: GLTFScene, scene: SceneModel): Unit = {
    new GLTFMaterialExporter(this).exportMaterials(scene.model.nodes)

    glScene.nodes = exportNodes(glScene, scene.model.nodes)

    if (config.exportCameras) {
      new GLTFCameraExporter(this).exportCameras(scene.cameras)
    }
    if (config.exportLights) {
      new GLTFLightExporter(this).exportLights(scene.lights)
    }

    new GLTFSkinExporter(this).exportSkins()
    new GLTFAnimationExporter(this).exportAnimations(scene.model.animations)
  }

  /** multi scene export */
  def exportAsset(asset: SceneAsset, file: FileHandle): Unit =
    exportSceneModels(asset.scenes.get, asset.scene.get, file)

  /** multi scene export */
  def exportSceneModels(scenes: DynamicArray[SceneModel], defaultScene: SceneModel, file: FileHandle): Unit = {
    beginMultiScene(file)

    var i = 0
    while (i < scenes.size) {
      val glScene = obtainScene()
      doExportScene(glScene, scenes(i))
      i += 1
    }

    root.scene = scenes.indexOfByRef(defaultScene)
    if (root.scene < 0) throw new GLTFIllegalException("scene not found")

    end(file)
  }

  /** multi scene export (Scene variant) */
  def exportScenes(scenes: DynamicArray[Scene], defaultScene: Scene, file: FileHandle): Unit = {
    beginMultiScene(file)

    var i = 0
    while (i < scenes.size) {
      val glScene = obtainScene()
      doExportScene(glScene, scenes(i))
      i += 1
    }

    root.scene = scenes.indexOfByRef(defaultScene)
    if (root.scene < 0) throw new GLTFIllegalException("scene not found")

    end(file)
  }

  private def beginMultiScene(file: FileHandle): Unit = {
    // get fileHandleName without the extension
    fileHandleName = file.nameWithoutExtension + "_"

    binManager = new GLTFBinaryExporter(file.parent(), config)

    root = new GLTF()
    root.asset = Nullable(new GLTFAsset())
    root.asset.get.version = Nullable("2.0")
    root.asset.get.generator = Nullable(GeneratorInfo)

    root.scenes = Nullable(ArrayBuffer[GLTFScene]())
  }

  private def beginSingleScene(file: FileHandle): GLTFScene = {
    beginMultiScene(file)
    root.scene = 0
    obtainScene()
  }

  private def obtainScene(): GLTFScene = {
    val scene = new GLTFScene()
    root.scenes.get += scene
    scene
  }

  private def end(file: FileHandle): Unit = {
    root.bufferViews = Nullable(binManager.views)
    root.buffers = Nullable(binManager.flushAllToFiles(file.nameWithoutExtension))

    val json = GLTFExporterJson.writeGltf(root)
    file.writeString(json, false)

    reset()
  }

  private def exportNodes(scene: GLTFScene, nodes: DynamicArray[Node]): Nullable[ArrayBuffer[Int]] = {
    var indices: Nullable[ArrayBuffer[Int]] = Nullable.empty
    var nIdx = 0
    while (nIdx < nodes.size) {
      val node = nodes(nIdx)
      // create node
      val data = obtainNode()
      nodeMapping.add(node)
      data.name = Nullable(node.id)

      // transform, either a matrix or individual component (we use individual components but it might be an option)
      if (!node.translation.isZero) {
        data.translation = Nullable(GLTFExportTypes.toArray(node.translation))
      }
      if (!node.scale.epsilonEquals(1, 1, 1, sge.math.MathUtils.FLOAT_ROUNDING_ERROR)) {
        data.scale = Nullable(GLTFExportTypes.toArray(node.scale))
      }
      if (!node.rotation.isIdentity()) {
        data.rotation = Nullable(GLTFExportTypes.toArray(node.rotation))
      }

      // indexing node
      if (indices.isEmpty) indices = Nullable(ArrayBuffer[Int]())
      indices.get += root.nodes.get.size - 1

      // create mesh
      if (node.parts.size > 0) {
        val gltfMesh = obtainMesh()
        data.mesh = Nullable(root.meshes.get.size - 1)

        gltfMesh.primitives = Nullable(ArrayBuffer[GLTFPrimitive]())
        var pIdx = 0
        while (pIdx < node.parts.size) {
          val nodePart      = node.parts(pIdx)
          val primitive     = meshExporter.exportMeshPart(nodePart.meshPart)
          val materialIndex = materialMapping.indexOfByRef(nodePart.material)
          if (materialIndex < 0) throw new GLTFRuntimeException("material not found")
          primitive.material = Nullable(materialIndex)
          gltfMesh.primitives.get += primitive
          pIdx += 1
        }
      }

      // recursive children export
      data.children = exportNodes(scene, node.children)
      nIdx += 1
    }
    indices
  }

  def obtainAccessor(): GLTFAccessor = {
    val a = new GLTFAccessor()
    if (root.accessors.isEmpty) root.accessors = Nullable(ArrayBuffer[GLTFAccessor]())
    root.accessors.get += a
    a
  }

  private def obtainNode(): GLTFNode = {
    val data = new GLTFNode()
    if (root.nodes.isEmpty) root.nodes = Nullable(ArrayBuffer[GLTFNode]())
    root.nodes.get += data
    data
  }

  private def obtainMesh(): GLTFMesh = {
    val data = new GLTFMesh()
    if (root.meshes.isEmpty) root.meshes = Nullable(ArrayBuffer[GLTFMesh]())
    root.meshes.get += data
    data
  }

  def useExtension(ext: String, required: Boolean): Unit = {
    if (root.extensionsUsed.isEmpty) {
      root.extensionsUsed = Nullable(ArrayBuffer[String]())
    }
    if (!root.extensionsUsed.get.contains(ext)) {
      root.extensionsUsed.get += ext
    }
    if (required) {
      if (root.extensionsRequired.isEmpty) {
        root.extensionsRequired = Nullable(ArrayBuffer[String]())
      }
      if (!root.extensionsRequired.get.contains(ext)) {
        root.extensionsRequired.get += ext
      }
    }
  }
}
