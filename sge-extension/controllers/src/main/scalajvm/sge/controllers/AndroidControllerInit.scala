/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Original source: gdx-controllers-android/src/com/badlogic/gdx/controllers/android/AndroidControllers.java
 *                  gdx-controllers-android/src/com/badlogic/gdx/controllers/android/AndroidController.java
 *                  gdx-controllers-android/src/com/badlogic/gdx/controllers/android/AndroidControllerEvent.java
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidControllers/AndroidController/AndroidControllerEvent -> AndroidControllerInit (merged)
 *   Convention: polling-based ControllerState snapshots (replaces event-queue + Controller implementation)
 *   Idiom: split packages; Nullable; no return (boundary/break); no bare null
 *   Merged with: AndroidController.java, AndroidControllerEvent.java
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

import scala.collection.mutable

/** Initializes the [[AndroidControllerBackend]] companion object's polling functions to use Android's InputDevice APIs on JVM (Android builds).
  *
  * This object tracks connected game controllers via [[android.view.InputDevice]], accumulates button and axis state from key/motion events, and provides polling snapshots as [[ControllerState]]
  * instances.
  *
  * The original gdx-controllers used an event-queue architecture with AndroidController objects, an event pool, and synchronized dispatch. SGE replaces this with a polling model: the Android-side
  * code tracks state in [[TrackedController]] instances, and [[AndroidControllerBackend]] polls snapshots from them each frame.
  *
  * To use, call [[init]] once at startup after the Android Activity is set up.
  */
object AndroidControllerInit {

  /** Whether to ignore key events that are not gamepad buttons (default: true, matching the original). */
  var ignoreNoGamepadButtons: Boolean = true

  /** Whether to use the new axis logic that re-orders stick axes and maps triggers to buttons (default: true, matching the original). */
  var useNewAxisLogic: Boolean = true

  // ── Internal state ─────────────────────────────────────────────────

  /** All currently tracked controllers, keyed by Android device ID. */
  private val controllerMap: mutable.Map[Int, TrackedController] = mutable.Map.empty

  /** Lock object for synchronizing controller state access. */
  private val lock: AnyRef = new AnyRef {}

  // ── Android KeyEvent constants ─────────────────────────────────────
  // These match android.view.KeyEvent and are used for button tracking.

  private val KEYCODE_DPAD_UP:       Int = 19
  private val KEYCODE_DPAD_DOWN:     Int = 20
  private val KEYCODE_DPAD_LEFT:     Int = 21
  private val KEYCODE_DPAD_RIGHT:    Int = 22
  private val KEYCODE_BACK:          Int = 4
  private val KEYCODE_BUTTON_L2:     Int = 104
  private val KEYCODE_BUTTON_R2:     Int = 105
  private val ACTION_DOWN:           Int = 0
  private val SOURCE_CLASS_JOYSTICK: Int = 0x00000010
  private val SOURCE_GAMEPAD:        Int = 0x00000401
  private val KEYBOARD_TYPE_ALPHA:   Int = 2

  // ── Android MotionEvent axis constants ─────────────────────────────

  private val AXIS_X:        Int = 0
  private val AXIS_Y:        Int = 1
  private val AXIS_Z:        Int = 11
  private val AXIS_RZ:       Int = 14
  private val AXIS_HAT_X:    Int = 15
  private val AXIS_HAT_Y:    Int = 16
  private val AXIS_LTRIGGER: Int = 17
  private val AXIS_RTRIGGER: Int = 18

  // ── Public API ──────────────────────────────────────────────────────

  /** Call this once at startup to wire the AndroidControllerBackend to the real Android implementation. Gathers currently connected controllers.
    */
  def init(): Unit = {
    AndroidControllerBackend.pollControllerImpl = pollController
    AndroidControllerBackend.getConnectedControllersImpl = getConnectedControllers
    gatherControllers()
  }

  /** Disposes the controller tracking state. Call on application shutdown. */
  def dispose(): Unit = lock.synchronized {
    controllerMap.clear()
  }

