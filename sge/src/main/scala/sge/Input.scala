/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Input.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait
 *   Idiom: split packages; Nullable used
 *   Issues: None
 *   Convention: Input.Keys constants → opaque type Key; Input.Buttons constants → opaque type Button
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1052
 * Covenant-baseline-methods: A,ALT_LEFT,ALT_RIGHT,ANY_KEY,APOSTROPHE,AT,B,BACK,BACKSLASH,BACKSPACE,BUTTON_A,BUTTON_B,BUTTON_C,BUTTON_CIRCLE,BUTTON_L1,BUTTON_L2,BUTTON_MODE,BUTTON_R1,BUTTON_R2,BUTTON_SELECT,BUTTON_START,BUTTON_THUMBL,BUTTON_THUMBR,BUTTON_X,BUTTON_Y,BUTTON_Z,Button,Buttons,C,CALL,CAMERA,CAPS_LOCK,CENTER,CLEAR,COLON,COMMA,CONTROL_LEFT,CONTROL_RIGHT,D,DEL,DOWN,DPAD_CENTER,DPAD_DOWN,DPAD_LEFT,DPAD_RIGHT,DPAD_UP,E,END,ENDCALL,ENTER,ENVELOPE,EQUALS,ESCAPE,EXPLORER,F,F1,F10,F11,F12,F13,F14,F15,F16,F17,F18,F19,F2,F20,F21,F22,F23,F24,F3,F4,F5,F6,F7,F8,F9,FOCUS,FORWARD,FORWARD_DEL,G,GRAVE,H,HEADSETHOOK,HOME,I,INSERT,Input,InputStringValidator,J,K,Key,KeyboardHeightObserver,Keys,L,LEFT,LEFT_BRACKET,M,MAX_KEYCODE,MEDIA_FAST_FORWARD,MEDIA_NEXT,MEDIA_PLAY_PAUSE,MEDIA_PREVIOUS,MEDIA_REWIND,MEDIA_STOP,MENU,META_ALT_LEFT_ON,META_ALT_ON,META_ALT_RIGHT_ON,META_SHIFT_LEFT_ON,META_SHIFT_ON,META_SHIFT_RIGHT_ON,META_SYM_ON,MIDDLE,MINUS,MUTE,N,NOTIFICATION,NUM,NUMPAD_0,NUMPAD_1,NUMPAD_2,NUMPAD_3,NUMPAD_4,NUMPAD_5,NUMPAD_6,NUMPAD_7,NUMPAD_8,NUMPAD_9,NUMPAD_ADD,NUMPAD_COMMA,NUMPAD_DIVIDE,NUMPAD_DOT,NUMPAD_ENTER,NUMPAD_EQUALS,NUMPAD_LEFT_PAREN,NUMPAD_MULTIPLY,NUMPAD_RIGHT_PAREN,NUMPAD_SUBTRACT,NUM_0,NUM_1,NUM_2,NUM_3,NUM_4,NUM_5,NUM_6,NUM_7,NUM_8,NUM_9,NUM_LOCK,O,OnscreenKeyboardType,Orientation,P,PAGE_DOWN,PAGE_UP,PAUSE,PERIOD,PICTSYMBOLS,PLUS,POUND,POWER,PRINT_SCREEN,Peripheral,Q,R,RIGHT,RIGHT_BRACKET,S,SCROLL_LOCK,SEARCH,SEMICOLON,SHIFT_LEFT,SHIFT_RIGHT,SLASH,SOFT_LEFT,SOFT_RIGHT,SPACE,STAR,SWITCH_CHARSET,SYM,T,TAB,TextInputListener,U,UNKNOWN,UP,V,VOLUME_DOWN,VOLUME_UP,VibrationType,W,WORLD_1,WORLD_2,X,Y,Z,accelerometerX,accelerometerY,accelerometerZ,apply,azimuth,canceled,closeTextInputField,currentEventTime,cursorCatched,deltaX,deltaY,getRotationMatrix,getTextInput,gyroscopeX,gyroscopeY,gyroscopeZ,initializeKeyNames,input,inputProcessor,isButtonJustPressed,isButtonPressed,isCatchKey,isKeyJustPressed,isKeyPressed,isPeripheralAvailable,isTouched,justTouched,keyNames,maxPointers,nativeOrientation,onKeyboardHeightChanged,openTextInputField,pitch,pressure,roll,rotation,setCatchKey,setCursorCatched,setCursorPosition,setInputProcessor,setKeyboardHeightObserver,setOnscreenKeyboardVisible,textInputFieldOpened,toInt,toString,touched,validate,valueOf,vibrate,x,y
 * Covenant-source-reference: com/badlogic/gdx/Input.java
 * Covenant-verified: 2026-04-19
 */
package sge

import sge.utils.{ Nanos, Nullable }

