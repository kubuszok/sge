/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ArraySelection.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package utils

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

/** A selection that supports range selection by knowing about the array of items being selected.
  * @author
  *   Nathan Sweet
  */
class ArraySelection[T](private var array: ArrayBuffer[T]) extends Selection[T] {
  private var rangeSelect: Boolean     = true
  private var rangeStart:  Nullable[T] = Nullable.empty

  override def choose(item: T)(using sge: Sge): Unit =
    if (_isDisabled) ()
    else if (!rangeSelect || !multiple) {
      super.choose(item)
    } else if (selected.nonEmpty && UIUtils.shift()) {
      val rangeStartIndex = rangeStart.fold(-1)(rs => array.indexOf(rs))
      if (rangeStartIndex != -1) {
        val oldRangeStart = rangeStart
        snapshot()
        // Select new range.
        var start = rangeStartIndex
        var end   = array.indexOf(item)
        if (start > end) {
          val temp = end
          end = start
          start = temp
        }
        if (!UIUtils.ctrl()) selected.clear()
        var i = start
        while (i <= end) {
          selected.add(array(i))
          i += 1
        }
        if (fireChangeEvent())
          revert()
        else
          changed()
        rangeStart = oldRangeStart
        cleanup()
      } else {
        super.choose(item)
        rangeStart = Nullable(item)
      }
    } else {
      super.choose(item)
      rangeStart = Nullable(item)
    }

  /** Called after the selection changes, clears the range start item. */
  override protected def changed(): Unit =
    rangeStart = Nullable.empty

  def getRangeSelect: Boolean = rangeSelect

  def setRangeSelect(rangeSelect: Boolean): Unit = this.rangeSelect = rangeSelect

  /** Removes objects from the selection that are no longer in the items array. If {@link #getRequired()} is true and there is no selected item, the first item is selected.
    */
  def validate(): Unit =
    if (array.isEmpty) {
      clear()
    } else {
      var changed  = false
      val iter     = items.iterator
      val toRemove = ArrayBuffer.empty[T]
      while (iter.hasNext) {
        val s = iter.next()
        if (!array.contains(s)) {
          toRemove += s
          changed = true
        }
      }
      toRemove.foreach(selected.remove)
      if (required && selected.isEmpty)
        set(array.head)
      else if (changed)
        this.changed()
    }
}
