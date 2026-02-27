/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Select.java
 * Original authors: Jon Renner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

/** This class is for selecting a ranked element (kth ordered statistic) from an unordered list in faster time than sorting the whole array. Typical applications include finding the nearest enemy
  * unit(s), and other operations which are likely to run as often as every x frames. Certain values of k will result in a partial sorting of the Array. <p> The lowest ranking element starts at 1, not 0.
  * 1 = first, 2 = second, 3 = third, etc. calling with a value of zero will result in a {@link SgeError} </p> <p> This class uses very minimal extra memory, as it makes no copies of the array. The
  * underlying algorithms used are a naive single-pass for k=min and k=max, and Hoare's quickselect for values in between. </p>
  * @author
  *   Jon Renner (original implementation)
  */
class Select {
  private var quickSelect: QuickSelect[AnyRef] = scala.compiletime.uninitialized

  def select[T](items: Array[T], comp: Ordering[T], kthLowest: Int, size: Int): T = {
    val idx = selectIndex(items, comp, kthLowest, size)
    items(idx)
  }

  def selectIndex[T](items: Array[T], comp: Ordering[T], kthLowest: Int, size: Int): Int = {
    if (size < 1) {
      throw SgeError.InvalidInput("cannot select from empty array (size < 1)")
    } else if (kthLowest > size) {
      throw SgeError.InvalidInput(s"Kth rank is larger than size. k: $kthLowest, size: $size")
    }
    val idx: Int =
      // naive partial selection sort almost certain to outperform quickselect where n is min or max
      if (kthLowest == 1) {
        // find min
        fastMin(items, comp, size)
      } else if (kthLowest == size) {
        // find max
        fastMax(items, comp, size)
      } else {
        // quickselect a better choice for cases of k between min and max
        if (quickSelect == null) quickSelect = new QuickSelect[AnyRef]()
        quickSelect.select(items.asInstanceOf[Array[AnyRef]], comp.asInstanceOf[Ordering[AnyRef]], kthLowest, size)
      }
    idx
  }

  /** Faster than quickselect for n = min */
  private def fastMin[T](items: Array[T], comp: Ordering[T], size: Int): Int = {
    var lowestIdx = 0
    for (i <- 1 until size) {
      val comparison = comp.compare(items(i), items(lowestIdx))
      if (comparison < 0) {
        lowestIdx = i
      }
    }
    lowestIdx
  }

  /** Faster than quickselect for n = max */
  private def fastMax[T](items: Array[T], comp: Ordering[T], size: Int): Int = {
    var highestIdx = 0
    for (i <- 1 until size) {
      val comparison = comp.compare(items(i), items(highestIdx))
      if (comparison > 0) {
        highestIdx = i
      }
    }
    highestIdx
  }
}

object Select {

  /** Provided for convenience */
  def instance(): Select =
    _instance match {
      case null =>
        _instance = new Select()
        _instance
      case existing => existing
    }

  private var _instance: Select = scala.compiletime.uninitialized
}
