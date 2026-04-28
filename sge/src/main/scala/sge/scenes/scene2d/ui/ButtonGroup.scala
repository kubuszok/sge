/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ButtonGroup.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; DynamicArray with MkArray.anyRef cast for generic T; varargs constructor
 *   Idiom: split packages
 *   Fixes: Java-style getters/setters → Scala property accessors (checked, checkedIndex, allChecked, buttons, minCheckCount, maxCheckCount)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 145
 * Covenant-baseline-methods: ButtonGroup,add,addAll,allChecked,buttons,canCheck,checked,checkedIndex,clear,i,lastChecked,maxCheckCount,minCheckCount,n,old,remove,removeAll,setChecked,shouldCheck,this,uncheckAll,uncheckLast
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/ButtonGroup.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package scenes
package scene2d
package ui

import sge.utils.{ DynamicArray, MkArray, Nullable }

/** Manages a group of buttons to enforce a minimum and maximum number of checked buttons. This enables "radio button" functionality and more. A button may only be in one group at a time. <p> The
  * {@link #canCheck(Button, boolean)} method can be overridden to control if a button check or uncheck is allowed.
  * @author
  *   Nathan Sweet
  */
class ButtonGroup[T <: Button]() {

  val buttons:             DynamicArray[T] = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
  val allChecked:          DynamicArray[T] = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
  var minCheckCount:       Int             = 1
  var maxCheckCount:       Int             = 1
  var uncheckLast:         Boolean         = true
  private var lastChecked: Nullable[T]     = Nullable.empty

  def this(buttons: T*) = {
    this()
    minCheckCount = 0
    buttons.foreach(add)
    minCheckCount = 1
  }

  def add(button: T): Unit = {
    button.buttonGroup = Nullable.empty
    val shouldCheck = button.isChecked || buttons.size < minCheckCount
    button.setChecked(false)
    button.buttonGroup = Nullable(this)
    buttons.add(button)
    button.setChecked(shouldCheck)
  }

  def addAll(buttons: T*): Unit =
    buttons.foreach(add)

  def remove(button: T): Unit = {
    button.buttonGroup = Nullable.empty
    buttons.removeValue(button)
    allChecked.removeValue(button)
  }

  def removeAll(buttons: T*): Unit =
    buttons.foreach(remove)

  def clear(): Unit = {
    buttons.clear()
    allChecked.clear()
  }

  /** Sets the first {@link TextButton} with the specified text to checked. */
  def setChecked(text: String): Unit = {
    var i = 0
    val n = buttons.size
    scala.util.boundary {
      while (i < n) {
        val button = buttons(i)
        button match {
          case tb: TextButton if text == new String(tb.text.toArray) =>
            button.setChecked(true)
            scala.util.boundary.break()
          case _ =>
        }
        i += 1
      }
    }
  }

  /** Called when a button is checked or unchecked. If overridden, generally changing button checked states should not be done from within this method.
    * @return
    *   True if the new state should be allowed.
    */
  protected[ui] def canCheck(button: T, newState: Boolean): Boolean = scala.util.boundary {
    if (button.isChecked == newState) scala.util.boundary.break(false)

    if (!newState) {
      // Keep button checked to enforce minCheckCount.
      if (allChecked.size <= minCheckCount) scala.util.boundary.break(false)
      allChecked.removeValue(button)
    } else {
      // Keep button unchecked to enforce maxCheckCount.
      if (maxCheckCount != -1 && allChecked.size >= maxCheckCount) {
        if (!uncheckLast) scala.util.boundary.break(false)
        var tries = 0
        scala.util.boundary {
          while (true) {
            val old = minCheckCount
            minCheckCount = 0
            lastChecked.foreach(_.setChecked(false)) // May have listeners that change button states.
            minCheckCount = old
            if (button.isChecked == newState) scala.util.boundary.break(false)
            if (allChecked.size < maxCheckCount) scala.util.boundary.break()
            tries += 1
            if (tries > 10) scala.util.boundary.break(false) // Unable to uncheck another button.
          }
        }
      }
      allChecked.add(button)
      lastChecked = Nullable(button)
    }

    true
  }

  /** Sets all buttons' {@link Button#isChecked()} to false, regardless of {@link #setMinCheckCount(int)}. */
  def uncheckAll(): Unit = {
    val old = minCheckCount
    minCheckCount = 0
    var i = 0
    val n = buttons.size
    while (i < n) {
      buttons(i).setChecked(false)
      i += 1
    }
    minCheckCount = old
  }

  /** @return The first checked button, or empty. */
  def checked: Nullable[T] =
    if (allChecked.nonEmpty) Nullable(allChecked(0))
    else Nullable.empty

  /** @return The first checked button index, or -1. */
  def checkedIndex: Int =
    if (allChecked.nonEmpty) buttons.indexOf(allChecked(0))
    else -1
}
