/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/FamilyManager.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: ArrayBuffer instead of Array/SnapshotArray; HashMap instead of ObjectMap
 *   Idiom: mutable.BitSet instead of Bits; Pool for BitSet reuse
 *   Idiom: snapshot copy during listener notification (replaces SnapshotArray.begin/end)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import sge.ecs.utils.ImmutableArray
import sge.utils.Pool

/** Manages [[Family]]-to-entity mappings and notifies [[EntityListener]]s when entity family membership changes due to component additions or removals.
  *
  * This is the most complex manager in the ECS -- it handles deferred listener registration via bitmask tracking and snapshot-based notification to allow safe concurrent modification.
  */
private[ecs] class FamilyManager(val entities: ImmutableArray[Entity]) {

  private val families:            HashMap[Family, ArrayBuffer[Entity]]    = HashMap.empty
  private val immutableFamilies:   HashMap[Family, ImmutableArray[Entity]] = HashMap.empty
  private val entityListeners:     ArrayBuffer[EntityListenerData]         = ArrayBuffer.empty
  private val entityListenerMasks: HashMap[Family, mutable.BitSet]         = HashMap.empty
  private val bitsPool:            BitSetPool                              = new BitSetPool()
  private var _notifying:          Boolean                                 = false

  def notifying: Boolean = _notifying

  def getEntitiesFor(family: Family): ImmutableArray[Entity] =
    registerFamily(family)

  def addEntityListener(family: Family, priority: Int, listener: EntityListener): Unit = {
    registerFamily(family)

    var insertionIndex = 0
    var searching      = true
    while (searching && insertionIndex < entityListeners.size)
      if (entityListeners(insertionIndex).priority <= priority) {
        insertionIndex += 1
      } else {
        searching = false
      }

    doAddEntityListenerAtIndex(family, priority, listener, insertionIndex)
  }

  private def doAddEntityListenerAtIndex(
    family:         Family,
    priority:       Int,
    listener:       EntityListener,
    insertionIndex: Int
  ): Unit = {
    // Shift up bitmasks by one step
    for (mask <- entityListenerMasks.valuesIterator) {
      val len = if (mask.isEmpty) 0 else mask.last + 1
      var k   = len
      while (k > insertionIndex) {
        if (mask.contains(k - 1)) {
          mask += k
        } else {
          mask -= k
        }
        k -= 1
      }
      mask -= insertionIndex
    }

    entityListenerMasks(family) += insertionIndex

    val data = new EntityListenerData()
    data.listener = listener
    data.priority = priority
    entityListeners.insert(insertionIndex, data)
  }

  def removeEntityListener(listener: EntityListener): Unit = {
    var i = 0
    while (i < entityListeners.size) {
      val data = entityListeners(i)
      if (data.listener eq listener) {
        // Shift down bitmasks by one step
        for (mask <- entityListenerMasks.valuesIterator) {
          val len = if (mask.isEmpty) 0 else mask.last + 1
          var k   = i
          while (k < len) {
            if (mask.contains(k + 1)) {
              mask += k
            } else {
              mask -= k
            }
            k += 1
          }
        }

        entityListeners.remove(i)
        i -= 1 // compensate for removal
      }
      i += 1
    }
  }

  /** Re-evaluates all family memberships for the given entity and fires add/remove events. */
  def updateFamilyMembership(entity: Entity): Unit = {
    // Find families that the entity was added to/removed from, and fill
    // the bitmasks with corresponding listener bits.
    val addListenerBits    = bitsPool.obtain()
    val removeListenerBits = bitsPool.obtain()

    for (family <- entityListenerMasks.keysIterator) {
      val familyIndex      = family.index
      val entityFamilyBits = entity.getFamilyBits

      val belongsToFamily = entityFamilyBits.contains(familyIndex)
      val matches         = family.matches(entity) && !entity.removing

      if (belongsToFamily != matches) {
        val listenersMask  = entityListenerMasks(family)
        val familyEntities = families(family)
        if (matches) {
          addListenerBits |= listenersMask
          familyEntities += entity
          entityFamilyBits += familyIndex
        } else {
          removeListenerBits |= listenersMask
          familyEntities -= entity
          entityFamilyBits -= familyIndex
        }
      }
    }

    // Notify listeners; set bits match indices of listeners
    _notifying = true
    // Snapshot: copy the current listener list to allow safe modification during notification
    val snapshot = entityListeners.toArray

    try {
      removeListenerBits.foreach { i =>
        if (i < snapshot.length) {
          snapshot(i).listener.entityRemoved(entity)
        }
      }

      addListenerBits.foreach { i =>
        if (i < snapshot.length) {
          snapshot(i).listener.entityAdded(entity)
        }
      }
    } finally {
      addListenerBits.clear()
      removeListenerBits.clear()
      bitsPool.free(addListenerBits)
      bitsPool.free(removeListenerBits)
      _notifying = false
    }
  }

  private def registerFamily(family: Family): ImmutableArray[Entity] =
    immutableFamilies.get(family) match {
      case Some(entitiesInFamily) =>
        entitiesInFamily
      case None =>
        val familyEntities   = ArrayBuffer.empty[Entity]
        val entitiesInFamily = new ImmutableArray[Entity](familyEntities)
        families.put(family, familyEntities)
        immutableFamilies.put(family, entitiesInFamily)
        entityListenerMasks.put(family, mutable.BitSet())

        entities.foreach { entity =>
          updateFamilyMembership(entity)
        }

        entitiesInFamily
    }
}

/** Holds an [[EntityListener]] together with its priority for ordered notification. */
final private[ecs] class EntityListenerData {
  var listener: EntityListener = scala.compiletime.uninitialized
  var priority: Int            = 0
}

/** Pool for reusing [[mutable.BitSet]] instances. */
final private[ecs] class BitSetPool extends Pool[mutable.BitSet] {
  override protected val max:             Int = Int.MaxValue
  override protected val initialCapacity: Int = 16

  override protected def newObject(): mutable.BitSet = mutable.BitSet()
}
