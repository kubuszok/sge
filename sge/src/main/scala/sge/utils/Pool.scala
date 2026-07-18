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
 *   Convention: Pool.Default takes `(using Poolable[A])` type class for reset; Pool.Poolable trait kept for backward compat
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 456
 * Covenant-baseline-methods: DISTSQR,Default,Flushable,Pool,Poolable,QuadTreeFloat,VALUE,X,Y,add,addToChild,clear,count,depth,discard,fill,findNearestInternal,flush,free,freeAll,freeObjects,growValues,height,initialCapacity,max,maxValuesCount,ne,nearest,newObject,nw,obtain,obtainChild,obtained,peak,pool,query,reset,se,setBounds,split,sw,values,width,x,y
 * Covenant-source-reference: com/badlogic/gdx/utils/Pool.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: a729bf1f0de099ebcc60562d72f008157677b559
 */
package sge
package utils

import sge.utils.createRef

import lowlevel.MkArray
import lowlevel.util.DynamicArray

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

  private val freeObjects = DynamicArray.createRef[A](initialCapacity)

  /** Monitor guarding every mutation and read of [[freeObjects]] and [[peak]].
    *
    * DOCUMENTED DEVIATION FROM LibGDX (ISS-603, ISS-797): upstream `Pool` and its `Pools` registry (ported as [[sge.utils.PoolManager]], instantiated JVM-globally as `Actor.POOLS` — Actor.scala:952 —
    * and `Actions.ACTION_POOLS` — Actions.scala:46) are uniformly unsynchronized and rely on a documented "game thread only" contract (see `GlyphLayout.java:42` — "This class is not thread safe ...
    * must only be used from the game thread"). SGE breaks that contract in SGE-ORIGINAL code (`SgeHttpClient` obtains a request on the caller thread — SgeHttpClient.scala:67 — and frees it on
    * `ExecutionContext.global` — SgeHttpClient.scala:167) and in its parallel test environment (munit runs suites concurrently in one forked JVM, so every `GlyphLayout.setText` shares the JVM-global
    * static `glyphRunPool`). Upstream's own answer to a genuinely cross-thread structure is monitor-guarding (`NetJavaImpl.java` synchronizes its cross-thread maps, lines 278/284/290). We therefore
    * make `Pool` internally thread-safe.
    *
    * The lock is a protected dedicated object (not `this`) so external code cannot accidentally interfere with the pool's invariants by synchronizing on the pool. JVM monitors are REENTRANT, which is
    * load-bearing here: `reset()` runs while the lock is held, and a user `reset()` may re-enter the SAME pool — e.g. `QuadTreeFloat.reset` frees its child nodes back into the very pool whose
    * `free`/`clear` is resetting the parent. Re-entry on the same monitor from the same thread is safe. Callbacks DO also nest into DIFFERENT pools at three sites: (a) `BitmapFontCache`'s
    * `pooledLayouts` (BitmapFontCache.scala:41, a `Flushable[GlyphLayout]`) free/flush runs `GlyphLayout.reset` (GlyphLayout.scala:504), which calls `glyphRunPool.freeAll`; (b) `Actor.POOLS`
    * (Actor.scala:952/956) frees `GlyphLayout` instances down the same path; (c) `PooledEngine.EntityPool` free/discard (PooledEngine.scala:81/114-115) runs `PooledEntity.reset` (:96) →
    * `removeInternal` (:91) → `componentPools.free`. Today every such lock edge points from an outer/composite pool to an inner LEAF pool whose element `reset()` touches no further pool, so no
    * lock-ordering (AB-BA) cycle exists. THE INVARIANT THAT KEEPS IT THAT WAY, which every new pool must preserve: cross-pool lock edges must always point outer → leaf, and a pooled element's
    * `reset()` must never free into an outer/composite pool — e.g. a user `Component.reset` freeing an entity would close an `EntityPool` ↔ `ComponentPool` AB-BA cycle.
    *
    * Ordering rationale (why `reset()`/`discard()` stay UNDER the lock rather than before it): moving `reset()` before the lock would either drop the `discard()` extension point on the full-pool path
    * (LibGDX and the one SGE override, `PooledEngine.EntityPool.discard`, both call `reset()` from `discard()`), or double-invoke `reset()` under a fill race — both break the "reset once per free,
    * discard() called on the discard path" invariant. Keeping the original single-threaded order verbatim and wrapping it in the reentrant monitor preserves EXACT semantics (same max-size discard
    * behaviour, same `reset()`/`discard()` invocation points and order, same peak accounting, same return values); the reentrant monitor already makes the QuadTreeFloat re-entry safe, so nothing is
    * gained by hoisting it out.
    *
    * `newObject()` also runs under the lock: it touches no shared state and no codebase pool re-enters a foreign pool from its factory, so this is safe; the serialization of allocation is negligible
    * (see perf note below).
    *
    * Performance: an uncontended monitor is a handful of nanoseconds — negligible against the allocation the pool exists to avoid.
    *
    * `protected` so subclasses that carry EXTRA shared state — currently only [[Pool.Flushable]] with its `obtained` list — guard it on the SAME monitor (reentrant, so wrapping a method that also
    * calls `super.obtain`/`super.free` is safe and adds no measurable cost).
    */
  protected val lock = new AnyRef

  protected def newObject(): A

  /** Returns an object from this pool. The object may be new (from [[newObject]]) or reused (previously [[free]]). */
  def obtain(): A =
    lock.synchronized {
      if (freeObjects.isEmpty) newObject() else freeObjects.pop()
    }

  /** Puts the specified object in the pool, making it eligible to be returned by {@link #obtain()} . If the pool already contains {@link #max} free objects, the specified object is
    * {@link #discard(Object) discarded} , it is not reset and not added to the pool. <p> The pool does not check if an object is already freed, so the same object must not be freed multiple times.
    */
  def free(obj: A): Unit =
    lock.synchronized {
      if (freeObjects.size < max) {
        freeObjects.add(obj)
        peak = peak max freeObjects.size
        reset(obj)
      } else
        discard(obj)
    }

  /** Adds the specified number of new free objects to the pool. Usually called early on as a pre-allocation mechanism but can be used at any time.
    *
    * @param size
    *   the number of objects to be added
    */
  def fill(size: Int): Unit =
    lock.synchronized {
      for (_ <- 0 until size)
        if (freeObjects.size < max) freeObjects.add(newObject())
      peak = peak max freeObjects.size
    }

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

  def freeAll(objects: Iterable[A]): Unit =
    lock.synchronized {
      objects.foreach { obj =>
        if (obj.asInstanceOf[AnyRef] ne null) { // @nowarn — null guard: original skips null items in the iterable
          if (freeObjects.size < max) {
            freeObjects.add(obj)
            reset(obj)
          } else {
            discard(obj)
          }
        }
      }
      peak = peak max freeObjects.size
    }

  def freeAll(objects: DynamicArray[? <: A]): Unit =
    lock.synchronized {
      objects.foreach { obj =>
        if (obj.asInstanceOf[AnyRef] ne null) { // @nowarn — null guard: original skips null items in the array
          val o = obj.asInstanceOf[A]
          if (freeObjects.size < max) {
            freeObjects.add(o)
            reset(o)
          } else {
            discard(o)
          }
        }
      }
      peak = peak max freeObjects.size
    }

  /** Removes and discards all free objects from this pool. */
  def clear(): Unit =
    lock.synchronized {
      freeObjects.foreach(discard)
      freeObjects.clear()
    }

  /** The number of objects available to be obtained. */
  def free: Int =
    lock.synchronized {
      freeObjects.size
    }

}
object Pool {

