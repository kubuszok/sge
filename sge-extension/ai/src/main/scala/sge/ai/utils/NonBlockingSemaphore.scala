/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/NonBlockingSemaphore.java,
 *   com/badlogic/gdx/ai/utils/SimpleNonBlockingSemaphore.java,
 *   com/badlogic/gdx/ai/utils/NonBlockingSemaphoreRepository.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`
 *   Merged with: `SimpleNonBlockingSemaphore.java`, `NonBlockingSemaphoreRepository.java`
 *   Idiom: static repository -> `object NonBlockingSemaphoreRepository`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 144
 * Covenant-baseline-methods: Factory,NonBlockingSemaphore,NonBlockingSemaphoreRepository,SimpleNonBlockingSemaphore,acquire,acquiredResources,addSemaphore,clear,createSemaphore,factory,getSemaphore,release,removeSemaphore,repo,sem,setFactory
 * Covenant-source-reference: com/badlogic/gdx/ai/utils/NonBlockingSemaphore.java
 *   Renames: `com.badlogic.gdx.ai.utils` -> `sge.ai.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`
 *   Idiom: static repository -> `object NonBlockingSemaphoreRepository`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 144
 * Covenant-baseline-methods: Factory,NonBlockingSemaphore,NonBlockingSemaphoreRepository,SimpleNonBlockingSemaphore,acquire,acquiredResources,addSemaphore,clear,createSemaphore,factory,getSemaphore,release,removeSemaphore,repo,sem,setFactory
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package utils

import scala.collection.mutable

/** A counting semaphore that does not block the thread when the requested resource is not available. No actual resource objects are used; the semaphore just keeps a count of the number available and
  * acts accordingly.
  *
  * @author
  *   davebaol (original implementation)
  */
trait NonBlockingSemaphore {

  /** Acquires a resource if available. Equivalent to `acquire(1)`.
    * @return
    *   `true` if the resource has been acquired; `false` otherwise.
    */
  def acquire(): Boolean = acquire(1)

  /** Acquires the specified number of resources if they all are available.
    * @return
    *   `true` if all the requested resources have been acquired; `false` otherwise.
    */
  def acquire(resources: Int): Boolean

  /** Releases a resource returning it to this semaphore. Equivalent to `release(1)`.
    * @return
    *   `true` if the resource has been released; `false` otherwise.
    */
  def release(): Boolean = release(1)

  /** Releases the specified number of resources returning it to this semaphore.
    * @return
    *   `true` if all the requested resources have been released; `false` otherwise.
    */
  def release(resources: Int): Boolean
}

object NonBlockingSemaphore {

  /** Abstract factory for creating concrete instances of classes implementing [[NonBlockingSemaphore]].
    *
    * @author
    *   davebaol (original implementation)
    */
  trait Factory {

    /** Creates a semaphore with the specified name and resources.
      * @param name
      *   the name of the semaphore
      * @param maxResources
      *   the maximum number of resources
      * @return
      *   the newly created semaphore.
      */
    def createSemaphore(name: String, maxResources: Int): NonBlockingSemaphore
  }
}

/** A non-blocking semaphore that does not ensure the atomicity of its operations, meaning that it's not thread-safe.
  *
  * @author
  *   davebaol (original implementation)
  */
class SimpleNonBlockingSemaphore(val name: String, val maxResources: Int) extends NonBlockingSemaphore {

  private var acquiredResources: Int = 0

  override def acquire(resources: Int): Boolean =
    if (acquiredResources + resources <= maxResources) {
      acquiredResources += resources
      true
    } else {
      false
    }

  override def release(resources: Int): Boolean =
    if (acquiredResources - resources >= 0) {
      acquiredResources -= resources
      true
    } else {
      false
    }
}

object SimpleNonBlockingSemaphore {

  /** A concrete factory that can create instances of [[SimpleNonBlockingSemaphore]].
    *
    * @author
    *   davebaol (original implementation)
    */
  class Factory extends NonBlockingSemaphore.Factory {
    override def createSemaphore(name: String, maxResources: Int): NonBlockingSemaphore =
      new SimpleNonBlockingSemaphore(name, maxResources)
  }
}

/** Repository for named [[NonBlockingSemaphore]] instances.
  *
  * @author
  *   davebaol (original implementation)
  */
object NonBlockingSemaphoreRepository {

  private val repo: mutable.HashMap[String, NonBlockingSemaphore] = mutable.HashMap.empty

  private var factory: NonBlockingSemaphore.Factory = new SimpleNonBlockingSemaphore.Factory

  def setFactory(factory: NonBlockingSemaphore.Factory): Unit =
    this.factory = factory

  def addSemaphore(name: String, maxResources: Int): NonBlockingSemaphore = {
    val sem = factory.createSemaphore(name, maxResources)
    repo.put(name, sem)
    sem
  }

  def getSemaphore(name: String): NonBlockingSemaphore =
    repo(name)

  def removeSemaphore(name: String): NonBlockingSemaphore =
    repo.remove(name).get

  def clear(): Unit =
    repo.clear()
}
