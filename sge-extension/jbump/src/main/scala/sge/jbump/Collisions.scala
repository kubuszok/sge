/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 274
 * Covenant-baseline-methods: Collisions,_size,add,applyPermutation,boxed,clear,collision,get,h1s,h2s,i,isEmpty,items,keySort,moveXs,moveYs,normalXs,normalYs,order,others,overlaps,remove,reorder,size,sort,swapMap,tis,touchXs,touchYs,types,w1s,w2s,x1s,x2s,y1s,y2s
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.Nullable

import scala.collection.mutable.{ ArrayBuffer, HashMap }

/** Collection of collision results stored in struct-of-arrays layout for performance. */
class Collisions {

  private val overlaps = ArrayBuffer.empty[Boolean]
  private val tis      = ArrayBuffer.empty[Float]
  private val moveXs   = ArrayBuffer.empty[Float]
  private val moveYs   = ArrayBuffer.empty[Float]
  private val normalXs = ArrayBuffer.empty[Int]
  private val normalYs = ArrayBuffer.empty[Int]
  private val touchXs  = ArrayBuffer.empty[Float]
  private val touchYs  = ArrayBuffer.empty[Float]
  private val x1s      = ArrayBuffer.empty[Float]
  private val y1s      = ArrayBuffer.empty[Float]
  private val w1s      = ArrayBuffer.empty[Float]
  private val h1s      = ArrayBuffer.empty[Float]
  private val x2s      = ArrayBuffer.empty[Float]
  private val y2s      = ArrayBuffer.empty[Float]
  private val w2s      = ArrayBuffer.empty[Float]
  private val h2s      = ArrayBuffer.empty[Float]
  val items:  ArrayBuffer[Item[?]]  = ArrayBuffer.empty
  val others: ArrayBuffer[Item[?]]  = ArrayBuffer.empty
  val types:  ArrayBuffer[Response] = ArrayBuffer.empty
  private var _size = 0

  def add(col: Collision): Unit =
    add(
      col.overlaps,
      col.ti,
      col.move.x,
      col.move.y,
      col.normal.x,
      col.normal.y,
      col.touch.x,
      col.touch.y,
      col.itemRect.x,
      col.itemRect.y,
      col.itemRect.w,
      col.itemRect.h,
      col.otherRect.x,
      col.otherRect.y,
      col.otherRect.w,
      col.otherRect.h,
      col.item,
      col.other,
      col.`type`
    )

  def add(
    overlap:      Boolean,
    ti:           Float,
    moveX:        Float,
    moveY:        Float,
    normalX:      Int,
    normalY:      Int,
    touchX:       Float,
    touchY:       Float,
    x1:           Float,
    y1:           Float,
    w1:           Float,
    h1:           Float,
    x2:           Float,
    y2:           Float,
    w2:           Float,
    h2:           Float,
    item:         Nullable[Item[?]],
    other:        Nullable[Item[?]],
    responseType: Nullable[Response]
  ): Unit = {
    _size += 1
    overlaps += overlap
    tis += ti
    moveXs += moveX
    moveYs += moveY
    normalXs += normalX
    normalYs += normalY
    touchXs += touchX
    touchYs += touchY
    x1s += x1
    y1s += y1
    w1s += w1
    h1s += h1
    x2s += x2
    y2s += y2
    w2s += w2
    h2s += h2
    items += item.get
    others += other.get
    types += responseType.get
  }

  private val collision = Collision()
  private val swapMap   = HashMap.empty[Int, Int]

  def get(index: Int): Nullable[Collision] =
    if (index >= _size) {
      Nullable.Null
    } else {
      collision.set(
        overlaps(index),
        tis(index),
        moveXs(index),
        moveYs(index),
        normalXs(index),
        normalYs(index),
        touchXs(index),
        touchYs(index),
        x1s(index),
        y1s(index),
        w1s(index),
        h1s(index),
        x2s(index),
        y2s(index),
        w2s(index),
        h2s(index)
      )
      collision.item = items(index)
      collision.other = others(index)
      collision.`type` = types(index)
      collision
    }

