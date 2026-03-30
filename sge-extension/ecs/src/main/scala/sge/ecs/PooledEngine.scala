/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/PooledEngine.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: sge.utils.Pool trait instead of LibGDX Pool/ReflectionPool
 *   Idiom: factory registry approach instead of ReflectionPool for components
 *   Idiom: direct getDeclaredConstructor().newInstance() as reflection fallback
 *   Idiom: HashMap[Class[?], Pool[?]] for component pools
 *   Idiom: PooledEntity is a private inner class extending Entity and Pool.Poolable
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

import scala.collection.mutable.HashMap

import sge.utils.Nullable
import sge.utils.Pool

/** Supports [[Entity]] and [[Component]] pooling. This improves performance in environments where
  * creating/deleting entities is frequent as it greatly reduces memory allocation.
  *
  *  - Create entities using [[createEntity]]
  *  - Create components using [[createComponent]]
  *  - Components should implement [[Pool.Poolable]] when in need to reset their state upon removal
  *
  * @author
  *   David Saltares (original implementation)
  */
class PooledEngine(
    entityPoolInitialSize: Int = 10,
    entityPoolMaxSize: Int = 100,
    componentPoolInitialSize: Int = 10,
    componentPoolMaxSize: Int = 100
) extends Engine {

  private val entityPool: EntityPool = new EntityPool(entityPoolInitialSize, entityPoolMaxSize)
  private val componentPools: ComponentPools = new ComponentPools(componentPoolInitialSize, componentPoolMaxSize)
  private val componentFactories: HashMap[Class[?], () => ?] = HashMap.empty

  /** Registers a factory function for creating components of the given type.
    * This is the cross-platform way to use [[createComponent]] — required on Scala.js and Scala Native
    * where reflection-based instantiation is not available.
    */
  def registerComponentFactory[T <: Component](componentClass: Class[T], factory: () => T): Unit = {
    componentFactories.put(componentClass, factory)
  }

  /** @return Clean [[Entity]] from the Engine pool. In order to add it to the [[Engine]], use [[addEntity]]. */
  override def createEntity(): Entity = entityPool.obtain()

  /** Retrieves a new [[Component]] from the [[Engine]] pool. It will be placed back in the pool whenever it's removed
    * from an [[Entity]] or the [[Entity]] itself is removed.
    * Overrides the default implementation of Engine (creating a new Object).
    */
  override def createComponent[T <: Component](componentType: Class[T]): Nullable[T] = {
    Nullable(componentPools.obtain(componentType))
  }

  /** Removes all free entities and components from their pools. Although this will likely result in garbage collection,
    * it will free up memory.
    */
  def clearPools(): Unit = {
    entityPool.clear()
    componentPools.clear()
  }

  override protected def removeEntityInternal(entity: Entity): Unit = {
    super.removeEntityInternal(entity)

    entity match {
      case pooled: PooledEntity => entityPool.free(pooled)
      case _ => ()
    }
  }

  private class PooledEntity extends Entity with Pool.Poolable {

    override private[ecs] def removeInternal(componentClass: Class[? <: Component]): Component = {
      val removed = super.removeInternal(componentClass)
      if (removed != null) {
        componentPools.free(removed)
      }
      removed
    }

    override def reset(): Unit = {
      removeAll()
      flags = 0
      componentAdded.removeAllListeners()
      componentRemoved.removeAllListeners()
      scheduledForRemoval = false
      removing = false
    }
  }

  private class EntityPool(poolInitialSize: Int, poolMaxSize: Int) extends Pool[PooledEntity] {
    override protected val initialCapacity: Int = poolInitialSize
    override protected val max: Int = poolMaxSize

    override protected def newObject(): PooledEntity = new PooledEntity()

    /** Forwarding this call ensures [[Pool.Poolable]] [[Component]] instances are returned to their respective
      * [[ComponentPools]] even if the [[EntityPool]] is full.
      */
    override protected def discard(pooledEntity: PooledEntity): Unit = {
      pooledEntity.reset()
    }
  }

  private class ComponentPools(poolInitialSize: Int, poolMaxSize: Int) {
    private val pools: HashMap[Class[?], Pool[?]] = HashMap.empty

    def obtain[T](componentType: Class[T]): T = {
      val pool = pools.getOrElseUpdate(componentType, createPool(componentType))
      pool.asInstanceOf[Pool[T]].obtain()
    }

    def free(obj: AnyRef): Unit = {
      val pool = pools.get(obj.getClass)
      pool.foreach { p =>
        p.asInstanceOf[Pool[AnyRef]].free(obj)
      }
    }

    def clear(): Unit = {
      pools.valuesIterator.foreach(_.clear())
    }

    private def createPool[T](componentType: Class[T]): Pool[T] = {
      val factory = componentFactories.get(componentType) match {
        case Some(f) => f.asInstanceOf[() => T]
        case None =>
          // Fallback: try to use the factory or throw a clear error
          () =>
            throw new IllegalArgumentException(
              "No component factory registered for " + componentType.getName +
                ". Call pooledEngine.registerComponentFactory(classOf[" + componentType.getSimpleName +
                "], () => new " + componentType.getSimpleName + "()) before using createComponent."
            )
      }
      new Pool[T] {
        override protected val initialCapacity: Int = poolInitialSize
        override protected val max: Int = poolMaxSize
        override protected def newObject(): T = factory()
      }
    }
  }
}
