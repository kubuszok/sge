/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 74
 * Covenant-baseline-methods: ControllerManager,ManageCurrentControllerListener,_currentController,addListener,axisMoved,buttonDown,buttonUp,clearListeners,connected,controllers,currentController,disconnected,getControllers,getListeners,removeListener
 * Covenant-source-reference: com/badlogic/gdx/controllers/ControllerManager.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 124b68125c7ef9c552085865379f77e8bee2ae3b
 */
package sge
package controllers

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

/** Abstract base for platform-specific controller manager implementations. Tracks connected controllers and the most recently used controller.
  *
  * @author
  *   Nathan Sweet
  */
abstract class ControllerManager {

  protected val controllers:      ArrayBuffer[Controller] = ArrayBuffer.empty
  private var _currentController: Nullable[Controller]    = Nullable.empty

  /** @return all currently connected controllers. */
  def getControllers: Seq[Controller] = controllers.toSeq

  /** @return
    *   the controller the player used most recently. This might return [[Nullable.empty]] if there is no controller connected or the last used controller disconnected.
    */
  def currentController: Nullable[Controller] = _currentController

  /** Add a global [[ControllerListener]] that can react to events from all [[Controller]] instances. The listener will be invoked on the rendering thread.
    */
  def addListener(listener: ControllerListener): Unit

  /** Removes a global [[ControllerListener]]. The method must be called on the rendering thread. */
  def removeListener(listener: ControllerListener): Unit

  /** @return all listeners currently registered. */
  def getListeners: Seq[ControllerListener]

  /** Removes every global [[ControllerListener]] previously added. */
  def clearListeners(): Unit

  /** Manages the currentController field. Must be added to controller listeners as the first listener. */
  protected class ManageCurrentControllerListener extends ControllerAdapter {

    override def connected(controller: Controller): Unit =
      _currentController.fold {
        _currentController = Nullable(controller)
      }(_ => ())

    override def disconnected(controller: Controller): Unit =
      _currentController.foreach { current =>
        if (current eq controller) {
          _currentController = Nullable.empty
        }
      }

    override def buttonDown(controller: Controller, buttonIndex: Int): Boolean = {
      _currentController = Nullable(controller)
      false
    }

    override def buttonUp(controller: Controller, buttonIndex: Int): Boolean = {
      _currentController = Nullable(controller)
      false
    }

    override def axisMoved(controller: Controller, axisIndex: Int, value: Float): Boolean = {
      _currentController = Nullable(controller)
      false
    }
  }
}