  /** Forward a key event from the Android view to the controller system.
    *
    * The host Activity should call this from its key event handler. The event is passed as AnyRef and accessed reflectively to avoid compile-time Android SDK dependency.
    *
    * @param deviceId
    *   the Android device ID from the KeyEvent
    * @param keyCode
    *   the key code from the KeyEvent
    * @param action
    *   the action (ACTION_DOWN or ACTION_UP)
    * @param source
    *   the input source from the KeyEvent's device
    * @return
    *   true if the event was consumed
    */
  def onKeyEvent(deviceId: Int, keyCode: Int, action: Int, source: Int): Boolean =
    if (ignoreNoGamepadButtons && !isGamepadButton(keyCode)) {
      false
    } else {
      lock.synchronized {
        controllerMap.get(deviceId) match {
          case None             => false
          case Some(controller) =>
            // If the button is already pressed and this is another DOWN, ignore (auto-repeat)
            if (controller.isButtonPressed(keyCode) && action == ACTION_DOWN) {
              true
            }
            // Ignore trigger buttons if this controller has trigger axes
            // (they are mapped from axis values instead)
            else if (controller.hasTriggerAxis && (keyCode == KEYCODE_BUTTON_L2 || keyCode == KEYCODE_BUTTON_R2)) {
              true
            } else {
              if (action == ACTION_DOWN) {
                controller.setButtonPressed(keyCode, pressed = true)
              } else {
                controller.setButtonPressed(keyCode, pressed = false)
              }
              // Consume the event unless it's BACK and the app doesn't catch it
              keyCode != KEYCODE_BACK
            }
        }
      }
    }

  /** Forward a generic motion event (joystick/gamepad axes) from the Android view.
    *
    * The host Activity should call this from its motion event handler. Axis values are extracted reflectively.
    *
    * @param deviceId
    *   the Android device ID
    * @param axisValues
    *   a map of axis ID to axis value, extracted from the MotionEvent
    * @param source
    *   the input source from the MotionEvent
    * @return
    *   true if the event was consumed
    */
  def onMotionEvent(deviceId: Int, axisValues: Map[Int, Float], source: Int): Boolean =
    if ((source & SOURCE_CLASS_JOYSTICK) == 0) false
    else {
      lock.synchronized {
        controllerMap.get(deviceId) match {
          case None             => false
          case Some(controller) =>
            // Handle POV (hat) axis -> dpad button mapping
            if (controller.hasPovAxis) {
              val povX = axisValues.getOrElse(AXIS_HAT_X, 0f)
              val povY = axisValues.getOrElse(AXIS_HAT_Y, 0f)

              if (povX != controller.povX) {
                // Release old direction
                if (controller.povX == 1f) controller.setButtonPressed(KEYCODE_DPAD_RIGHT, pressed = false)
                else if (controller.povX == -1f) controller.setButtonPressed(KEYCODE_DPAD_LEFT, pressed = false)
                // Press new direction
                if (povX == 1f) controller.setButtonPressed(KEYCODE_DPAD_RIGHT, pressed = true)
                else if (povX == -1f) controller.setButtonPressed(KEYCODE_DPAD_LEFT, pressed = true)
                controller.povX = povX
              }

              if (povY != controller.povY) {
                if (controller.povY == 1f) controller.setButtonPressed(KEYCODE_DPAD_DOWN, pressed = false)
                else if (controller.povY == -1f) controller.setButtonPressed(KEYCODE_DPAD_UP, pressed = false)
                if (povY == 1f) controller.setButtonPressed(KEYCODE_DPAD_DOWN, pressed = true)
                else if (povY == -1f) controller.setButtonPressed(KEYCODE_DPAD_UP, pressed = true)
                controller.povY = povY
              }
            }

            // Handle trigger axis -> trigger button mapping
            if (controller.hasTriggerAxis) {
              val lTrigger = axisValues.getOrElse(AXIS_LTRIGGER, 0f)
              val rTrigger = axisValues.getOrElse(AXIS_RTRIGGER, 0f)

              if (lTrigger != controller.lTrigger) {
                if (lTrigger == 1f) controller.setButtonPressed(KEYCODE_BUTTON_L2, pressed = true)
                else if (lTrigger == 0f) controller.setButtonPressed(KEYCODE_BUTTON_L2, pressed = false)
                controller.lTrigger = lTrigger
              }

              if (rTrigger != controller.rTrigger) {
                if (rTrigger == 1f) controller.setButtonPressed(KEYCODE_BUTTON_R2, pressed = true)
                else if (rTrigger == 0f) controller.setButtonPressed(KEYCODE_BUTTON_R2, pressed = false)
                controller.rTrigger = rTrigger
              }
            }

            // Update regular axes
            var axisIndex = 0
            controller.axisIds.foreach { axisId =>
              val axisValue = axisValues.getOrElse(axisId, 0f)
              controller.setAxis(axisIndex, axisValue)
              axisIndex += 1
            }

            true
        }
      }
    }

