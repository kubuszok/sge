/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/TextInputWrapper.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

trait TextInputWrapper {

  /** This method will be queried for the initial text. No guarantee of the calling thread is made */
  def getText(): String

  /** This method will be queried for the initial text selection start. No guarantee of the calling thread is made. Should be consistent with the text returned by {@link TextInputWrapper#getText()}
    */
  def getSelectionStart(): Int

  /** This method will be queried for the initial text selection end. No guarantee of the calling thread is made. Should be consistent with the text returned by {@link TextInputWrapper#getText()}
    */
  def getSelectionEnd(): Int

  /** This is called, when text was retrieved from the native input. Only use this to write back results. This will always be called on the main thread. For close logic use
    * {@link NativeInputConfiguration#setCloseCallback}
    */
  def writeResults(text: String, selectionStart: Int, selectionEnd: Int): Unit
}
