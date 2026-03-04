/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Model.java
 * Original authors: badlogic, xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Disposable -> AutoCloseable (dispose -> close).
 *   - All public fields match (materials, nodes, animations, meshes, meshParts, disposables).
 *   - Constructors require (using Sge) for texture loading.
 *   - nodePartBones uses scala.collection.mutable.Map instead of ObjectMap.
 *   - loadAnimations: null/continue pattern -> Nullable.foreach with isDefined guard.
 *   - convertMesh: indicesBuffer cast to Buffer for clear/position (same JDK9 workaround).
 *   - convertMaterial: uses scala mutable.Map for texture dedup vs ObjectMap.
 *   - TextureAttribute constructor called with extra index=0 arg (SGE signature difference).
 *   - getManagedDisposables returns DynamicArray (Java returns Iterable).
 *   - All lookup methods (getAnimation, getMaterial, getNode) return Nullable.
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d

import java.nio.Buffer

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.{ GL20, Mesh, Texture, VertexAttributes }
import sge.graphics.g3d.attributes.{ BlendingAttribute, ColorAttribute, FloatAttribute, TextureAttribute }
import sge.graphics.g3d.model.{ Animation, MeshPart, Node, NodeAnimation, NodeKeyframe, NodePart }
import sge.graphics.g3d.model.data._
import sge.graphics.g3d.utils.{ TextureDescriptor, TextureProvider }
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.{ ArrayMap, BufferUtils, DynamicArray, Nullable, SgeError }

/** A model represents a 3D assets. It stores a hierarchy of nodes. A node has a transform and optionally a graphical part in form of a {@link MeshPart} and {@link Material}. Mesh parts reference
  * subsets of vertices in one of the meshes of the model. Animations can be applied to nodes, to modify their transform (translation, rotation, scale) over time. </p>
  *
  * A model can be rendered by creating a {@link ModelInstance} from it. That instance has an additional transform to position the model in the world, and allows modification of materials and nodes
  * without destroying the original model. The original model is the owner of any meshes and textures, all instances created from the model share these resources. Disposing the model will
  * automatically make all instances invalid! </p>
  *
  * A model is created from {@link ModelData}, which in turn is loaded by a ModelLoader.
  *
  * @author
  *   badlogic, xoppa
  */
class Model extends AutoCloseable {

  /** the materials of the model, used by nodes that have a graphical representation FIXME not sure if superfluous, allows modification of materials without having to traverse the nodes *
    */
  val materials: DynamicArray[Material] = DynamicArray[Material]()

  /** root nodes of the model * */
  val nodes: DynamicArray[Node] = DynamicArray[Node]()

  /** animations of the model, modifying node transformations * */
  val animations: DynamicArray[Animation] = DynamicArray[Animation]()

  /** the meshes of the model * */
  val meshes: DynamicArray[Mesh] = DynamicArray[Mesh]()

  /** parts of meshes, used by nodes that have a graphical representation FIXME not sure if superfluous, stored in Nodes as well, could be useful to create bullet meshes *
    */
  val meshParts: DynamicArray[MeshPart] = DynamicArray[MeshPart]()

  /** Array of disposable resources like textures or meshes the Model is responsible for disposing * */
  protected val disposables: DynamicArray[AutoCloseable] = DynamicArray[AutoCloseable]()

  /** Constructs a new Model based on the {@link ModelData}. Texture files will be loaded from the internal file storage via an {@link TextureProvider.FileTextureProvider}.
    * @param modelData
    *   the {@link ModelData} got from e.g. ModelLoader
    */
  def this(modelData: ModelData)(using Sge) = {
    this()
    load(modelData, new TextureProvider.FileTextureProvider())
  }

  /** Constructs a new Model based on the {@link ModelData}.
    * @param modelData
    *   the {@link ModelData} got from e.g. ModelLoader
    * @param textureProvider
    *   the {@link TextureProvider} to use for loading the textures
    */
  def this(modelData: ModelData, textureProvider: TextureProvider)(using Sge) = {
    this()
    load(modelData, textureProvider)
  }

  protected def load(modelData: ModelData, textureProvider: TextureProvider)(using Sge): Unit = {
    loadMeshes(modelData.meshes)
    loadMaterials(modelData.materials, textureProvider)
    loadNodes(modelData.nodes)
    loadAnimations(modelData.animations)
    calculateTransforms()
  }