/** <p> Interface to the input facilities. This allows polling the state of the keyboard, the touch screen and the accelerometer. On some backends (desktop, gwt, etc) the touch screen is replaced by
  * mouse input. The accelerometer is of course not available on all backends. </p>
  *
  * <p> Instead of polling for events, one can process all input events with an {@link InputProcessor} . You can set the InputProcessor via the {@link #setInputProcessor(InputProcessor)} method. It
  * will be called before the {@link ApplicationListener#render()} method in each frame. </p>
  *
  * <p> Keyboard keys are translated to the constants in {@link Keys} transparently on all systems. Do not use system specific key constants. </p>
  *
  * <p> The class also offers methods to use (and test for the presence of) other input systems like vibration, compass, on-screen keyboards, and cursor capture. Support for simple input dialogs is
  * also provided. </p>
  *
  * @author
  *   mzechner
  */
trait Input {

  import Input.*

  /** @return The acceleration force in m/s^2 applied to the device in the X axis, including the force of gravity */
  def accelerometerX: Float

  /** @return The acceleration force in m/s^2 applied to the device in the Y axis, including the force of gravity */
  def accelerometerY: Float

  /** @return The acceleration force in m/s^2 applied to the device in the Z axis, including the force of gravity */
  def accelerometerZ: Float

  /** @return The rate of rotation in rad/s around the X axis */
  def gyroscopeX: Float

  /** @return The rate of rotation in rad/s around the Y axis */
  def gyroscopeY: Float

  /** @return The rate of rotation in rad/s around the Z axis */
  def gyroscopeZ: Float

  /** @return The maximum number of pointers supported */
  def maxPointers: Int

  /** @return
    *   The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.
    */
  def x: Pixels

  /** Returns the x coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id identifies the order in which the fingers went down on the screen, e.g. 0 is
    * the first finger, 1 is the second and so on. When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on the touch screen the
    * first free index will be used.
    *
    * @param pointer
    *   the pointer id.
    * @return
    *   the x coordinate
    */
  def x(pointer: Int): Pixels

  /** @return the different between the current pointer location and the last pointer location on the x-axis. */
  def deltaX: Pixels

  /** @return the different between the current pointer location and the last pointer location on the x-axis. */
  def deltaX(pointer: Int): Pixels

  /** @return
    *   The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.
    */
  def y: Pixels

  /** Returns the y coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id identifies the order in which the fingers went down on the screen, e.g. 0 is
    * the first finger, 1 is the second and so on. When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on the touch screen the
    * first free index will be used.
    *
    * @param pointer
    *   the pointer id.
    * @return
    *   the y coordinate
    */
  def y(pointer: Int): Pixels

  /** @return the different between the current pointer location and the last pointer location on the y-axis. */
  def deltaY: Pixels

  /** @return the different between the current pointer location and the last pointer location on the y-axis. */
  def deltaY(pointer: Int): Pixels

  /** @return whether the screen is currently touched. */
  def touched: Boolean

  /** @return whether a new touch down event just occurred. */
  def justTouched(): Boolean

  /** Whether the screen is currently touched by the pointer with the given index. Pointers are indexed from 0 to n. The pointer id identifies the order in which the fingers went down on the screen,
    * e.g. 0 is the first finger, 1 is the second and so on. When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on the touch
    * screen the first free index will be used.
    *
    * @param pointer
    *   the pointer
    * @return
    *   whether the screen is touched by the pointer
    */
  def isTouched(pointer: Int): Boolean

  /** @return the pressure of the first pointer */
  def pressure: Float

  /** Returns the pressure of the given pointer, where 0 is untouched. On Android it should be up to 1.0, but it can go above that slightly and its not consistent between devices. On iOS 1.0 is the
    * normal touch and significantly more of hard touch. Check relevant manufacturer documentation for details. Check availability with {@link Input#isPeripheralAvailable(Peripheral)} . If not
    * supported, returns 1.0 when touched.
    *
    * @param pointer
    *   the pointer id.
    * @return
    *   the pressure
    */
  def pressure(pointer: Int): Float

  /** Whether a given button is pressed or not. Button constants can be found in {@link Buttons} . On Android only the Buttons#LEFT constant is meaningful before version 4.0.
    * @param button
    *   the button to check.
    * @return
    *   whether the button is down or not.
    */
  def isButtonPressed(button: Button): Boolean

  /** Returns whether a given button has just been pressed. Button constants can be found in {@link Buttons} . On Android only the Buttons#LEFT constant is meaningful before version 4.0. On WebGL
    * (GWT), only LEFT, RIGHT and MIDDLE buttons are supported.
    *
    * @param button
    *   the button to check.
    * @return
    *   true or false.
    */
  def isButtonJustPressed(button: Button): Boolean

  /** Returns whether the key is pressed.
    *
    * @param key
    *   The key code as found in {@link Input.Keys} .
    * @return
    *   true or false.
    */
  def isKeyPressed(key: Key): Boolean

