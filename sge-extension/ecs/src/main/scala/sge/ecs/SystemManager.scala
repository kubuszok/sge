/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/SystemManager.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: ArrayBuffer instead of Array; HashMap instead of ObjectMap
 *   Idiom: Ordering[EntitySystem] instead of Comparator with inner class
 *   Idiom: Nullable[A] for getSystem return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 80
 * Covenant-baseline-methods: SystemListener,SystemManager,addSystem,getSystem,getSystems,idx,immutableSystems,oldSystem,removeAllSystems,removeSystem,sortSystems,sorted,systemAdded,systemComparator,systemRemoved,systemType,systems,systemsByClass
 * Covenant-source-reference: com/badlogic/ashley/core/SystemManager.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import sge.ecs.utils.ImmutableArray
import sge.utils.Nullable

/** Manages [[EntitySystem]]s within an [[Engine]]. Systems are kept sorted by priority and can be looked up by class.
  */
private[ecs] class SystemManager(listener: SystemManager.SystemListener) {

  private val systemComparator: Ordering[EntitySystem]          = Ordering.by(_.priority)
  private val systems:          ArrayBuffer[EntitySystem]       = ArrayBuffer.empty
  private val immutableSystems: ImmutableArray[EntitySystem]    = new ImmutableArray[EntitySystem](systems)
  private val systemsByClass:   HashMap[Class[?], EntitySystem] = HashMap.empty

  def addSystem(system: EntitySystem): Unit = {
    val systemType = system.getClass
    val oldSystem  = getSystem(systemType)

    oldSystem.foreach { old =>
      removeSystem(old)
    }

    systems += system
    systemsByClass.put(systemType, system)
    sortSystems()
    listener.systemAdded(system)
  }

  def removeSystem(system: EntitySystem): Unit = {
    val idx = systems.indexOf(system)
    if (idx >= 0) {
      systems.remove(idx)
      systemsByClass.remove(system.getClass)
      listener.systemRemoved(system)
    }
  }

  def removeAllSystems(): Unit =
    while (systems.nonEmpty)
      removeSystem(systems.head)

  def getSystem[A <: EntitySystem](systemType: Class[A]): Nullable[A] =
    Nullable.fromOption(systemsByClass.get(systemType).map(_.asInstanceOf[A]))

  def getSystems: ImmutableArray[EntitySystem] = immutableSystems

  private def sortSystems(): Unit = {
    val sorted = systems.sorted(using systemComparator)
    systems.clear()
    systems ++= sorted
  }
}

private[ecs] object SystemManager {

  /** Callback interface for engine notification when systems are added/removed. */
  trait SystemListener {
    def systemAdded(system:   EntitySystem): Unit
    def systemRemoved(system: EntitySystem): Unit
  }
}
