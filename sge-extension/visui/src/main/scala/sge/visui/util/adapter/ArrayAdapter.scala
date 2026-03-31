/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package adapter

import java.util.Comparator

import sge.scenes.scene2d.Actor
import sge.utils.DynamicArray

/** Built-in adapter implementation for [[DynamicArray]].
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
abstract class ArrayAdapter[ItemT, ViewT <: Actor](private val array: DynamicArray[ItemT])(using Sge) extends AbstractListAdapter[ItemT, ViewT] {

  override def indexOf(item: ItemT): Int = array.indexOf(item)

  override def size: Int = array.size

  override def get(index: Int): ItemT = array(index)

  override def add(element: ItemT): Unit = {
    array.add(element)
    itemAdded(element)
  }

  override protected def sort(comparator: Comparator[ItemT]): Unit = {
    given Ordering[ItemT] = (a: ItemT, b: ItemT) => comparator.compare(a, b)
    array.sort()
  }

  override def iterable: Iterable[ItemT] = {
    val buf = new scala.collection.mutable.ArrayBuffer[ItemT](array.size)
    var i   = 0
    while (i < array.size) {
      buf += array(i)
      i += 1
    }
    buf
  }

  // Delegates

  def addAll(other: DynamicArray[? <: ItemT]): Unit = {
    this.array.addAll(other)
    itemsChanged()
  }

  def set(index: Int, value: ItemT): Unit = {
    array(index) = value
    itemsChanged()
  }

  def insert(index: Int, value: ItemT): Unit = {
    array.insert(index, value)
    itemsChanged()
  }

  def swap(first: Int, second: Int): Unit = {
    array.swap(first, second)
    itemsChanged()
  }

  def removeValue(value: ItemT): Boolean = {
    val res = array.removeValue(value)
    if (res) itemRemoved(value)
    res
  }

  def removeIndex(index: Int): ItemT = {
    val item = array.removeIndex(index)
    itemRemoved(item)
    item
  }

  def clear(): Unit = {
    array.clear()
    itemsChanged()
  }

  def shuffle(): Unit = {
    array.shuffle()
    itemsChanged()
  }

  def reverse(): Unit = {
    array.reverse()
    itemsChanged()
  }

  def pop(): ItemT = {
    val item = array.pop()
    itemsChanged()
    item
  }
}