  /** Returns whether the key has just been pressed.
    *
    * @param key
    *   The key code as found in {@link Input.Keys} .
    * @return
    *   true or false.
    */
  def isKeyJustPressed(key: Key): Boolean

  /** System dependent method to input a string of text. A dialog box will be created with the given title and the given text as a message for the user. Will use the Default keyboard type. Once the
    * dialog has been closed the provided {@link TextInputListener} will be called on the rendering thread.
    *
    * @param listener
    *   The TextInputListener.
    * @param title
    *   The title of the text input dialog.
    * @param text
    *   The message presented to the user.
    */
  def getTextInput(listener: TextInputListener, title: String, text: String, hint: String): Unit

  /** System dependent method to input a string of text. A dialog box will be created with the given title and the given text as a message for the user. Once the dialog has been closed the provided
    * {@link TextInputListener} will be called on the rendering thread.
    *
    * @param listener
    *   The TextInputListener.
    * @param title
    *   The title of the text input dialog.
    * @param text
    *   The message presented to the user.
    * @param type
    *   which type of keyboard we wish to display
    */
  def getTextInput(listener: TextInputListener, title: String, text: String, hint: String, `type`: OnscreenKeyboardType): Unit

  /** Sets the on-screen keyboard visible if available. Will use the Default keyboard type.
    *
    * @param visible
    *   visible or not
    */
  def setOnscreenKeyboardVisible(visible: Boolean): Unit