  /** Notify the controller system that a device was added (e.g. from InputManager.InputDeviceListener).
    *
    * @param deviceId
    *   the Android device ID
    */
  def onDeviceAdded(deviceId: Int): Unit = lock.synchronized {
    addController(deviceId)
  }

  /** Notify the controller system that a device was removed.
    *
    * @param deviceId
    *   the Android device ID
    */
  def onDeviceRemoved(deviceId: Int): Unit = lock.synchronized {
    removeController(deviceId)
  }

  // ── Polling implementation ──────────────────────────────────────────

  private def getConnectedControllers(): Array[ControllerState] = lock.synchronized {
    controllerMap.values.filter(_.connected).map(_.toControllerState).toArray
  }

  private def pollController(index: Int): ControllerState = lock.synchronized {
    val controllers = controllerMap.values.filter(_.connected).toArray
    if (index < 0 || index >= controllers.length) ControllerState.Disconnected
    else controllers(index).toControllerState
  }

  // ── Controller discovery ────────────────────────────────────────────

  /** Scans for connected game controllers via InputDevice.getDeviceIds() using reflection.
    */
  private def gatherControllers(): Unit = lock.synchronized {
    try {
      val inputDeviceClass = Class.forName("android.view.InputDevice")
      val getDeviceIds     = inputDeviceClass.getMethod("getDeviceIds")
      val deviceIds        = getDeviceIds.invoke(null).asInstanceOf[Array[Int]] // scalafix:ok — Java reflection boundary

      val existingIds = controllerMap.keySet.toSet

      // Add new controllers
      deviceIds.foreach { deviceId =>
        if (!controllerMap.contains(deviceId)) {
          addController(deviceId)
        }
      }

      // Remove disconnected controllers
      val currentIds = deviceIds.toSet
      existingIds.foreach { id =>
        if (!currentIds.contains(id)) {
          removeController(id)
        }
      }
    } catch {
      // If Android APIs are not available (e.g. running on desktop JVM), silently ignore
      case _: ClassNotFoundException => ()
      case _: Exception              => ()
    }
  }

  /** Adds a controller for the given device ID if it is a game controller. Uses reflection to access Android InputDevice APIs.
    */
  private def addController(deviceId: Int): Unit =
    try {
      val inputDeviceClass = Class.forName("android.view.InputDevice")
      val getDevice        = inputDeviceClass.getMethod("getDevice", classOf[Int])
      val device           = getDevice.invoke(null, java.lang.Integer.valueOf(deviceId)) // scalafix:ok — Java reflection boundary

      if (device == null) () // scalafix:ok — Java reflection may return null
      else if (!isController(device)) ()
      else {
        val getName = inputDeviceClass.getMethod("getName")
        val name    = getName.invoke(device).asInstanceOf[String]

        val controller = TrackedController(deviceId, name, device)
        controllerMap.put(deviceId, controller)
      }
    } catch {
      // Ignore devices that can't be queried (matches original behavior)
      case _: Exception => ()
    }

  /** Removes the controller with the given device ID. */
  private def removeController(deviceId: Int): Unit =
    controllerMap.remove(deviceId).foreach { controller =>
      controller.connected = false
    }

