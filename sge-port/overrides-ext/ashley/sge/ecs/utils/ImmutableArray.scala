/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/utils/ImmutableArray.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */

/** Injected replacement: `Array<T>` retargets to `DynamicArray`, and three methods dispatch on a
  * non-literal boolean identity flag `BoolDispatch` cannot handle statically, plus a nested
  * `Array.ArrayIterable` reference the retarget removes. Drop-in parity with
  * sge's hand port (`Iterable[A]`, parenless `iterator`): both a `DynamicArray[A]` constructor
  * (what emitted ashley code passes) and an `ArrayBuffer[A]` one (sge's own tests) are provided. */
package sge.ecs.utils

import scala.collection.mutable.ArrayBuffer
import lowlevel.util.DynamicArray
import lowlevel.Nullable

/** Read-only wrapper around a mutable collection. This is a live view -- changes to the backing
  * data are visible through this wrapper.
  *
  * @author David Saltares (original implementation)
  */
final class ImmutableArray[A] private (
    private val dynArray: DynamicArray[A],
    private val bufArray: ArrayBuffer[A]
) extends Iterable[A] {

  /** Constructor for the emitted ashley code, which uses DynamicArray (via retarget). */
  def this(array: DynamicArray[A]) = this(array, null)

  /** Constructor for sge's own code, which uses ArrayBuffer. */
  def this(array: ArrayBuffer[A]) = this(null.asInstanceOf[DynamicArray[A]], array)

  // The empty backing array is built in the COMPANION: a local `given` inside a constructor
  // argument is lifted to a member by the Scala.js back end and refused as a self reference
  // (`super constructor cannot be passed a self reference this.given_MkArray_A`), while the JVM
  // compile accepts it -- measured at the 3.1ac merge, ashley js 0 -> 1. `DynamicArray.apply` is
  // `inline` and summons its `MkArray` at the call site, so the given must be in lexical scope.
  def this() = this(ImmutableArray.emptyBacking[A])

  private inline def useDyn: Boolean = dynArray != null

  override def size: Int = if (useDyn) dynArray.size else bufArray.size

  def apply(index: Int): A = if (useDyn) dynArray(index) else bufArray(index)

  /** Alias for [[apply]] -- the mechanically ported code calls `get(i)` because that is what the
    * java source declares. */
  def get(index: Int): A = apply(index)

  def contains(value: A, identity: Boolean): Boolean =
    if (useDyn) {
      if (identity) dynArray.containsByRef(value) else dynArray.contains(value)
    } else {
      bufArray.contains(value)
    }

  /** 1-arg overload for sge parity: the hand port's ImmutableArray delegates to Iterable.contains
    * which takes one argument. Forwards to the emitted 2-arg form (the faithful translation of
    * `ImmutableArray.contains(T, boolean)`) with `identity=false`, java's default. `@targetName`
    * avoids a JVM-level clash with `Iterable.contains[A1 >: A](elem: A1)`, which erases the same. */
  @scala.annotation.targetName("containsValue")
  def contains(value: A): Boolean = contains(value, false)

  def indexOf(value: A, identity: Boolean): Int =
    if (useDyn) {
      if (identity) dynArray.indexOfByRef(value) else dynArray.indexOf(value)
    } else {
      bufArray.indexOf(value)
    }

  def indexOf(value: A): Int =
    if (useDyn) dynArray.indexOf(value) else bufArray.indexOf(value)

  def lastIndexOf(value: A, identity: Boolean): Int =
    if (useDyn) {
      if (identity) dynArray.lastIndexOfByRef(value) else dynArray.lastIndexOf(value)
    } else {
      bufArray.lastIndexOf(value)
    }

  def lastIndexOf(value: A): Int =
    if (useDyn) dynArray.lastIndexOf(value) else bufArray.lastIndexOf(value)

  def random(): Nullable[A] = {
    val sz = size
    if (sz == 0) Nullable.empty[A]
    else Nullable(apply(lowlevel.math.MathUtils.random(sz - 1)))
  }

  def peek: A = if (useDyn) dynArray.peek else bufArray.last

  def first: A = if (useDyn) dynArray.first else bufArray.head

  /** Returns a shallow copy of the backing data as a Scala Array[Any]. */
  def toArray: Array[Any] =
    if (useDyn) dynArray.toArray.asInstanceOf[Array[Any]]
    else bufArray.toArray[Any]

  override def hashCode(): Int =
    if (useDyn) dynArray.hashCode() else bufArray.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: ImmutableArray[?] =>
      if (useDyn && other.useDyn) dynArray == other.dynArray
      else if (!useDyn && !other.useDyn) bufArray == other.bufArray
      else false
    case _ => false
  }

  override def iterator: Iterator[A] =
    if (useDyn) dynArray.iterator else bufArray.iterator

  override def toString(): String =
    if (useDyn) dynArray.toString() else bufArray.mkString("[", ", ", "]")

  def toString(separator: String): String = iterator.mkString(separator)
}

object ImmutableArray {
  private[utils] def emptyBacking[A]: DynamicArray[A] = {
    given lowlevel.MkArray[A] = lowlevel.MkArray.anyRef.asInstanceOf[lowlevel.MkArray[A]]
    DynamicArray[A]()
  }
}
