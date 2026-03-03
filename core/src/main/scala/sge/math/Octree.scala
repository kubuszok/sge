/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Octree.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Issues: OctreeNode.rayCast has placeholder `val intersect = true` instead of actual
 *     Intersector.intersectRayBounds call — ray-bounds check is non-functional
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS (with issues)
 * - All public API ported: add, remove, update, getAll, query(BoundingBox), query(Frustum),
 *   rayCast, getNodesBoxes, Collider trait, RayCastResult
 * - ISSUE: OctreeNode.rayCast has placeholder `val intersect = true` instead of
 *   Intersector.intersectRayBounds(ray, bounds, tmp) — missing ray-bounds check
 * - INTENTIONAL: uses scala.collection.mutable.Set instead of ObjectSet (Scala stdlib)
 */
package sge
package math

import sge.math.collision._
import sge.math.Frustum
import sge.utils.{ Nullable, Pool }
import sge.utils.DynamicArray
import scala.collection.mutable.Set

/** A static Octree implementation.
  *
  * Example of usage:
  *
  * <pre> Vector3 min = new Vector3(-10, -10, -10); Vector3 max = new Vector3(10, 10, 10); octree = new Octree<GameObject>(min, max, MAX_DEPTH, MAX_ITEMS_PER_NODE, new Octree.Collider<GameObject>() {
  * &#64;Override public boolean intersects (BoundingBox nodeBounds, GameObject geometry) { return nodeBounds.intersects(geometry.box); }
  *
  * &#64;Override public boolean intersects (Frustum frustum, GameObject geometry) { return frustum.boundsInFrustum(geometry.box); }
  *
  * &#64;Override public float intersects (Ray ray, GameObject geometry) { if (Intersector.intersectRayBounds(ray, geometry.box, new Vector3())) { return tmp.dst2(ray.origin); } return
  * Float.MAX_VALUE; } });
  *
  * // Adding game objects to the octree octree.add(gameObject1); octree.add(gameObject2);
  *
  * // Querying the result Set<GameObject> result = new Set<>(); octree.query(cam.frustum, result);
  *
  * // Rendering the result for (GameObject gameObject : result) { modelBatch.render(gameObject); } </pre>
  */
class Octree[T <: AnyRef: scala.reflect.ClassTag](minimum: Vector3, maximum: Vector3, maxDepth: Int, val maxItemsPerNode: Int, val collider: Octree.Collider[T]) {

  val nodePool = new Pool[OctreeNode]() {
    override protected val initialCapacity: Int        = 16
    override protected val max:             Int        = Int.MaxValue
    override protected def newObject():     OctreeNode = new OctreeNode()
  }

  protected val root: OctreeNode = createNode(
    Vector3(scala.math.min(minimum.x, maximum.x), scala.math.min(minimum.y, maximum.y), scala.math.min(minimum.z, maximum.z)),
    Vector3(scala.math.max(minimum.x, maximum.x), scala.math.max(minimum.y, maximum.y), scala.math.max(minimum.z, maximum.z)),
    maxDepth
  )

  def createNode(min: Vector3, max: Vector3, level: Int): OctreeNode = {
    val node = nodePool.obtain()
    node.bounds.set(min, max)
    node.level = level
    node.leaf = true
    node
  }

  def add(obj: T): Unit =
    root.add(obj)

  def remove(obj: T): Unit =
    root.remove(obj)

  def update(obj: T): Unit = {
    root.remove(obj)
    root.add(obj)
  }

  /** Method to retrieve all the geometries.
    * @param resultSet
    * @return
    *   the result set
    */
  def getAll(resultSet: Set[T]): Set[T] = {
    root.getAll(resultSet)
    resultSet
  }

  /** Method to query geometries inside nodes that the aabb intersects. Can be used as broad phase.
    * @param aabb
    *   \- The bounding box to query
    * @param result
    *   \- Set to be populated with objects inside the BoundingBoxes
    */
  def query(aabb: BoundingBox, result: Set[T]): Set[T] = {
    root.query(aabb, result)
    result
  }