  /** Sets the on-screen keyboard visible if available.
    *
    * @param visible
    *   visible or not
    * @param type
    *   which type of keyboard we wish to display. Can be null when hiding
    */
  def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType): Unit

  /** Sets the on-screen keyboard visible if available.
    *
    * @param configuration
    *   The configuration for the native input field
    */
  def openTextInputField(configuration: input.NativeInputConfiguration): Unit

  /** Closes the native input field and applies the result to the input wrapper.
    * @param sendReturn
    *   Whether a "return" key should be send after processing
    */
  def closeTextInputField(sendReturn: Boolean): Unit =
    closeTextInputField(sendReturn, Nullable.empty)

  /** Closes the native input field and applies the result to the input wrapper.
    * @param isConfirmative
    *   Whether the closing can be considered confirmative. Will be passed to the {@link NativeInputCloseCallback}
    * @param callback
    *   An optional callback to also run, when the close was processed. Will be called on the main thread. Will be called after {@link NativeInputCloseCallback}
    */
  def closeTextInputField(isConfirmative: Boolean, callback: Nullable[input.NativeInputConfiguration.NativeInputCloseCallback]): Unit = {}

  /** Returns if a native input field is currently open */
  def textInputFieldOpened: Boolean = false

  /** This will set a keyboard height callback. This will get called, whenever the keyboard height changes. Note: When using openTextInputField, it will report the height of the native input field
    * too.
    */
  def setKeyboardHeightObserver(observer: KeyboardHeightObserver): Unit

  /** Generates a simple haptic effect of a given duration or a vibration effect on devices without haptic capabilities. Note that on Android backend you'll need the permission <code> <uses-permission
    * android:name="android.permission.VIBRATE" /></code> in your manifest file in order for this to work. On iOS backend you'll need to set <code>useHaptics = true</code> for devices with haptics
    * capabilities to use them.
    *
    * @param milliseconds
    *   the number of milliseconds to vibrate.
    */
  def vibrate(milliseconds: Int): Unit

  /** Generates a simple haptic effect of a given duration and default amplitude. Note that on Android backend you'll need the permission <code> <uses-permission
    * android:name="android.permission.VIBRATE" /></code> in your manifest file in order for this to work. On iOS backend you'll need to set <code>useHaptics = true</code> for devices with haptics
    * capabilities to use them.
    *
    * @param milliseconds
    *   the duration of the haptics effect
    * @param fallback
    *   whether to use non-haptic vibrator on devices without haptics capabilities (or haptics disabled). Fallback non-haptic vibrations may ignore length parameter in some backends.
    */
  def vibrate(milliseconds: Int, fallback: Boolean): Unit

  /** Generates a simple haptic effect of a given duration and amplitude. Note that on Android backend you'll need the permission <code> <uses-permission android:name="android.permission.VIBRATE"
    * /></code> in your manifest file in order for this to work. On iOS backend you'll need to set <code>useHaptics = true</code> for devices with haptics capabilities to use them.
    *
    * @param milliseconds
    *   the duration of the haptics effect
    * @param amplitude
    *   the amplitude/strength of the haptics effect. Valid values in the range [0, 255].
    * @param fallback
    *   whether to use non-haptic vibrator on devices without haptics capabilities (or haptics disabled). Fallback non-haptic vibrations may ignore length and/or amplitude parameters in some backends.
    */
  def vibrate(milliseconds: Int, amplitude: Int, fallback: Boolean): Unit

  /** Generates a simple haptic effect of a type. VibrationTypes are length/amplitude haptic effect presets that depend on each device and are defined by manufacturers. Should give most consistent
    * results across devices and OSs. Note that on Android backend you'll need the permission <code> <uses-permission android:name="android.permission.VIBRATE" /></code> in your manifest file in order
    * for this to work. On iOS backend you'll need to set <code>useHaptics = true</code> for devices with haptics capabilities to use them.
    *
    * @param vibrationType
    *   the type of vibration
    */
  def vibrate(vibrationType: VibrationType): Unit

  /** The azimuth is the angle of the device's orientation around the z-axis. The positive z-axis points towards the earths center.
    *
    * @see
    *   <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[],
    *   float[])">http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])</a>
    * @return
    *   the azimuth in degrees
    */
  def azimuth: Float

  /** The pitch is the angle of the device's orientation around the x-axis. The positive x-axis roughly points to the west and is orthogonal to the z- and y-axis.
    * @see
    *   <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[],
    *   float[])">http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])</a>
    * @return
    *   the pitch in degrees
    */
  def pitch: Float

  /** The roll is the angle of the device's orientation around the y-axis. The positive y-axis points to the magnetic north pole of the earth.
    * @see
    *   <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[],
    *   float[])">http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])</a>
    * @return
    *   the roll in degrees
    */
  def roll: Float

  /** Returns the rotation matrix describing the devices rotation as per <a href= "http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[],
    * float[], float[])" >SensorManager#getRotationMatrix(float[], float[], float[], float[])</a>. Does not manipulate the matrix if the platform does not have an accelerometer.
    * @param matrix
    */
  def getRotationMatrix(matrix: Array[Float]): Unit

  /** @return the time of the event currently reported to the {@link InputProcessor}. */
  def currentEventTime: Nanos

  /** Sets whether the given key on Android or GWT should be caught. No effect on other platforms. All keys that are not caught may be handled by other apps or background processes on Android, or may
    * trigger default browser behaviour on GWT. For example, media or volume buttons are handled by background media players if present, or Space key triggers a scroll. All keys you need to control
    * your game should be caught to prevent unintended behaviour.
    *
    * @param keycode
    *   keycode to catch
    * @param catchKey
    *   whether to catch the given keycode
    */
  def setCatchKey(keycode: Key, catchKey: Boolean): Unit

  /** @param keycode
    *   keycode to check if caught
    * @return
    *   true if the given keycode is configured to be caught
    */
  def isCatchKey(keycode: Key): Boolean

  /** Sets the {@link InputProcessor} that will receive all touch and key input events. It will be called before the {@link ApplicationListener#render()} method each frame.
    *
    * @param processor
    *   the InputProcessor
    */
  def setInputProcessor(processor: InputProcessor): Unit

  /** @return the currently set {@link InputProcessor} or null. */
  def inputProcessor: InputProcessor

  /** Queries whether a {@link Peripheral} is currently available. In case of Android and the {@link Peripheral#HardwareKeyboard} this returns the whether the keyboard is currently slid out or not.
    *
    * @param peripheral
    *   the {@link Peripheral}
    * @return
    *   whether the peripheral is available or not.
    */
  def isPeripheralAvailable(peripheral: Peripheral): Boolean

  /** @return the rotation of the device with respect to its native orientation. */
  def rotation: Int

  /** @return the native orientation of the device. */
  def nativeOrientation: Orientation

  /** Only viable on the desktop. Will confine the mouse cursor location to the window and hide the mouse cursor. X and y coordinates are still reported as if the mouse was not catched.
    * @param catched
    *   whether to catch or not to catch the mouse cursor
    */
  def setCursorCatched(catched: Boolean): Unit

  /** @return whether the mouse cursor is catched. */
  def cursorCatched: Boolean

  /** Only viable on the desktop. Will set the mouse cursor location to the given window coordinates (origin top-left corner).
    * @param x
    *   the x-position
    * @param y
    *   the y-position
    */
  def setCursorPosition(x: Pixels, y: Pixels): Unit
}
object Input {

  /** Opaque type for keyboard key codes. Wraps `Int` with zero runtime overhead. Prevents accidental mixing of key codes with unrelated integers (pointer IDs, button codes, etc.). Use `Key(intValue)`
    * to construct from raw Int, and `.toInt` to unwrap when raw arithmetic or array indexing is needed.
    */
  opaque type Key = Int
  object Key {
    inline def apply(value: Int): Key = value

    given sge.utils.MkArray[Key] = sge.utils.MkArray.mkInt.asInstanceOf[sge.utils.MkArray[Key]]

    extension (k: Key) {
      inline def toInt: Int = k
    }
  }

