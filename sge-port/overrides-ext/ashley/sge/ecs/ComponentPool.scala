package sge.ecs

/*
 * Injected by sge's port policy; no upstream source.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: ComponentPool,clear,free,getFree,obtain,pooled
 * Covenant-source-reference: injected (no upstream)
 * Covenant-verified: 2026-09-23
 */
/** A component pool that builds instances from a [[ComponentFactory]] instead of reflectively. */
final class ComponentPool[T](componentType: Class[T], initialSize: Int, maxSize: Int)(using factory: ComponentFactory[T]) {

  private val free = new java.util.ArrayDeque[T](math.max(initialSize, 1))

  /** A pooled instance if one is free, else a new one. */
  def obtain(): T = {
    val pooled = free.pollLast()
    if (pooled != null) pooled
    else factory.create()
  }

  def free(obj: T): Unit =
    if (obj != null) {
      obj match {
        case p: sge.utils.Pool.Poolable => p.reset()
        case _ => ()
      }
      if (free.size < maxSize) free.addLast(obj)

    }
  def clear(): Unit = free.clear()

  /** how many instances are currently pooled -- upstream's `Pool.getFree`. */
  def getFree: Int = free.size

}