  protected def loadAnimations(modelAnimations: DynamicArray[ModelAnimation]): Unit =
    for (anim <- modelAnimations) {
      val animation = new Animation()
      animation.id = anim.id
      for (nanim <- anim.nodeAnimations) {
        val node = getNode(nanim.nodeId)
        if (node.isDefined) {
          node.foreach { n =>
            val nodeAnim = new NodeAnimation()
            nodeAnim.node = n

            Nullable(nanim.translation).foreach { trans =>
              val buf = DynamicArray[NodeKeyframe[Vector3]]()
              for (kf <- trans) {
                if (kf.keytime > animation.duration) animation.duration = kf.keytime
                val v = new Vector3()
                v.set(n.translation)
                kf.value.foreach(src => v.set(src))
                buf.add(new NodeKeyframe[Vector3](kf.keytime, v))
              }
              nodeAnim.translation = Nullable(buf)
            }

            Nullable(nanim.rotation).foreach { rot =>
              val buf = DynamicArray[NodeKeyframe[Quaternion]]()
              for (kf <- rot) {
                if (kf.keytime > animation.duration) animation.duration = kf.keytime
                val q = new Quaternion(0, 0, 0, 1)
                q.set(n.rotation)
                kf.value.foreach(src => q.set(src))
                buf.add(new NodeKeyframe[Quaternion](kf.keytime, q))
              }
              nodeAnim.rotation = Nullable(buf)
            }

            Nullable(nanim.scaling).foreach { scl =>
              val buf = DynamicArray[NodeKeyframe[Vector3]]()
              for (kf <- scl) {
                if (kf.keytime > animation.duration) animation.duration = kf.keytime
                val v = new Vector3()
                v.set(n.scale)
                kf.value.foreach(src => v.set(src))
                buf.add(new NodeKeyframe[Vector3](kf.keytime, v))
              }
              nodeAnim.scaling = Nullable(buf)
            }

            if (
              nodeAnim.translation.fold(false)(_.nonEmpty)
              || nodeAnim.rotation.fold(false)(_.nonEmpty)
              || nodeAnim.scaling.fold(false)(_.nonEmpty)
            ) {
              animation.nodeAnimations.add(nodeAnim)
            }
          }
        }
      }
      if (animation.nodeAnimations.nonEmpty) animations.add(animation)
    }

  private val nodePartBones: mutable.Map[NodePart, ArrayMap[String, Matrix4]] = mutable.Map.empty

  protected def loadNodes(modelNodes: DynamicArray[ModelNode]): Unit = {
    nodePartBones.clear()
    for (node <- modelNodes)
      nodes.add(loadNode(node))
    for ((nodePart, boneMap) <- nodePartBones) {
      if (nodePart.invBoneBindTransforms.isEmpty) {
        nodePart.invBoneBindTransforms = Nullable(ArrayMap[Node, Matrix4]())
      }
      nodePart.invBoneBindTransforms.foreach { binds =>
        binds.clear()
        for (i <- 0 until boneMap.size) {
          val key   = boneMap.getKeyAt(i)
          val value = boneMap.getValueAt(i)
          getNode(key).foreach { n =>
            binds.put(n, new Matrix4(value).inv())
          }
        }
      }
    }
  }

  protected def loadNode(modelNode: ModelNode): Node = {
    val node = new Node()
    node.id = modelNode.id

    Nullable(modelNode.translation).foreach(v => node.translation.set(v))
    Nullable(modelNode.rotation).foreach(q => node.rotation.set(q))
    Nullable(modelNode.scale).foreach(v => node.scale.set(v))
    // FIXME create temporary maps for faster lookup?
    Nullable(modelNode.parts).foreach { modelParts =>
      for (modelNodePart <- modelParts) {
        var meshPart:     Nullable[MeshPart] = Nullable.empty
        var meshMaterial: Nullable[Material] = Nullable.empty

        Nullable(modelNodePart.meshPartId).foreach { meshPartId =>
          for (part <- meshParts)
            if (meshPartId == part.id.getOrElse("")) {
              meshPart = Nullable(part)
            }
        }

        Nullable(modelNodePart.materialId).foreach { materialId =>
          for (material <- materials)
            if (materialId == material.id) {
              meshMaterial = Nullable(material)
            }
        }

        if (meshPart.isEmpty || meshMaterial.isEmpty) throw SgeError.InvalidInput("Invalid node: " + node.id)

        val nodePart = new NodePart()
        meshPart.foreach(mp => nodePart.meshPart = mp)
        meshMaterial.foreach(mm => nodePart.material = mm)
        node.parts.add(nodePart)
        Nullable(modelNodePart.bones).foreach(b => nodePartBones.put(nodePart, b))
      }
    }

    Nullable(modelNode.children).foreach { modelChildren =>
      for (child <- modelChildren)
        node.addChild(loadNode(child))
    }

    node
  }

  protected def loadMeshes(modelMeshes: DynamicArray[ModelMesh])(using Sge): Unit =
    for (mesh <- modelMeshes)
      convertMesh(mesh)

