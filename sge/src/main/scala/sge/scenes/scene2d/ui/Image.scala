/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Image.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null -> Nullable; Scaling enum -> opaque type/trait
 *   Idiom: split packages
 *   Fixes: Java-style getters/setters → Scala property accessors (drawable, scaling, imageAlign, imageX/Y/Width/Height)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 160
 * Covenant-baseline-methods: Image,_drawable,ax,ay,c,draw,drawable,drawable_,imageAlign,imageAlign_,imageHeight,imageWidth,imageX,imageY,layout,minHeight,minWidth,prefHeight,prefWidth,scaling,scaling_,setDrawable,sx,sy,this,toString
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/Image.java
 * Covenant-verified: 2026-04-19
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
class Image(initialDrawable: Nullable[Drawable] = Nullable.empty, private var _scaling: Scaling = Scaling.stretch, private var _imageAlign: Align = Align.center)(using Sge) extends Widget {

  var imageX:            Float              = 0
  var imageY:            Float              = 0
  var imageWidth:        Float              = 0
  var imageHeight:       Float              = 0
  private var _drawable: Nullable[Drawable] = Nullable.empty

  this.drawable = initialDrawable
  setSize(prefWidth, prefHeight)

  /** Creates an image stretched, and aligned center.
    * @param patch
    *   May be null.
    */
  def this(patch: NinePatch)(using Sge) = this(Nullable(NinePatchDrawable(patch)), Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center.
    * @param region
    *   May be null.
    */
  def this(region: TextureRegion)(using Sge) = this(Nullable(TextureRegionDrawable(region)), Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center. */
  def this(texture: Texture)(using Sge) = this(Nullable(TextureRegionDrawable(TextureRegion(texture))), Scaling.stretch, Align.center)

  /** Creates an image stretched, and aligned center. */
  def this(skin: Skin, drawableName: String)(using Sge) = this(Nullable(skin.getDrawable(drawableName)), Scaling.stretch, Align.center)

  override def layout(): Unit =
    _drawable.foreach { d =>
      val regionWidth  = d.minWidth
      val regionHeight = d.minHeight
      val width        = this.width
      val height       = this.height

      val size = _scaling.apply(regionWidth, regionHeight, width, height)
      imageWidth = size.x
      imageHeight = size.y

      if (_imageAlign.isLeft)
        imageX = 0
      else if (_imageAlign.isRight)
        imageX = (width - imageWidth).toInt.toFloat
      else
        imageX = (width / 2 - imageWidth / 2).toInt.toFloat

      if (_imageAlign.isTop)
        imageY = (height - imageHeight).toInt.toFloat
      else if (_imageAlign.isBottom)
        imageY = 0
      else
        imageY = (height / 2 - imageHeight / 2).toInt.toFloat
    }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val c = this.color
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)

    val ax = this.x
    val ay = this.y
    val sx = this.scaleX
    val sy = this.scaleY

    _drawable.foreach { d =>
      d match {
        case td: TransformDrawable =>
          val r = this.rotation
          if (sx != 1 || sy != 1 || r != 0) {
            td.draw(batch, ax + imageX, ay + imageY, originX - imageX, originY - imageY, imageWidth, imageHeight, sx, sy, r)
          } else {
            d.draw(batch, ax + imageX, ay + imageY, imageWidth * sx, imageHeight * sy)
          }
        case _ =>
          d.draw(batch, ax + imageX, ay + imageY, imageWidth * sx, imageHeight * sy)
      }
    }
  }

  def drawable: Nullable[Drawable] = _drawable

  /** Sets a new drawable for the image. The image's pref size is the drawable's min size. If using the image actor's size rather than the pref size, {@link #pack()} can be used to size the image to
    * its pref size.
    * @param drawable
    *   May be null.
    */
  def drawable_=(drawable: Nullable[Drawable]): Unit =
    if (this._drawable != drawable) {
      drawable.fold {
        invalidateHierarchy()
      } { d =>
        if (prefWidth != d.minWidth || prefHeight != d.minHeight) invalidateHierarchy()
      }
      this._drawable = drawable
    }

  def setDrawable(skin: Skin, drawableName: String): Unit =
    this.drawable = Nullable(skin.getDrawable(drawableName))

  def scaling: Scaling = _scaling

  def scaling_=(scaling: Scaling): Unit = {
    this._scaling = scaling
    invalidate()
  }

  def imageAlign: Align = _imageAlign

  def imageAlign_=(align: Align): Unit = {
    this._imageAlign = align
    invalidate()
  }

  override def minWidth: Float = 0

  override def minHeight: Float = 0

  override def prefWidth: Float = _drawable.map(_.minWidth).getOrElse(0f)

  override def prefHeight: Float = _drawable.map(_.minHeight).getOrElse(0f)

  override def toString: String =
    name.getOrElse {
      var className = getClass.getName
      val dotIndex  = className.lastIndexOf('.')
      if (dotIndex != -1) className = className.substring(dotIndex + 1)
      (if (className.indexOf('$') != -1) "Image " else "") + className + ": " + _drawable
    }
}
