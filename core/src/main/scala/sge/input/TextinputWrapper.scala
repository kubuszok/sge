/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/TextInputWrapper.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package input

trait TextInputWrapper {

  def getText(): String

  def getSelectionStart(): Int

  def getSelectionEnd(): Int

  def setText(text: String): Unit

  def setPosition(position: Int): Unit

  def shouldClose(): Boolean
}