  protected def convertMesh(modelMesh: ModelMesh)(using Sge): Unit = {
    var numIndices = 0
    for (part <- modelMesh.parts)
      numIndices += part.indices.length
    val hasIndices  = numIndices > 0
    val attributes  = new VertexAttributes(modelMesh.attributes*)
    val numVertices = modelMesh.vertices.length / (attributes.vertexSize / 4)

    val mesh = new Mesh(true, numVertices, numIndices, attributes)
    meshes.add(mesh)
    disposables.add(mesh)

    BufferUtils.copy(modelMesh.vertices, mesh.getVerticesBuffer(true), modelMesh.vertices.length, 0)
    var offset        = 0
    val indicesBuffer = mesh.getIndicesBuffer(true)
    indicesBuffer.asInstanceOf[Buffer].clear()
    for (part <- modelMesh.parts) {
      val meshPart = new MeshPart()
      meshPart.id = Nullable(part.id)
      meshPart.primitiveType = part.primitiveType
      meshPart.offset = offset
      meshPart.size = if (hasIndices) part.indices.length else numVertices
      meshPart.mesh = mesh
      if (hasIndices) {
        indicesBuffer.put(part.indices)
      }
      offset += meshPart.size
      meshParts.add(meshPart)
    }
    indicesBuffer.asInstanceOf[Buffer].position(0)
    for (part <- meshParts)
      part.update()
  }

  protected def loadMaterials(modelMaterials: DynamicArray[ModelMaterial], textureProvider: TextureProvider): Unit =
    for (mtl <- modelMaterials)
      materials.add(convertMaterial(mtl, textureProvider))

