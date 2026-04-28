/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/HdpiUtils.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: static methods as object methods; uses (using Sge) instead of Gdx. statics
 *   Idiom: split packages
 *   Idiom: opaque Pixels for glScissor/glViewport params, toLogical/toBackBuffer conversions
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 84
 * Covenant-baseline-methods: HdpiUtils,glScissor,glViewport,mode,setMode,toBackBufferX,toBackBufferY,toLogicalX,toLogicalY
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/HdpiUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 89fe31ac4c3d47355ace4cece969e62b02cf360b
 */
package sge
package graphics
package glutils

import sge.Sge

/** To deal with HDPI monitors properly, use the glViewport and glScissor functions of this class instead of directly calling OpenGL yourself. The logical coordinate system provided by the operating
  * system may not have the same resolution as the actual drawing surface to which OpenGL draws, also known as the backbuffer. This class will ensure, that you pass the correct values to OpenGL for
  * any function that expects backbuffer coordinates instead of logical coordinates.
  *
  * @author
  *   badlogic (original implementation)
  */
object HdpiUtils {
  private var mode: HdpiMode = HdpiMode.Logical

  /** Allows applications to override HDPI coordinate conversion for glViewport and glScissor calls.
    *
    * This function can be used to ignore the default behavior, for example when rendering a UI stage to an off-screen framebuffer:
    *
    * <pre> HdpiUtils.setMode(HdpiMode.Pixels); fb.begin(); stage.draw(); fb.end(); HdpiUtils.setMode(HdpiMode.Logical); </pre>
    *
    * @param mode
    *   set to HdpiMode.Pixels to ignore HDPI conversion for glViewport and glScissor functions
    */
  def setMode(mode: HdpiMode): Unit =
    HdpiUtils.mode = mode

  /** Calls {@link GL20#glScissor(int, int, int, int)} , expecting the coordinates and sizes given in logical coordinates and automatically converts them to backbuffer coordinates, which may be bigger
    * on HDPI screens.
    */
  def glScissor(x: Pixels, y: Pixels, width: Pixels, height: Pixels)(using Sge): Unit =
    if (
      mode == HdpiMode.Logical && (Sge().graphics.width != Sge().graphics.backBufferWidth
        || Sge().graphics.height != Sge().graphics.backBufferHeight)
    ) {
      Sge().graphics.gl.glScissor(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height))
    } else {
      Sge().graphics.gl.glScissor(x, y, width, height)
    }

  /** Calls {@link GL20#glViewport(int, int, int, int)} , expecting the coordinates and sizes given in logical coordinates and automatically converts them to backbuffer coordinates, which may be
    * bigger on HDPI screens.
    */
  def glViewport(x: Pixels, y: Pixels, width: Pixels, height: Pixels)(using Sge): Unit =
    if (
      mode == HdpiMode.Logical && (Sge().graphics.width != Sge().graphics.backBufferWidth
        || Sge().graphics.height != Sge().graphics.backBufferHeight)
    ) {
      Sge().graphics.gl.glViewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height))
    } else {
      Sge().graphics.gl.glViewport(x, y, width, height)
    }

  /** Converts an x-coordinate given in backbuffer coordinates to logical screen coordinates. */
  def toLogicalX(backBufferX: Pixels)(using Sge): Pixels =
    Pixels((backBufferX.toInt * Sge().graphics.width.toInt / Sge().graphics.backBufferWidth.toFloat).toInt)

  /** Converts an y-coordinate given in backbuffer coordinates to logical screen coordinates. */
  def toLogicalY(backBufferY: Pixels)(using Sge): Pixels =
    Pixels((backBufferY.toInt * Sge().graphics.height.toInt / Sge().graphics.backBufferHeight.toFloat).toInt)

  /** Converts an x-coordinate given in logical screen coordinates to backbuffer coordinates. */
  def toBackBufferX(logicalX: Pixels)(using Sge): Pixels =
    Pixels((logicalX.toInt * Sge().graphics.backBufferWidth.toInt / Sge().graphics.width.toFloat).toInt)

  /** Converts an y-coordinate given in logical screen coordinates to backbuffer coordinates. */
  def toBackBufferY(logicalY: Pixels)(using Sge): Pixels =
    Pixels((logicalY.toInt * Sge().graphics.backBufferHeight.toInt / Sge().graphics.height.toFloat).toInt)
}
