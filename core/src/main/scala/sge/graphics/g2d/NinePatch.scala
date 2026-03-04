/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/NinePatch.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: TextureRegion... patches varargs -> Nullable[TextureRegion]*
 *   Convention: Nullable for optional patch regions
 *   Idiom: boundary/break, Nullable, split packages
 *   TODO: Java-style getters/setters — getColor/setColor, getLeftWidth/setLeftWidth, getRightWidth/setRightWidth, getTopHeight/setTopHeight, getBottomHeight/setBottomHeight, getPadLeft/Right/Top/Bottom
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.Color
import sge.graphics.Texture
import sge.graphics.Texture.TextureFilter
import sge.math.MathUtils
import sge.utils.Nullable

/** A 3x3 grid of texture regions. Any of the regions may be omitted. Padding may be set as a hint on how to inset content on top of the ninepatch (by default the eight "edge" textures of the
  * ninepatch define the padding). When drawn, the four corner patches will not be scaled, the interior patch will be scaled in both directions, and the middle patch for each edge will be scaled in
  * only one direction. <p> Note this class does not accept ".9.png" textures that include the metadata border pixels describing the splits (and padding) for the ninepatch. That information is either
  * passed to a constructor or defined implicitly by the size of the individual patch textures. {@link TextureAtlas} is one way to generate a postprocessed ninepatch texture regions from ".9.png"
  * files.
  */
class NinePatch {
  private var texture:      Texture      = scala.compiletime.uninitialized
  private var bottomLeft:   Int          = scala.compiletime.uninitialized
  private var bottomCenter: Int          = scala.compiletime.uninitialized
  private var bottomRight:  Int          = scala.compiletime.uninitialized
  private var middleLeft:   Int          = scala.compiletime.uninitialized
  private var middleCenter: Int          = scala.compiletime.uninitialized
  private var middleRight:  Int          = scala.compiletime.uninitialized
  private var topLeft:      Int          = scala.compiletime.uninitialized
  private var topCenter:    Int          = scala.compiletime.uninitialized
  private var topRight:     Int          = scala.compiletime.uninitialized
  private var leftWidth:    Float        = scala.compiletime.uninitialized
  private var rightWidth:   Float        = scala.compiletime.uninitialized
  private var middleWidth:  Float        = scala.compiletime.uninitialized
  private var middleHeight: Float        = scala.compiletime.uninitialized
  private var topHeight:    Float        = scala.compiletime.uninitialized
  private var bottomHeight: Float        = scala.compiletime.uninitialized
  private var vertices:     Array[Float] = new Array[Float](9 * 4 * 5)
  private var idx:          Int          = scala.compiletime.uninitialized
  private val color:        Color        = new Color(Color.WHITE)
  private var padLeft:      Float        = -1
  private var padRight:     Float        = -1
  private var padTop:       Float        = -1
  private var padBottom:    Float        = -1

  /** Create a ninepatch by cutting up the given texture into nine patches. The subsequent parameters define the 4 lines that will cut the texture region into 9 pieces.
    * @param left
    *   Pixels from left edge.
    * @param right
    *   Pixels from right edge.
    * @param top
    *   Pixels from top edge.
    * @param bottom
    *   Pixels from bottom edge.
    */
  def this(texture: Texture, left: Int, right: Int, top: Int, bottom: Int) = {
    this()
    val region = new TextureRegion(texture)
    initializeFromRegion(region, left, right, top, bottom)
  }

  /** Create a ninepatch by cutting up the given texture region into nine patches. The subsequent parameters define the 4 lines that will cut the texture region into 9 pieces.
    * @param left
    *   Pixels from left edge.
    * @param right
    *   Pixels from right edge.
    * @param top
    *   Pixels from top edge.
    * @param bottom
    *   Pixels from bottom edge.
    */
  def this(region: TextureRegion, left: Int, right: Int, top: Int, bottom: Int) = {
    this()
    initializeFromRegion(region, left, right, top, bottom)
  }

  /** Construct a degenerate "nine" patch with only a center component. */
  def this(texture: Texture, color: Color) = {
    this()
    initializeFromTexture(texture)
    setColor(color)
  }

  /** Construct a degenerate "nine" patch with only a center component. */
  def this(texture: Texture) = {
    this()
    initializeFromTexture(texture)
  }

