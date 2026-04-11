/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * JVM GLFW joystick/gamepad implementation using Panama FFM.
 * GLFW is already loaded by WindowingOpsJvm at application startup,
 * so symbols are available via SymbolLookup.loaderLookup().
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle

/** Initializes the GlfwControllerBackend companion object's polling function to use actual GLFW FFI calls on JVM via Panama FFM downcall handles.
  *
  * This object's [[init]] method replaces the stub implementation, mirroring GlfwControllerNativeInit on Scala Native.
  */
object GlfwControllerJvmInit {

  // GLFWgamepadstate layout:
  //   unsigned char buttons[15]  → 15 bytes
  //   1 byte padding (alignment)
  //   float axes[6]              → 24 bytes
  //   Total: 40 bytes
  private val GAMEPAD_STATE_SIZE = 40L
  private val BUTTONS_OFFSET     = 0L
  private val AXES_OFFSET        = 16L // 15 bytes buttons + 1 padding byte

  // ─── Layout aliases ──────────────────────────────────────────────────

  private val I: ValueLayout.OfInt   = JAVA_INT
  private val F: ValueLayout.OfFloat = JAVA_FLOAT
  private val B: ValueLayout.OfByte  = JAVA_BYTE
  private val P: AddressLayout       = ADDRESS

  // ─── Symbol lookup and linker ────────────────────────────────────────

  private val linker: Linker       = Linker.nativeLinker()
  private val lookup: SymbolLookup = SymbolLookup.loaderLookup()

  private def sym(name: String): MemorySegment =
    lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GLFW symbol not found: $name"))

  // ─── Downcall handles ────────────────────────────────────────────────

  // glfwJoystickPresent(int jid) → int
  private val hJoystickPresent: MethodHandle =
    linker.downcallHandle(sym("glfwJoystickPresent"), FunctionDescriptor.of(I, I))

  // glfwJoystickIsGamepad(int jid) → int
  private val hJoystickIsGamepad: MethodHandle =
    linker.downcallHandle(sym("glfwJoystickIsGamepad"), FunctionDescriptor.of(I, I))

  // glfwGetGamepadState(int jid, GLFWgamepadstate* state) → int
  private val hGetGamepadState: MethodHandle =
    linker.downcallHandle(sym("glfwGetGamepadState"), FunctionDescriptor.of(I, I, P))

  // glfwGetJoystickName(int jid) → const char*
  private val hGetJoystickName: MethodHandle =
    linker.downcallHandle(sym("glfwGetJoystickName"), FunctionDescriptor.of(P, I))

  // glfwGetJoystickGUID(int jid) → const char*
  private val hGetJoystickGUID: MethodHandle =
    linker.downcallHandle(sym("glfwGetJoystickGUID"), FunctionDescriptor.of(P, I))

  // glfwGetJoystickAxes(int jid, int* count) → const float*
  private val hGetJoystickAxes: MethodHandle =
    linker.downcallHandle(sym("glfwGetJoystickAxes"), FunctionDescriptor.of(P, I, P))

  // glfwGetJoystickButtons(int jid, int* count) → const unsigned char*
  private val hGetJoystickButtons: MethodHandle =
    linker.downcallHandle(sym("glfwGetJoystickButtons"), FunctionDescriptor.of(P, I, P))

  // ─── Public API ──────────────────────────────────────────────────────

  /** Call this once at startup to wire the GlfwControllerBackend to real GLFW calls. */
  def init(): Unit =
    GlfwControllerBackend.pollControllerImpl = pollController

  // ─── Polling implementation ──────────────────────────────────────────

  /** Invoke a MethodHandle with a single int argument and return the int result. */
  private def invokeIntInt(mh: MethodHandle, arg: Int): Int =
    mh.invokeWithArguments(java.lang.Integer.valueOf(arg)).asInstanceOf[java.lang.Integer].intValue()

  /** Invoke a MethodHandle with a single int argument and return a MemorySegment result. */
  private def invokeIntPtr(mh: MethodHandle, arg: Int): MemorySegment =
    mh.invokeWithArguments(java.lang.Integer.valueOf(arg)).asInstanceOf[MemorySegment]

