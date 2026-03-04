/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Image.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; Scaling enum -> opaque type/trait; Skin constructor and setDrawable(Skin, String) commented out
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getDrawable/setDrawable, getAlign/setAlign, getImageX/Y, getImageWidth/Height
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.g2d.{ Batch, NinePatch, TextureRegion }
import sge.graphics.Texture
import sge.scenes.scene2d.utils.{ Drawable, NinePatchDrawable, TextureRegionDrawable, TransformDrawable }
import sge.utils.{ Align, Nullable, Scaling }

/** Displays a {@link Drawable}, scaled various way within the widgets bounds. The preferred size is the min size of the drawable. Only when using a {@link TextureRegionDrawable} will the actor's
  * scale, rotation, and origin be used when drawing.
  * @author
  *   Nathan Sweet
  */
class Image(drawable: Nullable[Drawable], private var scaling: Scaling, private var align: Align) extends Widget {

  private var imageX:      Float              = 0
  private var imageY:      Float              = 0
  private var imageWidth:  Float              = 0
  private var imageHeight: Float              = 0
  private var _drawable:   Nullable[Drawable] = Nullable.empty

  setDrawable(drawable)
  setSize(getPrefWidth, getPrefHeight)

  /** Creates an image with no drawable, stretched, and aligned center. */
  def this() = this(Nullable.empty, Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center.
    * @param patch
    *   May be null.
    */
  def this(patch: NinePatch) = this(Nullable(new NinePatchDrawable(patch)), Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center.
    * @param region
    *   May be null.
    */
  def this(region: TextureRegion) = this(Nullable(new TextureRegionDrawable(region)), Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center. */
  def this(texture: Texture) = this(Nullable(new TextureRegionDrawable(new TextureRegion(texture))), Scaling.stretch, Align.center)

  // /** Creates an image stretched, and aligned center. */
  // def this(skin: Skin, drawableName: String) = this(skin.getDrawable(drawableName), Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center.
    * @param drawable
    *   May be null.
    */
  def this(drawable: Nullable[Drawable]) = this(drawable, Scaling.stretch, Align.center)

  /** Creates an image aligned center.
    * @param drawable
    *   May be null.
    */
  def this(drawable: Nullable[Drawable], scaling: Scaling) = this(drawable, scaling, Align.center)

  override def layout(): Unit =
    _drawable.foreach { d =>
      val regionWidth  = d.getMinWidth
      val regionHeight = d.getMinHeight
      val width        = getWidth
      val height       = getHeight

      val size = scaling.apply(regionWidth, regionHeight, width, height)
      imageWidth = size.x
      imageHeight = size.y

      if (align.isLeft)
        imageX = 0
      else if (align.isRight)
        imageX = (width - imageWidth).toInt.toFloat
      else
        imageX = (width / 2 - imageWidth / 2).toInt.toFloat

      if (align.isTop)
        imageY = (height - imageHeight).toInt.toFloat
      else if (align.isBottom)
        imageY = 0
      else
        imageY = (height / 2 - imageHeight / 2).toInt.toFloat
    }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val color = getColor
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)

    val x      = getX
    val y      = getY
    val scaleX = getScaleX
    val scaleY = getScaleY

    _drawable.foreach { d =>
      d match {
        case td: TransformDrawable =>
          val rotation = getRotation
          if (scaleX != 1 || scaleY != 1 || rotation != 0) {
            td.draw(batch, x + imageX, y + imageY, getOriginX - imageX, getOriginY - imageY, imageWidth, imageHeight, scaleX, scaleY, rotation)
          } else {
            d.draw(batch, x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY)
          }
        case _ =>
          d.draw(batch, x + imageX, y + imageY, imageWidth * scaleX, imageHeight * scaleY)
      }
    }
  }

  // def setDrawable(skin: Skin, drawableName: String): Unit = {
  //   setDrawable(skin.getDrawable(drawableName))
  // }

  /** Sets a new drawable for the image. The image's pref size is the drawable's min size. If using the image actor's size rather than the pref size, {@link #pack()} can be used to size the image to
    * its pref size.
    * @param drawable
    *   May be null.
    */
  def setDrawable(drawable: Nullable[Drawable]): Unit =
    if (this._drawable != drawable) {
      drawable.fold {
        invalidateHierarchy()
      } { d =>
        if (getPrefWidth != d.getMinWidth || getPrefHeight != d.getMinHeight) invalidateHierarchy()
      }
      this._drawable = drawable
    }

  /** @return May be null. */
  def getDrawable: Nullable[Drawable] = _drawable

  def setScaling(scaling: Scaling): Unit = {
    this.scaling = scaling
    invalidate()
  }

  def setAlign(align: Align): Unit = {
    this.align = align
    invalidate()
  }

  def getAlign: Align = align

  override def getMinWidth: Float = 0

  override def getMinHeight: Float = 0

  override def getPrefWidth: Float = _drawable.fold(0f)(_.getMinWidth)

  override def getPrefHeight: Float = _drawable.fold(0f)(_.getMinHeight)

  def getImageX: Float = imageX

  def getImageY: Float = imageY

  def getImageWidth: Float = imageWidth

  def getImageHeight: Float = imageHeight

  override def toString: String =
    getName.getOrElse {
      var className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      if (dotIndex != -1) className = className.substring(dotIndex + 1)
      (if (className.indexOf('$') != -1) "Image " else "") + className + ": " + _drawable
    }
}
