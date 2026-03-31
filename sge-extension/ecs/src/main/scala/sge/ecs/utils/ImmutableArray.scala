/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/utils/ImmutableArray.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.utils` -> `sge.ecs.utils`
 *   Convention: split packages
 *   Idiom: wraps ArrayBuffer, implements Iterable[A]
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs
package utils

import scala.collection.mutable.ArrayBuffer

/** Read-only wrapper around a mutable [[ArrayBuffer]]. This is a live view -- changes to the backing buffer are visible through this wrapper.
  *
  * @author
  *   David Saltares (original implementation)
  */
final class ImmutableArray[A](private val array: ArrayBuffer[A]) extends Iterable[A] {

  def this() = this(ArrayBuffer.empty[A])

  override def size: Int = array.size

  def apply(index: Int): A = array(index)

  def contains(value: A): Boolean = array.contains(value)

  def indexOf(value: A): Int = array.indexOf(value)

  def lastIndexOf(value: A): Int = array.lastIndexOf(value)

  def peek: A = array.last

  def first: A = array.head

  override def iterator: Iterator[A] = array.iterator

  override def toString(): String = array.mkString("[", ", ", "]")
}