  /** Opaque type for mouse/touch button codes. Wraps `Int` with zero runtime overhead. Prevents accidental mixing of button codes with unrelated integers (key codes, pointer IDs, etc.). Use
    * `Button(intValue)` to construct from raw Int, and `.toInt` to unwrap when raw arithmetic or array indexing is needed.
    */
  opaque type Button = Int
  object Button {
    inline def apply(value: Int): Button = value

    given sge.utils.MkArray[Button] = sge.utils.MkArray.mkInt.asInstanceOf[sge.utils.MkArray[Button]]

    extension (b: Button) {
      inline def toInt: Int = b
    }
  }

  /** Callback interface for {@link Input#getTextInput(TextInputListener, String, String, String)}
    *
    * @author
    *   mzechner
    */
  trait TextInputListener {
    def input(text: String): Unit

    def canceled(): Unit
  }

  /** Mouse buttons.
    * @author
    *   mzechner
    */
  object Buttons {
    final val LEFT:    Button = 0
    final val RIGHT:   Button = 1
    final val MIDDLE:  Button = 2
    final val BACK:    Button = 3
    final val FORWARD: Button = 4
  }

  /** Keys.
    *
    * @author
    *   mzechner
    */
  object Keys {
    final val ANY_KEY:             Key = -1
    final val NUM_0:               Key = 7
    final val NUM_1:               Key = 8
    final val NUM_2:               Key = 9
    final val NUM_3:               Key = 10
    final val NUM_4:               Key = 11
    final val NUM_5:               Key = 12
    final val NUM_6:               Key = 13
    final val NUM_7:               Key = 14
    final val NUM_8:               Key = 15
    final val NUM_9:               Key = 16
    final val A:                   Key = 29
    final val ALT_LEFT:            Key = 57
    final val ALT_RIGHT:           Key = 58
    final val APOSTROPHE:          Key = 75
    final val AT:                  Key = 77
    final val B:                   Key = 30
    final val BACK:                Key = 4
    final val BACKSLASH:           Key = 73
    final val C:                   Key = 31
    final val CALL:                Key = 5
    final val CAMERA:              Key = 27
    final val CAPS_LOCK:           Key = 115
    final val CLEAR:               Key = 28
    final val COMMA:               Key = 55
    final val D:                   Key = 32
    final val DEL:                 Key = 67
    final val BACKSPACE:           Key = 67
    final val FORWARD_DEL:         Key = 112
    final val DPAD_CENTER:         Key = 23
    final val DPAD_DOWN:           Key = 20
    final val DPAD_LEFT:           Key = 21
    final val DPAD_RIGHT:          Key = 22
    final val DPAD_UP:             Key = 19
    final val CENTER:              Key = 23
    final val DOWN:                Key = 20
    final val LEFT:                Key = 21
    final val RIGHT:               Key = 22
    final val UP:                  Key = 19
    final val E:                   Key = 33
    final val ENDCALL:             Key = 6
    final val ENTER:               Key = 66
    final val ENVELOPE:            Key = 65
    final val EQUALS:              Key = 70
    final val EXPLORER:            Key = 64
    final val F:                   Key = 34
    final val FOCUS:               Key = 80
    final val G:                   Key = 35
    final val GRAVE:               Key = 68
    final val H:                   Key = 36
    final val HEADSETHOOK:         Key = 79
    final val HOME:                Key = 3
    final val I:                   Key = 37
    final val J:                   Key = 38
    final val K:                   Key = 39
    final val L:                   Key = 40
    final val LEFT_BRACKET:        Key = 71
    final val M:                   Key = 41
    final val MEDIA_FAST_FORWARD:  Key = 90
    final val MEDIA_NEXT:          Key = 87
    final val MEDIA_PLAY_PAUSE:    Key = 85
    final val MEDIA_PREVIOUS:      Key = 88
    final val MEDIA_REWIND:        Key = 89
    final val MEDIA_STOP:          Key = 86
    final val MENU:                Key = 82
    final val MINUS:               Key = 69
    final val MUTE:                Key = 91
    final val N:                   Key = 42
    final val NOTIFICATION:        Key = 83
    final val NUM:                 Key = 78
    final val O:                   Key = 43
    final val P:                   Key = 44
    final val PAUSE:               Key = 121 // aka break
    final val PERIOD:              Key = 56
    final val PLUS:                Key = 81
    final val POUND:               Key = 18
    final val POWER:               Key = 26
    final val PRINT_SCREEN:        Key = 120 // aka SYSRQ
    final val Q:                   Key = 45
    final val R:                   Key = 46
    final val RIGHT_BRACKET:       Key = 72
    final val S:                   Key = 47
    final val SCROLL_LOCK:         Key = 116
    final val SEARCH:              Key = 84
    final val SEMICOLON:           Key = 74
    final val SHIFT_LEFT:          Key = 59
    final val SHIFT_RIGHT:         Key = 60
    final val SLASH:               Key = 76
    final val SOFT_LEFT:           Key = 1
    final val SOFT_RIGHT:          Key = 2
    final val SPACE:               Key = 62
    final val STAR:                Key = 17
    final val SYM:                 Key = 63 // on MacOS, this is Command (⌘)
    final val T:                   Key = 48
    final val TAB:                 Key = 61
    final val U:                   Key = 49
    final val UNKNOWN:             Key = 0
    final val V:                   Key = 50
    final val VOLUME_DOWN:         Key = 25
    final val VOLUME_UP:           Key = 24
    final val W:                   Key = 51
    final val X:                   Key = 52
    final val Y:                   Key = 53
    final val Z:                   Key = 54
    final val META_ALT_LEFT_ON:    Key = 16
    final val META_ALT_ON:         Key = 2
    final val META_ALT_RIGHT_ON:   Key = 32
    final val META_SHIFT_LEFT_ON:  Key = 64
    final val META_SHIFT_ON:       Key = 1
    final val META_SHIFT_RIGHT_ON: Key = 128
    final val META_SYM_ON:         Key = 4
    final val CONTROL_LEFT:        Key = 129
    final val CONTROL_RIGHT:       Key = 130
    final val ESCAPE:              Key = 111
    final val END:                 Key = 123
    final val INSERT:              Key = 124
    final val PAGE_UP:             Key = 92
    final val PAGE_DOWN:           Key = 93
    final val PICTSYMBOLS:         Key = 94
    final val SWITCH_CHARSET:      Key = 95
    final val BUTTON_CIRCLE:       Key = 255
    final val BUTTON_A:            Key = 96
    final val BUTTON_B:            Key = 97
    final val BUTTON_C:            Key = 98
    final val BUTTON_X:            Key = 99
    final val BUTTON_Y:            Key = 100
    final val BUTTON_Z:            Key = 101
    final val BUTTON_L1:           Key = 102
    final val BUTTON_R1:           Key = 103
    final val BUTTON_L2:           Key = 104
    final val BUTTON_R2:           Key = 105
    final val BUTTON_THUMBL:       Key = 106
    final val BUTTON_THUMBR:       Key = 107
    final val BUTTON_START:        Key = 108
    final val BUTTON_SELECT:       Key = 109
    final val BUTTON_MODE:         Key = 110

