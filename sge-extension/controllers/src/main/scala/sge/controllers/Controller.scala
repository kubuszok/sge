/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 107
 * Covenant-baseline-methods: Controller,PlayerIdxUnset,addListener,axisCount,canVibrate,cancelVibration,getAxis,getButton,getButtonValue,isConnected,isVibrating,mapping,maxButtonIndex,minButtonIndex,name,playerIndex,playerIndex_,powerLevel,removeListener,startVibration,supportsPlayerIndex,uniqueId
 * Covenant-source-reference: com/badlogic/gdx/controllers/Controller.java
 * Covenant-verified: 2026-04-19
 */
package sge
package controllers

/** Represents a connected controller. Provides methods to query the state of buttons, axes and more information on the controller. Multiple [[ControllerListener]] instances can be registered with the
  * Controller to receive events in case the controller's state changes. Listeners will be invoked on the rendering thread.
  *
  * @author
  *   Nathan Sweet
  */
trait Controller {

  /** @return whether the button is pressed. */
  def getButton(buttonCode: Int): Boolean

  /** @return
    *   the analog pressure value of the button, between 0 and 1. Digital buttons return 0.0 or 1.0. Analog triggers return values in between. Uses a 0.5f threshold to determine pressed state (see
    *   [[getButton]]).
    */
  def getButtonValue(buttonCode: Int): Float

  /** @return the value of the axis, between -1 and 1. */
  def getAxis(axisCode: Int): Float

  /** @return the device name. */
  def name: String

  /** @return
    *   unique ID to recognize this controller if more than one of the same controller models are connected. Use this to map a controller to a player, but do not use it to save a button mapping.
    */
  def uniqueId: String

  /** @return the minimum button index code that can be queried. */
  def minButtonIndex: Int

  /** @return the maximum button index code that can be queried. */
  def maxButtonIndex: Int

  /** @return
    *   number of axes of this controller. Axis indices start at 0, so the maximum axis index is one under this value.
    */
  def axisCount: Int

  /** @return true when this Controller is still connected, false if it already disconnected. */
  def isConnected: Boolean

  /** @return
    *   whether the connected controller or the current controller implementation can rumble. Note that this is no guarantee that the connected controller itself can vibrate.
    */
  def canVibrate: Boolean

  /** @return if the controller is currently rumbling. */
  def isVibrating: Boolean

  /** Starts vibrating this controller, if possible.
    *
    * @param duration
    *   duration, in milliseconds
    * @param strength
    *   value between 0f and 1f
    */
  def startVibration(duration: Int, strength: Float): Unit

  /** Cancel any running vibration. May not be supported by implementations. */
  def cancelVibration(): Unit

  /** @return whether the connected controller (or the implementation) can return and set the current player index. */
  def supportsPlayerIndex: Boolean

  /** @return 0-based player index of this controller, or [[Controller.PlayerIdxUnset]] if none is set. */
  def playerIndex: Int

  /** Sets the player index of this controller. Please note that this does not always set indication lights of controllers, it is just an internal representation on some platforms.
    *
    * @param index
    *   typically 0 to 3 for player indices, and [[Controller.PlayerIdxUnset]] for unset
    */
  def playerIndex_=(index: Int): Unit

  /** @return
    *   button and axis mapping for this controller (or platform). The connected controller might not support all features.
    */
  def mapping: ControllerMapping

  /** @return
    *   value of enum [[ControllerPowerLevel]] indicating battery state of the connected controller, or [[ControllerPowerLevel.Unknown]] if information is not present.
    */
  def powerLevel: ControllerPowerLevel

  /** Adds a new [[ControllerListener]] to this [[Controller]]. The listener will receive calls in case the state of the controller changes. The listener will be invoked on the rendering thread.
    */
  def addListener(listener: ControllerListener): Unit

  /** Removes the given [[ControllerListener]]. */
  def removeListener(listener: ControllerListener): Unit
}

object Controller {

  /** Returned by [[Controller.playerIndex]] if no player index was set or feature is not supported. */
  val PlayerIdxUnset: Int = -1
}
