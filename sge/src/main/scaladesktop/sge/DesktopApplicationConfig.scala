/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3ApplicationConfiguration.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3ApplicationConfiguration -> DesktopApplicationConfig
 *   Renames: GLEmulation enum stays local; ANGLE_GLES20 is the SGE default (not GL20)
 *   Convention: GLFW monitor query statics ported to companion defs over WindowingOps FFI (ISS-759); lazily boot the platform default windowing (DesktopWindowing.default) like initializeGlfw()
 *   Convention: Java-style setters -> public vars; batch setters kept as convenience methods
 *   Idiom: Nullable for optional fields; split packages
 *   Deleted (ISS-762, pre-0.1.0 API removal): glEmulation + the GLEmulation native-GL variants
 *     (GL20/GL30/GL31/GL32) + glesContextMajorVersion/MinorVersion + setOpenGLEmulation. SGE is
 *     ANGLE-always: the desktop backend never creates a native OpenGL context, so the original's
 *     GLEmulation/glesContext knobs only matter on the native-GL path that SGE does not have — the
 *     original's ANGLE branch (Lwjgl3Application.createGlfwWindow / initializeGlfw) ignores them too.
 *   Deleted (ISS-762): debugStream + enableGLDebugOutput. GL debug output requires a native GL
 *     context (Lwjgl3Application.java:594-597 throws on the ANGLE branch); SGE fails fast on
 *     debug=true at startup instead, so a separate debug stream is dead.
 *   Deleted (ISS-762): maxNetThreads. Networking is owned by java.net.http, whose HttpClient manages
 *     its own executor/thread pool — the knob was never read.
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.files.FileType
import sge.graphics.glutils.HdpiMode
import sge.platform.WindowingOps
import lowlevel.Nullable
import java.io.PrintStream

/** Full application configuration for desktop (JVM + Native) applications. Extends [[DesktopWindowConfig]] with application-level settings like audio, GL, and preferences.
  *
  * All fields are public vars with sensible defaults. Convenience methods are provided for common multi-field operations.
  */
class DesktopApplicationConfig extends DesktopWindowConfig {

  /** Whether to disable audio. If true, audio instances will be noop implementations. */
  var disableAudio: Boolean = false

  /** Audio device configuration. */
  var audioDeviceSimultaneousSources: Int = 16
  var audioDeviceBufferSize:          Int = 512
  var audioDeviceBufferCount:         Int = 9

  /** Color buffer bit depth (per channel). */
  var r: Int = 8
  var g: Int = 8
  var b: Int = 8
  var a: Int = 8

  /** Depth buffer bit depth. */
  var depth: Int = 16

  /** Stencil buffer bit depth. */
  var stencil: Int = 0

  /** MSAA sample count. 0 means disabled. */
  var samples: Int = 0

  /** Whether the framebuffer is transparent. Results may vary by OS and GPU. */
  var transparentFramebuffer: Boolean = false

  /** Polling rate during idle time in non-continuous rendering mode. Must be positive. */
  var idleFPS: Int = 60

  /** Target framerate. CPU sleeps as needed. 0 means no sleep. */
  var foregroundFPS: Int = 0

  /** Whether to pause the application when the window is minimized. */
  var pauseWhenMinimized: Boolean = true

  /** Whether to pause the application when the window loses focus. */
  var pauseWhenLostFocus: Boolean = false

  /** Preferences storage directory and file type. */
  var preferencesDirectory: String   = ".prefs/"
  var preferencesFileType:  FileType = FileType.External

  /** HDPI handling mode. See [[HdpiMode]] for details. */
  var hdpiMode: HdpiMode = HdpiMode.Logical

  /** Whether to enable OpenGL debug message callbacks. Unsupported under SGE's ANGLE-only backend: a `true` value fails fast at application startup (ISS-762). */
  var debug: Boolean = false

  /** Stream for error output. */
  var errorStream: PrintStream = System.err

  /** SGE extensions whose dependencies are loaded once at application startup, before the game listener's `create()` runs. */
  var extensions: Seq[SgeExtension] = Seq.empty

