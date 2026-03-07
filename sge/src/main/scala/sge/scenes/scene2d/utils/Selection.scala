/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/Selection.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - OrderedSet<T> -> scala.collection.mutable.LinkedHashSet[T]
 * - Array<T> -> DynamicArray[T]
 * - @Null Actor/T -> Nullable[Actor]/Nullable[T]
 * - Implements Iterable[T] (Scala) instead of Iterable<T> (Java)
 * - Deprecated hasItems() method omitted (use notEmpty)
 * - choose/other methods: null item checks removed (Scala type safety)
 * - choose requires (using Sge) for UIUtils.ctrl calls
 * - toArray uses DynamicArray.createWithMk for generic T
 * - All public API methods faithfully ported
 * - Renames: getLastSelected → def lastSelected, getToggle/setToggle → var toggle,
 *   getMultiple/setMultiple → var multiple, getRequired/setRequired → var required,
 *   isDisabled/setDisabled → var disabled, getProgrammaticChangeEvents/setProgrammaticChangeEvents → var programmaticChangeEvents
 */
package sge
package scenes
package scene2d
package utils

import scala.collection.mutable.LinkedHashSet
import sge.Sge
import sge.utils.{ DynamicArray, MkArray, Nullable }

/** Manages selected objects. Optionally fires a {@link ChangeEvent} on an actor. Selection changes can be vetoed via {@link ChangeEvent#cancel()}.
  * @author
  *   Nathan Sweet
  */
class Selection[T]()(using Sge) extends Disableable with Iterable[T] {
  private var actor:            Nullable[Actor]  = Nullable.empty
  val selected:                 LinkedHashSet[T] = LinkedHashSet.empty
  private val old:              LinkedHashSet[T] = LinkedHashSet.empty
  protected var _isDisabled:    Boolean          = false
  var toggle:                   Boolean          = false
  var multiple:                 Boolean          = false
  var required:                 Boolean          = false
  var programmaticChangeEvents: Boolean          = true
  protected var _lastSelected:  Nullable[T]      = Nullable.empty

  /** @param actor An actor to fire {@link ChangeEvent} on when the selection changes, or null. */
  def setActor(actor: Nullable[Actor]): Unit = this.actor = actor

  /** Selects or deselects the specified item based on how the selection is configured, whether ctrl is currently pressed, etc. This is typically invoked by user interaction.
    */
  def choose(item: T): Unit =
    if (_isDisabled) ()
    else {
      snapshot()
      try {
        if ((toggle || UIUtils.ctrl()) && selected.contains(item)) {
          if (required && selected.size == 1) ()
          else {
            selected.remove(item)
            _lastSelected = Nullable.empty
          }
        } else {
          var modified = false
          if (!multiple || (!toggle && !UIUtils.ctrl())) {
            if (selected.size == 1 && selected.contains(item)) ()
            else {
              modified = selected.nonEmpty
              selected.clear()
            }
          }
          if (selected.add(item) || modified) {
            _lastSelected = Nullable(item)
          }
        }
        if (fireChangeEvent())
          revert()
        else
          changed()
      } finally
        cleanup()
    }

  def notEmpty: Boolean = selected.nonEmpty

  override def isEmpty: Boolean = selected.isEmpty

  override def size: Int = selected.size

  def items: LinkedHashSet[T] = selected

  /** Returns the first selected item, or null. */
  def first: Nullable[T] =
    if (selected.isEmpty) Nullable.empty
    else Nullable(selected.head)

  protected[utils] def snapshot(): Unit = {
    old.clear()
    old.addAll(selected)
  }

  protected[utils] def revert(): Unit = {
    selected.clear()
    selected.addAll(old)
  }

  protected[utils] def cleanup(): Unit =
    old.clear()

  /** Sets the selection to only the specified item. */
  def set(item: T): Unit =
    if (selected.size == 1 && selected.head == item) ()
    else {
      snapshot()
      selected.clear()
      selected.add(item)
      if (programmaticChangeEvents && fireChangeEvent())
        revert()
      else {
        _lastSelected = Nullable(item)
        changed()
      }
      cleanup()
    }

