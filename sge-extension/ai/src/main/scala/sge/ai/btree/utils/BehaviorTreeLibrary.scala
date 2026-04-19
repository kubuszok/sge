/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/BehaviorTreeLibrary.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`
 *   Convention: split packages, Nullable instead of null
 *   Idiom: FileHandleResolver/AssetManager integration removed (parser uses registry approach)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 104
 * Covenant-baseline-methods: BehaviorTreeLibrary,bt,createBehaviorTree,createRootTask,disposeBehaviorTree,hasArchetypeTree,registerArchetypeTree,repository,retrieveArchetypeTree
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package utils

import scala.collection.mutable

import sge.utils.Nullable

/** A `BehaviorTreeLibrary` is a repository of behavior tree archetypes. Behavior tree archetypes never run. Indeed, they are only cloned to create behavior tree instances that can run.
  *
  * @author
  *   davebaol (original implementation)
  */
class BehaviorTreeLibrary {

  protected val repository: mutable.HashMap[String, BehaviorTree[?]] = mutable.HashMap.empty

  /** Creates the root task of [[BehaviorTree]] for the specified reference.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the root task of the tree cloned from the archetype.
    */
  def createRootTask[T](treeReference: String): Task[T] =
    retrieveArchetypeTree(treeReference).getChild(0).cloneTask().asInstanceOf[Task[T]]

  /** Creates the [[BehaviorTree]] for the specified reference.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the tree cloned from the archetype.
    */
  def createBehaviorTree[T](treeReference: String): BehaviorTree[T] =
    createBehaviorTree(treeReference, Nullable.empty[T])

  /** Creates the [[BehaviorTree]] for the specified reference and blackboard object.
    * @param treeReference
    *   the tree identifier, typically a path
    * @param blackboard
    *   the blackboard object (it can be empty).
    * @return
    *   the tree cloned from the archetype.
    */
  def createBehaviorTree[T](treeReference: String, blackboard: Nullable[T]): BehaviorTree[T] = {
    val bt = retrieveArchetypeTree(treeReference).cloneTask().asInstanceOf[BehaviorTree[T]]
    blackboard.foreach(bt.setObject)
    bt
  }

  /** Retrieves the archetype tree from the library. If the library doesn't contain the archetype tree it throws.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the archetype tree.
    * @throws NoSuchElementException
    *   if the reference is not registered.
    */
  protected def retrieveArchetypeTree(treeReference: String): BehaviorTree[?] =
    repository.getOrElse(
      treeReference,
      throw new NoSuchElementException(s"No archetype tree registered for reference: $treeReference")
    )

  /** Registers the [[BehaviorTree]] archetypeTree with the specified reference. Existing archetypes in the repository with the same treeReference will be replaced.
    * @param treeReference
    *   the tree identifier, typically a path.
    * @param archetypeTree
    *   the archetype tree.
    */
  def registerArchetypeTree(treeReference: String, archetypeTree: BehaviorTree[?]): Unit =
    repository.put(treeReference, archetypeTree)

  /** Returns `true` if an archetype tree with the specified reference is registered in this library.
    * @param treeReference
    *   the tree identifier, typically a path.
    * @return
    *   `true` if the archetype is registered already; `false` otherwise.
    */
  def hasArchetypeTree(treeReference: String): Boolean =
    repository.contains(treeReference)

  /** Dispose behavior tree obtained by this library.
    * @param treeReference
    *   the tree identifier.
    * @param behaviorTree
    *   the tree to dispose.
    */
  def disposeBehaviorTree(treeReference: String, behaviorTree: BehaviorTree[?]): Unit =
    Task.taskCloner.foreach(_.freeTask(behaviorTree))
}