  def remove(index: Int): Unit =
    if (index < _size) {
      _size -= 1
      overlaps.remove(index)
      tis.remove(index)
      moveXs.remove(index)
      moveYs.remove(index)
      normalXs.remove(index)
      normalYs.remove(index)
      touchXs.remove(index)
      touchYs.remove(index)
      x1s.remove(index)
      y1s.remove(index)
      w1s.remove(index)
      h1s.remove(index)
      x2s.remove(index)
      y2s.remove(index)
      w2s.remove(index)
      h2s.remove(index)
      items.remove(index)
      others.remove(index)
      types.remove(index)
    }

  def size: Int = _size

  def isEmpty: Boolean = _size == 0

  def clear(): Unit = {
    _size = 0
    overlaps.clear()
    tis.clear()
    moveXs.clear()
    moveYs.clear()
    normalXs.clear()
    normalYs.clear()
    touchXs.clear()
    touchYs.clear()
    x1s.clear()
    y1s.clear()
    w1s.clear()
    h1s.clear()
    x2s.clear()
    y2s.clear()
    w2s.clear()
    h2s.clear()
    items.clear()
    others.clear()
    types.clear()
  }

  /** Reorders the elements of `buf` according to the given index permutation. For each position `i`, the element that was originally at `indices(i)` is moved to position `i`, using an intermediate
    * swap map to handle transitive index chains.
    *
    * @param indices
    *   a permutation of `[0, size)` specifying the desired order
    * @param buf
    *   the buffer to reorder in-place
    */
  def keySort[A](indices: IndexedSeq[Int], buf: ArrayBuffer[A]): Unit = {
    swapMap.clear()
    var i = 0
    while (i < indices.length) {
      var k = indices(i)
      while (swapMap.contains(k))
        k = swapMap(k)
      swapMap(i) = k
      i += 1
    }

    swapMap.foreach { (key, value) =>
      val tmp = buf(key)
      buf(key) = buf(value)
      buf(value) = tmp
    }
  }

  /** Sort collisions by ti, then by square distance. */
  def sort(): Unit = {
    val order = Array.tabulate(_size)(identity)

    val boxed = order.map(Integer.valueOf)
    java.util.Arrays.sort(
      boxed,
      (a: Integer, b: Integer) => {
        val ai = a.intValue
        val bi = b.intValue
        if (tis(ai) == tis(bi)) {
          val ad = Rect.rect_getSquareDistance(x1s(ai), y1s(ai), w1s(ai), h1s(ai), x2s(ai), y2s(ai), w2s(ai), h2s(ai))
          val bd = Rect.rect_getSquareDistance(x1s(ai), y1s(ai), w1s(ai), h1s(ai), x2s(bi), y2s(bi), w2s(bi), h2s(bi))
          java.lang.Float.compare(ad, bd)
        } else {
          java.lang.Float.compare(tis(ai), tis(bi))
        }
      }
    )
    var i = 0
    while (i < boxed.length) {
      order(i) = boxed(i).intValue
      i += 1
    }

    // Apply the permutation to all arrays
    applyPermutation(order)
  }

  private def applyPermutation(order: Array[Int]): Unit = {
    // Create copies and reorder
    def reorder[A](buf: ArrayBuffer[A]): Unit = {
      val copy = buf.toArray[Any]
      var i    = 0
      while (i < _size) {
        buf(i) = copy(order(i)).asInstanceOf[A]
        i += 1
      }
    }

    reorder(overlaps)
    reorder(tis)
    reorder(moveXs)
    reorder(moveYs)
    reorder(normalXs)
    reorder(normalYs)
    reorder(touchXs)
    reorder(touchYs)
    reorder(x1s)
    reorder(y1s)
    reorder(w1s)
    reorder(h1s)
    reorder(x2s)
    reorder(y2s)
    reorder(w2s)
    reorder(h2s)
    reorder(items)
    reorder(others)
    reorder(types)
  }
}
