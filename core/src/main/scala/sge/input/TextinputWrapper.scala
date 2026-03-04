/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/TextInputWrapper.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait
 *   Idiom: split packages
 *   TODOs: 0
 *   Missing: writeResults(String, Int, Int) replaced by setText + setPosition + shouldClose (API divergence)
 *   Missing: Javadoc comments from original source
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
