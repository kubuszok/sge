/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ButtonGroup.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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

  private val buttons:        DynamicArray[T] = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
  private val checkedButtons: DynamicArray[T] = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
  private var minCheckCount:  Int             = 1
  private var maxCheckCount:  Int             = 1
  private var uncheckLast:    Boolean         = true
  private var lastChecked:    Nullable[T]     = Nullable.empty

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
    checkedButtons.removeValue(button)
  }

  def removeAll(buttons: T*): Unit =
    buttons.foreach(remove)

  def clear(): Unit = {
    buttons.clear()
    checkedButtons.clear()
  }

  /** Sets the first {@link TextButton} with the specified text to checked. */
  def setChecked(text: String): Unit = {
    var i = 0
    val n = buttons.size
    scala.util.boundary {
      while (i < n) {
        val button = buttons(i)
        button match {
          case tb: TextButton if text == new String(tb.getText.toArray) =>
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
      if (checkedButtons.size <= minCheckCount) scala.util.boundary.break(false)
      checkedButtons.removeValue(button)
    } else {
      // Keep button unchecked to enforce maxCheckCount.
      if (maxCheckCount != -1 && checkedButtons.size >= maxCheckCount) {
        if (!uncheckLast) scala.util.boundary.break(false)
        var tries = 0
        scala.util.boundary {
          while (true) {
            val old = minCheckCount
            minCheckCount = 0
            lastChecked.foreach(_.setChecked(false)) // May have listeners that change button states.
            minCheckCount = old
            if (button.isChecked == newState) scala.util.boundary.break(false)
            if (checkedButtons.size < maxCheckCount) scala.util.boundary.break()
            tries += 1
            if (tries > 10) scala.util.boundary.break(false) // Unable to uncheck another button.
          }
        }
      }
      checkedButtons.add(button)
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

  /** @return The first checked button, or null. */
  def getChecked: Nullable[T] =
    if (checkedButtons.nonEmpty) Nullable(checkedButtons(0))
    else Nullable.empty

  /** @return The first checked button index, or -1. */
  def getCheckedIndex: Int =
    if (checkedButtons.nonEmpty) buttons.indexOf(checkedButtons(0))
    else -1

  def getAllChecked: DynamicArray[T] = checkedButtons

  def getButtons: DynamicArray[T] = buttons

  /** Sets the minimum number of buttons that must be checked. Default is 1. */
  def setMinCheckCount(minCheckCount: Int): Unit =
    this.minCheckCount = minCheckCount

  /** Sets the maximum number of buttons that can be checked. Set to -1 for no maximum. Default is 1. */
  def setMaxCheckCount(maxCheckCount: Int): Unit =
    this.maxCheckCount = if (maxCheckCount == 0) -1 else maxCheckCount

  /** If true, when the maximum number of buttons are checked and an additional button is checked, the last button to be checked is unchecked so that the maximum is not exceeded. If false, additional
    * buttons beyond the maximum are not allowed to be checked. Default is true.
    */
  def setUncheckLast(uncheckLast: Boolean): Unit =
    this.uncheckLast = uncheckLast
}