  // ---- convenience methods ----

  /** Sets the audio device configuration.
    * @param simultaneousSources
    *   maximum number of simultaneous sound sources (default 16)
    * @param bufferSize
    *   audio device buffer size in samples (default 512)
    * @param bufferCount
    *   audio device buffer count (default 9)
    */
  def setAudioConfig(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Unit = {
    audioDeviceSimultaneousSources = simultaneousSources
    audioDeviceBufferSize = bufferSize
    audioDeviceBufferCount = bufferCount
  }

  /** Sets the color, depth, stencil, and MSAA configuration.
    * @param r
    *   red bits (default 8)
    * @param g
    *   green bits (default 8)
    * @param b
    *   blue bits (default 8)
    * @param a
    *   alpha bits (default 8)
    * @param depth
    *   depth bits (default 16)
    * @param stencil
    *   stencil bits (default 0)
    * @param samples
    *   MSAA samples (default 0)
    */
  def setBackBufferConfig(r: Int, g: Int, b: Int, a: Int, depth: Int, stencil: Int, samples: Int): Unit = {
    this.r = r
    this.g = g
    this.b = b
    this.a = a
    this.depth = depth
    this.stencil = stencil
    this.samples = samples
  }

  /** Sets preferences storage location.
    * @param directory
    *   the directory to store preferences in (default ".prefs/")
    * @param fileType
    *   the file type for resolving the directory (default External)
    */
  def setPreferencesConfig(directory: String, fileType: FileType): Unit = {
    preferencesDirectory = directory
    preferencesFileType = fileType
  }

  /** Copies all configuration fields from another config. */
  def set(config: DesktopApplicationConfig): Unit = {
    super.setWindowConfiguration(config)
    disableAudio = config.disableAudio
    audioDeviceSimultaneousSources = config.audioDeviceSimultaneousSources
    audioDeviceBufferSize = config.audioDeviceBufferSize
    audioDeviceBufferCount = config.audioDeviceBufferCount
    r = config.r
    g = config.g
    b = config.b
    a = config.a
    depth = config.depth
    stencil = config.stencil
    samples = config.samples
    transparentFramebuffer = config.transparentFramebuffer
    idleFPS = config.idleFPS
    foregroundFPS = config.foregroundFPS
    pauseWhenMinimized = config.pauseWhenMinimized
    pauseWhenLostFocus = config.pauseWhenLostFocus
    preferencesDirectory = config.preferencesDirectory
    preferencesFileType = config.preferencesFileType
    hdpiMode = config.hdpiMode
    debug = config.debug
    errorStream = config.errorStream
    extensions = config.extensions
  }
}

object DesktopApplicationConfig {

  /** GL emulation modes for the desktop backend. SGE is ANGLE-always: the native-GL variants (GL20/GL30/GL31/GL32) were removed (ISS-762) because the desktop backend never creates a native OpenGL
    * context.
    */
  enum GLEmulation extends java.lang.Enum[GLEmulation] {

    /** ANGLE OpenGL ES 2.0 (Metal/Vulkan/D3D11 backend). This is the SGE default. */
    case ANGLE_GLES20
  }

  /** Creates a copy of the given configuration. */
  def copy(config: DesktopApplicationConfig): DesktopApplicationConfig = {
    val c = DesktopApplicationConfig()
    c.set(config)
    c
  }

  // ─── Pre-launch monitor / display-mode queries ────────────────────────
  // Faithful port of the Lwjgl3ApplicationConfiguration statics
  // (Lwjgl3ApplicationConfiguration.java:286-341). The original lazily boots GLFW via
  // Lwjgl3Application.initializeGlfw() before each query. SGE has no global GLFW, so we lazily
  // create + init the platform default WindowingOps (sge.platform.DesktopWindowing.default),
  // reusing a running application's windowing FFI when one is already installed in PlatformOps.
  // SGE naming: the original Java-style getX() statics become no-`get` accessors (project rule:
  // no Java-style getters); they carry FFI logic, so they are defs, not vars.
  //
  // Return types: the original statics DECLARE core Monitor/DisplayMode but covertly RETURN
  // handle-carrying Lwjgl3Monitor/Lwjgl3DisplayMode instances, which is what makes the canonical
  // recipe `config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode())` work —
  // setFullscreenMode/fullscreenMode recover the handle by downcast. SGE's core types are final
  // case classes (no covert subtype possible), so these queries return the desktop types
  // DesktopMonitor/DesktopDisplayMode directly: faithful to the original's actual runtime behavior,
  // and `config.fullscreenMode = Nullable(DesktopApplicationConfig.displayMode(monitor))` works
  // with no handle fabrication.