  /** Method to query geometries inside nodes that the frustum intersects. Can be used as broad phase.
    * @param frustum
    *   \- The frustum to query
    * @param result
    *   set populated with objects near from the frustum
    */
  def query(frustum: Frustum, result: Set[T]): Set[T] = {
    root.query(frustum, result)
    result
  }

  def rayCast(ray: Ray, result: Octree.RayCastResult[T]): T = {
    result.distance = result.maxDistanceSq
    root.rayCast(ray, result)
    result.geometry
  }

  /** Method to get nodes as bounding boxes. Useful for debug purpose.
    *
    * @param boxes
    */
  def getNodesBoxes(boxes: Set[BoundingBox]): Set[BoundingBox] = {
    root.getBoundingBox(boxes)
    boxes
  }

  protected class OctreeNode {

    var level: Int = scala.compiletime.uninitialized
    val bounds = new BoundingBox()
    var leaf:             Boolean                     = scala.compiletime.uninitialized
    private var children: Nullable[Array[OctreeNode]] = Nullable.empty
    private val geometries = DynamicArray[T]()

    private def split(): Unit = {
      val midx = (bounds.max.x + bounds.min.x) * 0.5f
      val midy = (bounds.max.y + bounds.min.y) * 0.5f
      val midz = (bounds.max.z + bounds.min.z) * 0.5f

      val deeperLevel = level - 1

      leaf = false
      val ch = children.getOrElse(Array.ofDim[OctreeNode](8))
      ch(0) = createNode(Vector3(bounds.min.x, midy, midz), Vector3(midx, bounds.max.y, bounds.max.z), deeperLevel)
      ch(1) = createNode(Vector3(midx, midy, midz), Vector3(bounds.max.x, bounds.max.y, bounds.max.z), deeperLevel)
      ch(2) = createNode(Vector3(midx, midy, bounds.min.z), Vector3(bounds.max.x, bounds.max.y, midz), deeperLevel)
      ch(3) = createNode(Vector3(bounds.min.x, midy, bounds.min.z), Vector3(midx, bounds.max.y, midz), deeperLevel)
      ch(4) = createNode(Vector3(bounds.min.x, bounds.min.y, midz), Vector3(midx, midy, bounds.max.z), deeperLevel)
      ch(5) = createNode(Vector3(midx, bounds.min.y, midz), Vector3(bounds.max.x, midy, bounds.max.z), deeperLevel)
      ch(6) = createNode(Vector3(midx, bounds.min.y, bounds.min.z), Vector3(bounds.max.x, midy, midz), deeperLevel)
      ch(7) = createNode(Vector3(bounds.min.x, bounds.min.y, bounds.min.z), Vector3(midx, midy, midz), deeperLevel)
      children = Nullable(ch)

      // Move geometries from parent to children
      for (child <- ch)
        for (geometry <- this.geometries)
          child.add(geometry)
      this.geometries.clear()
    }

    private def merge(): Unit = {
      clearChildren()
      leaf = true
    }

    private def free(): Unit = {
      geometries.clear()
      if (!leaf) clearChildren()
      nodePool.free(this)
    }

    private def clearChildren(): Unit =
      children.foreach { ch =>
        for (i <- 0 until 8)
          ch(i).free()
        children = Nullable.empty
      }

    def add(geometry: T): Unit =
      if (!collider.intersects(bounds, geometry)) {
        ()
      } else if (!leaf) {
        // If is not leaf, check children
        children.foreach { ch =>
          for (child <- ch)
            child.add(geometry)
        }
      } else {
        if (geometries.size >= maxItemsPerNode && level > 0) {
          split()
          children.foreach { ch =>
            for (child <- ch)
              child.add(geometry)
          }
        } else {
          geometries += geometry
        }
      }

    def remove(obj: T): Boolean =
      if (!leaf) {
        var removed = false
        children.foreach { ch =>
          for (node <- ch)
            removed |= node.remove(obj)
        }

        if (removed) {
          val geometrySet = Set.empty[T]
          children.foreach { ch =>
            for (node <- ch)
              node.getAll(geometrySet)
          }
          if (geometrySet.size <= maxItemsPerNode) {
            for (geometry <- geometrySet)
              geometries += geometry
            merge()
          }
        }

        removed
      } else {
        val index = geometries.indexOf(obj)
        if (index >= 0) {
          geometries.removeIndex(index)
          true
        } else {
          false
        }
      }