    final val NUMPAD_0: Key = 144
    final val NUMPAD_1: Key = 145
    final val NUMPAD_2: Key = 146
    final val NUMPAD_3: Key = 147
    final val NUMPAD_4: Key = 148
    final val NUMPAD_5: Key = 149
    final val NUMPAD_6: Key = 150
    final val NUMPAD_7: Key = 151
    final val NUMPAD_8: Key = 152
    final val NUMPAD_9: Key = 153

    final val NUMPAD_DIVIDE:      Key = 154
    final val NUMPAD_MULTIPLY:    Key = 155
    final val NUMPAD_SUBTRACT:    Key = 156
    final val NUMPAD_ADD:         Key = 157
    final val NUMPAD_DOT:         Key = 158
    final val NUMPAD_COMMA:       Key = 159
    final val NUMPAD_ENTER:       Key = 160
    final val NUMPAD_EQUALS:      Key = 161
    final val NUMPAD_LEFT_PAREN:  Key = 162
    final val NUMPAD_RIGHT_PAREN: Key = 163
    final val NUM_LOCK:           Key = 143

    final val WORLD_1: Key = 240
    final val WORLD_2: Key = 241

// final val BACKTICK = 0
// final val TILDE = 0
// final val UNDERSCORE = 0
// final val DOT = 0
// final val BREAK = 0
// final val PIPE = 0
// final val EXCLAMATION = 0
// final val QUESTIONMARK = 0

// ` | VK_BACKTICK
// ~ | VK_TILDE
// : | VK_COLON
// _ | VK_UNDERSCORE
// . | VK_DOT
// (break) | VK_BREAK
// | | VK_PIPE
// ! | VK_EXCLAMATION
// ? | VK_QUESTION
    final val COLON: Key = 243
    final val F1:    Key = 131
    final val F2:    Key = 132
    final val F3:    Key = 133
    final val F4:    Key = 134
    final val F5:    Key = 135
    final val F6:    Key = 136
    final val F7:    Key = 137
    final val F8:    Key = 138
    final val F9:    Key = 139
    final val F10:   Key = 140
    final val F11:   Key = 141
    final val F12:   Key = 142
    final val F13:   Key = 183
    final val F14:   Key = 184
    final val F15:   Key = 185
    final val F16:   Key = 186
    final val F17:   Key = 187
    final val F18:   Key = 188
    final val F19:   Key = 189
    final val F20:   Key = 190
    final val F21:   Key = 191
    final val F22:   Key = 192
    final val F23:   Key = 193
    final val F24:   Key = 194

    final val MAX_KEYCODE: Key = 255

