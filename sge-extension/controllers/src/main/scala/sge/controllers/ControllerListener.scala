/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 57
 * Covenant-baseline-methods: ControllerListener,axisMoved,buttonDown,buttonUp,connected,disconnected
 * Covenant-source-reference: com/badlogic/gdx/controllers/ControllerListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 124b68125c7ef9c552085865379f77e8bee2ae3b
 */
package sge
package controllers

/** Registered with [[Controllers]] or a specific [[Controller]] instance to receive events.
  *
  * @author
  *   Nathan Sweet
  */
trait ControllerListener {

  /** A [[Controller]] got connected. */
  def connected(controller: Controller): Unit

  /** A [[Controller]] got disconnected. */
  def disconnected(controller: Controller): Unit

  /** A button on the [[Controller]] was pressed. The buttonCode is controller specific.
    *
    * @param controller
    *   the controller
    * @param buttonCode
    *   the button code
    * @return
    *   whether to hand the event to other listeners
    */
  def buttonDown(controller: Controller, buttonCode: Int): Boolean

  /** A button on the [[Controller]] was released. The buttonCode is controller specific.
    *
    * @param controller
    *   the controller
    * @param buttonCode
    *   the button code
    * @return
    *   whether to hand the event to other listeners
    */
  def buttonUp(controller: Controller, buttonCode: Int): Boolean

  /** An axis on the [[Controller]] moved. The axisCode is controller specific. The axis value is in the range [-1, 1].
    *
    * @param controller
    *   the controller
    * @param axisCode
    *   the axis code
    * @param value
    *   the axis value, -1 to 1
    * @return
    *   whether to hand the event to other listeners
    */
  def axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean
}
