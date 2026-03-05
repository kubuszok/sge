/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3ApplicationConfiguration.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3ApplicationConfiguration -> DesktopApplicationConfig
 *   Renames: GLEmulation enum stays local; ANGLE_GLES20 is the SGE default (not GL20)
 *   Convention: GLFW monitor query static methods deferred until windowing FFI is available
 *   Convention: Java-style setters -> public vars; batch setters kept as convenience methods
 *   Idiom: Nullable for optional fields; split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.files.FileType
import sge.graphics.glutils.HdpiMode
import java.io.PrintStream

/** Full application configuration for desktop (JVM + Native) applications. Extends [[DesktopWindowConfig]] with application-level settings like audio, GL, and preferences.
  *
  * All fields are public vars with sensible defaults. Convenience methods are provided for common multi-field operations.
  */
class DesktopApplicationConfig extends DesktopWindowConfig {

  /** Whether to disable audio. If true, audio instances will be noop implementations. */
  var disableAudio: Boolean = false

  /** Maximum number of threads for network requests. */
  var maxNetThreads: Int = Int.MaxValue

  /** Audio device configuration. */
  var audioDeviceSimultaneousSources: Int = 16
  var audioDeviceBufferSize:          Int = 512
  var audioDeviceBufferCount:         Int = 9

  /** Which GL emulation mode to use. SGE defaults to ANGLE (OpenGL ES). */
  var glEmulation: DesktopApplicationConfig.GLEmulation = DesktopApplicationConfig.GLEmulation.ANGLE_GLES20

  /** OpenGL ES context version (major.minor). Default 3.2 for maximum ES 3.0 feature coverage. */
  var glesContextMajorVersion: Int = 3
  var glesContextMinorVersion: Int = 2

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

  /** Whether to enable OpenGL debug message callbacks. */
  var debug: Boolean = false

  /** Stream for debug output. */
  var debugStream: PrintStream = System.err

  /** Stream for error output. */
  var errorStream: PrintStream = System.err

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

  /** Sets which GL emulation version to use.
    * @param glVersion
    *   which GL emulation to use
    * @param majorVersion
    *   OpenGL ES major version (default 3)
    * @param minorVersion
    *   OpenGL ES minor version (default 2)
    */
  def setOpenGLEmulation(
    glVersion:    DesktopApplicationConfig.GLEmulation,
    majorVersion: Int,
    minorVersion: Int
  ): Unit = {
    glEmulation = glVersion
    glesContextMajorVersion = majorVersion
    glesContextMinorVersion = minorVersion
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

  /** Enables GL debug message callbacks.
    * @param enable
    *   whether to enable debug output
    * @param outputStream
    *   the stream for debug messages (e.g. System.err)
    */
  def enableGLDebugOutput(enable: Boolean, outputStream: PrintStream): Unit = {
    debug = enable
    debugStream = outputStream
  }

  /** Copies all configuration fields from another config. */
  def set(config: DesktopApplicationConfig): Unit = {
    super.setWindowConfiguration(config)
    disableAudio = config.disableAudio
    maxNetThreads = config.maxNetThreads
    audioDeviceSimultaneousSources = config.audioDeviceSimultaneousSources
    audioDeviceBufferSize = config.audioDeviceBufferSize
    audioDeviceBufferCount = config.audioDeviceBufferCount
    glEmulation = config.glEmulation
    glesContextMajorVersion = config.glesContextMajorVersion
    glesContextMinorVersion = config.glesContextMinorVersion
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
    debugStream = config.debugStream
    errorStream = config.errorStream
  }
}

object DesktopApplicationConfig {

  /** GL emulation modes for the desktop backend. SGE uses ANGLE by default. */
  enum GLEmulation extends java.lang.Enum[GLEmulation] {

    /** ANGLE OpenGL ES 2.0 (Metal/Vulkan/D3D11 backend). This is the SGE default. */
    case ANGLE_GLES20

    /** Native OpenGL 2.0 (legacy, for systems without ANGLE). */
    case GL20

    /** Native OpenGL 3.0+ context. */
    case GL30

    /** Native OpenGL 3.1+ context. */
    case GL31

    /** Native OpenGL 3.2+ context. */
    case GL32
  }

  /** Creates a copy of the given configuration. */
  def copy(config: DesktopApplicationConfig): DesktopApplicationConfig = {
    val c = DesktopApplicationConfig()
    c.set(config)
    c
  }

  // Monitor query methods (getDisplayMode, getMonitors, etc.) are deferred
  // until the windowing FFI layer is available. They require GLFW/SDL3 calls.
}