  protected def convertMaterial(mtl: ModelMaterial, textureProvider: TextureProvider): Material = {
    val result = new Material()
    result.id = mtl.id
    Nullable(mtl.ambient).foreach(c => result.set(new ColorAttribute(ColorAttribute.Ambient, c)))
    Nullable(mtl.diffuse).foreach(c => result.set(new ColorAttribute(ColorAttribute.Diffuse, c)))
    Nullable(mtl.specular).foreach(c => result.set(new ColorAttribute(ColorAttribute.Specular, c)))
    Nullable(mtl.emissive).foreach(c => result.set(new ColorAttribute(ColorAttribute.Emissive, c)))
    Nullable(mtl.reflection).foreach(c => result.set(new ColorAttribute(ColorAttribute.Reflection, c)))
    if (mtl.shininess > 0f) result.set(new FloatAttribute(FloatAttribute.Shininess, mtl.shininess))
    if (mtl.opacity != 1f) result.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, mtl.opacity))

    val textures = mutable.Map[String, Texture]()

    // FIXME uvScaling/uvTranslation totally ignored
    Nullable(mtl.textures).foreach { mtlTextures =>
      for (tex <- mtlTextures) {
        val texture = textures.getOrElseUpdate(tex.fileName, {
                                                 val t = textureProvider.load(tex.fileName)
                                                 disposables.add(t)
                                                 t
                                               }
        )

        val descriptor = new TextureDescriptor[Texture](texture)
        descriptor.minFilter = Nullable(texture.getMinFilter())
        descriptor.magFilter = Nullable(texture.getMagFilter())
        descriptor.uWrap = Nullable(texture.getUWrap())
        descriptor.vWrap = Nullable(texture.getVWrap())

        val offsetU = Nullable(tex.uvTranslation).fold(0f)(_.x)
        val offsetV = Nullable(tex.uvTranslation).fold(0f)(_.y)
        val scaleU  = Nullable(tex.uvScaling).fold(1f)(_.x)
        val scaleV  = Nullable(tex.uvScaling).fold(1f)(_.y)

        tex.usage match {
          case ModelTexture.USAGE_DIFFUSE =>
            result.set(new TextureAttribute(TextureAttribute.Diffuse, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case ModelTexture.USAGE_SPECULAR =>
            result.set(new TextureAttribute(TextureAttribute.Specular, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case ModelTexture.USAGE_BUMP =>
            result.set(new TextureAttribute(TextureAttribute.Bump, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case ModelTexture.USAGE_NORMAL =>
            result.set(new TextureAttribute(TextureAttribute.Normal, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case ModelTexture.USAGE_AMBIENT =>
            result.set(new TextureAttribute(TextureAttribute.Ambient, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case ModelTexture.USAGE_EMISSIVE =>
            result.set(new TextureAttribute(TextureAttribute.Emissive, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case ModelTexture.USAGE_REFLECTION =>
            result.set(new TextureAttribute(TextureAttribute.Reflection, descriptor, offsetU, offsetV, scaleU, scaleV, 0))
          case _ => // ignore unknown usage
        }
      }
    }

    result
  }

  /** Adds a {@link AutoCloseable} to be managed and disposed by this Model. Can be used to keep track of manually loaded textures for {@link ModelInstance}.
    * @param disposable
    *   the AutoCloseable
    */
  def manageDisposable(disposable: AutoCloseable): Unit =
    if (!disposables.contains(disposable)) disposables.add(disposable)

  /** @return the {@link AutoCloseable} objects that will be disposed when the {@link #close()} method is called. */
  def getManagedDisposables: DynamicArray[AutoCloseable] = disposables

  override def close(): Unit =
    for (disposable <- disposables)
      disposable.close()

  /** Calculates the local and world transform of all {@link Node} instances in this model, recursively. First each {@link Node#localTransform} transform is calculated based on the translation,
    * rotation and scale of each Node. Then each {@link Node#calculateWorldTransform()} is calculated, based on the parent's world transform and the local transform of each Node. Finally, the
    * animation bone matrices are updated accordingly. </p>
    *
    * This method can be used to recalculate all transforms if any of the Node's local properties (translation, rotation, scale) was modified.
    */
  def calculateTransforms(): Unit = {
    val n = nodes.size
    for (i <- 0 until n)
      nodes(i).calculateTransforms(true)
    for (i <- 0 until n)
      nodes(i).calculateBoneTransforms(true)
  }

  /** Calculate the bounding box of this model instance. This is a potential slow operation, it is advised to cache the result.
    * @param out
    *   the {@link BoundingBox} that will be set with the bounds.
    * @return
    *   the out parameter for chaining
    */
  def calculateBoundingBox(out: BoundingBox): BoundingBox = {
    out.inf()
    extendBoundingBox(out)
  }

  /** Extends the bounding box with the bounds of this model instance. This is a potential slow operation, it is advised to cache the result.
    * @param out
    *   the {@link BoundingBox} that will be extended with the bounds.
    * @return
    *   the out parameter for chaining
    */
  def extendBoundingBox(out: BoundingBox): BoundingBox = {
    val n = nodes.size
    for (i <- 0 until n)
      nodes(i).extendBoundingBox(out)
    out
  }

  /** @param id
    *   The ID of the animation to fetch (case sensitive).
    * @return
    *   The {@link Animation} with the specified id, or null if not available.
    */
  def getAnimation(id: String): Nullable[Animation] =
    getAnimation(id, true)

  /** @param id
    *   The ID of the animation to fetch.
    * @param ignoreCase
    *   whether to use case sensitivity when comparing the animation id.
    * @return
    *   The {@link Animation} with the specified id, or null if not available.
    */
  def getAnimation(id: String, ignoreCase: Boolean): Nullable[Animation] = boundary {
    val n = animations.size
    if (ignoreCase) {
      for (i <- 0 until n) {
        val animation = animations(i)
        if (animation.id.equalsIgnoreCase(id)) break(Nullable(animation))
      }
    } else {
      for (i <- 0 until n) {
        val animation = animations(i)
        if (animation.id == id) break(Nullable(animation))
      }
    }
    Nullable.empty
  }

  /** @param id
    *   The ID of the material to fetch.
    * @return
    *   The {@link Material} with the specified id, or null if not available.
    */
  def getMaterial(id: String): Nullable[Material] =
    getMaterial(id, true)

  /** @param id
    *   The ID of the material to fetch.
    * @param ignoreCase
    *   whether to use case sensitivity when comparing the material id.
    * @return
    *   The {@link Material} with the specified id, or null if not available.
    */
  def getMaterial(id: String, ignoreCase: Boolean): Nullable[Material] = boundary {
    val n = materials.size
    if (ignoreCase) {
      for (i <- 0 until n) {
        val material = materials(i)
        if (material.id.equalsIgnoreCase(id)) break(Nullable(material))
      }
    } else {
      for (i <- 0 until n) {
        val material = materials(i)
        if (material.id == id) break(Nullable(material))
      }
    }
    Nullable.empty
  }

  /** @param id
    *   The ID of the node to fetch.
    * @return
    *   The {@link Node} with the specified id, or null if not found.
    */
  def getNode(id: String): Nullable[Node] =
    getNode(id, true)

  /** @param id
    *   The ID of the node to fetch.
    * @param recursive
    *   false to fetch a root node only, true to search the entire node tree for the specified node.
    * @return
    *   The {@link Node} with the specified id, or null if not found.
    */
  def getNode(id: String, recursive: Boolean): Nullable[Node] =
    getNode(id, recursive, false)

  /** @param id
    *   The ID of the node to fetch.
    * @param recursive
    *   false to fetch a root node only, true to search the entire node tree for the specified node.
    * @param ignoreCase
    *   whether to use case sensitivity when comparing the node id.
    * @return
    *   The {@link Node} with the specified id, or null if not found.
    */
  def getNode(id: String, recursive: Boolean, ignoreCase: Boolean): Nullable[Node] =
    Node.getNode(nodes, id, recursive, ignoreCase)
}
