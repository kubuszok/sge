/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/SemaphoreGuard.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package decorator

import sge.ai.utils.NonBlockingSemaphore
import sge.ai.utils.NonBlockingSemaphoreRepository
import sge.utils.Nullable

/** A `SemaphoreGuard` decorator allows you to specify how many characters should be allowed to concurrently execute its child which represents a limited resource used in different behavior trees
  * (note that this does not necessarily involve multithreading concurrency).
  *
  * This is a simple mechanism for ensuring that a limited shared resource is not over subscribed. You might have a pool of 5 pathfinders, for example, meaning at most 5 characters can be pathfinding
  * at a time. Or you can associate a semaphore to the player character to ensure that at most 3 enemies can simultaneously attack him.
  *
  * This decorator fails when it cannot acquire the semaphore. This allows a selector task higher up the tree to find a different action that doesn't involve the contested resource.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
class SemaphoreGuard[E](
  /** The semaphore name. */
  var name: Nullable[String] = Nullable.empty,
  child:    Nullable[Task[E]] = Nullable.empty
) extends Decorator[E](child) {

  private var semaphore:         Nullable[NonBlockingSemaphore] = Nullable.empty
  private var semaphoreAcquired: Boolean                        = false

  /** Acquires the semaphore. Also, the first execution of this method retrieves the semaphore by name and stores it locally.
    *
    * This method is called when the task is entered.
    */
  override def start(): Unit = {
    if (semaphore.isEmpty) {
      semaphore = Nullable(NonBlockingSemaphoreRepository.getSemaphore(name.get))
    }
    semaphoreAcquired = semaphore.get.acquire()
    super.start()
  }

  /** Runs its child if the semaphore has been successfully acquired; immediately fails otherwise. */
  override def run(): Unit =
    if (semaphoreAcquired) {
      super.run()
    } else {
      fail()
    }

  /** Releases the semaphore.
    *
    * This method is called when the task exits.
    */
  override def end(): Unit = {
    if (semaphoreAcquired) {
      if (semaphore.isEmpty) {
        semaphore = Nullable(NonBlockingSemaphoreRepository.getSemaphore(name.get))
      }
      semaphore.get.release()
      semaphoreAcquired = false
    }
    super.end()
  }

  override def resetTask(): Unit = {
    super.resetTask()
    semaphore = Nullable.empty
    semaphoreAcquired = false
  }

  override protected def copyTo(task: Task[E]): Task[E] = {
    val sg = task.asInstanceOf[SemaphoreGuard[E]]
    sg.name = name
    sg.semaphore = Nullable.empty
    sg.semaphoreAcquired = false
    super.copyTo(task)
  }

  override def newInstance(): Task[E] = new SemaphoreGuard[E](child = Nullable.empty[Task[E]])

  override def reset(): Unit = {
    name = Nullable.empty
    semaphore = Nullable.empty
    semaphoreAcquired = false
    super.reset()
  }
}