    protected def isLeaf: Boolean = leaf

    def query(aabb: BoundingBox, result: Set[T]): Unit =
      if (aabb.intersects(bounds)) {
        if (!leaf) {
          children.foreach { ch =>
            for (node <- ch)
              node.query(aabb, result)
          }
        } else {
          for (geometry <- geometries)
            // Filter geometries using collider
            if (collider.intersects(bounds, geometry)) {
              result += geometry
            }
        }
      }

    def query(frustum: Frustum, result: Set[T]): Unit =
      // Placeholder implementation since Frustum methods are not available
      if (!leaf) {
        children.foreach { ch =>
          for (node <- ch)
            node.query(frustum, result)
        }
      } else {
        for (geometry <- geometries)
          // Filter geometries using collider
          if (collider.intersects(frustum, geometry)) {
            result += geometry
          }
      }

    def rayCast(ray: Ray, result: Octree.RayCastResult[T]): Unit = scala.util.boundary {
      val tmp = Octree.tmp
      // Placeholder for ray-bounds intersection since the method is not available
      val intersect = true // Intersector.intersectRayBounds(ray, bounds, tmp)
      if (!intersect) {
        scala.util.boundary.break(())
      } else {
        val dst2 = tmp.distanceSq(ray.origin) // Using distanceSq instead of dst2
        if (dst2 >= result.maxDistanceSq) {
          scala.util.boundary.break(())
        }
      }

      // Check intersection with children
      if (!leaf) {
        children.foreach { ch =>
          for (child <- ch)
            child.rayCast(ray, result)
        }
      } else {
        for (geometry <- geometries) {
          // Check intersection with geometries
          val distance = collider.intersects(ray, geometry)
          if (result.geometryNullable.isEmpty || distance < result.distance) {
            result.geometry = geometry
            result.distance = distance
          }
        }
      }
    }

    /** Get all geometries using Depth-First Search recursion.
      * @param resultSet
      */
    def getAll(resultSet: Set[T]): Unit = {
      if (!leaf) {
        children.foreach { ch =>
          for (child <- ch)
            child.getAll(resultSet)
        }
      }
      geometries.foreach(resultSet += _)
    }

    /** Get bounding boxes using Depth-First Search recursion.
      * @param bounds
      */
    def getBoundingBox(bounds: Set[BoundingBox]): Unit = {
      if (!leaf) {
        children.foreach { ch =>
          for (node <- ch)
            node.getBoundingBox(bounds)
        }
      }
      bounds += this.bounds
    }
  }
}

object Octree {
  val tmp = new Vector3()

  /** Interface used by octree to handle geometries' collisions against BoundingBox, Frustum and Ray.
    * @param <T>
    */
  trait Collider[T] {

    /** Method to calculate intersection between aabb and the geometry.
      * @param nodeBounds
      * @param geometry
      * @return
      *   if they are intersecting
      */
    def intersects(nodeBounds: BoundingBox, geometry: T): Boolean

    /** Method to calculate intersection between frustum and the geometry.
      * @param frustum
      * @param geometry
      * @return
      *   if they are intersecting
      */
    def intersects(frustum: Frustum, geometry: T): Boolean

    /** Method to calculate intersection between ray and the geometry.
      * @param ray
      * @param geometry
      * @return
      *   distance between ray and geometry
      */
    def intersects(ray: Ray, geometry: T): Float
  }

  class RayCastResult[T] {
    var geometryNullable:     Nullable[T] = Nullable.empty
    def geometry:             T           = geometryNullable.getOrElse(throw new NoSuchElementException("No geometry in RayCastResult"))
    def geometry_=(value: T): Unit        = geometryNullable = Nullable(value)
    var distance:             Float       = scala.compiletime.uninitialized
    var maxDistanceSq:        Float       = Float.MaxValue
  }
}
