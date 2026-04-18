/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-controllers/desktop/src/com/badlogic/gdx/controllers/desktop/DesktopControllerManager.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Architecture divergence: original DesktopControllerManager uses GLFW event callbacks;
 *     this implementation is polling-based (call poll() each frame) for cross-platform uniformity.
 *   - Vibration not supported in the polling-based stub (rumble/haptics blocked on Panama wiring).
 */
package sge
package controllers

import scala.collection.mutable.ArrayBuffer

/** Polling-based controller manager implementation that works on all platforms. Periodically polls [[ControllerOps]] to detect connect/disconnect events and state changes, then fires
  * [[ControllerListener]] callbacks.
  *
  * Call [[poll]] once per frame (e.g. from the render loop) to update controller state and dispatch events.
  *
  * @param ops
  *   the platform-specific controller polling backend
  */
class DefaultControllerManager(ops: ControllerOps) extends ControllerManager {

  private val listeners: ArrayBuffer[ControllerListener] = ArrayBuffer.empty
  private val manageCurrentListener = ManageCurrentControllerListener()
  private val polledControllers: ArrayBuffer[PolledController] = ArrayBuffer.empty

  override def addListener(listener: ControllerListener): Unit =
    listeners += listener

  override def removeListener(listener: ControllerListener): Unit = {
    val _ = listeners -= listener
  }

  override def getListeners: Seq[ControllerListener] = listeners.toSeq

  override def clearListeners(): Unit =
    listeners.clear()

  /** Polls all controller slots and dispatches connect/disconnect/button/axis events. Call this once per frame. */
  def poll(): Unit = {
    val states = ops.getConnectedControllers()

    // Detect disconnections: controllers we were tracking that are no longer connected
    val toRemove = ArrayBuffer.empty[PolledController]
    polledControllers.foreach { pc =>
      val stillConnected = states.exists(_.uniqueId == pc.uniqueId)
      if (!stillConnected) {
        pc.markDisconnected()
        toRemove += pc
        fireDisconnected(pc)
      }
    }
    toRemove.foreach { pc =>
      val _ = polledControllers -= pc
    }
    // Also remove from the base class controllers list
    controllers.filterInPlace { c =>
      !toRemove.exists(_.asInstanceOf[AnyRef] eq c.asInstanceOf[AnyRef])
    }

    // Detect new connections and update existing controllers
    states.foreach { state =>
      polledControllers.find(_.uniqueId == state.uniqueId) match {
        case Some(existing) =>
          existing.updateState(state, this)
        case None =>
          val pc = PolledController(state)
          polledControllers += pc
          controllers += pc
          fireConnected(pc)
      }
    }
  }

  private def fireConnected(controller: Controller): Unit = {
    manageCurrentListener.connected(controller)
    listeners.foreach(_.connected(controller))
  }

  private def fireDisconnected(controller: Controller): Unit = {
    manageCurrentListener.disconnected(controller)
    listeners.foreach(_.disconnected(controller))
  }

  private[controllers] def fireButtonDown(controller: Controller, buttonCode: Int): Unit = scala.util.boundary {
    if (manageCurrentListener.buttonDown(controller, buttonCode)) scala.util.boundary.break(())
    for (listener <- listeners)
      if (listener.buttonDown(controller, buttonCode)) scala.util.boundary.break(())
  }

  private[controllers] def fireButtonUp(controller: Controller, buttonCode: Int): Unit = scala.util.boundary {
    if (manageCurrentListener.buttonUp(controller, buttonCode)) scala.util.boundary.break(())
    for (listener <- listeners)
      if (listener.buttonUp(controller, buttonCode)) scala.util.boundary.break(())
  }

  private[controllers] def fireAxisMoved(controller: Controller, axisCode: Int, value: Float): Unit = scala.util.boundary {
    if (manageCurrentListener.axisMoved(controller, axisCode, value)) scala.util.boundary.break(())
    for (listener <- listeners)
      if (listener.axisMoved(controller, axisCode, value)) scala.util.boundary.break(())
  }
}

/** A [[Controller]] implementation backed by polling. Each frame, [[DefaultControllerManager]] updates the state from [[ControllerState]] snapshots and fires events for changes.
  */
