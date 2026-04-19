/*
 * Ported from simple-graphs - https://github.com/earlygrey/simple-graphs
 * Licensed under the ISC License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 90
 * Covenant-baseline-methods: Path,_emptyPath,_fixed,_length,add,addAll,checkFixed,clear,emptyPath,first,last,length,length_,removeAll,removeAt,removeIf,removeItem,retainAll,set,setFixed
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package graphs

import sge.graphs.internal.InternalArray

/** A path in the graph: an ordered sequence of vertices with a total length (sum of edge weights). */
class Path[V](initialSize: Int, resize: Boolean = false) extends InternalArray[V](initialSize, resize) {

  private var _length: Float   = 0f
  private var _fixed:  Boolean = false

  /** @return the length of this path, that is, the sum of the edge weights of all edges contained in the path. */
  def length: Float = _length

  protected[graphs] def length_=(len: Float): Unit =
    _length = len

  protected[graphs] def setFixed(fixed: Boolean): Unit =
    _fixed = fixed

  private def checkFixed(): Unit =
    if (_fixed) throw UnsupportedOperationException("You cannot modify this path.")

  override def add(item: V): Boolean = {
    checkFixed()
    super.add(item)
  }

  override def set(index: Int, item: V): V = {
    checkFixed()
    super.set(index, item)
  }

  override def removeItem(item: Any): Boolean = {
    checkFixed()
    super.removeItem(item)
  }

  override def addAll(collection: Iterable[V]): Boolean = {
    checkFixed()
    super.addAll(collection)
  }

  override def removeAt(index: Int): V = {
    checkFixed()
    super.removeAt(index)
  }

  override def clear(): Unit = {
    checkFixed()
    super.clear()
  }

  override def removeAll(c: Iterable[?]): Boolean = {
    checkFixed()
    super.removeAll(c)
  }

  override def retainAll(c: Iterable[?]): Boolean = {
    checkFixed()
    super.retainAll(c)
  }

  override def removeIf(filter: V => Boolean): Boolean = {
    checkFixed()
    super.removeIf(filter)
  }

  def first: V = {
    if (isEmpty) throw IllegalStateException("Path has no vertices.")
    get(0)
  }

  override def last: V = {
    if (isEmpty) throw IllegalStateException("Path has no vertices.")
    get(size - 1)
  }
}

object Path {
  private val _emptyPath: Path[Any] = new Path[Any](0, false)

  def emptyPath[V]: Path[V] = _emptyPath.asInstanceOf[Path[V]]
}
