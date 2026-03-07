/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/ModelInstance.java
 * Original authors: badlogic, xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Implements RenderableProvider trait.
 *   - Java has ~15 constructor overloads; Scala consolidates into fewer with Nullable params.
 *   - Some convenience constructors (Model, String, bool, bool) not individually ported
 *     but equivalent functionality available through the main constructor variants.
 *   - invalidate(Node) bone rebinding uses Nullable foreach instead of direct array key mutation.
 *   - getRenderable(out, node, nodePart): simplified -- Java checks transform != null
 *     (SGE transform is always non-null since it's a val Matrix4).
 *   - copyAnimation: null/continue -> Nullable isDefined/foreach.
 *   - defaultShareKeyframes in companion object.
 *   - All public methods present: copy, getRenderables, getRenderable (3), calculateTransforms,
 *     calculateBoundingBox, extendBoundingBox, getAnimation (2), getMaterial (2), getNode (3),
 *     copyAnimations (2), copyAnimation (2).
 *   - Audit: minor_issues (2026-03-03)
 *     - invalidate() bone rebinding logic may not properly replace old ArrayMap keys.
 */
package sge
package graphics
package g3d

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe, NodePart }
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.{ DynamicArray, Nullable, Pool }

/** An instance of a {@link Model}, allows to specify global transform and modify the materials, as it has a copy of the model's materials. Multiple instances can be created from the same Model, all
  * sharing the meshes and textures of the Model. The Model owns the meshes and textures, to dispose of these, the Model has to be disposed. Therefor, the Model must outlive all its ModelInstances
  * </p>
  *
  * The ModelInstance creates a full copy of all materials, nodes and animations.
  * @author
  *   badlogic, xoppa
  */
