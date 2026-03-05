/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original platform abstraction for OpenGL ES (ANGLE)
 *   Convention: trait defines FFI contract; JVM uses Panama, Native uses @extern
 *   Convention: will be code-generated for GL20/30/31/32 (~2,274 LOC per platform)
 *   Idiom: split packages
 */
package sge
package platform

/** OpenGL ES operations (ANGLE). Defines the FFI contract for loading the ANGLE shared library and making GL context current.
  *
  * The actual GL20/GL30/GL31/GL32 bindings are code-generated from a function signature definition file. This trait covers only the context management operations that aren't part of the GL spec
  * itself.
  *
  * Platform implementations:
  *   - JVM: Panama downcall handles to ANGLE (libEGL + libGLESv2)
  *   - Native: @extern C FFI to ANGLE
  *   - Browser: WebGL2 via scalajs-dom (separate implementation, not this trait)
  *   - Android: android.opengl.EGL14 + GLES20/30 (separate implementation)
  */
private[sge] trait GlOps {

  // ─── EGL context management ────────────────────────────────────────────

  /** Initializes the EGL display and creates a rendering context.
    * @param windowHandle
    *   the native window handle from the windowing system
    * @param r
    *   red bits
    * @param g
    *   green bits
    * @param b
    *   blue bits
    * @param a
    *   alpha bits
    * @param depth
    *   depth buffer bits
    * @param stencil
    *   stencil buffer bits
    * @param samples
    *   MSAA sample count (0 = disabled)
    * @return
    *   a native EGL context handle, or 0 on failure
    */
  def createContext(
    windowHandle: Long,
    r:            Int,
    g:            Int,
    b:            Int,
    a:            Int,
    depth:        Int,
    stencil:      Int,
    samples:      Int
  ): Long

  /** Destroys the EGL context and releases resources. */
  def destroyContext(contextHandle: Long): Unit

  /** Makes the EGL context current on the calling thread. */
  def makeCurrent(contextHandle: Long): Unit

  /** Swaps the EGL surface buffers (presents the rendered frame). */
  def swapEglBuffers(contextHandle: Long): Unit

  /** Sets the swap interval (0 = no vsync, 1 = vsync). */
  def setSwapInterval(interval: Int): Unit

  /** Loads a GL function pointer by name. Used by the code-generated GL20/30/31/32 bindings.
    * @param name
    *   the GL function name (e.g. "glClear", "glDrawArrays")
    * @return
    *   the function pointer address, or 0 if not found
    */
  def getProcAddress(name: String): Long
}
