/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package utils

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

  def left()(using sge: Sge): Boolean =
    sge.input.isButtonPressed(Input.Buttons.LEFT)

  def left(button: Int): Boolean =
    button == Input.Buttons.LEFT

  def right()(using sge: Sge): Boolean =
    sge.input.isButtonPressed(Input.Buttons.RIGHT)

  def right(button: Int): Boolean =
    button == Input.Buttons.RIGHT

  def middle()(using sge: Sge): Boolean =
    sge.input.isButtonPressed(Input.Buttons.MIDDLE)

  def middle(button: Int): Boolean =
    button == Input.Buttons.MIDDLE

  def shift()(using sge: Sge): Boolean =
    sge.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || sge.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)

  def shift(keycode: Int): Boolean =
    keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT

  def ctrl()(using sge: Sge): Boolean =
    if (isMac)
      sge.input.isKeyPressed(Input.Keys.SYM)
    else
      sge.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || sge.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)

  def ctrl(keycode: Int): Boolean =
    if (isMac)
      keycode == Input.Keys.SYM
    else
      keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.CONTROL_RIGHT

  def alt()(using sge: Sge): Boolean =
    sge.input.isKeyPressed(Input.Keys.ALT_LEFT) || sge.input.isKeyPressed(Input.Keys.ALT_RIGHT)

  def alt(keycode: Int): Boolean =
    keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT
}
