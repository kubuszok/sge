/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/objects/TextureMapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: nullable `TextureRegion` field typed as `Nullable[TextureRegion]`
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getX/setX, getY/setY, getOriginX/Y/setOriginX/Y, getScaleX/Y/setScaleX/Y, getRotation/setRotation, getTextureRegion/setTextureRegion
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package objects

import sge.graphics.g2d.TextureRegion
import sge.utils.Nullable

/** @brief Represents a map object containing a texture (region) */
class TextureMapObject(textureRegion: Nullable[TextureRegion]) extends MapObject {

  private var x:              Float                   = 0.0f
  private var y:              Float                   = 0.0f
  private var originX:        Float                   = 0.0f
  private var originY:        Float                   = 0.0f
  private var scaleX:         Float                   = 1.0f
  private var scaleY:         Float                   = 1.0f
  private var rotation:       Float                   = 0.0f
  private var _textureRegion: Nullable[TextureRegion] = textureRegion

  /** Creates an empty texture map object */
  def this() = this(Nullable.empty)

  /** @return x axis coordinate */
  def getX: Float = x

  /** @param x new x axis coordinate */
  def setX(x: Float): Unit =
    this.x = x

  /** @return y axis coordinate */
  def getY: Float = y

  /** @param y new y axis coordinate */
  def setY(y: Float): Unit =
    this.y = y

  /** @return x axis origin */
  def getOriginX: Float = originX

  /** @param x new x axis origin */
  def setOriginX(x: Float): Unit =
    this.originX = x

  /** @return y axis origin */
  def getOriginY: Float = originY

  /** @param y new axis origin */
  def setOriginY(y: Float): Unit =
    this.originY = y

  /** @return x axis scale */
  def getScaleX: Float = scaleX

  /** @param x new x axis scale */
  def setScaleX(x: Float): Unit =
    this.scaleX = x

  /** @return y axis scale */
  def getScaleY: Float = scaleY

  /** @param y new y axis scale */
  def setScaleY(y: Float): Unit =
    this.scaleY = y

  /** @return texture's rotation in radians */
  def getRotation: Float = rotation

  /** @param rotation new texture's rotation in radians */
  def setRotation(rotation: Float): Unit =
    this.rotation = rotation

  /** @return region */
  def getTextureRegion: Nullable[TextureRegion] = _textureRegion

  /** @param region new texture region */
  def setTextureRegion(region: Nullable[TextureRegion]): Unit =
    _textureRegion = region
}
