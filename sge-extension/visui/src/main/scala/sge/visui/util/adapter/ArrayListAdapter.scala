/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 91
 * Covenant-baseline-methods: ArrayListAdapter,add,addAll,clear,get,indexOf,iterable,remove,removeAll,res,set,size,sort
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/ArrayListAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package adapter

import java.util
import java.util.{ ArrayList, Comparator }

import sge.scenes.scene2d.Actor

/** Built-in adapter implementation for [[ArrayList]].
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
abstract class ArrayListAdapter[ItemT, ViewT <: Actor](private val array: ArrayList[ItemT])(using Sge) extends AbstractListAdapter[ItemT, ViewT] {

  override def iterable: Iterable[ItemT] = {
    import scala.jdk.CollectionConverters._
    array.asScala
  }

  override def size: Int = array.size()

  override def indexOf(item: ItemT): Int = array.indexOf(item)

  override def add(element: ItemT): Unit = {
    array.add(element)
    itemAdded(element)
  }

  override def get(index: Int): ItemT = array.get(index)

  override protected def sort(comparator: Comparator[ItemT]): Unit =
    array.sort(comparator)

  // Delegates

  def set(index: Int, element: ItemT): ItemT = {
    val res = array.set(index, element)
    itemsChanged()
    res
  }

  def add(index: Int, element: ItemT): Unit = {
    array.add(index, element)
    itemAdded(element)
  }

  def remove(index: Int): ItemT = {
    val res = array.remove(index)
    if (res != null) itemRemoved(res) // @nowarn -- ArrayList may return null
    res
  }

  def remove(item: ItemT): Boolean = {
    val res = array.remove(item)
    if (res) itemRemoved(item)
    res
  }

  def clear(): Unit = {
    array.clear()
    itemsChanged()
  }

  def addAll(c: util.Collection[? <: ItemT]): Boolean = {
    val res = array.addAll(c)
    itemsChanged()
    res
  }

  def addAll(index: Int, c: util.Collection[? <: ItemT]): Boolean = {
    val res = array.addAll(index, c)
    itemsChanged()
    res
  }

  def removeAll(c: util.Collection[?]): Boolean = {
    val res = array.removeAll(c)
    itemsChanged()
    res
  }
}
