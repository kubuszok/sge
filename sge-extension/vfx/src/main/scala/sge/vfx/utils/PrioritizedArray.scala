/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package utils

import scala.collection.mutable.ArrayBuffer

/** A container that maintains items sorted by priority. */
class PrioritizedArray[T] extends Iterable[T] {

  private val items: ArrayBuffer[PrioritizedArray.Entry[T]] = ArrayBuffer.empty

  def get(index: Int): T = items(index).item

  def add(item: T): Unit = add(item, 0)

  def add(item: T, priority: Int): Unit = {
    items += PrioritizedArray.Entry(item, priority)
    sortItems()
  }

  def remove(index: Int): Unit = {
    items.remove(index)
  }

  def remove(item: T): Unit = {
    val idx = items.indexWhere(_.item.asInstanceOf[AnyRef] eq item.asInstanceOf[AnyRef])
    if (idx >= 0) items.remove(idx)
  }

  def contains(item: T): Boolean =
    items.exists(_.item.asInstanceOf[AnyRef] eq item.asInstanceOf[AnyRef])

  def clear(): Unit = items.clear()

  override def size: Int = items.size

  def setPriority(item: T, priority: Int): Unit = {
    val idx = items.indexWhere(_.item.asInstanceOf[AnyRef] eq item.asInstanceOf[AnyRef])
    if (idx >= 0) {
      items(idx).priority = priority
      sortItems()
    }
  }

  override def iterator: Iterator[T] = items.iterator.map(_.item)

  private def sortItems(): Unit = {
    // Stable sort by priority
    val sorted = items.sortBy(_.priority)
    items.clear()
    items ++= sorted
  }
}

object PrioritizedArray {
  private class Entry[T](val item: T, var priority: Int)
}
