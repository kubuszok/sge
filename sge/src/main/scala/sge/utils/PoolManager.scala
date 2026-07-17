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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 98
 * Covenant-baseline-methods: PoolManager,addPool,clazz,clear,free,hasPool,obtain,obtainOrNull,oldPool,pool,poolOrNull,typePools
 * Covenant-source-reference: com/badlogic/gdx/utils/PoolManager.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 80398bf4c2814b27b5234e6a85487a0691968a31
 */
package sge
package utils

import lowlevel.Nullable

import scala.collection.mutable.{ Map => MutableMap }
import scala.reflect.ClassTag

/** A class that can be used to handle multiple pools together. Explicit pool registration is needed via {@link PoolManager#addPool}/{@link PoolManager#addPool}.
  */
class PoolManager {

  /** Type-keyed registry of pools.
    *
    * DOCUMENTED DEVIATION FROM LibGDX (ISS-803): upstream `PoolManager` (and the deprecated static `Pools`) back this registry with a plain unsynchronized `ObjectMap` and rely on the "game thread
    * only" contract (see the ISS-603 / ISS-797 deviation block in [[sge.utils.Pool]]). SGE already broke that contract by making [[sge.utils.Pool]] internally thread-safe, and this registry sits
    * directly next to those now-thread-safe pools (its JVM-global instances are `Actor.POOLS` and `Actions.ACTION_POOLS`, which the parallel test environment and SGE-original code exercise across
    * threads). For consistency we choose option (a): guard every mutation and read of `typePools` on the map's own monitor. Without this, concurrent `addPool`/`obtain` racing the shared HashMap's
    * resize can lose entries or throw from inside HashMap internals (reproduced by `PoolManagerConcurrencyIss803Suite`).
    *
    * The monitor is the private map instance itself (never exposed, so external code cannot interfere) and it guards ONLY the map structure. Per-pool operations (`obtain`/`free`/`clear`) run on each
    * [[sge.utils.Pool]]'s OWN lock, OUTSIDE this monitor, so there is no nested `PoolManager` -> `Pool` lock edge and thus no lock-ordering (AB-BA) concern with the Pool deviation.
    */
  private val typePools: MutableMap[Class[?], Pool[?]] = MutableMap.empty

  /** Registers a new pool with the given supplier. Will throw an exception, if a pool for the same class is already registered.
    */
  def addPool[T: ClassTag](poolSupplier: () => T)(using Poolable[T]): Unit =
    addPool(Pool.Default[T](poolSupplier))

  /** Registers the new pool. Will throw an exception, if a pool for the same class is already registered */
  def addPool[T: ClassTag](pool: Pool[T]): Unit = {
    val clazz   = summon[ClassTag[T]].runtimeClass
    val oldPool = typePools.synchronized(typePools.put(clazz, pool))
    if (oldPool.isDefined) {
      throw SgeError.InvalidInput(
        s"Attempt to add pool with already existing class: $clazz, register using poolManager.addPool[${clazz.getSimpleName}](() => new ${clazz.getSimpleName}())"
      )
    }
  }

  /** Returns the pool registered for the class. Will throw an exception, if no pool for this class is registered */
  def pool[T: ClassTag]: Pool[T] = {
    val clazz = summon[ClassTag[T]].runtimeClass
    typePools.synchronized(typePools.get(clazz)) match {
      case Some(pool) => pool.asInstanceOf[Pool[T]]
      case None       =>
        throw SgeError.InvalidInput(
          s"Attempt to get pool with unknown class: $clazz, register using poolManager.addPool[${clazz.getSimpleName}](() => new ${clazz.getSimpleName}())"
        )
    }
  }

  /** Returns the pool registered for the class. Will return Nullable.empty, if no pool for this class is registered */
  def poolOrNull[T: ClassTag]: Nullable[Pool[T]] =
    Nullable.fromOption(typePools.synchronized(typePools.get(summon[ClassTag[T]].runtimeClass)).map(_.asInstanceOf[Pool[T]]))

  /** Whether a pool for this class is already registered */
  def hasPool(clazz: Class[?]): Boolean =
    typePools.synchronized(typePools.contains(clazz))

  /** Returns a new pooled object for the class. Will throw an exception, if no pool for this class is registered. Free with {@link PoolManager#free}
    */
  def obtain[T: ClassTag]: T = {
    val clazz = summon[ClassTag[T]].runtimeClass
    typePools.synchronized(typePools.get(clazz)) match {
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
    typePools.synchronized(typePools.get(summon[ClassTag[T]].runtimeClass)) match {
      case Some(pool) => Nullable(pool.asInstanceOf[Pool[T]].obtain())
      case None       => Nullable.empty
    }

  /** Frees a pooled object. Will throw an exception, if no pool for this class is registered. It is unchecked, whether the object was obtained by the registered pool.
    */
  def free[T](obj: T): Unit =
    typePools.synchronized(typePools.get(obj.getClass)) match {
      case Some(pool) => pool.asInstanceOf[Pool[T]].free(obj)
      case None       =>
        throw SgeError.InvalidInput(
          s"Attempt to free pooled object with unknown class: ${obj.getClass}, register using poolManager.addPool[${obj.getClass.getSimpleName}](() => new ${obj.getClass.getSimpleName}())"
        )
    }

  /** Clears all contents of the managed pools */
  def clear(): Unit =
    // Snapshot the pools under the map monitor, then clear each on its own Pool lock (outside this monitor).
    typePools.synchronized(typePools.values.toList).foreach(_.clear())
}
