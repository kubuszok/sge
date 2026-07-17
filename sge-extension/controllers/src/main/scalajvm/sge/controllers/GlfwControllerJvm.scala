/*
 * Ported from gdx-controllers - https://github.com/libgdx/gdx-controllers
 * Licensed under the Apache License, Version 2.0
 *
 * JVM GLFW joystick/gamepad implementation using Panama FFM.
 *
 * init() makes libglfw available to SymbolLookup.loaderLookup() the same way
 * WindowingOpsJvm.apply does (multiarch NativeLibLoader classpath extraction +
 * System.load through the system linker), so gamepad polling works even before a
 * DesktopApplication has started. Symbol/handle resolution is deferred to the first
 * poll and degrades to Disconnected when GLFW is unavailable/uninitialized, so init()
 * is infallible and the per-frame auto-poll never throws — mirroring the Native
 * GlfwControllerNativeInit guarantees (link-time symbol availability +
 * GLFW_NOT_INITIALIZED answering glfwJoystickPresent == 0).
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package controllers

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import lowlevel.Nullable

/** Initializes the GlfwControllerBackend companion object's polling function to use actual GLFW FFI calls on JVM via Panama FFM downcall handles.
  *
  * This object's [[init]] method replaces the Disconnected default in [[GlfwControllerBackend]], mirroring GlfwControllerNativeInit on Scala Native.
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

  // ─── Linker ──────────────────────────────────────────────────────────

  private val linker: Linker = Linker.nativeLinker()

  // ─── Downcall handles ────────────────────────────────────────────────

  /** The seven GLFW joystick/gamepad downcall handles, resolved together once GLFW is loaded. Held in a single value so a poll either has all handles or (before/without GLFW) none — the latter
    * degrading to Disconnected instead of throwing (Native GLFW_NOT_INITIALIZED parity).
    */
  final private class GlfwJoystickHandles(
    // glfwJoystickPresent(int jid) → int
    val joystickPresent: MethodHandle,
    // glfwJoystickIsGamepad(int jid) → int
    val joystickIsGamepad: MethodHandle,
    // glfwGetGamepadState(int jid, GLFWgamepadstate* state) → int
    val getGamepadState: MethodHandle,
    // glfwGetJoystickName(int jid) → const char*
    val getJoystickName: MethodHandle,
    // glfwGetJoystickGUID(int jid) → const char*
    val getJoystickGUID: MethodHandle,
    // glfwGetJoystickAxes(int jid, int* count) → const float*
    val getJoystickAxes: MethodHandle,
    // glfwGetJoystickButtons(int jid, int* count) → const unsigned char*
    val getJoystickButtons: MethodHandle
  )

  private def sym(lookup: SymbolLookup, name: String): MemorySegment =
    lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"GLFW symbol not found: $name"))

  /** Resolves the seven joystick downcall handles from the application-loader symbol lookup. Returns empty when GLFW has not been System.load-ed into this process (no DesktopApplication started and
    * [[ensureGlfwLoaded]] could not extract the native provider) — so polling degrades to Disconnected rather than throwing an UnsatisfiedLinkError, mirroring the way the Native backend answers a
    * GLFW_NOT_INITIALIZED joystick query with glfwJoystickPresent == 0.
    */
  private def resolveHandles(): Nullable[GlfwJoystickHandles] =
    try {
      val lookup = SymbolLookup.loaderLookup()
      Nullable(
        new GlfwJoystickHandles(
          linker.downcallHandle(sym(lookup, "glfwJoystickPresent"), FunctionDescriptor.of(I, I)),
          linker.downcallHandle(sym(lookup, "glfwJoystickIsGamepad"), FunctionDescriptor.of(I, I)),
          linker.downcallHandle(sym(lookup, "glfwGetGamepadState"), FunctionDescriptor.of(I, I, P)),
          linker.downcallHandle(sym(lookup, "glfwGetJoystickName"), FunctionDescriptor.of(P, I)),
          linker.downcallHandle(sym(lookup, "glfwGetJoystickGUID"), FunctionDescriptor.of(P, I)),
          linker.downcallHandle(sym(lookup, "glfwGetJoystickAxes"), FunctionDescriptor.of(P, I, P)),
          linker.downcallHandle(sym(lookup, "glfwGetJoystickButtons"), FunctionDescriptor.of(P, I, P))
        )
      )
    } catch {
      case _: LinkageError => Nullable.empty
    }

  /** Handles are resolved lazily on the first poll — never in the object initializer — so that touching [[GlfwControllerJvmInit]] can never poison its class with an UnsatisfiedLinkError escaping
    * clinit, and so that [[init]] (which loads GLFW first) has run before symbols are looked up.
    */
  private lazy val handles: Nullable[GlfwJoystickHandles] = resolveHandles()

  // ─── Public API ──────────────────────────────────────────────────────

  /** Makes libglfw available to `SymbolLookup.loaderLookup()` the same way WindowingOpsJvm.apply does: extract the vendored native library via multiarch's classpath NativeLibLoader, then System.load
    * it through the system's dynamic linker (which resolves platform framework dependencies). Idempotent — a second System.load of the same library in the same class loader is a no-op, so this is
    * safe to call alongside a DesktopApplication that already loaded GLFW for windowing. Infallible: in a headless/CI process without the desktop native provider on the classpath, the failure is
    * swallowed and polling simply degrades to Disconnected (Native GLFW_NOT_INITIALIZED parity).
    */
  private def ensureGlfwLoaded(): Unit =
    try {
      val found = multiarch.core.NativeLibLoader.load("glfw")
      System.load(found.toAbsolutePath.toString)
    } catch {
      // No GLFW native provider on the classpath (headless test/CI). Symbol resolution
      // then finds nothing and pollController degrades to Disconnected — never throws.
      case _: Throwable => ()
    }

  /** Call this once at startup to wire the GlfwControllerBackend to real GLFW calls. Infallible and safe to call in any bootstrap order: it loads GLFW itself (see [[ensureGlfwLoaded]]) and only
    * installs the polling function into the [[GlfwControllerBackend]] seam, exactly like GlfwControllerNativeInit.init() on Scala Native.
    */
  def init(): Unit = {
    ensureGlfwLoaded()
    GlfwControllerBackend.pollControllerImpl = pollController
  }

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

  /** Per-frame poll entry point installed into [[GlfwControllerBackend.pollControllerImpl]]. Degrades to Disconnected when GLFW is unavailable/uninitialized (no resolved handles) so the auto-poll
    * hook never throws, mirroring GlfwControllerNative.pollController's glfwJoystickPresent == 0 short-circuit.
    */
  private def pollController(index: Int): ControllerState =
    handles.fold(ControllerState.Disconnected)(h => pollController(h, index))

  private def pollController(h: GlfwJoystickHandles, index: Int): ControllerState =
    if (index < 0 || index > 15) ControllerState.Disconnected
    else if (invokeIntInt(h.joystickPresent, index) == 0) ControllerState.Disconnected
    else {
      val namePtr = invokeIntPtr(h.getJoystickName, index)
      val name    =
        if (namePtr == MemorySegment.NULL || namePtr.address() == 0L) ""
        else namePtr.reinterpret(256L).getString(0L)

      val guidPtr = invokeIntPtr(h.getJoystickGUID, index)
      val guid    =
        if (guidPtr == MemorySegment.NULL || guidPtr.address() == 0L) ""
        else guidPtr.reinterpret(256L).getString(0L)

      // Try gamepad API first (normalized Xbox layout)
      if (invokeIntInt(h.joystickIsGamepad, index) != 0) {
        val arena = Arena.ofConfined()
        try {
          val stateBytes = arena.allocate(GAMEPAD_STATE_SIZE)
          if (invokeIntPtrInt(h.getGamepadState, index, stateBytes) != 0) {
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
            ControllerState.fromDigitalButtons(name, GlfwControllerBackend.uniqueIdFor(guid, index), connected = true, buttons, axes, ControllerPowerLevel.Unknown)
          } else {
            // Gamepad state failed, fall back to raw joystick
            pollRawJoystick(h, index, name, guid)
          }
        } finally arena.close()
      } else {
        // Not a gamepad, use raw joystick API
        pollRawJoystick(h, index, name, guid)
      }
    }

  private def pollRawJoystick(h: GlfwJoystickHandles, index: Int, name: String, guid: String): ControllerState = {
    val arena = Arena.ofConfined()
    try {
      val axisCountSeg   = arena.allocate(I)
      val buttonCountSeg = arena.allocate(I)

      val axesPtr    = invokeIntPtrPtr(h.getJoystickAxes, index, axisCountSeg)
      val buttonsPtr = invokeIntPtrPtr(h.getJoystickButtons, index, buttonCountSeg)

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

      ControllerState.fromDigitalButtons(name, GlfwControllerBackend.uniqueIdFor(guid, index), connected = true, buttons, axes, ControllerPowerLevel.Unknown)
    } finally arena.close()
  }
}
