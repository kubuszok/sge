/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/PoolManager.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `GdxRuntimeException` -> `SgeError.InvalidInput`; libGDX `ObjectMap` -> `scala.collection.mutable.Map`
 *   Convention: uses `Nullable` instead of null returns; Scala `MutableMap` for internal storage; ClassTag instead of Class[T] parameters
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.collection.mutable.{ Map => MutableMap }
import scala.reflect.ClassTag

/** A class that can be used to handle multiple pools together. Explicit pool registration is needed via {@link PoolManager#addPool}/{@link PoolManager#addPool}.
  */
class PoolManager {

  private val typePools: MutableMap[Class[?], Pool[?]] = MutableMap.empty

  /** Registers a new pool with the given supplier. Will throw an exception, if a pool for the same class is already registered.
    */
  def addPool[T: ClassTag](poolSupplier: () => T): Unit =
    addPool(Pool.Default[T](poolSupplier))

  /** Registers the new pool. Will throw an exception, if a pool for the same class is already registered */
  def addPool[T: ClassTag](pool: Pool[T]): Unit = {
    val clazz   = summon[ClassTag[T]].runtimeClass
    val oldPool = typePools.put(clazz, pool)
    if (oldPool.isDefined) {
      throw SgeError.InvalidInput(
        s"Attempt to add pool with already existing class: $clazz, register using poolManager.addPool[${clazz.getSimpleName}](() => new ${clazz.getSimpleName}())"
      )
    }
  }

  /** Returns the pool registered for the class. Will throw an exception, if no pool for this class is registered */
  def getPool[T: ClassTag]: Pool[T] = {
    val clazz = summon[ClassTag[T]].runtimeClass
    typePools.get(clazz) match {
      case Some(pool) => pool.asInstanceOf[Pool[T]]
      case None       =>
        throw SgeError.InvalidInput(
          s"Attempt to get pool with unknown class: $clazz, register using poolManager.addPool[${clazz.getSimpleName}](() => new ${clazz.getSimpleName}())"
        )
    }
  }

  /** Returns the pool registered for the class. Will return Nullable.empty, if no pool for this class is registered */
  def getPoolOrNull[T: ClassTag]: Nullable[Pool[T]] =
    Nullable.fromOption(typePools.get(summon[ClassTag[T]].runtimeClass).map(_.asInstanceOf[Pool[T]]))

  /** Whether a pool for this class is already registered */
  def hasPool(clazz: Class[?]): Boolean =
    typePools.contains(clazz)

  /** Returns a new pooled object for the class. Will throw an exception, if no pool for this class is registered. Free with {@link PoolManager#free}
    */
  def obtain[T: ClassTag]: T = {
    val clazz = summon[ClassTag[T]].runtimeClass
    typePools.get(clazz) match {
      case Some(pool) => pool.asInstanceOf[Pool[T]].obtain()
      case None       =>
        throw SgeError.InvalidInput(
          s"Attempt to get pooled object with unknown class: $clazz, register using poolManager.addPool[${clazz.getSimpleName}](() => new ${clazz.getSimpleName}())"
        )
    }
  }

  /** Returns a new pooled object for the class. Will return Nullable.empty, if no pool for this class is registered. Free with {@link PoolManager#free}
    */
  def obtainOrNull[T: ClassTag]: Nullable[T] =
    typePools.get(summon[ClassTag[T]].runtimeClass) match {
      case Some(pool) => Nullable(pool.asInstanceOf[Pool[T]].obtain())
      case None       => Nullable.empty
    }

  /** Frees a pooled object. Will throw an exception, if no pool for this class is registered. It is unchecked, whether the object was obtained by the registered pool.
    */
  def free[T](obj: T): Unit =
    typePools.get(obj.getClass) match {
      case Some(pool) => pool.asInstanceOf[Pool[T]].free(obj)
      case None       =>
        throw SgeError.InvalidInput(
          s"Attempt to free pooled object with unknown class: ${obj.getClass}, register using poolManager.addPool[${obj.getClass.getSimpleName}](() => new ${obj.getClass.getSimpleName}())"
        )
    }

  /** Clears all contents of the managed pools */
  def clear(): Unit =
    typePools.values.foreach(_.clear())
}
