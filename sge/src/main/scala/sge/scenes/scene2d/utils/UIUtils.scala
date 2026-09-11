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
 * - SharedLibraryLoader.os/Os enum -> per-platform OS detection seam
 *   (UIUtilsPlatform). JVM/Native read System.getProperty("os.name"); JS
 *   super-sources the GWT UIUtils emulation and reads window.navigator.platform.
 * - The 5 is* flags delegate to UIUtilsPlatform (this file is shared scala/, so
 *   it cannot host platform-specific code); kept as cache-once vals.
 * - Gdx.input -> Sge().input (using Sge context parameter)
 * - No-arg methods require (using Sge); int-arg overloads are pure
 * - All methods faithfully ported
 * Convention: opaque Button/Key types for type-safe button/key code params
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 86
 * Covenant-baseline-methods: UIUtils,alt,ctrl,isAndroid,isIos,isLinux,isMac,isWindows,left,middle,right,shift
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/UIUtils.java
 * Covenant-verified: 2026-06-16
 *
 * upstream-commit: 70725e2c7598f7fa02214814a85adad5134bb277
 */
package sge
package scenes
package scene2d
package utils

import sge.Input.{ Button, Key }

object UIUtils {

  val isAndroid: Boolean = UIUtilsPlatform.isAndroid
  val isMac:     Boolean = UIUtilsPlatform.isMac
  val isWindows: Boolean = UIUtilsPlatform.isWindows
  val isLinux:   Boolean = UIUtilsPlatform.isLinux
  val isIos:     Boolean = UIUtilsPlatform.isIos

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
