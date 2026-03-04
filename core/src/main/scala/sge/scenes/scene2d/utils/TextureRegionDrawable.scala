/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/TextureRegionDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Copy constructor: cannot call super(drawable) -> manual property copy
 * - setRegion null-check -> Nullable(region).foreach
 * - instanceof check -> pattern matching for AtlasRegion in tint()
 * - All methods faithfully ported
 * - TODO: Java-style getters/setters — setRegion
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.{ Color, Texture }
import sge.graphics.g2d.{ Batch, Sprite, TextureAtlas, TextureRegion }
import sge.utils.Nullable

/** Drawable for a {@link TextureRegion}.
  * @author
  *   Nathan Sweet
  */
class TextureRegionDrawable() extends BaseDrawable with TransformDrawable {
  private var region: TextureRegion = scala.compiletime.uninitialized

  def this(texture: Texture) = {
    this()
    setRegion(TextureRegion(texture))
  }

  def this(region: TextureRegion) = {
    this()
    setRegion(region)
  }

  def this(drawable: TextureRegionDrawable) = {
    this()
    // Copy base drawable properties
    drawable match {
      case bd: BaseDrawable =>
        setLeftWidth(bd.getLeftWidth)
        setRightWidth(bd.getRightWidth)
        setTopHeight(bd.getTopHeight)
        setBottomHeight(bd.getBottomHeight)
        setMinWidth(bd.getMinWidth)
        setMinHeight(bd.getMinHeight)
        setName(bd.getName)
    }
    setRegion(drawable.region)
  }

  override def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit =
    batch.draw(region, x, y, width, height)

  override def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    batch.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)

  def setRegion(region: TextureRegion): Unit = {
    this.region = region
    Nullable(region).foreach { r =>
      setMinWidth(r.regionWidth.toFloat)
      setMinHeight(r.regionHeight.toFloat)
    }
  }

  def getRegion: TextureRegion = region

  /** Creates a new drawable that renders the same as this drawable tinted the specified color. */
  def tint(tint: Color): Drawable = {
    val sprite: Sprite = region match {
      case ar: TextureAtlas.AtlasRegion => TextureAtlas.AtlasSprite(ar)
      case _ => Sprite(region)
    }
    sprite.color = tint
    sprite.setSize(getMinWidth, getMinHeight)
    val drawable = SpriteDrawable(sprite)
    drawable.setLeftWidth(getLeftWidth)
    drawable.setRightWidth(getRightWidth)
    drawable.setTopHeight(getTopHeight)
    drawable.setBottomHeight(getBottomHeight)
    drawable
  }
}
