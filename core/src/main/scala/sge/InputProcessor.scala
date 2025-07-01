package sge

/** An InputProcessor is used to receive input events from the keyboard and the touch screen (mouse on the desktop). For this it has to be registered with the
  * {@link Input#setInputProcessor(InputProcessor)} method. It will be called each frame before the call to {@link ApplicationListener#render()} . Each method returns a boolean in case you want to use
  * this with the {@link InputMultiplexer} to chain input processors.
  *
  * @author
  *   mzechner (original implementation)
  */
trait InputProcessor {

  /** Called when a key was pressed
    *
    * @param keycode
    *   one of the constants in {@link Input.Keys}
    * @return
    *   whether the input was processed
    */
  def keyDown(keycode: Int): Boolean

  /** Called when a key was released
    *
    * @param keycode
    *   one of the constants in {@link Input.Keys}
    * @return
    *   whether the input was processed
    */
  def keyUp(keycode: Int): Boolean

  /** Called when a key was typed
    *
    * @param character
    *   The character
    * @return
    *   whether the input was processed
    */
  def keyTyped(character: Char): Boolean

  /** Called when the screen was touched or a mouse button was pressed. The button parameter will be {@link Buttons#LEFT} on iOS.
    * @param screenX
    *   The x coordinate, origin is in the upper left corner
    * @param screenY
    *   The y coordinate, origin is in the upper left corner
    * @param pointer
    *   the pointer for the event.
    * @param button
    *   the button
    * @return
    *   whether the input was processed
    */
  def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean

  /** Called when a finger was lifted or a mouse button was released. The button parameter will be {@link Buttons#LEFT} on iOS.
    * @param pointer
    *   the pointer for the event.
    * @param button
    *   the button
    * @return
    *   whether the input was processed
    */
  def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean

  /** Called when the touch gesture is cancelled. Reason may be from OS interruption to touch becoming a large surface such as the user cheek). Relevant on Android and iOS only. The button parameter
    * will be {@link Buttons#LEFT} on iOS.
    * @param pointer
    *   the pointer for the event.
    * @param button
    *   the button
    * @return
    *   whether the input was processed
    */
  def touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean

  /** Called when a finger or the mouse was dragged.
    * @param pointer
    *   the pointer for the event.
    * @return
    *   whether the input was processed
    */
  def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean

  /** Called when the mouse was moved without any buttons being pressed. Will not be called on iOS.
    * @return
    *   whether the input was processed
    */
  def mouseMoved(screenX: Int, screenY: Int): Boolean

  /** Called when the mouse wheel was scrolled. Will not be called on iOS.
    * @param amountX
    *   the horizontal scroll amount, negative or positive depending on the direction the wheel was scrolled.
    * @param amountY
    *   the vertical scroll amount, negative or positive depending on the direction the wheel was scrolled.
    * @return
    *   whether the input was processed.
    */
  def scrolled(amountX: Float, amountY: Float): Boolean
}