private[controllers] class PolledController(initialState: ControllerState) extends Controller {

  private var _name:                  String                          = initialState.name
  private val _uniqueId:              String                          = initialState.uniqueId
  private var _connected:             Boolean                         = initialState.connected
  private var _buttons:               Array[Boolean]                  = initialState.buttons.clone()
  private var _buttonValues:          Array[Float]                    = initialState.buttonValues.clone()
  private var _axes:                  Array[Float]                    = initialState.axes.clone()
  private var _powerLevel:            ControllerPowerLevel            = initialState.powerLevel
  private var _playerIndex:           Int                             = Controller.PlayerIdxUnset
  private val perControllerListeners: ArrayBuffer[ControllerListener] = ArrayBuffer.empty

  def uniqueId: String = _uniqueId

  override def getButton(buttonCode: Int): Boolean =
    if (buttonCode >= 0 && buttonCode < _buttons.length) _buttons(buttonCode) else false

  override def getButtonValue(buttonCode: Int): Float =
    if (buttonCode >= 0 && buttonCode < _buttonValues.length) _buttonValues(buttonCode) else 0f

  override def getAxis(axisCode: Int): Float =
    if (axisCode >= 0 && axisCode < _axes.length) _axes(axisCode) else 0f

  override def name: String = _name

  override def minButtonIndex: Int = 0

  override def maxButtonIndex: Int = _buttons.length - 1

  override def axisCount: Int = _axes.length

  override def isConnected: Boolean = _connected

  override def canVibrate: Boolean = false

  override def isVibrating: Boolean = false

  override def startVibration(duration: Int, strength: Float): Unit = {
    // Vibration not supported in polling-based stub
  }

  override def cancelVibration(): Unit = {
    // Vibration not supported in polling-based stub
  }

  override def supportsPlayerIndex: Boolean = true

  override def playerIndex: Int = _playerIndex

  override def playerIndex_=(index: Int): Unit =
    _playerIndex = index

  override def mapping: ControllerMapping = ControllerMapping.StandardMapping

  override def powerLevel: ControllerPowerLevel = _powerLevel

  override def addListener(listener: ControllerListener): Unit =
    perControllerListeners += listener

  override def removeListener(listener: ControllerListener): Unit = {
    val _ = perControllerListeners -= listener
  }

  /** Updates internal state from a new [[ControllerState]] snapshot and fires events for changes. */
  private[controllers] def updateState(state: ControllerState, manager: DefaultControllerManager): Unit = {
    _name = state.name
    _powerLevel = state.powerLevel

    // Detect button changes
    val maxButtons = scala.math.min(_buttons.length, state.buttons.length)
    var i          = 0
    while (i < maxButtons) {
      if (_buttons(i) != state.buttons(i)) {
        _buttons(i) = state.buttons(i)
        if (state.buttons(i)) {
          manager.fireButtonDown(this, i)
          perControllerListeners.foreach(_.buttonDown(this, i))
        } else {
          manager.fireButtonUp(this, i)
          perControllerListeners.foreach(_.buttonUp(this, i))
        }
      }
      i += 1
    }
    // Update analog values alongside digital state
    val maxBtnVals = scala.math.min(_buttonValues.length, state.buttonValues.length)
    var bv         = 0
    while (bv < maxBtnVals) {
      _buttonValues(bv) = state.buttonValues(bv)
      bv += 1
    }

    // Handle size changes (new buttons appeared)
    if (state.buttons.length != _buttons.length) {
      _buttons = state.buttons.clone()
      _buttonValues = state.buttonValues.clone()
    }

    // Detect axis changes
    val maxAxes = scala.math.min(_axes.length, state.axes.length)
    var j       = 0
    while (j < maxAxes) {
      if (_axes(j) != state.axes(j)) {
        _axes(j) = state.axes(j)
        manager.fireAxisMoved(this, j, state.axes(j))
        perControllerListeners.foreach(_.axisMoved(this, j, state.axes(j)))
      }
      j += 1
    }
    // Handle size changes (new axes appeared)
    if (state.axes.length != _axes.length) {
      _axes = state.axes.clone()
    }
  }

  /** Marks this controller as disconnected. */
  private[controllers] def markDisconnected(): Unit =
    _connected = false
}
