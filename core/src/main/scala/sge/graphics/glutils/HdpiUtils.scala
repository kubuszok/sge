/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/HdpiUtils.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: static methods as object methods; uses (using Sge) instead of Gdx. statics
 *   Idiom: split packages
 *   TODO: opaque Pixels for glScissor/glViewport params, toLogical/toBackBuffer conversions -- see docs/improvements/opaque-types.md
 *   TODO: typed GL enums -- viewport/scissor params via GL20 -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
  def glScissor(x: Int, y: Int, width: Int, height: Int)(using Sge): Unit =
    if (
      mode == HdpiMode.Logical && (Sge().graphics.getWidth() != Sge().graphics.getBackBufferWidth()
        || Sge().graphics.getHeight() != Sge().graphics.getBackBufferHeight())
    ) {
      Sge().graphics.gl.glScissor(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height))
    } else {
      Sge().graphics.gl.glScissor(x, y, width, height)
    }

  /** Calls {@link GL20#glViewport(int, int, int, int)} , expecting the coordinates and sizes given in logical coordinates and automatically converts them to backbuffer coordinates, which may be
    * bigger on HDPI screens.
    */
  def glViewport(x: Int, y: Int, width: Int, height: Int)(using Sge): Unit =
    if (
      mode == HdpiMode.Logical && (Sge().graphics.getWidth() != Sge().graphics.getBackBufferWidth()
        || Sge().graphics.getHeight() != Sge().graphics.getBackBufferHeight())
    ) {
      Sge().graphics.gl.glViewport(toBackBufferX(x), toBackBufferY(y), toBackBufferX(width), toBackBufferY(height))
    } else {
      Sge().graphics.gl.glViewport(x, y, width, height)
    }

  /** Converts an x-coordinate given in backbuffer coordinates to logical screen coordinates. */
  def toLogicalX(backBufferX: Int)(using Sge): Int =
    (backBufferX * Sge().graphics.getWidth() / Sge().graphics.getBackBufferWidth().toFloat).toInt

  /** Converts an y-coordinate given in backbuffer coordinates to logical screen coordinates. */
  def toLogicalY(backBufferY: Int)(using Sge): Int =
    (backBufferY * Sge().graphics.getHeight() / Sge().graphics.getBackBufferHeight().toFloat).toInt

  /** Converts an x-coordinate given in logical screen coordinates to backbuffer coordinates. */
  def toBackBufferX(logicalX: Int)(using Sge): Int =
    (logicalX * Sge().graphics.getBackBufferWidth() / Sge().graphics.getWidth().toFloat).toInt

  /** Converts an y-coordinate given in logical screen coordinates to backbuffer coordinates. */
  def toBackBufferY(logicalY: Int)(using Sge): Int =
    (logicalY * Sge().graphics.getBackBufferHeight() / Sge().graphics.getHeight().toFloat).toInt
}
