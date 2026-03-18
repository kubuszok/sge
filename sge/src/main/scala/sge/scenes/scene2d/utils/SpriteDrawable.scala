/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/SpriteDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Copy constructor: cannot call super(drawable) in Scala -> manual property copy
 * - instanceof check -> Scala pattern matching for AtlasSprite in tint()
 * - All methods faithfully ported
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, Sprite, TextureAtlas }

/** Drawable for a {@link Sprite}.
  * @author
  *   Nathan Sweet
  */
class SpriteDrawable() extends BaseDrawable with TransformDrawable {
  private var _sprite: Sprite = scala.compiletime.uninitialized

  def this(sprite: Sprite) = {
    this()
    setSprite(sprite)
  }

  def this(drawable: SpriteDrawable) = {
    this()
    // Copy base drawable properties
    drawable match {
      case bd: BaseDrawable =>
        leftWidth = bd.leftWidth
        rightWidth = bd.rightWidth
        topHeight = bd.topHeight
        bottomHeight = bd.bottomHeight
        minWidth = bd.minWidth
        minHeight = bd.minHeight
        name = bd.name
    }
    setSprite(drawable.sprite)
  }

  override def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit = {
    val spriteColor = sprite.color
    val oldColor    = sprite.packedColor
    sprite.color = spriteColor.mul(batch.color)

    sprite.rotation = 0
    sprite.setScale(1, 1)
    sprite.setBounds(x, y, width, height)
    sprite.draw(batch)

    sprite.packedColor = oldColor
  }

  override def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit = {

    val spriteColor = sprite.color
    val oldColor    = sprite.packedColor
    sprite.color = spriteColor.mul(batch.color)

    sprite.setOrigin(originX, originY)
    sprite.rotation = rotation
    sprite.setScale(scaleX, scaleY)
    sprite.setBounds(x, y, width, height)
    sprite.draw(batch)

    sprite.packedColor = oldColor
  }

  def setSprite(sprite: Sprite): Unit = {
    this._sprite = sprite
    minWidth = sprite.width
    minHeight = sprite.height
  }

  def sprite: Sprite = _sprite

  /** Creates a new drawable that renders the same as this drawable tinted the specified color. */
  def tint(tint: Color): SpriteDrawable = {
    val newSprite: Sprite = sprite match {
      case as: TextureAtlas.AtlasSprite => TextureAtlas.AtlasSprite(as)
      case _ => Sprite(sprite)
    }
    newSprite.color = tint
    newSprite.setSize(minWidth, minHeight)
    val drawable = SpriteDrawable(newSprite)
    drawable.leftWidth = leftWidth
    drawable.rightWidth = rightWidth
    drawable.topHeight = topHeight
    drawable.bottomHeight = bottomHeight
    drawable
  }
}
