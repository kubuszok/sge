/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapObject.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: (none)
 *   Convention: all fields private with getter/setter defs, matching Java pattern
 *   Idiom: 1:1 faithful port, no null usage (Color.WHITE.cpy() is non-null)
 *   TODO: Java-style getters/setters — convert to var or def x/def x_= (setName, setColor, setOpacity, setVisible)
 *   Audited: 2026-03-03
 */
package sge
package maps

import sge.graphics.Color

/** Generic Map entity with basic attributes like name, opacity, color */
class MapObject {
  private var name:       String        = ""
  private var opacity:    Float         = 1.0f
  private var visible:    Boolean       = true
  private val properties: MapProperties = new MapProperties()
  private var color:      Color         = Color.WHITE.cpy()

  /** @return object's name */
  def getName: String = name

  /** @param name new name for the object */
  def setName(name: String): Unit =
    this.name = name

  /** @return object's color */
  def getColor: Color = color

  /** @param color new color for the object */
  def setColor(color: Color): Unit =
    this.color = color

  /** @return object's opacity */
  def getOpacity: Float = opacity

  /** @param opacity new opacity value for the object */
  def setOpacity(opacity: Float): Unit =
    this.opacity = opacity

  /** @return whether the object is visible or not */
  def isVisible: Boolean = visible

  /** @param visible toggles object's visibility */
  def setVisible(visible: Boolean): Unit =
    this.visible = visible

  /** @return object's properties set */
  def getProperties: MapProperties = properties
}