  /** Invoke a MethodHandle with (int, MemorySegment) arguments and return the int result. */
  private def invokeIntPtrInt(mh: MethodHandle, arg0: Int, arg1: MemorySegment): Int =
    mh.invokeWithArguments(java.lang.Integer.valueOf(arg0), arg1).asInstanceOf[java.lang.Integer].intValue()

  /** Invoke a MethodHandle with (int, MemorySegment) arguments and return a MemorySegment result. */
  private def invokeIntPtrPtr(mh: MethodHandle, arg0: Int, arg1: MemorySegment): MemorySegment =
    mh.invokeWithArguments(java.lang.Integer.valueOf(arg0), arg1).asInstanceOf[MemorySegment]

  private def pollController(index: Int): ControllerState =
    if (index < 0 || index > 15) ControllerState.Disconnected
    else if (invokeIntInt(hJoystickPresent, index) == 0) ControllerState.Disconnected
    else {
      val namePtr = invokeIntPtr(hGetJoystickName, index)
      val name    =
        if (namePtr == MemorySegment.NULL || namePtr.address() == 0L) ""
        else namePtr.reinterpret(256L).getString(0L)

      val guidPtr = invokeIntPtr(hGetJoystickGUID, index)
      val guid    =
        if (guidPtr == MemorySegment.NULL || guidPtr.address() == 0L) ""
        else guidPtr.reinterpret(256L).getString(0L)

      // Try gamepad API first (normalized Xbox layout)
      if (invokeIntInt(hJoystickIsGamepad, index) != 0) {
        val arena = Arena.ofConfined()
        try {
          val stateBytes = arena.allocate(GAMEPAD_STATE_SIZE)
          if (invokeIntPtrInt(hGetGamepadState, index, stateBytes) != 0) {
            val buttons = new Array[Boolean](15)
            var bi      = 0
            while (bi < 15) {
              buttons(bi) = stateBytes.get(B, BUTTONS_OFFSET + bi.toLong) != 0.toByte
              bi += 1
            }
            val axes = new Array[Float](6)
            var ai   = 0
            while (ai < 6) {
              axes(ai) = stateBytes.get(F, AXES_OFFSET + ai.toLong * 4L)
              ai += 1
            }
            ControllerState.fromDigitalButtons(name, guid, connected = true, buttons, axes, ControllerPowerLevel.Unknown)
          } else {
            // Gamepad state failed, fall back to raw joystick
            pollRawJoystick(index, name, guid)
          }
        } finally arena.close()
      } else {
        // Not a gamepad, use raw joystick API
        pollRawJoystick(index, name, guid)
      }
    }

  private def pollRawJoystick(index: Int, name: String, guid: String): ControllerState = {
    val arena = Arena.ofConfined()
    try {
      val axisCountSeg   = arena.allocate(I)
      val buttonCountSeg = arena.allocate(I)

      val axesPtr    = invokeIntPtrPtr(hGetJoystickAxes, index, axisCountSeg)
      val buttonsPtr = invokeIntPtrPtr(hGetJoystickButtons, index, buttonCountSeg)

      val axisCount   = axisCountSeg.get(I, 0L)
      val buttonCount = buttonCountSeg.get(I, 0L)

      // Reinterpret the returned pointers to cover the full element range
      val axesSeg    = axesPtr.reinterpret(axisCount.toLong * 4L)
      val buttonsSeg = buttonsPtr.reinterpret(buttonCount.toLong)

      val axes = new Array[Float](axisCount)
      var ai   = 0
      while (ai < axisCount) {
        axes(ai) = axesSeg.get(F, ai.toLong * 4L)
        ai += 1
      }

      val buttons = new Array[Boolean](buttonCount)
      var bi      = 0
      while (bi < buttonCount) {
        buttons(bi) = buttonsSeg.get(B, bi.toLong) != 0.toByte
        bi += 1
      }

      ControllerState.fromDigitalButtons(name, guid, connected = true, buttons, axes, ControllerPowerLevel.Unknown)
    } finally arena.close()
  }
}