  /** Construct a degenerate "nine" patch with only a center component. */
  def this(region: TextureRegion, color: Color) = {
    this()
    initializeFromRegion(region)
    setColor(color)
  }

  /** Construct a degenerate "nine" patch with only a center component. */
  def this(region: TextureRegion) = {
    this()
    initializeFromRegion(region)
  }

  /** Construct a nine patch from the given nine texture regions. The provided patches must be consistently sized (e.g., any left edge textures must have the same width, etc). Patches may be
    * <code>null</code>. Patch indices are specified via the public members {@link #TOP_LEFT} , {@link #TOP_CENTER} , etc.
    */
  def this(patches: TextureRegion*) = {
    this()
    val patchArray: Array[Nullable[TextureRegion]] = patches.map(Nullable(_)).toArray
    if (patchArray.length != 9) throw new IllegalArgumentException("NinePatch needs nine TextureRegions")

    load(patchArray)
    validatePatches(patchArray)
  }

  def this(ninePatch: NinePatch) = {
    this()
    copyFrom(ninePatch, ninePatch.color)
  }

  def this(ninePatch: NinePatch, color: Color) = {
    this()
    copyFrom(ninePatch, color)
  }

  private def initializeFromRegion(region: TextureRegion, left: Int, right: Int, top: Int, bottom: Int): Unit = {
    val middleWidth  = region.getRegionWidth() - left - right
    val middleHeight = region.getRegionHeight() - top - bottom

    val patches = Array.fill(9)(Nullable.empty[TextureRegion])
    if (top > 0) {
      if (left > 0) patches(NinePatch.TOP_LEFT) = Nullable(new TextureRegion(region, 0, 0, left, top))
      if (middleWidth > 0) patches(NinePatch.TOP_CENTER) = Nullable(new TextureRegion(region, left, 0, middleWidth, top))
      if (right > 0) patches(NinePatch.TOP_RIGHT) = Nullable(new TextureRegion(region, left + middleWidth, 0, right, top))
    }
    if (middleHeight > 0) {
      if (left > 0) patches(NinePatch.MIDDLE_LEFT) = Nullable(new TextureRegion(region, 0, top, left, middleHeight))
      if (middleWidth > 0) patches(NinePatch.MIDDLE_CENTER) = Nullable(new TextureRegion(region, left, top, middleWidth, middleHeight))
      if (right > 0) patches(NinePatch.MIDDLE_RIGHT) = Nullable(new TextureRegion(region, left + middleWidth, top, right, middleHeight))
    }
    if (bottom > 0) {
      if (left > 0) patches(NinePatch.BOTTOM_LEFT) = Nullable(new TextureRegion(region, 0, top + middleHeight, left, bottom))
      if (middleWidth > 0) patches(NinePatch.BOTTOM_CENTER) = Nullable(new TextureRegion(region, left, top + middleHeight, middleWidth, bottom))
      if (right > 0) patches(NinePatch.BOTTOM_RIGHT) = Nullable(new TextureRegion(region, left + middleWidth, top + middleHeight, right, bottom))
    }

    // If split only vertical, move splits from right to center.
    if (left == 0 && middleWidth == 0) {
      patches(NinePatch.TOP_CENTER) = patches(NinePatch.TOP_RIGHT)
      patches(NinePatch.MIDDLE_CENTER) = patches(NinePatch.MIDDLE_RIGHT)
      patches(NinePatch.BOTTOM_CENTER) = patches(NinePatch.BOTTOM_RIGHT)
      patches(NinePatch.TOP_RIGHT) = Nullable.empty
      patches(NinePatch.MIDDLE_RIGHT) = Nullable.empty
      patches(NinePatch.BOTTOM_RIGHT) = Nullable.empty
    }
    // If split only horizontal, move splits from bottom to center.
    if (top == 0 && middleHeight == 0) {
      patches(NinePatch.MIDDLE_LEFT) = patches(NinePatch.BOTTOM_LEFT)
      patches(NinePatch.MIDDLE_CENTER) = patches(NinePatch.BOTTOM_CENTER)
      patches(NinePatch.MIDDLE_RIGHT) = patches(NinePatch.BOTTOM_RIGHT)
      patches(NinePatch.BOTTOM_LEFT) = Nullable.empty
      patches(NinePatch.BOTTOM_CENTER) = Nullable.empty
      patches(NinePatch.BOTTOM_RIGHT) = Nullable.empty
    }

    load(patches)
  }