  /** Objects implementing this interface will have [[Pool#reset()]] called when passed to [[Pool#free]]. */
  trait Poolable {

    /** Resets the object for reuse. Object references should be nulled and fields may be set to default values. */
    def reset(): Unit
  }

  /** A ready-made [[Pool]] that builds instances with the supplied factory and resets them through the given [[sge.utils.Poolable]] type class instance instead of the [[Pool.Poolable]] trait.
    */
  class Default[A](createNewObject: () => A, protected val initialCapacity: Int = 16, protected val max: Int = Int.MaxValue)(using poolable: sge.utils.Poolable[A]) extends Pool[A] {
    override def newObject():             A    = createNewObject()
    override protected def reset(obj: A): Unit = poolable.reset(obj)
  }

  /** A [[Pool]] that additionally tracks every [[obtain]]ed instance so the whole batch can be returned at once with [[flush]], rather than freeing each one individually.
    */
  trait Flushable[A] extends Pool[A] {
    protected val obtained = DynamicArray.createRef[A]()

    // `obtained` is additional shared state on top of the base `freeObjects`;
    // guard it on the same reentrant monitor so a Flushable pool is as
    // thread-safe as the base pool (super.obtain/super.free/super.freeAll
    // re-acquire the same lock reentrantly). See Pool.lock.
    override def obtain(): A =
      lock.synchronized {
        val result = super.obtain()
        obtained.add(result)
        result
      }

    /** Frees all obtained instances. */
    def flush(): Unit =
      lock.synchronized {
        super.freeAll(obtained.iterator.toSeq)
        obtained.clear()
      }

    // All removals from `obtained` use REFERENCE identity (lls *ByRef variants),
    // faithful to upstream FlushablePool.java which removes with identity=true
    // (`removeValue(object, true)` at :52, `removeAll(objects, true)` at :58):
    // pooled instances can be structurally equal yet distinct, and value-equality
    // removal can evict the WRONG instance from `obtained` — leaving the actually
    // freed one behind to be double-freed by flush() (same instance in the free
    // list twice), or dropping a still-checked-out one so flush() never frees it.
    override def free(obj: A): Unit =
      lock.synchronized {
        obtained.removeValueByRef(obj)
        super.free(obj)
      }

    override def freeAll(objects: Iterable[A]): Unit =
      lock.synchronized {
        objects.foreach(obtained.removeValueByRef)
        super.freeAll(objects)
      }

    // Mirrors the freeAll(Iterable) override for the DynamicArray overload:
    // upstream FlushablePool.freeAll(Array<T>) does `obtained.removeAll(objects, true)`
    // before delegating (FlushablePool.java:57-60). Without this, freeing through the
    // DynamicArray overload would leave the items in `obtained`, so a later flush()
    // would free them again — a double-free (same instance in the free list twice).
    // Removal mutates `obtained` under the same reentrant lock; super.freeAll
    // re-acquires it reentrantly. See Pool.lock.
    override def freeAll(objects: DynamicArray[? <: A]): Unit =
      lock.synchronized {
        obtained.removeAllByRef(objects)
        super.freeAll(objects)
      }
  }

  /** A quad tree that stores a float for each point.
    * @author
    *   Nathan Sweet (original implementation)
    */
  class QuadTreeFloat(val maxValues: Int = 16, val maxDepth: Int = 8) extends Poolable {
    import QuadTreeFloat._
    import lowlevel.Nullable

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
