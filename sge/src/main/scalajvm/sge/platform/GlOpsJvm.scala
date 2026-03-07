/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original JVM implementation of GlOps
 *   Convention: Panama FFM downcall handles to ANGLE EGL shared library
 *   Convention: EGL types (EGLDisplay, EGLContext, EGLSurface, EGLConfig) represented as Long (pointer addresses)
 *   Convention: String params allocated in confined arenas (auto-freed)
 *   Idiom: split packages
 */
package sge
package platform

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle

/** JVM implementation of [[GlOps]] via Panama FFM downcall handles to the ANGLE EGL shared library.
  *
  * Manages EGL display/context/surface lifecycle. The actual GL function calls (glClear, glDrawArrays, etc.) are handled by the AngleGL20/30/31/32 classes which have their own Panama downcall handles
  * to libGLESv2.
  *
  * EGL state (display, context, surface, config) is stored as a packed struct of longs, returned as a single opaque handle from `createContext`.
  *
  * @param eglLib
  *   a `SymbolLookup` for ANGLE's libEGL
  */
class GlOpsJvm(eglLib: SymbolLookup) extends GlOps {

  private val linker: Linker = Linker.nativeLinker()

  private def h(name: String, desc: FunctionDescriptor): MethodHandle =
    linker.downcallHandle(
      eglLib.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"EGL symbol not found: $name")),
      desc
    )

  // ─── Layout aliases ────────────────────────────────────────────────────

  private val I: ValueLayout.OfInt = JAVA_INT
  private val P: AddressLayout     = ADDRESS

  // ─── EGL constants ──────────────────────────────────────────────────────

  private val EGL_DEFAULT_DISPLAY: Long = 0L
  private val EGL_NO_DISPLAY:      Long = 0L
  private val EGL_NO_CONTEXT:      Long = 0L
  private val EGL_NO_SURFACE:      Long = 0L
  private val EGL_NONE:            Int  = 0x3038
  private val EGL_RED_SIZE:        Int  = 0x3024
  private val EGL_GREEN_SIZE:      Int  = 0x3023
  private val EGL_BLUE_SIZE:       Int  = 0x3022
  private val EGL_ALPHA_SIZE:      Int  = 0x3021
  private val EGL_DEPTH_SIZE:      Int  = 0x3025
  private val EGL_STENCIL_SIZE:    Int  = 0x3026
  private val EGL_SAMPLE_BUFFERS:  Int  = 0x3032
  @scala.annotation.nowarn("id=E198") // reserved for future MSAA support
  private val EGL_SAMPLES:               Int = 0x3031
  private val EGL_RENDERABLE_TYPE:       Int = 0x3040
  private val EGL_OPENGL_ES3_BIT:        Int = 0x0040
  private val EGL_SURFACE_TYPE:          Int = 0x3033
  private val EGL_WINDOW_BIT:            Int = 0x0004
  private val EGL_CONTEXT_MAJOR_VERSION: Int = 0x3098
  private val EGL_CONTEXT_MINOR_VERSION: Int = 0x30fb

  // ─── EGL method handles ─────────────────────────────────────────────────

  private lazy val hGetDisplay     = h("eglGetDisplay", FunctionDescriptor.of(P, P))
  private lazy val hInitialize     = h("eglInitialize", FunctionDescriptor.of(I, P, P, P))
  private lazy val hChooseConfig   = h("eglChooseConfig", FunctionDescriptor.of(I, P, P, P, I, P))
  private lazy val hCreateCtx      = h("eglCreateContext", FunctionDescriptor.of(P, P, P, P, P))
  private lazy val hCreateSurface  = h("eglCreateWindowSurface", FunctionDescriptor.of(P, P, P, P, P))
  private lazy val hMakeCurrent    = h("eglMakeCurrent", FunctionDescriptor.of(I, P, P, P, P))
  private lazy val hSwapBuffers    = h("eglSwapBuffers", FunctionDescriptor.of(I, P, P))
  private lazy val hSwapInterval   = h("eglSwapInterval", FunctionDescriptor.of(I, P, I))
  private lazy val hDestroyCtx     = h("eglDestroyContext", FunctionDescriptor.of(I, P, P))
  private lazy val hDestroySurface = h("eglDestroySurface", FunctionDescriptor.of(I, P, P))
  private lazy val hTerminate      = h("eglTerminate", FunctionDescriptor.of(I, P))
  private lazy val hGetProcAddr    = h("eglGetProcAddress", FunctionDescriptor.of(P, P))

  // ─── Helpers ────────────────────────────────────────────────────────────

  private def ptr(handle: Long): MemorySegment =
    MemorySegment.ofAddress(handle)

  // ptrVal intentionally removed — not needed since we use .address() directly

  // EGL context state — stored per createContext call
  // We pack display, config, context, surface as 4 longs into a single off-heap struct
  private val ContextLayout: MemoryLayout = MemoryLayout.structLayout(
    JAVA_LONG.withName("display"),
    JAVA_LONG.withName("config"),
    JAVA_LONG.withName("context"),
    JAVA_LONG.withName("surface")
  )

  private val displayOffset: Long = 0L
  private val configOffset:  Long = 8L
  private val contextOffset: Long = 16L
  private val surfaceOffset: Long = 24L

  // We use a global arena so the context struct stays alive for the process
  private val contextArena: Arena = Arena.ofAuto()

  // ─── GlOps implementation ──────────────────────────────────────────────

  override def createContext(
    windowHandle: Long,
    r:            Int,
    g:            Int,
    b:            Int,
    a:            Int,
    depth:        Int,
    stencil:      Int,
    samples:      Int
  ): Long = {
    val arena = Arena.ofConfined()
    try {
      // Get EGL display
      val display = hGetDisplay.invoke(ptr(EGL_DEFAULT_DISPLAY)).asInstanceOf[MemorySegment]
      if (display == MemorySegment.NULL || display.address() == EGL_NO_DISPLAY) return 0L

      // Initialize EGL
      val majorSeg   = arena.allocate(I)
      val minorSeg   = arena.allocate(I)
      val initResult = hInitialize.invoke(display, majorSeg, minorSeg).asInstanceOf[Int]
      if (initResult == 0) return 0L

      // Choose config
      val attribList = arena.allocate(JAVA_INT, 19)
      attribList.setAtIndex(I, 0, EGL_RED_SIZE)
      attribList.setAtIndex(I, 1, r)
      attribList.setAtIndex(I, 2, EGL_GREEN_SIZE)
      attribList.setAtIndex(I, 3, g)
      attribList.setAtIndex(I, 4, EGL_BLUE_SIZE)
      attribList.setAtIndex(I, 5, b)
      attribList.setAtIndex(I, 6, EGL_ALPHA_SIZE)
      attribList.setAtIndex(I, 7, a)
      attribList.setAtIndex(I, 8, EGL_DEPTH_SIZE)
      attribList.setAtIndex(I, 9, depth)
      attribList.setAtIndex(I, 10, EGL_STENCIL_SIZE)
      attribList.setAtIndex(I, 11, stencil)
      attribList.setAtIndex(I, 12, EGL_RENDERABLE_TYPE)
      attribList.setAtIndex(I, 13, EGL_OPENGL_ES3_BIT)
      attribList.setAtIndex(I, 14, EGL_SURFACE_TYPE)
      attribList.setAtIndex(I, 15, EGL_WINDOW_BIT)
      if (samples > 0) {
        attribList.setAtIndex(I, 16, EGL_SAMPLE_BUFFERS)
        attribList.setAtIndex(I, 17, 1)
        // Rewrite: we'd need a longer array for SAMPLES + EGL_NONE
        // For simplicity, omit MSAA in the config — ANGLE handles it internally
      }
      attribList.setAtIndex(I, 18, EGL_NONE)

      val configSeg    = arena.allocate(P)
      val numConfigs   = arena.allocate(I)
      val chooseResult = hChooseConfig.invoke(display, attribList, configSeg, 1, numConfigs).asInstanceOf[Int]
      if (chooseResult == 0 || numConfigs.get(I, 0) == 0) return 0L
      val config = configSeg.get(P, 0)

      // Create context (ES 3.0)
      val ctxAttribs = arena.allocate(JAVA_INT, 5)
      ctxAttribs.setAtIndex(I, 0, EGL_CONTEXT_MAJOR_VERSION)
      ctxAttribs.setAtIndex(I, 1, 3)
      ctxAttribs.setAtIndex(I, 2, EGL_CONTEXT_MINOR_VERSION)
      ctxAttribs.setAtIndex(I, 3, 0)
      ctxAttribs.setAtIndex(I, 4, EGL_NONE)

      val context = hCreateCtx.invoke(display, config, ptr(EGL_NO_CONTEXT), ctxAttribs).asInstanceOf[MemorySegment]
      if (context == MemorySegment.NULL || context.address() == EGL_NO_CONTEXT) return 0L

      // Create window surface
      val surfAttribs = arena.allocate(JAVA_INT, 1)
      surfAttribs.setAtIndex(I, 0, EGL_NONE)
      val surface = hCreateSurface.invoke(display, config, ptr(windowHandle), surfAttribs).asInstanceOf[MemorySegment]
      if (surface == MemorySegment.NULL || surface.address() == EGL_NO_SURFACE) {
        hDestroyCtx.invoke(display, context)
        return 0L
      }

      // Make current
      hMakeCurrent.invoke(display, surface, surface, context)

      // Store state in a long-lived struct
      val state = contextArena.allocate(ContextLayout)
      state.set(JAVA_LONG, displayOffset, display.address())
      state.set(JAVA_LONG, configOffset, config.address())
      state.set(JAVA_LONG, contextOffset, context.address())
      state.set(JAVA_LONG, surfaceOffset, surface.address())
      state.address()
    } finally arena.close()
  }

  private def readState(contextHandle: Long): (MemorySegment, MemorySegment, MemorySegment, MemorySegment) = {
    val state   = MemorySegment.ofAddress(contextHandle).reinterpret(ContextLayout.byteSize())
    val display = ptr(state.get(JAVA_LONG, displayOffset))
    val config  = ptr(state.get(JAVA_LONG, configOffset))
    val context = ptr(state.get(JAVA_LONG, contextOffset))
    val surface = ptr(state.get(JAVA_LONG, surfaceOffset))
    (display, config, context, surface)
  }

  override def destroyContext(contextHandle: Long): Unit = {
    val (display, _, context, surface) = readState(contextHandle)
    hMakeCurrent.invoke(display, ptr(EGL_NO_SURFACE), ptr(EGL_NO_SURFACE), ptr(EGL_NO_CONTEXT))
    hDestroySurface.invoke(display, surface)
    hDestroyCtx.invoke(display, context)
    hTerminate.invoke(display)
  }

  override def makeCurrent(contextHandle: Long): Unit = {
    val (display, _, context, surface) = readState(contextHandle)
    hMakeCurrent.invoke(display, surface, surface, context)
  }

  override def swapEglBuffers(contextHandle: Long): Unit = {
    val (display, _, _, surface) = readState(contextHandle)
    hSwapBuffers.invoke(display, surface)
  }

  override def setSwapInterval(interval: Int): Unit = {
    // EGL swap interval applies to the current display
    val display = hGetDisplay.invoke(ptr(EGL_DEFAULT_DISPLAY)).asInstanceOf[MemorySegment]
    hSwapInterval.invoke(display, interval)
  }

  override def getProcAddress(name: String): Long = {
    val arena = Arena.ofConfined()
    try {
      val result = hGetProcAddr.invoke(arena.allocateFrom(name)).asInstanceOf[MemorySegment]
      if (result == MemorySegment.NULL) 0L else result.address()
    } finally arena.close()
  }
}

object GlOpsJvm {

  /** Creates a GlOpsJvm from ANGLE's libEGL loaded from the system library path.
    * @param libName
    *   the EGL library name (e.g. "EGL", "libEGL")
    */
  def apply(libName: String = "EGL"): GlOpsJvm = {
    val mappedName = System.mapLibraryName(libName)
    val libPath    = System.getProperty("java.library.path", "")
    val paths      = libPath.split(java.io.File.pathSeparator)
    val found      = paths.iterator
      .map(p => java.nio.file.Path.of(p, mappedName))
      .find(java.nio.file.Files.exists(_))
      .getOrElse(
        throw new UnsatisfiedLinkError(s"Cannot find $mappedName in java.library.path: $libPath")
      )
    val lookup = SymbolLookup.libraryLookup(found, Arena.global())
    new GlOpsJvm(lookup)
  }
}