  private def initializeFromTexture(texture: Texture): Unit = {
    val region = new TextureRegion(texture)
    initializeFromRegion(region)
  }

  private def initializeFromRegion(region: TextureRegion): Unit =
    load(
      Array[Nullable[TextureRegion]](
        //
        Nullable.empty,
        Nullable.empty,
        Nullable.empty, //
        Nullable.empty,
        Nullable(region),
        Nullable.empty, //
        Nullable.empty,
        Nullable.empty,
        Nullable.empty //
      )
    )

  private def validatePatches(patches: Array[Nullable[TextureRegion]]): Unit = {
    if (
      patches(NinePatch.TOP_LEFT).fold(false)(_.getRegionWidth() != leftWidth)
      || patches(NinePatch.MIDDLE_LEFT).fold(false)(_.getRegionWidth() != leftWidth)
      || patches(NinePatch.BOTTOM_LEFT).fold(false)(_.getRegionWidth() != leftWidth)
    ) {
      throw new IllegalArgumentException("Left side patches must have the same width")
    }
    if (
      patches(NinePatch.TOP_RIGHT).fold(false)(_.getRegionWidth() != rightWidth)
      || patches(NinePatch.MIDDLE_RIGHT).fold(false)(_.getRegionWidth() != rightWidth)
      || patches(NinePatch.BOTTOM_RIGHT).fold(false)(_.getRegionWidth() != rightWidth)
    ) {
      throw new IllegalArgumentException("Right side patches must have the same width")
    }
    if (
      patches(NinePatch.BOTTOM_LEFT).fold(false)(_.getRegionHeight() != bottomHeight)
      || patches(NinePatch.BOTTOM_CENTER).fold(false)(_.getRegionHeight() != bottomHeight)
      || patches(NinePatch.BOTTOM_RIGHT).fold(false)(_.getRegionHeight() != bottomHeight)
    ) {
      throw new IllegalArgumentException("Bottom side patches must have the same height")
    }
    if (
      patches(NinePatch.TOP_LEFT).fold(false)(_.getRegionHeight() != topHeight)
      || patches(NinePatch.TOP_CENTER).fold(false)(_.getRegionHeight() != topHeight)
      || patches(NinePatch.TOP_RIGHT).fold(false)(_.getRegionHeight() != topHeight)
    ) {
      throw new IllegalArgumentException("Top side patches must have the same height")
    }
  }

  private def copyFrom(ninePatch: NinePatch, color: Color): Unit = {
    texture = ninePatch.texture

    bottomLeft = ninePatch.bottomLeft
    bottomCenter = ninePatch.bottomCenter
    bottomRight = ninePatch.bottomRight
    middleLeft = ninePatch.middleLeft
    middleCenter = ninePatch.middleCenter
    middleRight = ninePatch.middleRight
    topLeft = ninePatch.topLeft
    topCenter = ninePatch.topCenter
    topRight = ninePatch.topRight

    leftWidth = ninePatch.leftWidth
    rightWidth = ninePatch.rightWidth
    middleWidth = ninePatch.middleWidth
    middleHeight = ninePatch.middleHeight
    topHeight = ninePatch.topHeight
    bottomHeight = ninePatch.bottomHeight

    padLeft = ninePatch.padLeft
    padTop = ninePatch.padTop
    padBottom = ninePatch.padBottom
    padRight = ninePatch.padRight

    vertices = new Array[Float](ninePatch.vertices.length)
    System.arraycopy(ninePatch.vertices, 0, vertices, 0, ninePatch.vertices.length)
    idx = ninePatch.idx
    this.color.set(color)
  }

