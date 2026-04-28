/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ArraySelection.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Array<T> -> DynamicArray[T]
 * - choose requires (using Sge) for UIUtils.shift/ctrl calls
 * - null rangeStart -> Nullable[T]
 * - validate: iter.remove() -> collect-then-remove pattern (no ConcurrentModification)
 * - validate uses DynamicArray.createWithMk for generic T collection
 * - All methods faithfully ported
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 101
 * Covenant-baseline-methods: ArraySelection,_rangeSelect,changed,choose,rangeSelect,rangeStart,setRangeSelect,validate
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/ArraySelection.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: dcde42972f6defe3d5bd92c702eb23344a6ab189
 */
package sge
package scenes
package scene2d
package utils

import sge.Sge
import sge.utils.{ DynamicArray, MkArray, Nullable }

/** A selection that supports range selection by knowing about the array of items being selected.
  * @author
  *   Nathan Sweet
  */
class ArraySelection[T](private val array: DynamicArray[T])(using Sge) extends Selection[T]() {
  private var _rangeSelect: Boolean     = true
  private var rangeStart:   Nullable[T] = Nullable.empty

  override def choose(item: T): Unit =
    if (_isDisabled) ()
    else if (!rangeSelect || !multiple) {
      super.choose(item)
    } else if (selected.nonEmpty && UIUtils.shift()) {
      val rangeStartIndex = rangeStart.map(rs => array.indexOf(rs)).getOrElse(-1)
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

  def rangeSelect: Boolean = _rangeSelect

  def setRangeSelect(rangeSelect: Boolean): Unit = this._rangeSelect = rangeSelect

  /** Removes objects from the selection that are no longer in the items array. If {@link #getRequired()} is true and there is no selected item, the first item is selected.
    */
  def validate(): Unit =
    if (array.isEmpty) {
      clear()
    } else {
      var changed  = false
      val iter     = items.iterator
      val toRemove = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
      while (iter.hasNext) {
        val s = iter.next()
        if (!array.contains(s)) {
          toRemove.add(s)
          changed = true
        }
      }
      toRemove.foreach(selected.remove)
      if (required && selected.isEmpty)
        set(array.first)
      else if (changed)
        this.changed()
    }
}
