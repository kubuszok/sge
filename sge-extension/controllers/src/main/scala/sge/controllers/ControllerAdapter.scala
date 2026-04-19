/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: ControllerAdapter,axisMoved,buttonDown,buttonUp,connected,disconnected
 * Covenant-source-reference: com/badlogic/gdx/controllers/ControllerAdapter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package controllers

/** A base implementation for [[ControllerListener]]. Subclass this if you are only interested in a few specific events.
  *
  * @author
  *   mzechner
  */
class ControllerAdapter extends ControllerListener {

  override def connected(controller: Controller): Unit = {}

  override def disconnected(controller: Controller): Unit = {}

  override def buttonDown(controller: Controller, buttonCode: Int): Boolean =
    false

  override def buttonUp(controller: Controller, buttonCode: Int): Boolean =
    false

  override def axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean =
    false
}
