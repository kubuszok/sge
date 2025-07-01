package sge
package utils

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/** Provides methods to sort arrays of objects. Sorting requires working memory and this class allows that memory to be reused to avoid allocation. The sorting is otherwise identical to the
  * Arrays.sort methods (uses timsort).<br> <br> Note that sorting primitive arrays with the Arrays.sort methods does not allocate memory (unless sorting large arrays of char, short, or byte).
  * @author
  *   Nathan Sweet (original implementation)
  */
object Sort {
  private var comparableTimSort = new ComparableTimSort()

  def sort[T <: Comparable[T]: ClassTag](a: ArrayBuffer[T]): Unit =
    comparableTimSort.doSort(a.toArray.asInstanceOf[Array[AnyRef]], 0, a.size)

  /** The specified objects must implement {@link Comparable}. */
  def sort(a: Array[AnyRef]): Unit =
    comparableTimSort.doSort(a, 0, a.length)

  /** The specified objects must implement {@link Comparable}. */
  def sort(a: Array[AnyRef], fromIndex: Int, toIndex: Int): Unit =
    comparableTimSort.doSort(a, fromIndex, toIndex)

  def sort[T: ClassTag](a: ArrayBuffer[T], c: Ordering[T]): Unit = {
    val array = a.toArray
    TimSort.sort(array, c)
    // Copy back to ArrayBuffer
    a.clear()
    a.addAll(array)
  }

  def sort[T](a: Array[T], c: Ordering[T]): Unit =
    TimSort.sort(a, c)

  def sort[T](a: Array[T], c: Ordering[T], fromIndex: Int, toIndex: Int): Unit =
    TimSort.sort(a, fromIndex, toIndex, c)
}
