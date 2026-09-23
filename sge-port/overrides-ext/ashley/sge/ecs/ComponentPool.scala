package sge.ecs

/** A component pool that builds instances from a FACTORY instead of reflectively. */
final class ComponentPool[T](componentType: Class[T], initialSize: Int, maxSize: Int) {

  private val free = new java.util.ArrayDeque[T](math.max(initialSize, 1))

  /** A pooled instance if one is free, else a new one. Never null for a registered type. */
  def obtain(): T = {
    val pooled = free.pollLast()
    if (pooled != null) pooled
    else
      // the cast is where the unbounded parameter is paid for: every runtime use IS a Component
      // (this pool is only ever built from `PooledEngine.ComponentPools`), but the static bound
      // cannot say so without breaking the call site.
      ComponentFactories.create(componentType.asInstanceOf[Class[? <: Component]]).asInstanceOf[T]

  /** Return an instance to the pool, resetting it first when it is `Poolable` — upstream's
    * contract, and the reason a pooled component does not carry state across uses. */
  }
  def free(obj: T): Unit = {
    if (obj != null) {
      obj match {
        case p: sge.utils.Pool.Poolable => p.reset()
        case _                                       => ()
      }
      if (free.size < maxSize) free.addLast(obj)

    }
  }
  def clear(): Unit = free.clear()

  /** how many instances are currently pooled — upstream's `Pool.getFree`. */
  def getFree: Int = free.size

}