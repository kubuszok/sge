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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 64
 * Covenant-baseline-methods: ImmutableArray,apply,contains,equals,first,hashCode,indexOf,iterator,lastIndexOf,peek,random,size,this,toArray,toString
 * Covenant-source-reference: com/badlogic/ashley/utils/ImmutableArray.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs
package utils

import scala.collection.mutable.ArrayBuffer
import sge.math.MathUtils
import sge.utils.Nullable

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

  def random(): Nullable[A] =
    if (array.isEmpty) Nullable.empty[A]
    else Nullable(array(MathUtils.random(array.size - 1)))

  def peek: A = array.last

  def first: A = array.head

  /** Returns a shallow copy of the backing data as a Scala Array[Any]. */
  def toArray: Array[Any] = array.toArray[Any]

  override def hashCode(): Int = array.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: ImmutableArray[?] => array == other.array
    case _ => false
  }

  override def iterator: Iterator[A] = array.iterator

  override def toString(): String = array.mkString("[", ", ", "]")

  def toString(separator: String): String = array.mkString(separator)
}
