/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Pool.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Merged with: `DefaultPool.java` -> `Pool.Default`; `FlushablePool.java` -> `Pool.Flushable`; `QuadTreeFloat.java` -> `Pool.QuadTreeFloat`
 *   Renames: `Pool` abstract class -> `Pool` trait; `freeAll(Array)` -> `freeAll(Iterable)` + `freeAll(DynamicArray)`
 *   Convention: `Pool` is a trait (not abstract class); uses `MkArray.anyRef` for internal `freeObjects`; `return` -> `boundary`/`break`
 *   Idiom: split packages
 *   Issues: `Pool` changed from `abstract class` to `trait` — intentional design improvement but changes instantiation semantics
 *   TODO: Pool.Poolable trait → Poolable[A] type class; Pool[A] should take given Poolable[A]
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

/** A pool of objects that can be reused to avoid allocation.
  *
  * @author
  *   Nathan Sweet (original implementation)
  */
trait Pool[A] {

  /** The maximum number of objects that will be pooled. */
  protected val max: Int

  protected val initialCapacity: Int

  var peak: Int = 0

  private val freeObjects = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[A]], initialCapacity, true)

  protected def newObject(): A

  /** Returns an object from this pool. The object may be new (from [[newObject]]) or reused (previously [[free]]). */
  def obtain(): A =
    if (freeObjects.isEmpty) newObject() else freeObjects.removeIndex(0)

  /** Puts the specified object in the pool, making it eligible to be returned by {@link #obtain()} . If the pool already contains {@link #max} free objects, the specified object is
    * {@link #discard(Object) discarded} , it is not reset and not added to the pool. <p> The pool does not check if an object is already freed, so the same object must not be freed multiple times.
    */
  def free(obj: A): Unit =
    if (freeObjects.size < max) {
      freeObjects.add(obj)
      peak = peak max freeObjects.size
      reset(obj)
    } else
      discard(obj)

  /** Adds the specified number of new free objects to the pool. Usually called early on as a pre-allocation mechanism but can be used at any time.
    *
    * @param size
    *   the number of objects to be added
    */
  def fill(size: Int): Unit =
    for (_ <- 0 until size)
      if (freeObjects.size < max) freeObjects.add(newObject())
  peak = peak max freeObjects.size

  /** Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls {@link Poolable#reset()} if the object is {@link Poolable} .
    */
  protected def reset(obj: A): Unit = obj match {
    case obj: Pool.Poolable => obj.reset()
    case _ => ()
  }

  /** Called when an object is discarded. This is the case when an object is freed, but the maximum capacity of the pool is reached, and when the pool is {@link #clear() cleared}
    */
  protected def discard(obj: A): Unit =
    reset(obj)

  def freeAll(objects: Iterable[A]): Unit = {
    objects.foreach { obj =>
      if (freeObjects.size < max) {
        freeObjects.add(obj)
        reset(obj)
      } else {
        discard(obj)
      }
    }
    peak = peak max freeObjects.size
  }

  def freeAll(objects: DynamicArray[? <: A]): Unit = {
    objects.foreach { obj =>
      if (freeObjects.size < max) {
        freeObjects.add(obj.asInstanceOf[A])
        reset(obj.asInstanceOf[A])
      } else {
        discard(obj.asInstanceOf[A])
      }
    }
    peak = peak max freeObjects.size
  }

  /** Removes and discards all free objects from this pool. */
  def clear(): Unit = {
    freeObjects.foreach(discard)
    freeObjects.clear()
  }

  /** The number of objects available to be obtained. */
  def getFree: Int =
    freeObjects.size

}
object Pool {

  /** Objects implementing this interface will have [[Pool#reset()]] called when passed to [[Pool#free]]. */
  trait Poolable {

    /** Resets the object for reuse. Object references should be nulled and fields may be set to default values. */
    def reset(): Unit
  }

  class Default[A](createNewObject: () => A, protected val initialCapacity: Int = 16, protected val max: Int = Int.MaxValue) extends Pool[A] {
    override def newObject(): A = createNewObject()
  }

