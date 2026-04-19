/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/Include.java
 * Original authors: davebaol, implicit-invocation
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 97
 * Covenant-baseline-methods: Include,cloneTask,copyTo,createSubtreeRootTask,include,isLazy,newInstance,reset,rootTask,start,subtree
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package decorator

import sge.ai.btree.utils.BehaviorTreeLibraryManager
import sge.utils.Nullable

/** An `Include` decorator grafts a subtree. When the subtree is grafted depends on the value of the `lazy` attribute: at clone-time if is `false`, at run-time if is `true`.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol, implicit-invocation (original implementation)
  */
class Include[E](
  /** The path of the subtree to include. */
  var subtree: Nullable[String] = Nullable.empty,
  /** Whether the subtree should be included at clone-time (false, the default) or at run-time (true). */
  var isLazy: Boolean = false
) extends Decorator[E](Nullable.empty[Task[E]]) {

  /** The first call of this method lazily sets its child to the referenced subtree created through the [[BehaviorTreeLibraryManager]]. Subsequent calls do nothing since the child has already been
    * set. An `UnsupportedOperationException` is thrown if this `Include` is eager.
    *
    * @throws UnsupportedOperationException
    *   if this `Include` is eager
    */
  override def start(): Unit = {
    if (!isLazy)
      throw new UnsupportedOperationException("A non-lazy Include isn't meant to be run!")

    if (child.isEmpty) {
      // Lazy include is grafted at run-time
      addChild(createSubtreeRootTask())
    }
  }

  /** Returns a clone of the referenced subtree if this `Include` is eager; otherwise returns a clone of itself. */
  override def cloneTask(): Task[E] =
    if (isLazy) {
      super.cloneTask()
    } else {
      // Non lazy include is grafted at clone-time
      createSubtreeRootTask()
    }

  /** Copies this `Include` to the given task. A [[TaskCloneException]] is thrown if this `Include` is eager.
    * @param task
    *   the task to be filled
    * @return
    *   the given task for chaining
    * @throws TaskCloneException
    *   if this `Include` is eager.
    */
  override protected def copyTo(task: Task[E]): Task[E] = {
    if (!isLazy) throw new TaskCloneException("A non-lazy Include should never be copied.")

    val include = task.asInstanceOf[Include[E]]
    include.subtree = subtree
    include.isLazy = isLazy
    guard.foreach { g =>
      include.guard = Nullable(g.cloneTask())
    }

    task
  }

  private def createSubtreeRootTask(): Task[E] = {
    val rootTask = BehaviorTreeLibraryManager.instance.createRootTask[E](subtree.get)
    guard.foreach { g =>
      rootTask.guard = Nullable(g.cloneTask())
    }
    rootTask
  }

  override def newInstance(): Task[E] = new Include[E](Nullable.empty[String], false)

  override def reset(): Unit = {
    isLazy = false
    subtree = Nullable.empty
    super.reset()
  }
}
