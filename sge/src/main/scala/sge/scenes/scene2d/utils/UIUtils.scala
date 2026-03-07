/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java final class with static methods -> Scala object
 * - SharedLibraryLoader.os/Os enum -> System.getProperty os detection
 * - Gdx.input -> Sge().input (using Sge context parameter)
 * - No-arg methods require (using Sge); int-arg overloads are pure
 * - All methods faithfully ported
 * Convention: opaque Button/Key types for type-safe button/key code params
 */
package sge
package scenes
package scene2d
package utils

import sge.Input.{ Button, Key }

object UIUtils {

  val isAndroid: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("android")
  }
  val isMac: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("mac")
  }
  val isWindows: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("windows")
  }
  val isLinux: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("linux") && !os.contains("android")
  }
  val isIos: Boolean = {
    val os = System.getProperty("os.name", "").toLowerCase
    os.contains("ios")
  }

  def left()(using Sge): Boolean =
    Sge().input.isButtonPressed(Input.Buttons.LEFT)

  def left(button: Button): Boolean =
    button == Input.Buttons.LEFT

  def right()(using Sge): Boolean =
    Sge().input.isButtonPressed(Input.Buttons.RIGHT)

  def right(button: Button): Boolean =
    button == Input.Buttons.RIGHT

  def middle()(using Sge): Boolean =
    Sge().input.isButtonPressed(Input.Buttons.MIDDLE)

  def middle(button: Button): Boolean =
    button == Input.Buttons.MIDDLE

  def shift()(using Sge): Boolean =
    Sge().input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Sge().input.isKeyPressed(Input.Keys.SHIFT_RIGHT)

  def shift(keycode: Key): Boolean =
    keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT

  def ctrl()(using Sge): Boolean =
    if (isMac)
      Sge().input.isKeyPressed(Input.Keys.SYM)
    else
      Sge().input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Sge().input.isKeyPressed(Input.Keys.CONTROL_RIGHT)

  def ctrl(keycode: Key): Boolean =
    if (isMac)
      keycode == Input.Keys.SYM
    else
      keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.CONTROL_RIGHT

  def alt()(using Sge): Boolean =
    Sge().input.isKeyPressed(Input.Keys.ALT_LEFT) || Sge().input.isKeyPressed(Input.Keys.ALT_RIGHT)

  def alt(keycode: Key): Boolean =
    keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT
}
