/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original Scala Native implementation of GlOps
 *   Convention: @extern C FFI to ANGLE EGL shared library
 *   Convention: EGL types (EGLDisplay, EGLContext, EGLSurface, EGLConfig) as Ptr[Byte]
 *   Convention: EGL context state packed in malloc'd struct (4 pointers)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import scala.scalanative.libc.stdlib
import scala.scalanative.runtime.{ Intrinsics, fromRawPtr, toRawPtr }
import scala.scalanative.unsafe.*
import scala.util.boundary
import scala.util.boundary.break

@link("EGL")
@extern
private object EglC {
  def eglGetDisplay(displayId:    Ptr[Byte]):                                                                           Ptr[Byte] = extern
  def eglInitialize(dpy:          Ptr[Byte], major:    Ptr[CInt], minor:   Ptr[CInt]):                                  CInt      = extern
  def eglChooseConfig(dpy:        Ptr[Byte], attribs:  Ptr[CInt], configs: Ptr[Ptr[Byte]], size: CInt, num: Ptr[CInt]): CInt      = extern
  def eglCreateContext(dpy:       Ptr[Byte], config:   Ptr[Byte], share:   Ptr[Byte], attribs:   Ptr[CInt]):            Ptr[Byte] = extern
  def eglCreateWindowSurface(dpy: Ptr[Byte], config:   Ptr[Byte], win:     Ptr[Byte], attribs:   Ptr[CInt]):            Ptr[Byte] = extern
  def eglMakeCurrent(dpy:         Ptr[Byte], draw:     Ptr[Byte], read:    Ptr[Byte], ctx:       Ptr[Byte]):            CInt      = extern
  def eglSwapBuffers(dpy:         Ptr[Byte], surface:  Ptr[Byte]):                                                      CInt      = extern
  def eglSwapInterval(dpy:        Ptr[Byte], interval: CInt):                                                           CInt      = extern
  def eglDestroyContext(dpy:      Ptr[Byte], ctx:      Ptr[Byte]):                                                      CInt      = extern
  def eglDestroySurface(dpy:      Ptr[Byte], surface:  Ptr[Byte]):                                                      CInt      = extern
  def eglTerminate(dpy:           Ptr[Byte]):                                                                           CInt      = extern
  def eglGetProcAddress(procname: CString):                                                                             Ptr[Byte] = extern
}

