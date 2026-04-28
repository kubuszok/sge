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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: TextInputWrapper,selectionEnd,selectionStart,text,writeResults
 * Covenant-source-reference: com/badlogic/gdx/input/TextInputWrapper.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3b8493d39e25e352c32c088243be406cc49fb435
 */
package sge
package input

trait TextInputWrapper {

  /** This method will be queried for the initial text. No guarantee of the calling thread is made */
  def text: String

  /** This method will be queried for the initial text selection start. No guarantee of the calling thread is made. Should be consistent with the text returned by {@link TextInputWrapper#getText()}
    */
  def selectionStart: Int

  /** This method will be queried for the initial text selection end. No guarantee of the calling thread is made. Should be consistent with the text returned by {@link TextInputWrapper#getText()}
    */
  def selectionEnd: Int

  /** This is called, when text was retrieved from the native input. Only use this to write back results. This will always be called on the main thread. For close logic use
    * {@link NativeInputConfiguration#setCloseCallback}
    */
  def writeResults(text: String, selectionStart: Int, selectionEnd: Int): Unit
}