  /** Checks whether an InputDevice is a game controller (joystick + gamepad or non-alphabetic keyboard, excluding fingerprint sensors). Uses reflection.
    */
  private def isController(device: AnyRef): Boolean =
    try {
      val cls                = device.getClass()
      val getSources         = cls.getMethod("getSources")
      val getKeyboardType    = cls.getMethod("getKeyboardType")
      val getName            = cls.getMethod("getName")
      val sources            = getSources.invoke(device).asInstanceOf[java.lang.Integer].intValue()
      val keyboardType       = getKeyboardType.invoke(device).asInstanceOf[java.lang.Integer].intValue()
      val deviceName         = getName.invoke(device).asInstanceOf[String]
      val isJoystick         = (sources & SOURCE_CLASS_JOYSTICK) == SOURCE_CLASS_JOYSTICK
      val isGamepadOrNonAlph = (sources & SOURCE_GAMEPAD) == SOURCE_GAMEPAD || keyboardType != KEYBOARD_TYPE_ALPHA
      isJoystick && isGamepadOrNonAlph && deviceName != "uinput-fpc"
    } catch {
      case _: Exception => false
    }

  /** Checks whether a keycode is a gamepad button. This mirrors KeyEvent.isGamepadButton() on Android.
    */
  private def isGamepadButton(keyCode: Int): Boolean =
    // Gamepad button range: KEYCODE_BUTTON_A(96) through KEYCODE_BUTTON_MODE(110)
    // Plus DPAD keys (19-22) and DPAD_CENTER(23)
    (keyCode >= 96 && keyCode <= 110) ||
      (keyCode >= 19 && keyCode <= 23) ||
      keyCode == 4 || // KEYCODE_BACK
      keyCode == 108 // KEYCODE_BUTTON_START (KEYCODE_MENU maps to 82 but not a gamepad button)

  // ── TrackedController ───────────────────────────────────────────────

  /** Mutable state for a single tracked Android controller. Accumulates button/axis state from events and produces [[ControllerState]] snapshots for polling.
    *
    * This replaces the original AndroidController class and AndroidControllerEvent event objects.
    */
  private class TrackedController(val deviceId: Int, val name: String, device: AnyRef) {

    var connected: Boolean = true

    /** The set of currently pressed button keycodes. */
    private val pressedButtons: mutable.Set[Int] = mutable.Set.empty

    /** The axis values, indexed by position in axisIds. */
    private val axisValues: Array[Float] = {
      val ids = discoverAxisIds(device)
      new Array[Float](ids.length)
    }

    /** The ordered list of axis IDs (Android MotionEvent.AXIS_* constants) for this controller. */
    val axisIds: Array[Int] = discoverAxisIds(device)

    /** Whether this controller has POV (hat) axes that should be mapped to dpad buttons. */
    val hasPovAxis: Boolean = {
      // Note: POV axes are removed from axisIds but we check the device's original ranges
      val rawIds = discoverRawAxisIds(device)
      rawIds.contains(AXIS_HAT_X) && rawIds.contains(AXIS_HAT_Y)
    }

    /** Whether this controller has trigger axes that should be mapped to trigger buttons. */
    val hasTriggerAxis: Boolean =
      useNewAxisLogic && discoverRawAxisIds(device).exists(_ == AXIS_LTRIGGER) && discoverRawAxisIds(device).exists(_ == AXIS_RTRIGGER)

    /** Current POV X value (for tracking changes). */
    var povX: Float = 0f

    /** Current POV Y value (for tracking changes). */
    var povY: Float = 0f

    /** Current left trigger value (for tracking changes). */
    var lTrigger: Float = 0f

    /** Current right trigger value (for tracking changes). */
    var rTrigger: Float = 0f

    /** Unique identifier for this controller instance. */
    private val uniqueId: String = java.util.UUID.randomUUID().toString

    def isButtonPressed(keyCode: Int): Boolean = pressedButtons.contains(keyCode)

    def setButtonPressed(keyCode: Int, pressed: Boolean): Unit =
      if (pressed) pressedButtons += keyCode
      else pressedButtons -= keyCode