  private def load(patches: Array[Nullable[TextureRegion]]): Unit = {
    patches(NinePatch.BOTTOM_LEFT).fold {
      bottomLeft = -1
    } { p =>
      bottomLeft = add(p, isStretchW = false, isStretchH = false)
      leftWidth = p.getRegionWidth().toFloat
      bottomHeight = p.getRegionHeight().toFloat
    }
    patches(NinePatch.BOTTOM_CENTER).fold {
      bottomCenter = -1
    } { p =>
      bottomCenter = add(p, patches(NinePatch.BOTTOM_LEFT).isDefined || patches(NinePatch.BOTTOM_RIGHT).isDefined, isStretchH = false)
      middleWidth = Math.max(middleWidth, p.getRegionWidth().toFloat)
      bottomHeight = Math.max(bottomHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.BOTTOM_RIGHT).fold {
      bottomRight = -1
    } { p =>
      bottomRight = add(p, isStretchW = false, isStretchH = false)
      rightWidth = Math.max(rightWidth, p.getRegionWidth().toFloat)
      bottomHeight = Math.max(bottomHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.MIDDLE_LEFT).fold {
      middleLeft = -1
    } { p =>
      middleLeft = add(p, isStretchW = false, patches(NinePatch.TOP_LEFT).isDefined || patches(NinePatch.BOTTOM_LEFT).isDefined)
      leftWidth = Math.max(leftWidth, p.getRegionWidth().toFloat)
      middleHeight = Math.max(middleHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.MIDDLE_CENTER).fold {
      middleCenter = -1
    } { p =>
      middleCenter = add(
        p,
        patches(NinePatch.MIDDLE_LEFT).isDefined || patches(NinePatch.MIDDLE_RIGHT).isDefined,
        patches(NinePatch.TOP_CENTER).isDefined || patches(NinePatch.BOTTOM_CENTER).isDefined
      )
      middleWidth = Math.max(middleWidth, p.getRegionWidth().toFloat)
      middleHeight = Math.max(middleHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.MIDDLE_RIGHT).fold {
      middleRight = -1
    } { p =>
      middleRight = add(p, isStretchW = false, patches(NinePatch.TOP_RIGHT).isDefined || patches(NinePatch.BOTTOM_RIGHT).isDefined)
      rightWidth = Math.max(rightWidth, p.getRegionWidth().toFloat)
      middleHeight = Math.max(middleHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.TOP_LEFT).fold {
      topLeft = -1
    } { p =>
      topLeft = add(p, isStretchW = false, isStretchH = false)
      leftWidth = Math.max(leftWidth, p.getRegionWidth().toFloat)
      topHeight = Math.max(topHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.TOP_CENTER).fold {
      topCenter = -1
    } { p =>
      topCenter = add(p, patches(NinePatch.TOP_LEFT).isDefined || patches(NinePatch.TOP_RIGHT).isDefined, isStretchH = false)
      middleWidth = Math.max(middleWidth, p.getRegionWidth().toFloat)
      topHeight = Math.max(topHeight, p.getRegionHeight().toFloat)
    }
    patches(NinePatch.TOP_RIGHT).fold {
      topRight = -1
    } { p =>
      topRight = add(p, isStretchW = false, isStretchH = false)
      rightWidth = Math.max(rightWidth, p.getRegionWidth().toFloat)
      topHeight = Math.max(topHeight, p.getRegionHeight().toFloat)
    }
    if (idx < vertices.length) {
      val newVertices = new Array[Float](idx)
      System.arraycopy(vertices, 0, newVertices, 0, idx)
      vertices = newVertices
    }
  }

  private def add(region: TextureRegion, isStretchW: Boolean, isStretchH: Boolean): Int = {
    if (Nullable(texture).isEmpty)
      texture = region.getTexture()
    else if (texture != region.getTexture())
      throw new IllegalArgumentException("All regions must be from the same texture.")

    // Add half pixel offsets on stretchable dimensions to avoid color bleeding when GL_LINEAR
    // filtering is used for the texture. This nudges the texture coordinate to the center
    // of the texel where the neighboring pixel has 0% contribution in linear blending mode.
    var u  = region.getU()
    var v  = region.getV()
    var u2 = region.getU2()
    var v2 = region.getV2()
    if (texture.getMagFilter() == TextureFilter.Linear || texture.getMinFilter() == TextureFilter.Linear) {
      if (isStretchW) {
        val halfTexelWidth = 0.5f * 1f / texture.getWidth
        u += halfTexelWidth
        u2 -= halfTexelWidth
      }
      if (isStretchH) {
        val halfTexelHeight = 0.5f * 1f / texture.getHeight
        v -= halfTexelHeight
        v2 += halfTexelHeight
      }
    }

    val vertices = this.vertices
    val i        = idx
    vertices(i + 3) = u
    vertices(i + 4) = v

    vertices(i + 8) = u
    vertices(i + 9) = v2

    vertices(i + 13) = u2
    vertices(i + 14) = v2

    vertices(i + 18) = u2
    vertices(i + 19) = v
    idx += 20
    i
  }

  /** Set the coordinates and color of a ninth of the patch. */
  private def set(idx: Int, x: Float, y: Float, width: Float, height: Float, color: Float): Unit = {
    val fx2      = x + width
    val fy2      = y + height
    val vertices = this.vertices
    vertices(idx) = x
    vertices(idx + 1) = y
    vertices(idx + 2) = color

    vertices(idx + 5) = x
    vertices(idx + 6) = fy2
    vertices(idx + 7) = color

    vertices(idx + 10) = fx2
    vertices(idx + 11) = fy2
    vertices(idx + 12) = color

    vertices(idx + 15) = fx2
    vertices(idx + 16) = y
    vertices(idx + 17) = color
  }

  private def prepareVertices(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit = {
    val centerX      = x + leftWidth
    val centerY      = y + bottomHeight
    val centerWidth  = width - rightWidth - leftWidth
    val centerHeight = height - topHeight - bottomHeight
    val rightX       = x + width - rightWidth
    val topY         = y + height - topHeight
    val c            = NinePatch.tmpDrawColor.set(color).mul(batch.getColor()).toFloatBits()
    if (bottomLeft != -1) set(bottomLeft, x, y, leftWidth, bottomHeight, c)
    if (bottomCenter != -1) set(bottomCenter, centerX, y, centerWidth, bottomHeight, c)
    if (bottomRight != -1) set(bottomRight, rightX, y, rightWidth, bottomHeight, c)
    if (middleLeft != -1) set(middleLeft, x, centerY, leftWidth, centerHeight, c)
    if (middleCenter != -1) set(middleCenter, centerX, centerY, centerWidth, centerHeight, c)
    if (middleRight != -1) set(middleRight, rightX, centerY, rightWidth, centerHeight, c)
    if (topLeft != -1) set(topLeft, x, topY, leftWidth, topHeight, c)
    if (topCenter != -1) set(topCenter, centerX, topY, centerWidth, topHeight, c)
    if (topRight != -1) set(topRight, rightX, topY, rightWidth, topHeight, c)
  }

  def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit = {
    prepareVertices(batch, x, y, width, height)
    batch.draw(texture, vertices, 0, idx)
  }

  def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {
    prepareVertices(batch, x, y, width, height)
    val worldOriginX = x + originX
    val worldOriginY = y + originY
    val n            = this.idx
    val vertices     = this.vertices
    if (rotation != 0) {
      for (i <- 0 until n by 5) {
        val vx  = (vertices(i) - worldOriginX) * scaleX
        val vy  = (vertices(i + 1) - worldOriginY) * scaleY
        val cos = MathUtils.cosDeg(rotation)
        val sin = MathUtils.sinDeg(rotation)
        vertices(i) = cos * vx - sin * vy + worldOriginX
        vertices(i + 1) = sin * vx + cos * vy + worldOriginY
      }
    } else if (scaleX != 1 || scaleY != 1) {
      for (i <- 0 until n by 5) {
        vertices(i) = (vertices(i) - worldOriginX) * scaleX + worldOriginX
        vertices(i + 1) = (vertices(i + 1) - worldOriginY) * scaleY + worldOriginY
      }
    }
    batch.draw(texture, vertices, 0, n)
  }

  /** Copy given color. The color will be blended with the batch color, then combined with the texture colors at {@link NinePatch#draw(Batch, float, float, float, float) draw} time. Default is
    * {@link Color#WHITE} .
    */
  def setColor(color: Color): Unit =
    this.color.set(color)

  def getColor(): Color =
    color

  def getLeftWidth(): Float =
    leftWidth

  /** Set the draw-time width of the three left edge patches */
  def setLeftWidth(leftWidth: Float): Unit =
    this.leftWidth = leftWidth

  def getRightWidth(): Float =
    rightWidth

  /** Set the draw-time width of the three right edge patches */
  def setRightWidth(rightWidth: Float): Unit =
    this.rightWidth = rightWidth

  def getTopHeight(): Float =
    topHeight

  /** Set the draw-time height of the three top edge patches */
  def setTopHeight(topHeight: Float): Unit =
    this.topHeight = topHeight

  def getBottomHeight(): Float =
    bottomHeight

  /** Set the draw-time height of the three bottom edge patches */
  def setBottomHeight(bottomHeight: Float): Unit =
    this.bottomHeight = bottomHeight

  def getMiddleWidth(): Float =
    middleWidth

  /** Set the width of the middle column of the patch. At render time, this is implicitly the requested render-width of the entire nine patch, minus the left and right width. This value is only used
    * for computing the {@link #getTotalWidth() default total width} .
    */
  def setMiddleWidth(middleWidth: Float): Unit =
    this.middleWidth = middleWidth

  def getMiddleHeight(): Float =
    middleHeight

  /** Set the height of the middle row of the patch. At render time, this is implicitly the requested render-height of the entire nine patch, minus the top and bottom height. This value is only used
    * for computing the {@link #getTotalHeight() default total height} .
    */
  def setMiddleHeight(middleHeight: Float): Unit =
    this.middleHeight = middleHeight

  def getTotalWidth(): Float =
    leftWidth + middleWidth + rightWidth

  def getTotalHeight(): Float =
    topHeight + middleHeight + bottomHeight

  /** Set the padding for content inside this ninepatch. By default the padding is set to match the exterior of the ninepatch, so the content should fit exactly within the middle patch.
    */
  def setPadding(left: Float, right: Float, top: Float, bottom: Float): Unit = {
    this.padLeft = left
    this.padRight = right
    this.padTop = top
    this.padBottom = bottom
  }

  /** Returns the left padding if set, else returns {@link #getLeftWidth()}. */
  def getPadLeft(): Float =
    if (padLeft == -1) getLeftWidth() else padLeft

  /** See {@link #setPadding(float, float, float, float)} */
  def setPadLeft(left: Float): Unit =
    this.padLeft = left

  /** Returns the right padding if set, else returns {@link #getRightWidth()}. */
  def getPadRight(): Float =
    if (padRight == -1) getRightWidth() else padRight

  /** See {@link #setPadding(float, float, float, float)} */
  def setPadRight(right: Float): Unit =
    this.padRight = right

  /** Returns the top padding if set, else returns {@link #getTopHeight()}. */
  def getPadTop(): Float =
    if (padTop == -1) getTopHeight() else padTop

  /** See {@link #setPadding(float, float, float, float)} */
  def setPadTop(top: Float): Unit =
    this.padTop = top

  /** Returns the bottom padding if set, else returns {@link #getBottomHeight()}. */
  def getPadBottom(): Float =
    if (padBottom == -1) getBottomHeight() else padBottom

  /** See {@link #setPadding(float, float, float, float)} */
  def setPadBottom(bottom: Float): Unit =
    this.padBottom = bottom

  /** Multiplies the top/left/bottom/right sizes and padding by the specified amount. */
  def scale(scaleX: Float, scaleY: Float): Unit = {
    leftWidth *= scaleX
    rightWidth *= scaleX
    topHeight *= scaleY
    bottomHeight *= scaleY
    middleWidth *= scaleX
    middleHeight *= scaleY
    if (padLeft != -1) padLeft *= scaleX
    if (padRight != -1) padRight *= scaleX
    if (padTop != -1) padTop *= scaleY
    if (padBottom != -1) padBottom *= scaleY
  }

  def getTexture(): Texture =
    texture
}

object NinePatch {
  val TOP_LEFT      = 0
  val TOP_CENTER    = 1
  val TOP_RIGHT     = 2
  val MIDDLE_LEFT   = 3
  val MIDDLE_CENTER = 4
  val MIDDLE_RIGHT  = 5
  val BOTTOM_LEFT   = 6

  /** Indices for the {@link #NinePatch(TextureRegion...)} constructor. */
  val BOTTOM_CENTER = 7 // This field has the javadoc comment because it appears first in the javadocs.
  val BOTTOM_RIGHT  = 8

  private val tmpDrawColor = new Color()
}