  def setAll(items: DynamicArray[T]): Unit = {
    var added = false
    snapshot()
    _lastSelected = Nullable.empty
    selected.clear()
    var i = 0
    val n = items.size
    while (i < n) {
      val item = items(i)
      if (selected.add(item)) added = true
      i += 1
    }
    if (added) {
      if (programmaticChangeEvents && fireChangeEvent())
        revert()
      else if (items.size > 0) {
        _lastSelected = Nullable(items(items.size - 1))
        changed()
      }
    }
    cleanup()
  }

  /** Adds the item to the selection. */
  def add(item: T): Unit =
    if (!selected.add(item)) ()
    else if (programmaticChangeEvents && fireChangeEvent())
      selected.remove(item)
    else {
      _lastSelected = Nullable(item)
      changed()
    }

  def addAll(items: DynamicArray[T]): Unit = {
    var added = false
    snapshot()
    var i = 0
    val n = items.size
    while (i < n) {
      val item = items(i)
      if (selected.add(item)) added = true
      i += 1
    }
    if (added) {
      if (programmaticChangeEvents && fireChangeEvent())
        revert()
      else {
        _lastSelected = Nullable(items(items.size - 1))
        changed()
      }
    }
    cleanup()
  }

  def remove(item: T): Unit =
    if (!selected.remove(item)) ()
    else if (programmaticChangeEvents && fireChangeEvent())
      selected.add(item)
    else {
      _lastSelected = Nullable.empty
      changed()
    }

  def removeAll(items: DynamicArray[T]): Unit = {
    var removed = false
    snapshot()
    var i = 0
    val n = items.size
    while (i < n) {
      val item = items(i)
      if (selected.remove(item)) removed = true
      i += 1
    }
    if (removed) {
      if (programmaticChangeEvents && fireChangeEvent())
        revert()
      else {
        _lastSelected = Nullable.empty
        changed()
      }
    }
    cleanup()
  }

  def clear(): Unit =
    if (selected.isEmpty) {
      _lastSelected = Nullable.empty
    } else {
      snapshot()
      selected.clear()
      if (programmaticChangeEvents && fireChangeEvent())
        revert()
      else {
        _lastSelected = Nullable.empty
        changed()
      }
      cleanup()
    }

  /** Called after the selection changes. The default implementation does nothing. */
  protected def changed(): Unit = {}

  /** Fires a change event on the selection's actor, if any. Called internally when the selection changes, depending on {@link #setProgrammaticChangeEvents(boolean)}.
    * @return
    *   true if the change should be undone.
    */
  def fireChangeEvent(): Boolean =
    actor.exists { a =>
      val changeEvent = Actor.POOLS.obtain(classOf[ChangeListener.ChangeEvent])
      try
        a.fire(changeEvent)
      finally
        Actor.POOLS.free(changeEvent)
    }

  /** @param item May be null (returns false). */
  def contains(item: Nullable[T]): Boolean =
    item.exists(selected.contains)

  /** Makes a best effort to return the last item selected, else returns an arbitrary item or null if the selection is empty. */
  def lastSelected: Nullable[T] =
    if (_lastSelected.isDefined) _lastSelected
    else if (selected.nonEmpty) Nullable(selected.head)
    else Nullable.empty

  override def iterator: Iterator[T] = selected.iterator

  def toArray: DynamicArray[T] = {
    val result = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
    selected.foreach(result.add)
    result
  }

  def toArray(array: DynamicArray[T]): DynamicArray[T] = {
    selected.foreach(array.add)
    array
  }

  /** If true, prevents {@link #choose(Object)} from changing the selection. Default is false. */
  def setDisabled(isDisabled: Boolean): Unit = this._isDisabled = isDisabled

  def isDisabled: Boolean = _isDisabled

  override def toString: String = selected.toString
}
