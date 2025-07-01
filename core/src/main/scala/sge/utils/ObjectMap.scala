package sge
package utils

import scala.collection.mutable.{ Map => MutableMap }

class ObjectMap[K, V] {
  private val underlying = scala.collection.mutable.Map[K, V]()

  // Custom get method that returns null instead of Option
  def get(key: K): V = underlying.get(key).orNull.asInstanceOf[V]

  // Custom put method that returns the previous value or null
  def put(key: K, value: V): V = underlying.put(key, value).orNull.asInstanceOf[V]

  // Method to get keys as Iterable
  def keys(): Iterable[K] = underlying.keys

  // Delegate other methods to underlying map
  def contains(key: K): Boolean = underlying.contains(key)
  def size:             Int     = underlying.size
  def isEmpty:          Boolean = underlying.isEmpty
  def clear():          Unit    = underlying.clear()
  def remove(key:   K): V       = underlying.remove(key).orNull.asInstanceOf[V]
}
