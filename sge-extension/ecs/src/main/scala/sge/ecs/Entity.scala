/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Entity.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: mutable.BitSet instead of Bits
 *   Idiom: ArrayBuffer instead of Array for componentsArray
 *   Idiom: Bag uses raw null internally (private[ecs])
 *   Idiom: Nullable[A] in public getComponent return type
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import sge.ecs.signals.Signal
import sge.ecs.utils.{Bag, ImmutableArray}
import sge.utils.Nullable

/** Simple containers of [[Component]]s that give them "data". The component's data is then processed by
  * [[EntitySystem]]s.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
class Entity {

  /** A flag that can be used to bit mask this entity. Up to the user to manage. */
  var flags: Int = 0

  /** Will dispatch an event when a component is added. */
  val componentAdded: Signal[Entity] = new Signal[Entity]()

  /** Will dispatch an event when a component is removed. */
  val componentRemoved: Signal[Entity] = new Signal[Entity]()

  private[ecs] var scheduledForRemoval: Boolean = false
  private[ecs] var removing: Boolean = false
  private[ecs] var componentOperationHandler: ComponentOperationHandler = scala.compiletime.uninitialized

  private val components: Bag[Component] = new Bag[Component]()
  private val componentsArray: ArrayBuffer[Component] = ArrayBuffer.empty
  private val immutableComponentsArray: ImmutableArray[Component] = new ImmutableArray[Component](componentsArray)
  private val componentBits: mutable.BitSet = mutable.BitSet()
  private val familyBits: mutable.BitSet = mutable.BitSet()

  /** Adds a [[Component]] to this Entity. If a [[Component]] of the same type already exists, it will be replaced.
    * @return The Entity for easy chaining
    */
  def add(component: Component): Entity = {
    if (addInternal(component)) {
      if (componentOperationHandler != null) {
        componentOperationHandler.add(this)
      } else {
        notifyComponentAdded()
      }
    }
    this
  }

  /** Adds a [[Component]] to this Entity. If a [[Component]] of the same type already exists, it will be replaced.
    * @return The Component for direct component manipulation (e.g. PooledComponent)
    */
  def addAndReturn[T <: Component](component: T): T = {
    add(component)
    component
  }

  /** Removes the [[Component]] of the specified type. Since there is only ever one component of one type, we don't need
    * an instance reference.
    * @return The removed [[Component]], or Nullable.empty if the Entity did not contain such a component.
    */
  def remove[T <: Component](componentClass: Class[T]): Nullable[T] = {
    val componentType = ComponentType.getFor(componentClass)
    val componentTypeIndex = componentType.index

    if (components.isIndexWithinBounds(componentTypeIndex)) {
      val removeComponent = components.get(componentTypeIndex)

      if (removeComponent != null && removeInternal(componentClass) != null) {
        if (componentOperationHandler != null) {
          componentOperationHandler.remove(this)
        } else {
          notifyComponentRemoved()
        }
      }

      Nullable(removeComponent.asInstanceOf[T])
    } else {
      Nullable.empty[T]
    }
  }

  /** Removes all the [[Component]]s from the Entity. */
  def removeAll(): Unit = {
    while (componentsArray.nonEmpty) {
      remove(componentsArray.head.getClass.asInstanceOf[Class[Component]])
    }
  }

  /** @return immutable collection with all the Entity [[Component]]s. */
  def getComponents: ImmutableArray[Component] = immutableComponentsArray

  /** Retrieve a component from this [[Entity]] by class.
    *
    * ''Note:'' the preferred way of retrieving [[Component]]s is using [[ComponentMapper]]s. This method is provided for
    * convenience; using a ComponentMapper provides O(1) access to components while this method provides only O(logn).
    *
    * @param componentClass the class of the component to be retrieved.
    * @return the instance of the specified [[Component]] attached to this [[Entity]], or Nullable.empty if no such
    *         [[Component]] exists.
    */
  def getComponent[T <: Component](componentClass: Class[T]): Nullable[T] = {
    getComponent(ComponentType.getFor(componentClass))
  }

  /** Internal use.
    * @return The [[Component]] object for the specified class, or null if the Entity does not have any components for
    *         that class.
    */
  private[ecs] def getComponent[T <: Component](componentType: ComponentType): Nullable[T] = {
    val componentTypeIndex = componentType.index
    if (componentTypeIndex < components.getCapacity) {
      Nullable(components.get(componentTypeIndex).asInstanceOf[T])
    } else {
      Nullable.empty[T]
    }
  }

  /** @return Whether or not the Entity has a [[Component]] for the specified type. */
  private[ecs] def hasComponent(componentType: ComponentType): Boolean = {
    componentBits.contains(componentType.index)
  }

  /** @return This Entity's component bits, describing all the [[Component]]s it contains. */
  private[ecs] def getComponentBits: mutable.BitSet = componentBits

  /** @return This Entity's [[Family]] bits, describing all the [[EntitySystem]]s it currently is being processed by. */
  private[ecs] def getFamilyBits: mutable.BitSet = familyBits

  /** @return whether or not the component was added. */
  private[ecs] def addInternal(component: Component): Boolean = {
    val componentClass = component.getClass
    val oldComponent = getComponentRaw(componentClass)

    if (component eq oldComponent) {
      false
    } else {
      if (oldComponent != null) {
        removeInternal(componentClass)
      }

      val componentTypeIndex = ComponentType.getIndexFor(componentClass)
      components.set(componentTypeIndex, component)
      componentsArray += component
      componentBits += componentTypeIndex

      true
    }
  }

  /** @return the component if the specified class was found and removed. Otherwise, null */
  private[ecs] def removeInternal(componentClass: Class[? <: Component]): Component = {
    val componentType = ComponentType.getFor(componentClass)
    val componentTypeIndex = componentType.index
    val removeComponent = components.get(componentTypeIndex)

    if (removeComponent != null) {
      components.set(componentTypeIndex, null) // @nowarn: null in internal sparse storage
      componentsArray -= removeComponent
      componentBits -= componentTypeIndex
      removeComponent
    } else {
      null // @nowarn: internal method, null signals absence
    }
  }

  private[ecs] def notifyComponentAdded(): Unit = {
    componentAdded.dispatch(this)
  }

  private[ecs] def notifyComponentRemoved(): Unit = {
    componentRemoved.dispatch(this)
  }

  /** @return true if the entity is scheduled to be removed */
  def isScheduledForRemoval: Boolean = scheduledForRemoval

  /** @return true if the entity is being removed */
  def isRemoving: Boolean = removing

  /** Internal helper that returns raw nullable Component (not wrapped in Nullable). */
  private def getComponentRaw(componentClass: Class[? <: Component]): Component = {
    val componentTypeIndex = ComponentType.getIndexFor(componentClass)
    if (componentTypeIndex < components.getCapacity) {
      components.get(componentTypeIndex)
    } else {
      null // internal use only
    }
  }
}