private[sge] object GlOpsNative extends GlOps {

  private inline def ptrFromLong(h: Long): Ptr[Byte] =
    if (h == 0L) null else fromRawPtr[Byte](Intrinsics.castLongToRawPtr(h))

  private inline def longFromPtr(p: Ptr[Byte]): Long =
    if (p == null) 0L else Intrinsics.castRawPtrToLong(toRawPtr(p))

  // ─── EGL constants ────────────────────────────────────────────────────

  private val EGL_NONE:                  CInt = 0x3038
  private val EGL_RED_SIZE:              CInt = 0x3024
  private val EGL_GREEN_SIZE:            CInt = 0x3023
  private val EGL_BLUE_SIZE:             CInt = 0x3022
  private val EGL_ALPHA_SIZE:            CInt = 0x3021
  private val EGL_DEPTH_SIZE:            CInt = 0x3025
  private val EGL_STENCIL_SIZE:          CInt = 0x3026
  private val EGL_SAMPLE_BUFFERS:        CInt = 0x3032
  private val EGL_RENDERABLE_TYPE:       CInt = 0x3040
  private val EGL_OPENGL_ES3_BIT:        CInt = 0x0040
  private val EGL_SURFACE_TYPE:          CInt = 0x3033
  private val EGL_WINDOW_BIT:            CInt = 0x0004
  private val EGL_CONTEXT_MAJOR_VERSION: CInt = 0x3098
  private val EGL_CONTEXT_MINOR_VERSION: CInt = 0x30fb

  // Context state struct: 4 pointers (display, config, context, surface)
  private val ContextStructSize = 32 // 4 pointers x 8 bytes (64-bit)

  // ─── GlOps implementation ────────────────────────────────────────────

  override def createContext(
    windowHandle: Long,
    r:            Int,
    g:            Int,
    b:            Int,
    a:            Int,
    depth:        Int,
    stencil:      Int,
    samples:      Int
  ): Long = boundary {
    // Get display
    val display = EglC.eglGetDisplay(null)
    if (display == null) break(0L)

    // Initialize
    val major      = stackalloc[CInt]()
    val minor      = stackalloc[CInt]()
    val initResult = EglC.eglInitialize(display, major, minor)
    if (initResult == 0) break(0L)

    // Choose config
    val attribs = stackalloc[CInt](19)
    attribs(0) = EGL_RED_SIZE; attribs(1) = r
    attribs(2) = EGL_GREEN_SIZE; attribs(3) = g
    attribs(4) = EGL_BLUE_SIZE; attribs(5) = b
    attribs(6) = EGL_ALPHA_SIZE; attribs(7) = a
    attribs(8) = EGL_DEPTH_SIZE; attribs(9) = depth
    attribs(10) = EGL_STENCIL_SIZE; attribs(11) = stencil
    attribs(12) = EGL_RENDERABLE_TYPE; attribs(13) = EGL_OPENGL_ES3_BIT
    attribs(14) = EGL_SURFACE_TYPE; attribs(15) = EGL_WINDOW_BIT
    if (samples > 0) {
      attribs(16) = EGL_SAMPLE_BUFFERS; attribs(17) = 1
    }
    attribs(18) = EGL_NONE

    val configOut  = stackalloc[Ptr[Byte]]()
    val numConfigs = stackalloc[CInt]()
    val chooseOk   = EglC.eglChooseConfig(display, attribs, configOut, 1, numConfigs)
    if (chooseOk == 0 || !numConfigs == 0) break(0L)
    val config = !configOut

    // Create context (ES 3.0)
    val ctxAttribs = stackalloc[CInt](5)
    ctxAttribs(0) = EGL_CONTEXT_MAJOR_VERSION; ctxAttribs(1) = 3
    ctxAttribs(2) = EGL_CONTEXT_MINOR_VERSION; ctxAttribs(3) = 0
    ctxAttribs(4) = EGL_NONE

    val context = EglC.eglCreateContext(display, config, null, ctxAttribs)
    if (context == null) break(0L)

    // Create window surface
    val surfAttribs = stackalloc[CInt](1)
    surfAttribs(0) = EGL_NONE
    val surface = EglC.eglCreateWindowSurface(display, config, ptrFromLong(windowHandle), surfAttribs)
    if (surface == null) {
      EglC.eglDestroyContext(display, context)
      break(0L)
    }

    // Make current
    EglC.eglMakeCurrent(display, surface, surface, context)

    // Store state in a heap-allocated struct (4 pointers)
    val state = stdlib.malloc(ContextStructSize)
    val ptrs  = state.asInstanceOf[Ptr[Ptr[Byte]]]
    ptrs(0) = display
    ptrs(1) = config
    ptrs(2) = context
    ptrs(3) = surface
    longFromPtr(state)
  }

  private def readState(contextHandle: Long): (Ptr[Byte], Ptr[Byte], Ptr[Byte], Ptr[Byte]) = {
    val ptrs = ptrFromLong(contextHandle).asInstanceOf[Ptr[Ptr[Byte]]]
    (ptrs(0), ptrs(1), ptrs(2), ptrs(3))
  }

  override def destroyContext(contextHandle: Long): Unit = {
    val (display, _, context, surface) = readState(contextHandle)
    EglC.eglMakeCurrent(display, null, null, null)
    EglC.eglDestroySurface(display, surface)
    EglC.eglDestroyContext(display, context)
    EglC.eglTerminate(display)
    stdlib.free(ptrFromLong(contextHandle))
  }

  override def makeCurrent(contextHandle: Long): Unit = {
    val (display, _, context, surface) = readState(contextHandle)
    EglC.eglMakeCurrent(display, surface, surface, context)
  }

  override def swapEglBuffers(contextHandle: Long): Unit = {
    val (display, _, _, surface) = readState(contextHandle)
    EglC.eglSwapBuffers(display, surface)
  }

  override def setSwapInterval(interval: Int): Unit = {
    val display = EglC.eglGetDisplay(null)
    EglC.eglSwapInterval(display, interval)
  }

  override def getProcAddress(name: String): Long = {
    val zone = Zone.open()
    try {
      val result = EglC.eglGetProcAddress(toCString(name)(using zone))
      if (result == null) 0L else longFromPtr(result)
    } finally zone.close()
  }
}
