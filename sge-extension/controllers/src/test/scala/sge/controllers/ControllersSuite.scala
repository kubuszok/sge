/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

import scala.collection.mutable.ArrayBuffer

class ControllersSuite extends munit.FunSuite {

  /** A test [[ControllerOps]] that returns configurable controller states. */
  private class TestControllerOps extends ControllerOps {
    var states: Array[ControllerState] = Array.empty

    override def maxControllers: Int = 4

    override def getConnectedControllers(): Array[ControllerState] = states

    override def pollController(index: Int): ControllerState =
      if (index >= 0 && index < states.length) states(index) else ControllerState.Disconnected
  }

  private def makeState(
    name:       String = "TestPad",
    uniqueId:   String = "test-001",
    numButtons: Int = 15,
    numAxes:    Int = 6
  ): ControllerState =
    ControllerState(
      name = name,
      uniqueId = uniqueId,
      connected = true,
      buttons = Array.fill(numButtons)(false),
      axes = Array.fill(numAxes)(0f),
      powerLevel = ControllerPowerLevel.Unknown
    )

  test("Controllers throws when not initialized") {
    Controllers.dispose() // Ensure clean state
    intercept[IllegalStateException] {
      Controllers.getControllers
    }
  }

  test("Controllers.initialize and dispose lifecycle") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    Controllers.initialize(manager)
    assertEquals(Controllers.getControllers.size, 0)
    Controllers.dispose()
    intercept[IllegalStateException] {
      Controllers.getControllers
    }
  }

  test("DefaultControllerManager detects new controller connection") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    val events  = ArrayBuffer.empty[String]

    manager.addListener(new ControllerAdapter {
      override def connected(controller: Controller): Unit =
        events += s"connected:${controller.name}"
    })

    // First poll: no controllers
    manager.poll()
    assertEquals(manager.getControllers.size, 0)

    // Second poll: one controller appears
    ops.states = Array(makeState())
    manager.poll()
    assertEquals(manager.getControllers.size, 1)
    assertEquals(events.toList, List("connected:TestPad"))
  }

  test("DefaultControllerManager detects controller disconnection") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    val events  = ArrayBuffer.empty[String]

    manager.addListener(
      new ControllerAdapter {
        override def connected(controller: Controller): Unit =
          events += s"connected:${controller.name}"
        override def disconnected(controller: Controller): Unit =
          events += s"disconnected:${controller.name}"
      }
    )

    // Connect
    ops.states = Array(makeState())
    manager.poll()
    assertEquals(manager.getControllers.size, 1)

    // Disconnect
    ops.states = Array.empty
    manager.poll()
    assertEquals(manager.getControllers.size, 0)
    assertEquals(events.toList, List("connected:TestPad", "disconnected:TestPad"))
  }

  test("DefaultControllerManager fires button events") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    val events  = ArrayBuffer.empty[String]

    manager.addListener(
      new ControllerAdapter {
        override def buttonDown(controller: Controller, buttonCode: Int): Boolean = {
          events += s"buttonDown:$buttonCode"
          false
        }
        override def buttonUp(controller: Controller, buttonCode: Int): Boolean = {
          events += s"buttonUp:$buttonCode"
          false
        }
      }
    )

    // Connect controller
    ops.states = Array(makeState())
    manager.poll()

    // Press button 0
    val pressed = makeState()
    pressed.buttons(0) = true
    ops.states = Array(pressed)
    manager.poll()
    assertEquals(events.toList, List("buttonDown:0"))

    // Release button 0
    events.clear()
    ops.states = Array(makeState())
    manager.poll()
    assertEquals(events.toList, List("buttonUp:0"))
  }

  test("DefaultControllerManager fires axis events") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    val events  = ArrayBuffer.empty[String]

    manager.addListener(
      new ControllerAdapter {
        override def axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean = {
          events += s"axisMoved:$axisCode:$value"
          false
        }
      }
    )

    // Connect controller
    ops.states = Array(makeState())
    manager.poll()

    // Move axis 0
    val moved = makeState()
    moved.axes(0) = 0.75f
    ops.states = Array(moved)
    manager.poll()
    assertEquals(events.toList, List("axisMoved:0:0.75"))
  }

  test("DefaultControllerManager tracks current controller") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    Controllers.initialize(manager)

    // No controller yet
    assert(Controllers.current.isEmpty, "No current controller expected")

    // Connect
    ops.states = Array(makeState())
    manager.poll()
    assert(Controllers.current.isDefined, "Current controller should be set after connect")

    // Disconnect
    ops.states = Array.empty
    manager.poll()
    assert(Controllers.current.isEmpty, "Current controller should be empty after disconnect")

    Controllers.dispose()
  }

  test("DefaultControllerManager handles multiple controllers") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)

    ops.states = Array(
      makeState(name = "Pad1", uniqueId = "pad-1"),
      makeState(name = "Pad2", uniqueId = "pad-2")
    )
    manager.poll()
    assertEquals(manager.getControllers.size, 2)

    // Disconnect first, keep second
    ops.states = Array(makeState(name = "Pad2", uniqueId = "pad-2"))
    manager.poll()
    assertEquals(manager.getControllers.size, 1)
    assertEquals(manager.getControllers.head.name, "Pad2")
  }

  test("ControllerAdapter methods return false by default") {
    val adapter = ControllerAdapter()
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    ops.states = Array(makeState())
    manager.poll()
    val controller = manager.getControllers.head
    assertEquals(adapter.buttonDown(controller, 0), false)
    assertEquals(adapter.buttonUp(controller, 0), false)
    assertEquals(adapter.axisMoved(controller, 0, 0.5f), false)
  }

  test("ControllerPowerLevel enum values exist") {
    val values = ControllerPowerLevel.values
    assertEquals(values.length, 6)
    assert(values.contains(ControllerPowerLevel.Unknown))
    assert(values.contains(ControllerPowerLevel.Empty))
    assert(values.contains(ControllerPowerLevel.Low))
    assert(values.contains(ControllerPowerLevel.Medium))
    assert(values.contains(ControllerPowerLevel.Full))
    assert(values.contains(ControllerPowerLevel.Wired))
  }

  test("PolledController reports correct state") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)

    val state = makeState(numButtons = 4, numAxes = 2)
    state.buttons(1) = true
    state.axes(0) = -0.5f
    ops.states = Array(state)
    manager.poll()

    val controller = manager.getControllers.head
    assertEquals(controller.getButton(0), false)
    assertEquals(controller.getButton(1), true)
    assertEquals(controller.getAxis(0), -0.5f)
    assertEquals(controller.getAxis(1), 0f)
    assertEquals(controller.isConnected, true)
    assertEquals(controller.name, "TestPad")
    assertEquals(controller.minButtonIndex, 0)
    assertEquals(controller.maxButtonIndex, 3)
    assertEquals(controller.axisCount, 2)
  }

  test("PolledController.getButton returns false for out-of-range index") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    ops.states = Array(makeState(numButtons = 2))
    manager.poll()

    val controller = manager.getControllers.head
    assertEquals(controller.getButton(-1), false)
    assertEquals(controller.getButton(99), false)
  }

  test("PolledController.getAxis returns 0 for out-of-range index") {
    val ops     = TestControllerOps()
    val manager = DefaultControllerManager(ops)
    ops.states = Array(makeState(numAxes = 2))
    manager.poll()

    val controller = manager.getControllers.head
    assertEquals(controller.getAxis(-1), 0f)
    assertEquals(controller.getAxis(99), 0f)
  }

  test("Controller.PlayerIdxUnset is -1") {
    assertEquals(Controller.PlayerIdxUnset, -1)
  }

  test("remove and clear listeners") {
    val ops      = TestControllerOps()
    val manager  = DefaultControllerManager(ops)
    val listener = ControllerAdapter()
    manager.addListener(listener)
    assertEquals(manager.getListeners.size, 1)
    manager.removeListener(listener)
    assertEquals(manager.getListeners.size, 0)

    manager.addListener(listener)
    manager.addListener(ControllerAdapter())
    assertEquals(manager.getListeners.size, 2)
    manager.clearListeners()
    assertEquals(manager.getListeners.size, 0)
  }
}