    def setAxis(index: Int, value: Float): Unit =
      if (index >= 0 && index < axisValues.length) axisValues(index) = value

    def getAxis(index: Int): Float =
      if (index >= 0 && index < axisValues.length) axisValues(index) else 0f

    /** Produces a [[ControllerState]] snapshot of the current state.
      *
      * Because Android uses keycodes as button IDs (which can be up to 300), we create a buttons array large enough to cover all possible keycodes. This matches the original AndroidController which
      * returned maxButtonIndex = 300.
      */
    def toControllerState: ControllerState = {
      val maxButton  = 301 // see Android KeyEvent class, max button index is ~300
      val buttons    = new Array[Boolean](maxButton)
      val buttonVals = new Array[Float](maxButton)
      pressedButtons.foreach { keyCode =>
        if (keyCode >= 0 && keyCode < maxButton) {
          buttons(keyCode) = true
          buttonVals(keyCode) = 1.0f
        }
      }

      val axes = axisValues.clone()

      ControllerState(
        name = name,
        uniqueId = uniqueId,
        connected = connected,
        buttons = buttons,
        buttonValues = buttonVals,
        axes = axes,
        powerLevel = ControllerPowerLevel.Unknown
      )
    }

    /** Discovers axis IDs from the device's motion ranges using reflection. Applies the same filtering and re-ordering as the original AndroidController constructor:
      *   - Removes POV (hat) axes (they are mapped to dpad buttons)
      *   - If useNewAxisLogic: removes trigger axes, re-orders sticks to indices 0-3
      */
    private def discoverAxisIds(device: AnyRef): Array[Int] = {
      val rawIds = discoverRawAxisIds(device)
      val ids    = mutable.ArrayBuffer.from(rawIds)

      // Remove POV axes (mapped to dpad buttons)
      if (ids.contains(AXIS_HAT_X) && ids.contains(AXIS_HAT_Y)) {
        ids -= AXIS_HAT_X
        ids -= AXIS_HAT_Y
      }

      if (useNewAxisLogic) {
        // Remove trigger axes (mapped to trigger buttons)
        if (ids.contains(AXIS_LTRIGGER) && ids.contains(AXIS_RTRIGGER)) {
          ids -= AXIS_LTRIGGER
          ids -= AXIS_RTRIGGER
        }

        // Move left stick axes to indices 0-1
        if (ids.contains(AXIS_X) && ids.contains(AXIS_Y)) {
          ids -= AXIS_X
          ids -= AXIS_Y
          ids.insert(0, AXIS_Y)
          ids.insert(0, AXIS_X)
        }

        // Move right stick axes to indices 2-3
        if (ids.contains(AXIS_Z) && ids.contains(AXIS_RZ)) {
          ids -= AXIS_Z
          ids -= AXIS_RZ
          val insertAt = scala.math.min(2, ids.length)
          ids.insert(insertAt, AXIS_RZ)
          ids.insert(insertAt, AXIS_Z)
        }
      }

      ids.toArray
    }

    /** Gets the raw axis IDs from the device's motion ranges via reflection. */
    private def discoverRawAxisIds(device: AnyRef): Array[Int] =
      try {
        val cls              = device.getClass()
        val getMotionRanges  = cls.getMethod("getMotionRanges")
        val ranges           = getMotionRanges.invoke(device).asInstanceOf[java.util.List[?]]
        val motionRangeClass = Class.forName("android.view.InputDevice$MotionRange")
        val getAxis          = motionRangeClass.getMethod("getAxis")
        val getSource        = motionRangeClass.getMethod("getSource")
        val ids              = mutable.ArrayBuffer.empty[Int]

        val it = ranges.iterator()
        while (it.hasNext()) {
          val range  = it.next()
          val source = getSource.invoke(range).asInstanceOf[java.lang.Integer].intValue()
          if ((source & SOURCE_CLASS_JOYSTICK) != 0) {
            val axisId = getAxis.invoke(range).asInstanceOf[java.lang.Integer].intValue()
            ids += axisId
          }
        }
        ids.toArray
      } catch {
        case _: Exception => Array.empty[Int]
      }
  }
}