  trait Flushable[A] extends Pool[A] {
    protected val obtained = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[A]], 16, true)

    override def obtain(): A = {
      val result = super.obtain()
      obtained.add(result)
      result
    }

    /** Frees all obtained instances. */
    def flush(): Unit = {
      super.freeAll(obtained.iterator.toSeq)
      obtained.clear()
    }

    override def free(obj: A): Unit = {
      obtained.removeValue(obj)
      super.free(obj)
    }

    override def freeAll(objects: Iterable[A]): Unit = {
      objects.foreach(obtained.removeValue)
      super.freeAll(objects)
    }
  }

  /** A quad tree that stores a float for each point.
    * @author
    *   Nathan Sweet (original implementation)
    */
  class QuadTreeFloat(val maxValues: Int = 16, val maxDepth: Int = 8) extends Poolable {
    import QuadTreeFloat._
    import sge.utils.Nullable

    private val maxValuesCount = maxValues * 3
    var x:      Float                   = scala.compiletime.uninitialized
    var y:      Float                   = scala.compiletime.uninitialized
    var width:  Float                   = scala.compiletime.uninitialized
    var height: Float                   = scala.compiletime.uninitialized
    var depth:  Int                     = scala.compiletime.uninitialized
    var nw:     Nullable[QuadTreeFloat] = Nullable.empty
    var ne:     Nullable[QuadTreeFloat] = Nullable.empty
    var sw:     Nullable[QuadTreeFloat] = Nullable.empty
    var se:     Nullable[QuadTreeFloat] = Nullable.empty

    /** For each entry, stores the value, x, and y. */
    var values: Array[Float] = new Array[Float](maxValuesCount)

    /** The number of elements stored in values (3 values per quad tree entry). */
    var count: Int = 0

    def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
      this.x = x
      this.y = y
      this.width = width
      this.height = height
    }

    def add(value: Float, valueX: Float, valueY: Float): Unit = boundary {
      val count = this.count
      if (count == -1) {
        addToChild(value, valueX, valueY)
        break()
      }
      if (depth < maxDepth) {
        if (count == maxValuesCount) {
          split(value, valueX, valueY)
          break()
        }
      } else if (count == values.length) {
        values = java.util.Arrays.copyOf(values, growValues())
      }
      values(count) = value
      values(count + 1) = valueX
      values(count + 2) = valueY
      this.count += 3
    }

    private def split(value: Float, valueX: Float, valueY: Float): Unit = {
      val values = this.values
      var i      = 0
      while (i < maxValuesCount) {
        addToChild(values(i), values(i + 1), values(i + 2))
        i += 3
      }
      // values isn't nulled because the trees are pooled.
      count = -1
      addToChild(value, valueX, valueY)
    }

    private def addToChild(value: Float, valueX: Float, valueY: Float): Unit = {
      val halfWidth  = width / 2
      val halfHeight = height / 2
      val child      = if (valueX < x + halfWidth) {
        if (valueY < y + halfHeight) {
          sw.getOrElse {
            val c = obtainChild(x, y, halfWidth, halfHeight, depth + 1)
            sw = Nullable(c)
            c
          }
        } else {
          nw.getOrElse {
            val c = obtainChild(x, y + halfHeight, halfWidth, halfHeight, depth + 1)
            nw = Nullable(c)
            c
          }
        }
      } else {
        if (valueY < y + halfHeight) {
          se.getOrElse {
            val c = obtainChild(x + halfWidth, y, halfWidth, halfHeight, depth + 1)
            se = Nullable(c)
            c
          }
        } else {
          ne.getOrElse {
            val c = obtainChild(x + halfWidth, y + halfHeight, halfWidth, halfHeight, depth + 1)
            ne = Nullable(c)
            c
          }
        }
      }
      child.add(value, valueX, valueY)
    }

    private def obtainChild(x: Float, y: Float, width: Float, height: Float, depth: Int): QuadTreeFloat = {
      val child = pool.obtain()
      child.x = x
      child.y = y
      child.width = width
      child.height = height
      child.depth = depth
      child
    }

    /** Returns a new length for values when it is not enough to hold all the entries after maxDepth has been reached.
      */
    protected def growValues(): Int = count + 10 * 3

    /** @param results
      *   For each entry found within the radius, if any, the value, x, y, and square of the distance to the entry are added to this array. See VALUE, X, Y, and DISTSQR.
      */
    def query(centerX: Float, centerY: Float, radius: Float, results: DynamicArray[Float]): Unit =
      query(centerX, centerY, radius * radius, centerX - radius, centerY - radius, radius * 2, results)

    private def query(centerX: Float, centerY: Float, radiusSqr: Float, rectX: Float, rectY: Float, rectSize: Float, results: DynamicArray[Float]): Unit = boundary {
      if (!(x < rectX + rectSize && x + width > rectX && y < rectY + rectSize && y + height > rectY)) break()
      val count = this.count
      if (count != -1) {
        val values = this.values
        var i      = 1
        while (i < count) {
          val px = values(i)
          val py = values(i + 1)
          val dx = px - centerX
          val dy = py - centerY
          val d  = dx * dx + dy * dy
          if (d <= radiusSqr) {
            results.add(values(i - 1))
            results.add(px)
            results.add(py)
            results.add(d)
          }
          i += 3
        }
      } else {
        nw.foreach(_.query(centerX, centerY, radiusSqr, rectX, rectY, rectSize, results))
        sw.foreach(_.query(centerX, centerY, radiusSqr, rectX, rectY, rectSize, results))
        ne.foreach(_.query(centerX, centerY, radiusSqr, rectX, rectY, rectSize, results))
        se.foreach(_.query(centerX, centerY, radiusSqr, rectX, rectY, rectSize, results))
      }
    }

    /** @param results
      *   For each entry found within the rectangle, if any, the value, x, and y of the entry are added to this array. See VALUE, X, and Y.
      */
    def query(rect: sge.math.Rectangle, results: DynamicArray[Float]): Unit = boundary {
      if (x >= rect.x + rect.width || x + width <= rect.x || y >= rect.y + rect.height || y + height <= rect.y) break()
      val count = this.count
      if (count != -1) {
        val values = this.values
        var i      = 1
        while (i < count) {
          val px = values(i)
          val py = values(i + 1)
          if (rect.contains(px, py)) {
            results.add(values(i - 1))
            results.add(px)
            results.add(py)
          }
          i += 3
        }
      } else {
        nw.foreach(_.query(rect, results))
        sw.foreach(_.query(rect, results))
        ne.foreach(_.query(rect, results))
        se.foreach(_.query(rect, results))
      }
    }

    /** @param result
      *   For the entry nearest to the specified point, the value, x, y, and square of the distance to the value are added to this array after it is cleared. See VALUE, X, Y, and DISTSQR.
      * @return
      *   false if no entry was found because the quad tree was empty or the specified point is farther than the larger of the quad tree's width or height from an entry. If false is returned the
      *   result array is empty.
      */
    def nearest(x: Float, y: Float, result: DynamicArray[Float]): Boolean = boundary {
      // Find nearest value in a cell that contains the point.
      result.clear()
      result.add(0)
      result.add(0)
      result.add(0)
      result.add(Float.PositiveInfinity)
      findNearestInternal(x, y, result)
      val nearValue = result(0)
      val nearX     = result(1)
      val nearY     = result(2)
      var nearDist  = result(3)
      val found     = nearDist != Float.PositiveInfinity
      if (!found) {
        nearDist = Math.max(width, height)
        nearDist *= nearDist
      }

      // Check for a nearer value in a neighboring cell.
      result.clear()
      query(x, y, Math.sqrt(nearDist).toFloat, result)
      var i              = 3
      val n              = result.size
      var finalNearValue = nearValue
      var finalNearX     = nearX
      var finalNearY     = nearY
      var finalNearDist  = nearDist
      while (i < n) {
        val dist = result(i)
        if (dist < finalNearDist) {
          finalNearDist = dist
          finalNearValue = result(i - 3)
          finalNearX = result(i - 2)
          finalNearY = result(i - 1)
        }
        i += 4
      }
      if (!found && result.isEmpty) break(false)
      result.clear()
      result.add(finalNearValue)
      result.add(finalNearX)
      result.add(finalNearY)
      result.add(finalNearDist)
      true
    }

    private def findNearestInternal(x: Float, y: Float, result: DynamicArray[Float]): Unit = boundary {
      if (!(this.x < x && this.x + width > x && this.y < y && this.y + height > y)) break()

      val count = this.count
      if (count != -1) {
        var nearValue = result(0)
        var nearX     = result(1)
        var nearY     = result(2)
        var nearDist  = result(3)
        val values    = this.values
        var i         = 1
        while (i < count) {
          val px   = values(i)
          val py   = values(i + 1)
          val dx   = px - x
          val dy   = py - y
          val dist = dx * dx + dy * dy
          if (dist < nearDist) {
            nearDist = dist
            nearValue = values(i - 1)
            nearX = px
            nearY = py
          }
          i += 3
        }
        result(0) = nearValue
        result(1) = nearX
        result(2) = nearY
        result(3) = nearDist
      } else {
        nw.foreach(_.findNearestInternal(x, y, result))
        sw.foreach(_.findNearestInternal(x, y, result))
        ne.foreach(_.findNearestInternal(x, y, result))
        se.foreach(_.findNearestInternal(x, y, result))
      }
    }

    def reset(): Unit = {
      if (count == -1) {
        nw.foreach { child =>
          pool.free(child)
        }
        nw = Nullable.empty
        sw.foreach { child =>
          pool.free(child)
        }
        sw = Nullable.empty
        ne.foreach { child =>
          pool.free(child)
        }
        ne = Nullable.empty
        se.foreach { child =>
          pool.free(child)
        }
        se = Nullable.empty
      }
      count = 0
      if (values.length > maxValuesCount) values = new Array[Float](maxValuesCount)
    }
  }

  object QuadTreeFloat {
    val VALUE   = 0
    val X       = 1
    val Y       = 2
    val DISTSQR = 3

    private val pool = Pool.Default[QuadTreeFloat](() => QuadTreeFloat(), 128, 4096)
  }
}
