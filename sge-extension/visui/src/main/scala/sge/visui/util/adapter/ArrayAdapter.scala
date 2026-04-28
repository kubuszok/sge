/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 131
 * Covenant-baseline-methods: ArrayAdapter,add,addAll,buf,clear,get,i,indexOf,insert,item,iterable,pop,removeAll,removeAllByRef,removeIndex,removeRange,removeValue,res,reverse,set,shuffle,size,sort,swap
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/ArrayAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
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

  def removeRange(start: Int, end: Int): Unit = {
    array.removeRange(start, end)
    itemsChanged()
  }

  def removeAll(other: DynamicArray[? <: ItemT]): Boolean = {
    val res = array.removeAll(other)
    itemsChanged()
    res
  }

  def removeAllByRef(other: DynamicArray[? <: ItemT]): Boolean = {
    val res = array.removeAllByRef(other)
    itemsChanged()
    res
  }

  def addAll(other: DynamicArray[? <: ItemT], start: Int, count: Int): Unit = {
    var i = start
    while (i < start + count && i < other.size) {
      array.add(other(i))
      i += 1
    }
    itemsChanged()
  }
}
