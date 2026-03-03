/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/Disableable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface -> Scala trait
 * - Faithful port, no API changes
 */
package sge
package scenes
package scene2d
package utils

trait Disableable {
  def setDisabled(isDisabled: Boolean): Unit

  def isDisabled: Boolean
}
