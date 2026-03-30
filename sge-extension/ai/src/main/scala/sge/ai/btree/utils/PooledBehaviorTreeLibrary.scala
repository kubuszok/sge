/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/PooledBehaviorTreeLibrary.java
 * Original authors: mgsx
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`; `Pool` -> `sge.utils.Pool`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package utils

import scala.collection.mutable

import sge.utils.DynamicArray
import sge.utils.Nullable

/** A `BehaviorTreeLibrary` using reference pools.
  *
  * [[BehaviorTree]] instances created by `PooledBehaviorTreeLibrary` should be disposed by calling [[BehaviorTreeLibrary.disposeBehaviorTree]].
  *
  * @author
  *   mgsx (original implementation)
  */
class PooledBehaviorTreeLibrary extends BehaviorTreeLibrary {

  private val pools: mutable.HashMap[String, DynamicArray[BehaviorTree[?]]] = mutable.HashMap.empty

  /** Retrieve pool by tree reference, create it if not already exists. */
  private def getPool(treeReference: String): DynamicArray[BehaviorTree[?]] =
    pools.getOrElseUpdate(treeReference, DynamicArray[BehaviorTree[?]]())

  /** Creates concrete tree instance. */
  protected def newBehaviorTree[T](treeReference: String): BehaviorTree[T] =
    super.createBehaviorTree[T](treeReference, Nullable.empty)

  override def createBehaviorTree[T](treeReference: String, blackboard: Nullable[T]): BehaviorTree[T] = {
    val pool = getPool(treeReference)
    val tree = if (pool.size > 0) {
      pool.pop().asInstanceOf[BehaviorTree[T]]
    } else {
      newBehaviorTree[T](treeReference)
    }
    blackboard.foreach(tree.setObject)
    tree
  }

  override def disposeBehaviorTree(treeReference: String, behaviorTree: BehaviorTree[?]): Unit = {
    val pool = getPool(treeReference)
    behaviorTree.reset()
    pool.add(behaviorTree)
  }

  /** Clear pool for a tree reference. */
  def clear(treeReference: String): Unit =
    pools.get(treeReference).foreach(_.clear())

  /** Clear all pools. */
  def clear(): Unit = {
    pools.values.foreach(_.clear())
    pools.clear()
  }
}
