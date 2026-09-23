/*
 * Injected replacement for com.badlogic.gdx.utils.Pool.
 *
 * Source: sge/sge/src/main/scala/sge/utils/Pool.scala (the hand port's trait form)
 * Original upstream: com/badlogic/gdx/utils/Pool.java
 * Original license: Apache-2.0 (see libGDX upstream)
 *
 * sge hand-ported Pool as a TRAIT with abstract vals `initialCapacity` and `max`
 * (Pool.scala:12 "Issues: Pool changed from abstract class to trait"; divergence-
 * investigator verdict: justified, kind=api; AD-003).
 *
 * Differences from sge's Pool.scala:
 * - `DynamicArray.createRef` replaced with inline `MkArray.anyRef` cast (the extension
 *   method is sge-original code not available in the mechanical port)
 * - `Pool.Default`, `Pool.Flushable`, `Pool.QuadTreeFloat` omitted (SGE-originals;
 *   the mechanical port emits DefaultPool and FlushablePool from their own Java files)
 * - Thread-safety lock RETAINED (sge ISS-603, ISS-797)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge.utils

import lowlevel.MkArray
import lowlevel.util.DynamicArray

/** A pool of objects that can be reused to avoid allocation.
  * @see PoolManager
  * @author Nathan Sweet */
trait Pool[A] {

  /** The maximum number of objects that will be pooled. */
  protected[utils] val max: Int

  protected[utils] val initialCapacity: Int

  var peak: Int = 0

  private val freeObjects: DynamicArray[A] = {
    given MkArray[A] = MkArray.anyRef[AnyRef].asInstanceOf[MkArray[A]]
    DynamicArray[A](true, initialCapacity)
  }

  protected val lock: AnyRef = new AnyRef

  protected def newObject(): A

  def obtain(): A = {
    lock.synchronized {
      if (freeObjects.isEmpty) newObject() else freeObjects.pop()
    }
  }

  def free(obj: A): Unit = {
    lock.synchronized {
      if (freeObjects.size < max) {
        freeObjects.add(obj)
        peak = peak max freeObjects.size
        reset(obj)
      } else
        discard(obj)
    }
  }

  def fill(size: Int): Unit = {
    lock.synchronized {
      for (_ <- 0 until size)
        if (freeObjects.size < max) freeObjects.add(newObject())
      peak = peak max freeObjects.size
    }
  }

  protected def reset(obj: A): Unit = obj match {
    case obj: Pool.Poolable => obj.reset()
    case _ => ()
  }

  protected def discard(obj: A): Unit = {
    reset(obj)
  }

  def freeAll(objects: DynamicArray[? <: A]): Unit = {
    lock.synchronized {
      objects.foreach { obj =>
        if (obj.asInstanceOf[AnyRef] ne null) {
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
  }

  def clear(): Unit = {
    lock.synchronized {
      freeObjects.foreach(discard)
      freeObjects.clear()
    }
  }

  /** The number of objects available to be obtained. */
  def getFree: Int = {
    lock.synchronized {
      freeObjects.size
    }
  }
}

object Pool {
  /** Objects implementing this interface will have [[Pool#reset]] called when passed to [[Pool#free]]. */
  trait Poolable {
    /** Resets the object for reuse. */
    def reset(): Unit
  }
}
