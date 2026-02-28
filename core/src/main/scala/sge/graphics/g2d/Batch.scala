/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Batch.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.math.Affine2
import sge.math.Matrix4
import sge.graphics.Color
import sge.graphics.glutils.ShaderProgram
import sge.utils.Nullable

/** A Batch is used to draw 2D rectangles that reference a texture (region). The class will batch the drawing commands and optimize them for processing by the GPU. <p> To draw something with a Batch
  * one has to first call the {@link Batch#begin()} method which will setup appropriate render states. When you are done with drawing you have to call the {@link Batch#end()} which will actually draw
  * the things you specified. <p> All drawing commands of the Batch operate in screen coordinates. The screen coordinate system has an x-axis pointing to the right, an y-axis pointing upwards and the
  * origin is in the lower left corner of the screen. You can also provide your own transformation and projection matrices if you so wish. <p> A Batch is managed. In case the OpenGL context is lost
  * all OpenGL resources a Batch uses internally get invalidated. A context is lost when a user switches to another application or receives an incoming call on Android. A Batch will be automatically
  * reloaded after the OpenGL context is restored. <p> A Batch is a pretty heavy object so you should only ever have one in your program. <p> A Batch works with OpenGL ES 2.0. It will use its own
  * custom shader to draw all provided sprites. You can set your own custom shader via {@link #setShader(ShaderProgram)} . <p> A Batch has to be disposed if it is no longer used.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
trait Batch extends AutoCloseable {

  /** Sets up the Batch for drawing. This will disable depth buffer writing. It enables blending and texturing. If you have more texture units enabled than the first one you have to disable them
    * before calling this. Uses a screen coordinate system by default where everything is given in pixels. You can specify your own projection and modelview matrices via
    * {@link #setProjectionMatrix(Matrix4)} and {@link #setTransformMatrix(Matrix4)} .
    */
  def begin(): Unit

  /** Finishes off rendering. Enables depth writes, disables blending and texturing. Must always be called after a call to {@link #begin()}
    */
  def end(): Unit

  /** Sets the color used to tint images when they are added to the Batch. Default is {@link Color#WHITE}. */
  def setColor(tint: Color): Unit

  /** @see #setColor(Color) */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit

  /** @return
    *   the rendering color of this Batch. If the returned instance is manipulated, {@link #setColor(Color)} must be called afterward.
    */
  def getColor(): Color

  /** Sets the rendering color of this Batch, expanding the alpha from 0-254 to 0-255.
    * @see
    *   #setColor(Color)
    * @see
    *   Color#toFloatBits()
    */
  def setPackedColor(packedColor: Float): Unit

  /** @return
    *   the rendering color of this Batch in vertex format (alpha compressed to 0-254)
    * @see
    *   Color#toFloatBits()
    */
  def getPackedColor(): Float

  /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The rectangle is offset by originX, originY relative to the origin. Scale specifies the scaling
    * factor by which the rectangle should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the rectangle around originX, originY. The portion of the
    * {@link Texture} given by srcX, srcY and srcWidth, srcHeight is used. These coordinates and sizes are given in texels. FlipX and flipY specify whether the texture portion should be flipped
    * horizontally or vertically.
    * @param x
    *   the x-coordinate in screen space
    * @param y
    *   the y-coordinate in screen space
    * @param originX
    *   the x-coordinate of the scaling and rotation origin relative to the screen space coordinates
    * @param originY
    *   the y-coordinate of the scaling and rotation origin relative to the screen space coordinates
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @param scaleX
    *   the scale of the rectangle around originX/originY in x
    * @param scaleY
    *   the scale of the rectangle around originX/originY in y
    * @param rotation
    *   the angle of counter clockwise rotation of the rectangle around originX/originY, in degrees
    * @param srcX
    *   the x-coordinate in texel space
    * @param srcY
    *   the y-coordinate in texel space
    * @param srcWidth
    *   the source with in texels
    * @param srcHeight
    *   the source height in texels
    * @param flipX
    *   whether to flip the sprite horizontally
    * @param flipY
    *   whether to flip the sprite vertically
    */
  def draw(
    texture:   Texture,
    x:         Float,
    y:         Float,
    originX:   Float,
    originY:   Float,
    width:     Float,
    height:    Float,
    scaleX:    Float,
    scaleY:    Float,
    rotation:  Float,
    srcX:      Int,
    srcY:      Int,
    srcWidth:  Int,
    srcHeight: Int,
    flipX:     Boolean,
    flipY:     Boolean
  ): Unit

  /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The portion of the {@link Texture} given by srcX, srcY and srcWidth, srcHeight is used. These
    * coordinates and sizes are given in texels. FlipX and flipY specify whether the texture portion should be flipped horizontally or vertically.
    * @param x
    *   the x-coordinate in screen space
    * @param y
    *   the y-coordinate in screen space
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    * @param srcX
    *   the x-coordinate in texel space
    * @param srcY
    *   the y-coordinate in texel space
    * @param srcWidth
    *   the source with in texels
    * @param srcHeight
    *   the source height in texels
    * @param flipX
    *   whether to flip the sprite horizontally
    * @param flipY
    *   whether to flip the sprite vertically
    */
  def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit

  /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The portion of the {@link Texture} given by srcX, srcY and srcWidth, srcHeight are used. These
    * coordinates and sizes are given in texels.
    * @param x
    *   the x-coordinate in screen space
    * @param y
    *   the y-coordinate in screen space
    * @param srcX
    *   the x-coordinate in texel space
    * @param srcY
    *   the y-coordinate in texel space
    * @param srcWidth
    *   the source with in texels
    * @param srcHeight
    *   the source height in texels
    */
  def draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int): Unit

  /** Draws a rectangle with the bottom left corner at x,y having the given width and height in pixels. The portion of the {@link Texture} given by u, v and u2, v2 are used. These coordinates and
    * sizes are given in texture size percentage. The rectangle will have the given tint {@link Color} .
    * @param x
    *   the x-coordinate in screen space
    * @param y
    *   the y-coordinate in screen space
    * @param width
    *   the width in pixels
    * @param height
    *   the height in pixels
    */
  def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u: Float, v: Float, u2: Float, v2: Float): Unit

  /** Draws a rectangle with the bottom left corner at x,y having the width and height of the texture.
    * @param x
    *   the x-coordinate in screen space
    * @param y
    *   the y-coordinate in screen space
    */
  def draw(texture: Texture, x: Float, y: Float): Unit

  /** Draws a rectangle with the bottom left corner at x,y and stretching the region to cover the given width and height. */
  def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float): Unit

  /** Draws a rectangle using the given vertices. There must be 4 vertices, each made up of 5 elements in this order: x, y, color, u, v. The {@link #getColor()} from the Batch is not applied.
    */
  def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit

  /** Draws a rectangle with the bottom left corner at x,y having the width and height of the region. */
  def draw(region: TextureRegion, x: Float, y: Float): Unit

  /** Draws a rectangle with the bottom left corner at x,y and stretching the region to cover the given width and height. */
  def draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float): Unit

  /** Draws a rectangle with the bottom left corner at x,y and stretching the region to cover the given width and height. The rectangle is offset by originX, originY relative to the origin. Scale
    * specifies the scaling factor by which the rectangle should be scaled around originX, originY. Rotation specifies the angle of counter clockwise rotation of the rectangle around originX, originY.
    * @param rotation
    *   rotation in degrees
    */
  def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit

  /** Draws a rectangle with the texture coordinates rotated 90 degrees. The bottom left corner at x,y and stretching the region to cover the given width and height. The rectangle is offset by
    * originX, originY relative to the origin. Scale specifies the scaling factor by which the rectangle should be scaled around originX, originY. Rotation specifies the angle of counter clockwise
    * rotation of the rectangle around originX, originY.
    * @param rotation
    *   rotation in degrees
    * @param clockwise
    *   If true, the texture coordinates are rotated 90 degrees clockwise. If false, they are rotated 90 degrees counter clockwise.
    */
  def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit

  /** Draws a rectangle transformed by the given matrix. */
  def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit

  /** Causes any pending sprites to be rendered, without ending the Batch. */
  def flush(): Unit

  /** Disables blending for drawing sprites. Calling this within {@link #begin()}/{@link #end()} will flush the batch. */
  def disableBlending(): Unit

  /** Enables blending for drawing sprites. Calling this within {@link #begin()}/{@link #end()} will flush the batch. */
  def enableBlending(): Unit

  /** Sets the blending function to be used when rendering sprites.
    * @param srcFunc
    *   the source function, e.g. GL20.GL_SRC_ALPHA. If set to -1, Batch won't change the blending function.
    * @param dstFunc
    *   the destination function, e.g. GL20.GL_ONE_MINUS_SRC_ALPHA
    */
  def setBlendFunction(srcFunc: Int, dstFunc: Int): Unit

  /** Sets separate (color/alpha) blending function to be used when rendering sprites.
    * @param srcFuncColor
    *   the source color function, e.g. GL20.GL_SRC_ALPHA. If set to -1, Batch won't change the blending function.
    * @param dstFuncColor
    *   the destination color function, e.g. GL20.GL_ONE_MINUS_SRC_ALPHA.
    * @param srcFuncAlpha
    *   the source alpha function, e.g. GL20.GL_SRC_ALPHA.
    * @param dstFuncAlpha
    *   the destination alpha function, e.g. GL20.GL_ONE_MINUS_SRC_ALPHA.
    */
  def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit

  def getBlendSrcFunc(): Int

  def getBlendDstFunc(): Int

  def getBlendSrcFuncAlpha(): Int

  def getBlendDstFuncAlpha(): Int

  /** Returns the current projection matrix. Changing this within {@link #begin()} /{@link #end()} results in undefined behaviour.
    */
  def getProjectionMatrix(): Matrix4

  /** Returns the current transform matrix. Changing this within {@link #begin()} /{@link #end()} results in undefined behaviour.
    */
  def getTransformMatrix(): Matrix4

  /** Sets the projection matrix to be used by this Batch. If this is called inside a {@link #begin()} /{@link #end()} block, the current batch is flushed to the gpu.
    */
  def setProjectionMatrix(projection: Matrix4): Unit

  /** Sets the transform matrix to be used by this Batch. */
  def setTransformMatrix(transform: Matrix4): Unit

  /** Sets the shader to be used in a GLES 2.0 environment. Vertex position attribute is called "a_position", the texture coordinates attribute is called "a_texCoord0", the color attribute is called
    * "a_color". See {@link ShaderProgram#POSITION_ATTRIBUTE} , {@link ShaderProgram#COLOR_ATTRIBUTE} and {@link ShaderProgram#TEXCOORD_ATTRIBUTE} which gets "0" appended to indicate the use of the
    * first texture unit. The combined transform and projection matrx is uploaded via a mat4 uniform called "u_projTrans". The texture sampler is passed via a uniform called "u_texture". <p> Call this
    * method with a Nullable.empty argument to use the default shader. <p> This method will flush the batch before setting the new shader, you can call it in between {@link #begin()} and
    * {@link #end()} .
    * @param shader
    *   the {@link ShaderProgram} or Nullable.empty to use the default shader.
    */
  def setShader(shader: Nullable[ShaderProgram]): Unit

  /** @return the current {@link ShaderProgram} set by {@link #setShader(ShaderProgram)} or the defaultShader */
  def getShader(): ShaderProgram

  /** @return true if blending for sprites is enabled */
  def isBlendingEnabled(): Boolean

  /** @return true if currently between begin and end. */
  def isDrawing(): Boolean
}

object Batch {
  final val X1 = 0
  final val Y1 = 1
  final val C1 = 2
  final val U1 = 3
  final val V1 = 4
  final val X2 = 5
  final val Y2 = 6
  final val C2 = 7
  final val U2 = 8
  final val V2 = 9
  final val X3 = 10
  final val Y3 = 11
  final val C3 = 12
  final val U3 = 13
  final val V3 = 14
  final val X4 = 15
  final val Y4 = 16
  final val C4 = 17
  final val U4 = 18
  final val V4 = 19
}