    /** @return
      *   a human readable representation of the keycode. The returned value can be used in {@link Input.Keys#valueOf(String)}
      */
    def toString(keycode: Key): Nullable[String] = {
      if (keycode < 0) throw new IllegalArgumentException("keycode cannot be negative, keycode: " + keycode)
      if (keycode > MAX_KEYCODE) throw new IllegalArgumentException("keycode cannot be greater than 255, keycode: " + keycode)
      Nullable(
        keycode match {
          // META* variables should not be used with this method.
          case UNKNOWN =>
            "Unknown"
          case SOFT_LEFT =>
            "Soft Left"
          case SOFT_RIGHT =>
            "Soft Right"
          case HOME =>
            "Home"
          case BACK =>
            "Back"
          case CALL =>
            "Call"
          case ENDCALL =>
            "End Call"
          case NUM_0 =>
            "0"
          case NUM_1 =>
            "1"
          case NUM_2 =>
            "2"
          case NUM_3 =>
            "3"
          case NUM_4 =>
            "4"
          case NUM_5 =>
            "5"
          case NUM_6 =>
            "6"
          case NUM_7 =>
            "7"
          case NUM_8 =>
            "8"
          case NUM_9 =>
            "9"
          case STAR =>
            "*"
          case POUND =>
            "#"
          case UP =>
            "Up"
          case DOWN =>
            "Down"
          case LEFT =>
            "Left"
          case RIGHT =>
            "Right"
          case CENTER =>
            "Center"
          case VOLUME_UP =>
            "Volume Up"
          case VOLUME_DOWN =>
            "Volume Down"
          case POWER =>
            "Power"
          case CAMERA =>
            "Camera"
          case CLEAR =>
            "Clear"
          case A =>
            "A"
          case B =>
            "B"
          case C =>
            "C"
          case D =>
            "D"
          case E =>
            "E"
          case F =>
            "F"
          case G =>
            "G"
          case H =>
            "H"
          case I =>
            "I"
          case J =>
            "J"
          case K =>
            "K"
          case L =>
            "L"
          case M =>
            "M"
          case N =>
            "N"
          case O =>
            "O"
          case P =>
            "P"
          case Q =>
            "Q"
          case R =>
            "R"
          case S =>
            "S"
          case T =>
            "T"
          case U =>
            "U"
          case V =>
            "V"
          case W =>
            "W"
          case X =>
            "X"
          case Y =>
            "Y"
          case Z =>
            "Z"
          case COMMA =>
            ","
          case PERIOD =>
            "."
          case ALT_LEFT =>
            "L-Alt"
          case ALT_RIGHT =>
            "R-Alt"
          case SHIFT_LEFT =>
            "L-Shift"
          case SHIFT_RIGHT =>
            "R-Shift"
          case TAB =>
            "Tab"
          case SPACE =>
            "Space"
          case SYM =>
            "SYM"
          case EXPLORER =>
            "Explorer"
          case ENVELOPE =>
            "Envelope"
          case ENTER =>
            "Enter"
          case DEL =>
            "Delete" // also BACKSPACE
          case GRAVE =>
            "`"
          case MINUS =>
            "-"
          case EQUALS =>
            "="
          case LEFT_BRACKET =>
            "["
          case RIGHT_BRACKET =>
            "]"
          case BACKSLASH =>
            "\\"
          case SEMICOLON =>
            ";"
          case APOSTROPHE =>
            "'"
          case SLASH =>
            "/"
          case AT =>
            "@"
          case NUM =>
            "Num"
          case HEADSETHOOK =>
            "Headset Hook"
          case FOCUS =>
            "Focus"
          case PLUS =>
            "Plus"
          case MENU =>
            "Menu"
          case NOTIFICATION =>
            "Notification"
          case SEARCH =>
            "Search"
          case MEDIA_PLAY_PAUSE =>
            "Play/Pause"
          case MEDIA_STOP =>
            "Stop Media"
          case MEDIA_NEXT =>
            "Next Media"
          case MEDIA_PREVIOUS =>
            "Prev Media"
          case MEDIA_REWIND =>
            "Rewind"
          case MEDIA_FAST_FORWARD =>
            "Fast Forward"
          case MUTE =>
            "Mute"
          case PAGE_UP =>
            "Page Up"
          case PAGE_DOWN =>
            "Page Down"
          case PICTSYMBOLS =>
            "PICTSYMBOLS"
          case SWITCH_CHARSET =>
            "SWITCH_CHARSET"
          case BUTTON_A =>
            "A Button"
          case BUTTON_B =>
            "B Button"
          case BUTTON_C =>
            "C Button"
          case BUTTON_X =>
            "X Button"
          case BUTTON_Y =>
            "Y Button"
          case BUTTON_Z =>
            "Z Button"
          case BUTTON_L1 =>
            "L1 Button"
          case BUTTON_R1 =>
            "R1 Button"
          case BUTTON_L2 =>
            "L2 Button"
          case BUTTON_R2 =>
            "R2 Button"
          case BUTTON_THUMBL =>
            "Left Thumb"
          case BUTTON_THUMBR =>
            "Right Thumb"
          case BUTTON_START =>
            "Start"
          case BUTTON_SELECT =>
            "Select"
          case BUTTON_MODE =>
            "Button Mode"
          case FORWARD_DEL =>
            "Forward Delete"
          case CONTROL_LEFT =>
            "L-Ctrl"
          case CONTROL_RIGHT =>
            "R-Ctrl"
          case ESCAPE =>
            "Escape"
          case END =>
            "End"
          case INSERT =>
            "Insert"
          case NUMPAD_0 =>
            "Numpad 0"
          case NUMPAD_1 =>
            "Numpad 1"
          case NUMPAD_2 =>
            "Numpad 2"
          case NUMPAD_3 =>
            "Numpad 3"
          case NUMPAD_4 =>
            "Numpad 4"
          case NUMPAD_5 =>
            "Numpad 5"
          case NUMPAD_6 =>
            "Numpad 6"
          case NUMPAD_7 =>
            "Numpad 7"
          case NUMPAD_8 =>
            "Numpad 8"
          case NUMPAD_9 =>
            "Numpad 9"
          case COLON =>
            ":"
          case F1 =>
            "F1"
          case F2 =>
            "F2"
          case F3 =>
            "F3"
          case F4 =>
            "F4"
          case F5 =>
            "F5"
          case F6 =>
            "F6"
          case F7 =>
            "F7"
          case F8 =>
            "F8"
          case F9 =>
            "F9"
          case F10 =>
            "F10"
          case F11 =>
            "F11"
          case F12 =>
            "F12"
          case F13 =>
            "F13"
          case F14 =>
            "F14"
          case F15 =>
            "F15"
          case F16 =>
            "F16"
          case F17 =>
            "F17"
          case F18 =>
            "F18"
          case F19 =>
            "F19"
          case F20 =>
            "F20"
          case F21 =>
            "F21"
          case F22 =>
            "F22"
          case F23 =>
            "F23"
          case F24 =>
            "F24"
          case NUMPAD_DIVIDE =>
            "Num /"
          case NUMPAD_MULTIPLY =>
            "Num *"
          case NUMPAD_SUBTRACT =>
            "Num -"
          case NUMPAD_ADD =>
            "Num +"
          case NUMPAD_DOT =>
            "Num ."
          case NUMPAD_COMMA =>
            "Num ,"
          case NUMPAD_ENTER =>
            "Num Enter"
          case NUMPAD_EQUALS =>
            "Num ="
          case NUMPAD_LEFT_PAREN =>
            "Num ("
          case NUMPAD_RIGHT_PAREN =>
            "Num )"
          case NUM_LOCK =>
            "Num Lock"
          case CAPS_LOCK =>
            "Caps Lock"
          case SCROLL_LOCK =>
            "Scroll Lock"
          case PAUSE =>
            "Pause"
          case PRINT_SCREEN =>
            "Print"
          // BUTTON_CIRCLE unhandled, as it conflicts with the more likely to be pressed F12
          case _ =>
            // key name not found
            null: String
        }
      )
    }

