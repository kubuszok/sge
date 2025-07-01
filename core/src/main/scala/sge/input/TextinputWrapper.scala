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