  /** Lazily obtains a windowing ops, reusing the running application's FFI or booting the platform default (equivalent to `Lwjgl3Application.initializeGlfw()`). */
  private def windowingOps: WindowingOps =
    Nullable(sge.platform.PlatformOps.windowing).fold {
      DesktopApplicationConfig.synchronized {
        Nullable(sge.platform.PlatformOps.windowing).fold {
          val ops = sge.platform.DesktopWindowing.default()
          ops.init()
          sge.platform.PlatformOps.windowing = ops
          ops
        }(identity)
      }
    }(identity)

  private def toDesktopMonitor(ops: WindowingOps, handle: Long): DesktopMonitor = {
    val (x, y) = ops.getMonitorPos(handle)
    DesktopMonitor(handle, x, y, ops.getMonitorName(handle))
  }

  private def toDesktopDisplayMode(handle: Long, mode: (Int, Int, Int, Int, Int, Int)): DesktopDisplayMode = {
    val (w, h, rr, rb, gb, bb) = mode
    DesktopDisplayMode(handle, w, h, rr, rb + gb + bb)
  }

  /** The connected monitors (original: static `Monitor[] getMonitors()`, actually returning `Lwjgl3Monitor[]`). */
  def monitors: Array[DesktopMonitor] = {
    val ops = windowingOps
    ops.monitors.map(h => toDesktopMonitor(ops, h))
  }

  /** The primary monitor (original: static `Monitor getPrimaryMonitor()`, actually returning an `Lwjgl3Monitor`). */
  def primaryMonitor: DesktopMonitor = {
    val ops = windowingOps
    toDesktopMonitor(ops, ops.primaryMonitor)
  }

  /** The currently active display mode of the primary monitor (original: static `DisplayMode getDisplayMode()`, actually returning an `Lwjgl3DisplayMode`). Suitable for
    * `config.fullscreenMode = Nullable(...)` directly.
    */
  def displayMode: DesktopDisplayMode = {
    val ops    = windowingOps
    val handle = ops.primaryMonitor
    toDesktopDisplayMode(handle, ops.getVideoMode(handle))
  }

  /** The currently active display mode of the given monitor (original: static `DisplayMode getDisplayMode(Monitor)`, actually returning an `Lwjgl3DisplayMode`). Suitable for
    * `config.fullscreenMode = Nullable(...)` directly.
    */
  def displayMode(monitor: DesktopMonitor): DesktopDisplayMode = {
    val ops = windowingOps
    toDesktopDisplayMode(monitor.monitorHandle, ops.getVideoMode(monitor.monitorHandle))
  }

  /** The available display modes of the primary monitor (original: static `DisplayMode[] getDisplayModes()`, actually returning `Lwjgl3DisplayMode[]`). */
  def displayModes: Array[DesktopDisplayMode] = {
    val ops    = windowingOps
    val handle = ops.primaryMonitor
    ops.getVideoModes(handle).map(m => toDesktopDisplayMode(handle, m))
  }

  /** The available display modes of the given monitor (original: static `DisplayMode[] getDisplayModes(Monitor)`, actually returning `Lwjgl3DisplayMode[]`). */
  def displayModes(monitor: DesktopMonitor): Array[DesktopDisplayMode] = {
    val ops = windowingOps
    ops.getVideoModes(monitor.monitorHandle).map(m => toDesktopDisplayMode(monitor.monitorHandle, m))
  }
}