    private var keyNames: collection.mutable.Map[String, Key] = scala.compiletime.uninitialized

    /** @param keyname
      *   the keyname returned by the {@link Keys#toString(int)} method
      * @return
      *   the keycode
      */
    def valueOf(keyname: String): Key = {
      if (Nullable(keyNames).isEmpty) initializeKeyNames()
      keyNames.getOrElse(keyname, ANY_KEY)
    }

    /** lazily intialized in {@link Keys#valueOf(String)} */
    private def initializeKeyNames(): Unit = {
      keyNames = collection.mutable.Map[String, Key]()
      for (i <- 0 until 256)
        toString(Key(i)).foreach(name => keyNames.put(name, Key(i)))
    }
  }

  /** Enumeration of potentially available peripherals. Use with {@link Input#isPeripheralAvailable(Peripheral)} .
    * @author
    *   mzechner
    */
  enum Peripheral {
    case HardwareKeyboard, OnscreenKeyboard, MultitouchScreen, Accelerometer, Compass, Vibrator, HapticFeedback, Gyroscope, RotationVector, Pressure
  }

  trait InputStringValidator {

    /** @param toCheck
      *   The string that should be validated
      * @return
      *   true, if the string is acceptable, false if not.
      */
    def validate(toCheck: String): Boolean
  }

  trait KeyboardHeightObserver {
    def onKeyboardHeightChanged(height: Int): Unit
  }

  enum OnscreenKeyboardType {
    case Default, NumberPad, PhonePad, Email, Password, URI
  }

  enum VibrationType {
    case LIGHT, MEDIUM, HEAVY
  }

  enum Orientation {
    case Landscape, Portrait
  }
}