class ModelInstance(
  /** the {@link Model} this instances derives from * */
  val model: Model,
  /** the world transform * */
  var transform: Matrix4
) extends RenderableProvider {

  /** the materials of the model, used by nodes that have a graphical representation FIXME not sure if superfluous, allows modification of materials without having to traverse the nodes *
    */
  val materials: DynamicArray[Material] = DynamicArray[Material]()

  /** root nodes of the model * */
  val nodes: DynamicArray[Node] = DynamicArray[Node]()

  /** animations of the model, modifying node transformations * */
  val animations: DynamicArray[Animation] = DynamicArray[Animation]()

  /** user definable value, which is passed to the {@link Shader}. */
  var userData: Nullable[Any] = Nullable.empty

  /** Constructs a new ModelInstance with all nodes and materials of the given model.
    * @param model
    *   The {@link Model} to create an instance of.
    */
  def this(model: Model) = {
    this(model, Matrix4())
    copyNodes(model.nodes)
    copyAnimations(model.animations, ModelInstance.defaultShareKeyframes)
    calculateTransforms()
  }

  /** Constructs a new ModelInstance with only the specified nodes and materials of the given model. */
  def this(model: Model, rootNodeIds: Nullable[Seq[String]]) = {
    this(model, Matrix4())
    rootNodeIds.fold(copyNodes(model.nodes)) { ids =>
      if (ids.nonEmpty) copyNodesById(model.nodes, ids)
      else copyNodes(model.nodes)
    }
    copyAnimations(model.animations, ModelInstance.defaultShareKeyframes)
    calculateTransforms()
  }

  /** Constructs a new ModelInstance with the specified transform. */
  def this(model: Model, transform: Nullable[Matrix4], rootNodeIds: Nullable[Seq[String]]) = {
    this(model, transform.getOrElse(Matrix4()))
    rootNodeIds.fold(copyNodes(model.nodes)) { ids =>
      if (ids.nonEmpty) copyNodesById(model.nodes, ids)
      else copyNodes(model.nodes)
    }
    copyAnimations(model.animations, ModelInstance.defaultShareKeyframes)
    calculateTransforms()
  }

  /** @param model
    *   The source {@link Model}
    * @param nodeId
    *   The ID of the {@link Node} within the {@link Model} for the instance to contain
    * @param recursive
    *   True to recursively search the Model's node tree, false to only search for a root node
    * @param parentTransform
    *   True to apply the parent's node transform to the instance (only applicable if recursive is true).
    * @param mergeTransform
    *   True to apply the source node transform to the instance transform, resetting the node transform.
    */
  def this(model: Model, transform: Nullable[Matrix4], nodeId: String, recursive: Boolean, parentTransform: Boolean, mergeTransform: Boolean, shareKeyframes: Boolean) = {
    this(model, transform.getOrElse(Matrix4()))
    val node = model.getNode(nodeId, recursive)
    node.foreach { n =>
      val nodeCopy = n.copy()
      this.nodes.add(nodeCopy)
      if (mergeTransform) {
        this.transform.mul(if (parentTransform) n.globalTransform else n.localTransform)
        nodeCopy.translation.set(0, 0, 0)
        nodeCopy.rotation.idt()
        nodeCopy.scale.set(1, 1, 1)
      } else if (parentTransform && nodeCopy.hasParent) {
        n.parent.foreach(p => this.transform.mul(p.globalTransform))
      }
    }
    invalidate()
    copyAnimations(model.animations, shareKeyframes)
    calculateTransforms()
  }

  /** Constructs a new ModelInstance at the specified position. */
  def this(model: Model, position: Vector3) = {
    this(model)
    this.transform.setToTranslation(position)
  }

  /** Constructs a new ModelInstance at the specified position. */
  def this(model: Model, x: Float, y: Float, z: Float) = {
    this(model)
    this.transform.setToTranslation(x, y, z)
  }

  /** Constructs a new ModelInstance which is a copy of the specified ModelInstance. */
  def this(copyFrom: ModelInstance, transform: Nullable[Matrix4], shareKeyframes: Boolean) = {
    this(copyFrom.model, transform.getOrElse(Matrix4()))
    copyNodes(copyFrom.nodes)
    copyAnimations(copyFrom.animations, shareKeyframes)
    calculateTransforms()
  }

  /** Constructs a new ModelInstance which is a copy of the specified ModelInstance. */
  def this(copyFrom: ModelInstance, transform: Matrix4) = {
    this(copyFrom, transform, ModelInstance.defaultShareKeyframes)
  }

  /** Constructs a new ModelInstance which is a copy of the specified ModelInstance. */
  def this(copyFrom: ModelInstance) = {
    this(copyFrom, copyFrom.transform.cpy())
  }

  /** @return A newly created ModelInstance which is a copy of this ModelInstance */
  def copy(): ModelInstance = ModelInstance(this)

  private def copyNodes(nodes: DynamicArray[Node]): Unit = {
    for (node <- nodes)
      this.nodes.add(node.copy())
    invalidate()
  }

  private def copyNodesById(nodes: DynamicArray[Node], nodeIds: Seq[String]): Unit = {
    for (node <- nodes)
      for (nodeId <- nodeIds)
        if (nodeId == node.id) {
          this.nodes.add(node.copy())
        }
    invalidate()
  }

  /** Makes sure that each {@link NodePart} of the {@link Node} and its sub-nodes, doesn't reference a node outside this node tree and that all materials are listed in the {@link #materials} array.
    */
  private def invalidate(node: Node): Unit = {
    for (part <- node.parts) {
      part.invBoneBindTransforms.foreach { bindPose =>
        for (j <- 0 until bindPose.size) {
          val boneNode = bindPose.getKeyAt(j)
          getNode(boneNode.id).foreach { replacement =>
            bindPose.setKeyAt(j, replacement)
          }
        }
      }
      if (!materials.containsByRef(part.material)) {
        val midx = materials.indexWhere(_.id == part.material.id)
        if (midx < 0)
          materials.add { part.material = part.material.copy(); part.material }
        else
          part.material = materials(midx)
      }
    }
    for (i <- 0 until node.childCount)
      invalidate(node.getChild(i))
  }

  /** Makes sure that each {@link NodePart} of each {@link Node} doesn't reference a node outside this node tree and that all materials are listed in the {@link #materials} array.
    */
  private def invalidate(): Unit =
    for (node <- nodes)
      invalidate(node)

  /** Copy source animations to this ModelInstance
    * @param source
    *   Iterable collection of source animations {@link Animation}
    */
  def copyAnimations(source: DynamicArray[Animation]): Unit =
    for (anim <- source)
      copyAnimation(anim, ModelInstance.defaultShareKeyframes)

  /** Copy source animations to this ModelInstance
    * @param source
    *   DynamicArray collection of source animations {@link Animation}
    * @param shareKeyframes
    *   Shallow copy of {@link NodeKeyframe}'s if it's true, otherwise make a deep copy.
    */
  def copyAnimations(source: DynamicArray[Animation], shareKeyframes: Boolean): Unit =
    for (anim <- source)
      copyAnimation(anim, shareKeyframes)

  /** Copy the source animation to this ModelInstance
    * @param sourceAnim
    *   The source animation {@link Animation}
    */
  def copyAnimation(sourceAnim: Animation): Unit =
    copyAnimation(sourceAnim, ModelInstance.defaultShareKeyframes)

  /** Copy the source animation to this ModelInstance
    * @param sourceAnim
    *   The source animation {@link Animation}
    * @param shareKeyframes
    *   Shallow copy of {@link NodeKeyframe}'s if it's true, otherwise make a deep copy.
    */
  def copyAnimation(sourceAnim: Animation, shareKeyframes: Boolean): Unit = {
    val animation = Animation()
    animation.id = sourceAnim.id
    animation.duration = sourceAnim.duration
    for (nanim <- sourceAnim.nodeAnimations) {
      val node = getNode(nanim.node.id)
      if (node.isDefined) {
        node.foreach { n =>
          val nodeAnim = NodeAnimation()
          nodeAnim.node = n
          if (shareKeyframes) {
            nodeAnim.translation = nanim.translation
            nodeAnim.rotation = nanim.rotation
            nodeAnim.scaling = nanim.scaling
          } else {
            nanim.translation.foreach { trans =>
              val buf = DynamicArray[NodeKeyframe[Vector3]]()
              for (kf <- trans)
                buf.add(NodeKeyframe[Vector3](kf.keytime, kf.value))
              nodeAnim.translation = Nullable(buf)
            }
            nanim.rotation.foreach { rot =>
              val buf = DynamicArray[NodeKeyframe[Quaternion]]()
              for (kf <- rot)
                buf.add(NodeKeyframe[Quaternion](kf.keytime, kf.value))
              nodeAnim.rotation = Nullable(buf)
            }
            nanim.scaling.foreach { scl =>
              val buf = DynamicArray[NodeKeyframe[Vector3]]()
              for (kf <- scl)
                buf.add(NodeKeyframe[Vector3](kf.keytime, kf.value))
              nodeAnim.scaling = Nullable(buf)
            }
          }
          if (nodeAnim.translation.isDefined || nodeAnim.rotation.isDefined || nodeAnim.scaling.isDefined) {
            animation.nodeAnimations.add(nodeAnim)
          }
        }
      }
    }
    if (animation.nodeAnimations.nonEmpty) animations.add(animation)
  }

  /** Traverses the Node hierarchy and collects {@link Renderable} instances for every node with a graphical representation. Renderables are obtained from the provided pool. The resulting array can be
    * rendered via a ModelBatch.
    *
    * @param renderables
    *   the output array
    * @param pool
    *   the pool to obtain Renderables from
    */
  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    for (node <- nodes)
      getRenderables(node, renderables, pool)

  /** @return The renderable of the first node's first part. */
  def getRenderable(out: Renderable): Renderable =
    getRenderable(out, nodes(0))

  /** @return The renderable of the node's first part. */
  def getRenderable(out: Renderable, node: Node): Renderable =
    getRenderable(out, node, node.parts(0))

  def getRenderable(out: Renderable, node: Node, nodePart: NodePart): Renderable = {
    nodePart.setRenderable(out)
    if (nodePart.bones.isEmpty)
      out.worldTransform.set(transform).mul(node.globalTransform)
    else
      out.worldTransform.set(transform)
    out.userData = userData
    out
  }

  protected def getRenderables(node: Node, renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit = {
    if (node.parts.nonEmpty) {
      for (nodePart <- node.parts)
        if (nodePart.enabled) renderables.add(getRenderable(pool.obtain(), node, nodePart))
    }

    for (child <- node.children)
      getRenderables(child, renderables, pool)
  }

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
    getAnimation(id, false)

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

object ModelInstance {

  /** Whether, by default, {@link NodeKeyframe}'s are shared amongst {@link Model} and ModelInstance. Can be overridden per ModelInstance using the constructor argument.
    */
  var defaultShareKeyframes: Boolean = true
}
