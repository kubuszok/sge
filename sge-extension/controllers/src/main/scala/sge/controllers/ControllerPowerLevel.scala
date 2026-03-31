/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

/** Power level of a connected controller, indicating battery state.
  *
  * @author Benjamin Schulte
  */
enum ControllerPowerLevel extends java.lang.Enum[ControllerPowerLevel] {

  /** Power level unknown. */
  case Unknown

  /** Power level 0-5%. */
  case Empty

  /** Power level 6-20%. */
  case Low

  /** Power level 21-70%. */
  case Medium

  /** Power level 71-100%. */
  case Full

  /** Controller is wired or charging. */
  case Wired
}
