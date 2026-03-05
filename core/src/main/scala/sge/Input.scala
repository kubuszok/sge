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
 *   TODO: Input.Buttons → opaque type Button with constants in companion + extension toString; Input.Keys → opaque type Key with constants in companion + extension toString/valueOf
 *   TODO: opaque Pixels for getX/Y, getDeltaX/Y, setCursorPosition params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.utils.Nullable

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
  def getAccelerometerX(): Float

  /** @return The acceleration force in m/s^2 applied to the device in the Y axis, including the force of gravity */
  def getAccelerometerY(): Float

  /** @return The acceleration force in m/s^2 applied to the device in the Z axis, including the force of gravity */
  def getAccelerometerZ(): Float

  /** @return The rate of rotation in rad/s around the X axis */
  def getGyroscopeX(): Float

  /** @return The rate of rotation in rad/s around the Y axis */
  def getGyroscopeY(): Float

  /** @return The rate of rotation in rad/s around the Z axis */
  def getGyroscopeZ(): Float

  /** @return The maximum number of pointers supported */
  def getMaxPointers(): Int

  /** @return
    *   The x coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.
    */
  def getX(): Int

  /** Returns the x coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id identifies the order in which the fingers went down on the screen, e.g. 0 is
    * the first finger, 1 is the second and so on. When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on the touch screen the
    * first free index will be used.
    *
    * @param pointer
    *   the pointer id.
    * @return
    *   the x coordinate
    */
  def getX(pointer: Int): Int

  /** @return the different between the current pointer location and the last pointer location on the x-axis. */
  def getDeltaX(): Int

  /** @return the different between the current pointer location and the last pointer location on the x-axis. */
  def getDeltaX(pointer: Int): Int

  /** @return
    *   The y coordinate of the last touch on touch screen devices and the current mouse position on desktop for the first pointer in screen coordinates. The screen origin is the top left corner.
    */
  def getY(): Int

  /** Returns the y coordinate in screen coordinates of the given pointer. Pointers are indexed from 0 to n. The pointer id identifies the order in which the fingers went down on the screen, e.g. 0 is
    * the first finger, 1 is the second and so on. When two fingers are touched down and the first one is lifted the second one keeps its index. If another finger is placed on the touch screen the
    * first free index will be used.
    *
    * @param pointer
    *   the pointer id.
    * @return
    *   the y coordinate
    */
  def getY(pointer: Int): Int

  /** @return the different between the current pointer location and the last pointer location on the y-axis. */
  def getDeltaY(): Int

  /** @return the different between the current pointer location and the last pointer location on the y-axis. */
  def getDeltaY(pointer: Int): Int

  /** @return whether the screen is currently touched. */
  def isTouched(): Boolean

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
  def getPressure(): Float

  /** Returns the pressure of the given pointer, where 0 is untouched. On Android it should be up to 1.0, but it can go above that slightly and its not consistent between devices. On iOS 1.0 is the
    * normal touch and significantly more of hard touch. Check relevant manufacturer documentation for details. Check availability with {@link Input#isPeripheralAvailable(Peripheral)} . If not
    * supported, returns 1.0 when touched.
    *
    * @param pointer
    *   the pointer id.
    * @return
    *   the pressure
    */
  def getPressure(pointer: Int): Float

  /** Whether a given button is pressed or not. Button constants can be found in {@link Buttons} . On Android only the Buttons#LEFT constant is meaningful before version 4.0.
    * @param button
    *   the button to check.
    * @return
    *   whether the button is down or not.
    */
  def isButtonPressed(button: Int): Boolean

  /** Returns whether a given button has just been pressed. Button constants can be found in {@link Buttons} . On Android only the Buttons#LEFT constant is meaningful before version 4.0. On WebGL
    * (GWT), only LEFT, RIGHT and MIDDLE buttons are supported.
    *
    * @param button
    *   the button to check.
    * @return
    *   true or false.
    */
  def isButtonJustPressed(button: Int): Boolean

  /** Returns whether the key is pressed.
    *
    * @param key
    *   The key code as found in {@link Input.Keys} .
    * @return
    *   true or false.
    */
  def isKeyPressed(key: Int): Boolean

  /** Returns whether the key has just been pressed.
    *
    * @param key
    *   The key code as found in {@link Input.Keys} .
    * @return
    *   true or false.
    */
  def isKeyJustPressed(key: Int): Boolean

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
  def closeTextInputField(sendReturn: Boolean): Unit

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
  def getAzimuth(): Float

  /** The pitch is the angle of the device's orientation around the x-axis. The positive x-axis roughly points to the west and is orthogonal to the z- and y-axis.
    * @see
    *   <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[],
    *   float[])">http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])</a>
    * @return
    *   the pitch in degrees
    */
  def getPitch(): Float

  /** The roll is the angle of the device's orientation around the y-axis. The positive y-axis points to the magnetic north pole of the earth.
    * @see
    *   <a href="http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[],
    *   float[])">http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[], float[], float[])</a>
    * @return
    *   the roll in degrees
    */
  def getRoll(): Float

  /** Returns the rotation matrix describing the devices rotation as per <a href= "http://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[], float[],
    * float[], float[])" >SensorManager#getRotationMatrix(float[], float[], float[], float[])</a>. Does not manipulate the matrix if the platform does not have an accelerometer.
    * @param matrix
    */
  def getRotationMatrix(matrix: Array[Float]): Unit

  /** @return the time of the event currently reported to the {@link InputProcessor}. */
  def getCurrentEventTime(): Long

  /** Sets whether the given key on Android or GWT should be caught. No effect on other platforms. All keys that are not caught may be handled by other apps or background processes on Android, or may
    * trigger default browser behaviour on GWT. For example, media or volume buttons are handled by background media players if present, or Space key triggers a scroll. All keys you need to control
    * your game should be caught to prevent unintended behaviour.
    *
    * @param keycode
    *   keycode to catch
    * @param catchKey
    *   whether to catch the given keycode
    */
  def setCatchKey(keycode: Int, catchKey: Boolean): Unit

  /** @param keycode
    *   keycode to check if caught
    * @return
    *   true if the given keycode is configured to be caught
    */
  def isCatchKey(keycode: Int): Boolean

  /** Sets the {@link InputProcessor} that will receive all touch and key input events. It will be called before the {@link ApplicationListener#render()} method each frame.
    *
    * @param processor
    *   the InputProcessor
    */
  def setInputProcessor(processor: InputProcessor): Unit

  /** @return the currently set {@link InputProcessor} or null. */
  def getInputProcessor(): InputProcessor

  /** Queries whether a {@link Peripheral} is currently available. In case of Android and the {@link Peripheral#HardwareKeyboard} this returns the whether the keyboard is currently slid out or not.
    *
    * @param peripheral
    *   the {@link Peripheral}
    * @return
    *   whether the peripheral is available or not.
    */
  def isPeripheralAvailable(peripheral: Peripheral): Boolean

  /** @return the rotation of the device with respect to its native orientation. */
  def getRotation(): Int

  /** @return the native orientation of the device. */
  def getNativeOrientation(): Orientation

  /** Only viable on the desktop. Will confine the mouse cursor location to the window and hide the mouse cursor. X and y coordinates are still reported as if the mouse was not catched.
    * @param catched
    *   whether to catch or not to catch the mouse cursor
    */
  def setCursorCatched(catched: Boolean): Unit

  /** @return whether the mouse cursor is catched. */
  def isCursorCatched(): Boolean

  /** Only viable on the desktop. Will set the mouse cursor location to the given window coordinates (origin top-left corner).
    * @param x
    *   the x-position
    * @param y
    *   the y-position
    */
  def setCursorPosition(x: Int, y: Int): Unit
}
object Input {

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
    final val LEFT    = 0
    final val RIGHT   = 1
    final val MIDDLE  = 2
    final val BACK    = 3
    final val FORWARD = 4
  }

  /** Keys.
    *
    * @author
    *   mzechner
    */
  object Keys {
    final val ANY_KEY             = -1
    final val NUM_0               = 7
    final val NUM_1               = 8
    final val NUM_2               = 9
    final val NUM_3               = 10
    final val NUM_4               = 11
    final val NUM_5               = 12
    final val NUM_6               = 13
    final val NUM_7               = 14
    final val NUM_8               = 15
    final val NUM_9               = 16
    final val A                   = 29
    final val ALT_LEFT            = 57
    final val ALT_RIGHT           = 58
    final val APOSTROPHE          = 75
    final val AT                  = 77
    final val B                   = 30
    final val BACK                = 4
    final val BACKSLASH           = 73
    final val C                   = 31
    final val CALL                = 5
    final val CAMERA              = 27
    final val CAPS_LOCK           = 115
    final val CLEAR               = 28
    final val COMMA               = 55
    final val D                   = 32
    final val DEL                 = 67
    final val BACKSPACE           = 67
    final val FORWARD_DEL         = 112
    final val DPAD_CENTER         = 23
    final val DPAD_DOWN           = 20
    final val DPAD_LEFT           = 21
    final val DPAD_RIGHT          = 22
    final val DPAD_UP             = 19
    final val CENTER              = 23
    final val DOWN                = 20
    final val LEFT                = 21
    final val RIGHT               = 22
    final val UP                  = 19
    final val E                   = 33
    final val ENDCALL             = 6
    final val ENTER               = 66
    final val ENVELOPE            = 65
    final val EQUALS              = 70
    final val EXPLORER            = 64
    final val F                   = 34
    final val FOCUS               = 80
    final val G                   = 35
    final val GRAVE               = 68
    final val H                   = 36
    final val HEADSETHOOK         = 79
    final val HOME                = 3
    final val I                   = 37
    final val J                   = 38
    final val K                   = 39
    final val L                   = 40
    final val LEFT_BRACKET        = 71
    final val M                   = 41
    final val MEDIA_FAST_FORWARD  = 90
    final val MEDIA_NEXT          = 87
    final val MEDIA_PLAY_PAUSE    = 85
    final val MEDIA_PREVIOUS      = 88
    final val MEDIA_REWIND        = 89
    final val MEDIA_STOP          = 86
    final val MENU                = 82
    final val MINUS               = 69
    final val MUTE                = 91
    final val N                   = 42
    final val NOTIFICATION        = 83
    final val NUM                 = 78
    final val O                   = 43
    final val P                   = 44
    final val PAUSE               = 121 // aka break
    final val PERIOD              = 56
    final val PLUS                = 81
    final val POUND               = 18
    final val POWER               = 26
    final val PRINT_SCREEN        = 120 // aka SYSRQ
    final val Q                   = 45
    final val R                   = 46
    final val RIGHT_BRACKET       = 72
    final val S                   = 47
    final val SCROLL_LOCK         = 116
    final val SEARCH              = 84
    final val SEMICOLON           = 74
    final val SHIFT_LEFT          = 59
    final val SHIFT_RIGHT         = 60
    final val SLASH               = 76
    final val SOFT_LEFT           = 1
    final val SOFT_RIGHT          = 2
    final val SPACE               = 62
    final val STAR                = 17
    final val SYM                 = 63 // on MacOS, this is Command (⌘)
    final val T                   = 48
    final val TAB                 = 61
    final val U                   = 49
    final val UNKNOWN             = 0
    final val V                   = 50
    final val VOLUME_DOWN         = 25
    final val VOLUME_UP           = 24
    final val W                   = 51
    final val X                   = 52
    final val Y                   = 53
    final val Z                   = 54
    final val META_ALT_LEFT_ON    = 16
    final val META_ALT_ON         = 2
    final val META_ALT_RIGHT_ON   = 32
    final val META_SHIFT_LEFT_ON  = 64
    final val META_SHIFT_ON       = 1
    final val META_SHIFT_RIGHT_ON = 128
    final val META_SYM_ON         = 4
    final val CONTROL_LEFT        = 129
    final val CONTROL_RIGHT       = 130
    final val ESCAPE              = 111
    final val END                 = 123
    final val INSERT              = 124
    final val PAGE_UP             = 92
    final val PAGE_DOWN           = 93
    final val PICTSYMBOLS         = 94
    final val SWITCH_CHARSET      = 95
    final val BUTTON_CIRCLE       = 255
    final val BUTTON_A            = 96
    final val BUTTON_B            = 97
    final val BUTTON_C            = 98
    final val BUTTON_X            = 99
    final val BUTTON_Y            = 100
    final val BUTTON_Z            = 101
    final val BUTTON_L1           = 102
    final val BUTTON_R1           = 103
    final val BUTTON_L2           = 104
    final val BUTTON_R2           = 105
    final val BUTTON_THUMBL       = 106
    final val BUTTON_THUMBR       = 107
    final val BUTTON_START        = 108
    final val BUTTON_SELECT       = 109
    final val BUTTON_MODE         = 110

    final val NUMPAD_0 = 144
    final val NUMPAD_1 = 145
    final val NUMPAD_2 = 146
    final val NUMPAD_3 = 147
    final val NUMPAD_4 = 148
    final val NUMPAD_5 = 149
    final val NUMPAD_6 = 150
    final val NUMPAD_7 = 151
    final val NUMPAD_8 = 152
    final val NUMPAD_9 = 153

    final val NUMPAD_DIVIDE      = 154
    final val NUMPAD_MULTIPLY    = 155
    final val NUMPAD_SUBTRACT    = 156
    final val NUMPAD_ADD         = 157
    final val NUMPAD_DOT         = 158
    final val NUMPAD_COMMA       = 159
    final val NUMPAD_ENTER       = 160
    final val NUMPAD_EQUALS      = 161
    final val NUMPAD_LEFT_PAREN  = 162
    final val NUMPAD_RIGHT_PAREN = 163
    final val NUM_LOCK           = 143

    final val WORLD_1 = 240
    final val WORLD_2 = 241

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
    final val COLON = 243
    final val F1    = 131
    final val F2    = 132
    final val F3    = 133
    final val F4    = 134
    final val F5    = 135
    final val F6    = 136
    final val F7    = 137
    final val F8    = 138
    final val F9    = 139
    final val F10   = 140
    final val F11   = 141
    final val F12   = 142
    final val F13   = 183
    final val F14   = 184
    final val F15   = 185
    final val F16   = 186
    final val F17   = 187
    final val F18   = 188
    final val F19   = 189
    final val F20   = 190
    final val F21   = 191
    final val F22   = 192
    final val F23   = 193
    final val F24   = 194

    final val MAX_KEYCODE = 255

    /** @return
      *   a human readable representation of the keycode. The returned value can be used in {@link Input.Keys#valueOf(String)}
      */
    def toString(keycode: Int): Nullable[String] = {
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

    private var keyNames: collection.mutable.Map[String, Int] = scala.compiletime.uninitialized

    /** @param keyname
      *   the keyname returned by the {@link Keys#toString(int)} method
      * @return
      *   the int keycode
      */
    def valueOf(keyname: String): Int = {
      if (Nullable(keyNames).isEmpty) initializeKeyNames()
      keyNames.getOrElse(keyname, -1)
    }

    /** lazily intialized in {@link Keys#valueOf(String)} */
    private def initializeKeyNames(): Unit = {
      keyNames = collection.mutable.Map[String, Int]()
      for (i <- 0 until 256)
        toString(i).foreach(name => keyNames.put(name, i))
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
