/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ScissorStack.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java class with static methods -> Scala object
 * - Gdx.gl/Gdx.graphics -> Sge().graphics (using Sge context parameter)
 * - Array<Rectangle> -> DynamicArray[Rectangle]
 * - @Null Rectangle peekScissors -> Nullable[Rectangle]
 * - All methods faithfully ported
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 160
 * Covenant-baseline-methods: ScissorStack,calculateScissors,fix,old,peekScissors,popScissors,pushScissors,scissors,tmp,viewport
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/ScissorStack.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.{ Camera, EnableCap }
import sge.graphics.glutils.HdpiUtils
import sge.math.{ Matrix4, Rectangle, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

/** A stack of {@link Rectangle} objects to be used for clipping via {@link GL20#glScissor(int, int, int, int)}. When a new Rectangle is pushed onto the stack, it will be merged with the current top
  * of stack. The minimum area of overlap is then set as the real top of the stack.
  * @author
  *   mzechner
  */
object ScissorStack {
  private val scissors: DynamicArray[Rectangle] = DynamicArray[Rectangle]()
  private val tmp:      Vector3                 = Vector3()
  private val viewport: Rectangle               = Rectangle()

  /** Pushes a new scissor {@link Rectangle} onto the stack, merging it with the current top of the stack. The smallest area of overlap between the top of stack rectangle and the provided rectangle is
    * pushed onto the stack. This will invoke {@link GL20#glScissor(int, int, int, int)} with the final top of stack rectangle. In case no scissor is yet on the stack this will also enable
    * {@link GL20#GL_SCISSOR_TEST} automatically. <p> Any drawing should be flushed before pushing scissors.
    * @return
    *   true if the scissors were pushed. false if the scissor area was zero, in this case the scissors were not pushed and no drawing should occur.
    */
  def pushScissors(scissor: Rectangle)(using Sge): Boolean = {
    fix(scissor)

    if (scissors.isEmpty) {
      if (scissor.width < 1 || scissor.height < 1) false
      else {
        Sge().graphics.gl20.glEnable(EnableCap.ScissorTest)
        scissors.add(scissor)
        HdpiUtils.glScissor(Pixels(scissor.x.toInt), Pixels(scissor.y.toInt), Pixels(scissor.width.toInt), Pixels(scissor.height.toInt))
        true
      }
    } else {
      // merge scissors
      val parent = scissors(scissors.size - 1)
      val minX   = Math.max(parent.x, scissor.x)
      val maxX   = Math.min(parent.x + parent.width, scissor.x + scissor.width)
      if (maxX - minX < 1) false
      else {
        val minY = Math.max(parent.y, scissor.y)
        val maxY = Math.min(parent.y + parent.height, scissor.y + scissor.height)
        if (maxY - minY < 1) false
        else {
          scissor.x = minX
          scissor.y = minY
          scissor.width = maxX - minX
          scissor.height = Math.max(1, maxY - minY)
          scissors.add(scissor)
          HdpiUtils.glScissor(Pixels(scissor.x.toInt), Pixels(scissor.y.toInt), Pixels(scissor.width.toInt), Pixels(scissor.height.toInt))
          true
        }
      }
    }
  }

  /** Pops the current scissor rectangle from the stack and sets the new scissor area to the new top of stack rectangle. In case no more rectangles are on the stack, {@link GL20#GL_SCISSOR_TEST} is
    * disabled. <p> Any drawing should be flushed before popping scissors.
    */
  def popScissors()(using Sge): Rectangle = {
    val old = scissors.removeIndex(scissors.size - 1)
    if (scissors.isEmpty)
      Sge().graphics.gl20.glDisable(EnableCap.ScissorTest)
    else {
      val scissor = scissors(scissors.size - 1)
      HdpiUtils.glScissor(Pixels(scissor.x.toInt), Pixels(scissor.y.toInt), Pixels(scissor.width.toInt), Pixels(scissor.height.toInt))
    }
    old
  }

  /** @return null if there are no scissors. */
  def peekScissors: Nullable[Rectangle] =
    if (scissors.isEmpty) Nullable.empty
    else Nullable(scissors(scissors.size - 1))

  private def fix(rect: Rectangle): Unit = {
    rect.x = Math.round(rect.x).toFloat
    rect.y = Math.round(rect.y).toFloat
    rect.width = Math.round(rect.width).toFloat
    rect.height = Math.round(rect.height).toFloat
    if (rect.width < 0) {
      rect.width = -rect.width
      rect.x -= rect.width
    }
    if (rect.height < 0) {
      rect.height = -rect.height
      rect.y -= rect.height
    }
  }

  /** Calculates a scissor rectangle using 0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight() as the viewport.
    * @see
    *   #calculateScissors(Camera, float, float, float, float, Matrix4, Rectangle, Rectangle)
    */
  def calculateScissors(camera: Camera, batchTransform: Matrix4, area: Rectangle, scissor: Rectangle)(using Sge): Unit =
    calculateScissors(camera, 0, 0, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat, batchTransform, area, scissor)

  /** Calculates a scissor rectangle in OpenGL ES window coordinates from a {@link Camera}, a transformation {@link Matrix4} and an axis aligned {@link Rectangle}. The rectangle will get transformed
    * by the camera and transform matrices and is then projected to screen coordinates. Note that only axis aligned rectangles will work with this method. If either the Camera or the Matrix4 have
    * rotational components, the output of this method will not be suitable for {@link GL20#glScissor(int, int, int, int)}.
    * @param camera
    *   the {@link Camera}
    * @param batchTransform
    *   the transformation {@link Matrix4}
    * @param area
    *   the {@link Rectangle} to transform to window coordinates
    * @param scissor
    *   the Rectangle to store the result in
    */
  def calculateScissors(camera: Camera, viewportX: Float, viewportY: Float, viewportWidth: Float, viewportHeight: Float, batchTransform: Matrix4, area: Rectangle, scissor: Rectangle): Unit = {
    tmp.set(area.x, area.y, 0)
    tmp.mul(batchTransform)
    camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight)
    scissor.x = tmp.x
    scissor.y = tmp.y

    tmp.set(area.x + area.width, area.y + area.height, 0)
    tmp.mul(batchTransform)
    camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight)
    scissor.width = tmp.x - scissor.x
    scissor.height = tmp.y - scissor.y
  }

  /** @return the current viewport in OpenGL ES window coordinates based on the currently applied scissor */
  def viewport(using Sge): Rectangle =
    if (scissors.isEmpty) {
      viewport.set(0, 0, Sge().graphics.width.toFloat, Sge().graphics.height.toFloat)
      viewport
    } else {
      val scissor = scissors(scissors.size - 1)
      viewport.set(scissor)
      viewport
    }
}
