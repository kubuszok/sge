/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/BehaviorTreeLibraryManager.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`
 *   Convention: split packages, Nullable instead of null
 *   Idiom: Java singleton pattern -> Scala object with mutable library field
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 75
 * Covenant-baseline-methods: BehaviorTreeLibraryManager,createBehaviorTree,createRootTask,disposeBehaviorTree,instance,library
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package utils

import sge.utils.Nullable

/** The `BehaviorTreeLibraryManager` is a singleton in charge of the creation of behavior trees using the underlying library. If no library is explicitly set (see the method
  * [[BehaviorTreeLibraryManager.library_=]]), a default library instantiated by [[BehaviorTreeLibrary]] is used instead.
  *
  * @author
  *   davebaol (original implementation)
  */
object BehaviorTreeLibraryManager {

  /** The singleton instance. */
  val instance: BehaviorTreeLibraryManager = new BehaviorTreeLibraryManager()
}

final class BehaviorTreeLibraryManager private () {

  /** The behavior tree library. */
  var library: BehaviorTreeLibrary = new BehaviorTreeLibrary()

  /** Creates the root task of [[BehaviorTree]] for the specified reference.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the root task of the tree cloned from the archetype.
    */
  def createRootTask[T](treeReference: String): Task[T] =
    library.createRootTask(treeReference)

  /** Creates the [[BehaviorTree]] for the specified reference.
    * @param treeReference
    *   the tree identifier, typically a path
    * @return
    *   the tree cloned from the archetype.
    */
  def createBehaviorTree[T](treeReference: String): BehaviorTree[T] =
    library.createBehaviorTree(treeReference)

  /** Creates the [[BehaviorTree]] for the specified reference and blackboard object.
    * @param treeReference
    *   the tree identifier, typically a path
    * @param blackboard
    *   the blackboard object (it can be empty).
    * @return
    *   the tree cloned from the archetype.
    */
  def createBehaviorTree[T](treeReference: String, blackboard: Nullable[T]): BehaviorTree[T] =
    library.createBehaviorTree(treeReference, blackboard)

  /** Dispose behavior tree obtained by this library manager.
    * @param treeReference
    *   the tree identifier.
    * @param behaviorTree
    *   the tree to dispose.
    */
  def disposeBehaviorTree(treeReference: String, behaviorTree: BehaviorTree[?]): Unit =
    library.disposeBehaviorTree(treeReference, behaviorTree)
}
