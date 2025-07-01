package sge
package input

import java.io.{ DataInputStream, IOException }
import java.net.{ InetAddress, ServerSocket, Socket }
import scala.collection.mutable.ArrayBuffer
import sge.utils.SdeError
import sge.Input.{ KeyboardHeightObserver, OnscreenKeyboardType, Orientation, Peripheral, VibrationType }
import sge.input.NativeInputConfiguration

trait RemoteInputListener {
  def onConnected():    Unit
  def onDisconnected(): Unit
}

/** <p> An {@link Input} implementation that receives touch, key, accelerometer and compass events from a remote Android device. Just instantiate it and specify the port it should listen on for
  * incoming connections (default 8190). Then store the new RemoteInput instance in Gdx.input. That's it. </p>
  *
  * <p> On your Android device you can use the gdx-remote application available on the Google Code page as an APK or in SVN (extensions/gdx-remote). Open it, specify the IP address and the port of the
  * PC your libGDX app is running on and then tap away. </p>
  *
  * <p> The touch coordinates will be translated to the desktop window's coordinate system, no matter the orientation of the device </p>
  *
  * @author
  *   mzechner (original implementation)
  */
class RemoteInput(port: Int = RemoteInput.DEFAULT_PORT, listener: Option[RemoteInputListener] = None)(implicit sde: sge.Sge) extends Runnable with Input {

  def this()(implicit sde: sge.Sge) = this(RemoteInput.DEFAULT_PORT, None)
  def this(listener:       RemoteInputListener)(implicit sde: sge.Sge) = this(RemoteInput.DEFAULT_PORT, Some(listener))
  def this(port:           Int)(implicit sde:                 sge.Sge) = this(port, None)

  class KeyEvent {
    var timeStamp: Long = scala.compiletime.uninitialized
    var `type`:    Int  = scala.compiletime.uninitialized
    var keyCode:   Int  = scala.compiletime.uninitialized
    var keyChar:   Char = scala.compiletime.uninitialized
  }

  object KeyEvent {
    final val KEY_DOWN  = 0
    final val KEY_UP    = 1
    final val KEY_TYPED = 2
  }

  class TouchEvent {
    var timeStamp: Long = scala.compiletime.uninitialized
    var `type`:    Int  = scala.compiletime.uninitialized
    var x:         Int  = scala.compiletime.uninitialized
    var y:         Int  = scala.compiletime.uninitialized
    var pointer:   Int  = scala.compiletime.uninitialized
  }

  object TouchEvent {
    final val TOUCH_DOWN    = 0
    final val TOUCH_UP      = 1
    final val TOUCH_DRAGGED = 2
  }

  class EventTrigger(touchEvent: TouchEvent, keyEvent: KeyEvent) extends Runnable {
    override def run(): Unit = {
      justTouchedFlag = false
      if (keyJustPressed) {
        keyJustPressed = false
        for (i <- justPressedKeys.indices)
          justPressedKeys(i) = false
      }

      if (processor != null) {
        if (touchEvent != null) {
          touchEvent.`type` match {
            case TouchEvent.TOUCH_DOWN =>
              deltaX(touchEvent.pointer) = 0
              deltaY(touchEvent.pointer) = 0
              processor.touchDown(touchEvent.x, touchEvent.y, touchEvent.pointer, Input.Buttons.LEFT)
              touchedPointers(touchEvent.pointer) = true
              justTouchedFlag = true
            case TouchEvent.TOUCH_UP =>
              deltaX(touchEvent.pointer) = 0
              deltaY(touchEvent.pointer) = 0
              processor.touchUp(touchEvent.x, touchEvent.y, touchEvent.pointer, Input.Buttons.LEFT)
              touchedPointers(touchEvent.pointer) = false
            case TouchEvent.TOUCH_DRAGGED =>
              deltaX(touchEvent.pointer) = touchEvent.x - touchX(touchEvent.pointer)
              deltaY(touchEvent.pointer) = touchEvent.y - touchY(touchEvent.pointer)
              processor.touchDragged(touchEvent.x, touchEvent.y, touchEvent.pointer)
          }
          touchX(touchEvent.pointer) = touchEvent.x
          touchY(touchEvent.pointer) = touchEvent.y
        }
        if (keyEvent != null) {
          keyEvent.`type` match {
            case KeyEvent.KEY_DOWN =>
              processor.keyDown(keyEvent.keyCode)
              if (!keys(keyEvent.keyCode)) {
                keyCount += 1
                keys(keyEvent.keyCode) = true
              }
              keyJustPressed = true
              justPressedKeys(keyEvent.keyCode) = true
            case KeyEvent.KEY_UP =>
              processor.keyUp(keyEvent.keyCode)
              if (keys(keyEvent.keyCode)) {
                keyCount -= 1
                keys(keyEvent.keyCode) = false
              }
            case KeyEvent.KEY_TYPED =>
              processor.keyTyped(keyEvent.keyChar)
          }
        }
      } else {
        if (touchEvent != null) {
          touchEvent.`type` match {
            case TouchEvent.TOUCH_DOWN =>
              deltaX(touchEvent.pointer) = 0
              deltaY(touchEvent.pointer) = 0
              touchedPointers(touchEvent.pointer) = true
              justTouchedFlag = true
            case TouchEvent.TOUCH_UP =>
              deltaX(touchEvent.pointer) = 0
              deltaY(touchEvent.pointer) = 0
              touchedPointers(touchEvent.pointer) = false
            case TouchEvent.TOUCH_DRAGGED =>
              deltaX(touchEvent.pointer) = touchEvent.x - touchX(touchEvent.pointer)
              deltaY(touchEvent.pointer) = touchEvent.y - touchY(touchEvent.pointer)
          }
          touchX(touchEvent.pointer) = touchEvent.x
          touchY(touchEvent.pointer) = touchEvent.y
        }
        if (keyEvent != null) {
          if (keyEvent.`type` == KeyEvent.KEY_DOWN) {
            if (!keys(keyEvent.keyCode)) {
              keyCount += 1
              keys(keyEvent.keyCode) = true
            }
            keyJustPressed = true
            justPressedKeys(keyEvent.keyCode) = true
          }
          if (keyEvent.`type` == KeyEvent.KEY_UP) {
            if (keys(keyEvent.keyCode)) {
              keyCount -= 1
              keys(keyEvent.keyCode) = false
            }
          }
        }
      }
    }
  }

  private var serverSocket: ServerSocket = scala.compiletime.uninitialized
  private val accel   = Array.ofDim[Float](3)
  private val gyrate  = Array.ofDim[Float](3)
  private val compass = Array.ofDim[Float](3)
  private var multiTouch:   Boolean = false
  private var remoteWidth:  Float   = 0f
  private var remoteHeight: Float   = 0f
  private var connected:    Boolean = false

  var keyCount:        Int            = 0
  val keys:            Array[Boolean] = Array.ofDim[Boolean](256)
  var keyJustPressed:  Boolean        = false
  val justPressedKeys: Array[Boolean] = Array.ofDim[Boolean](256)
  val deltaX:          Array[Int]     = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val deltaY:          Array[Int]     = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val touchX:          Array[Int]     = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val touchY:          Array[Int]     = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val touchedPointers: Array[Boolean] = Array.ofDim[Boolean](RemoteInput.MAX_TOUCHES)
  var justTouchedFlag: Boolean        = false
  var processor:       InputProcessor = null
  val ips: Array[String] =
    try {
      serverSocket = new ServerSocket(port)
      val thread = new Thread(this)
      thread.setDaemon(true)
      thread.start()
      val allByName = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())
      val result    = Array.ofDim[String](allByName.length)
      for (i <- allByName.indices)
        result(i) = allByName(i).getHostAddress()
      result
    } catch {
      case e: Exception =>
        throw SdeError.NetworkError(s"Couldn't open listening socket at port '$port'", Some(e))
    }

  override def run(): Unit =
    while (true)
      try {
        connected = false
        listener.foreach(_.onDisconnected())

        println(s"listening, port $port")
        val socket = serverSocket.accept()
        socket.setTcpNoDelay(true)
        socket.setSoTimeout(3000)
        connected = true
        listener.foreach(_.onConnected())

        val in = new DataInputStream(socket.getInputStream())
        multiTouch = in.readBoolean()
        while (true) {
          val event = in.readInt()
          var keyEvent:   KeyEvent   = null
          var touchEvent: TouchEvent = null
          event match {
            case RemoteSender.ACCEL =>
              accel(0) = in.readFloat()
              accel(1) = in.readFloat()
              accel(2) = in.readFloat()
            case RemoteSender.COMPASS =>
              compass(0) = in.readFloat()
              compass(1) = in.readFloat()
              compass(2) = in.readFloat()
            case RemoteSender.SIZE =>
              remoteWidth = in.readFloat()
              remoteHeight = in.readFloat()
            case RemoteSender.GYRO =>
              gyrate(0) = in.readFloat()
              gyrate(1) = in.readFloat()
              gyrate(2) = in.readFloat()
            case RemoteSender.KEY_DOWN =>
              keyEvent = new KeyEvent()
              keyEvent.keyCode = in.readInt()
              keyEvent.`type` = KeyEvent.KEY_DOWN
            case RemoteSender.KEY_UP =>
              keyEvent = new KeyEvent()
              keyEvent.keyCode = in.readInt()
              keyEvent.`type` = KeyEvent.KEY_UP
            case RemoteSender.KEY_TYPED =>
              keyEvent = new KeyEvent()
              keyEvent.keyChar = in.readChar()
              keyEvent.`type` = KeyEvent.KEY_TYPED
            case RemoteSender.TOUCH_DOWN =>
              touchEvent = new TouchEvent()
              touchEvent.x = ((in.readInt() / remoteWidth) * RemoteInput.DEFAULT_SCREEN_WIDTH).toInt
              touchEvent.y = ((in.readInt() / remoteHeight) * RemoteInput.DEFAULT_SCREEN_HEIGHT).toInt
              touchEvent.pointer = in.readInt()
              touchEvent.`type` = TouchEvent.TOUCH_DOWN
            case RemoteSender.TOUCH_UP =>
              touchEvent = new TouchEvent()
              touchEvent.x = ((in.readInt() / remoteWidth) * RemoteInput.DEFAULT_SCREEN_WIDTH).toInt
              touchEvent.y = ((in.readInt() / remoteHeight) * RemoteInput.DEFAULT_SCREEN_HEIGHT).toInt
              touchEvent.pointer = in.readInt()
              touchEvent.`type` = TouchEvent.TOUCH_UP
            case RemoteSender.TOUCH_DRAGGED =>
              touchEvent = new TouchEvent()
              touchEvent.x = ((in.readInt() / remoteWidth) * RemoteInput.DEFAULT_SCREEN_WIDTH).toInt
              touchEvent.y = ((in.readInt() / remoteHeight) * RemoteInput.DEFAULT_SCREEN_HEIGHT).toInt
              touchEvent.pointer = in.readInt()
              touchEvent.`type` = TouchEvent.TOUCH_DRAGGED
          }

          // TODO: Post this to main thread when Application interface is available
          new EventTrigger(touchEvent, keyEvent).run()
        }
      } catch {
        case e: IOException =>
          e.printStackTrace()
      }

  def isConnected(): Boolean = connected

  override def getAccelerometerX():        Float   = accel(0)
  override def getAccelerometerY():        Float   = accel(1)
  override def getAccelerometerZ():        Float   = accel(2)
  override def getGyroscopeX():            Float   = gyrate(0)
  override def getGyroscopeY():            Float   = gyrate(1)
  override def getGyroscopeZ():            Float   = gyrate(2)
  override def getMaxPointers():           Int     = RemoteInput.MAX_TOUCHES
  override def getX():                     Int     = touchX(0)
  override def getX(pointer:        Int):  Int     = touchX(pointer)
  override def getY():                     Int     = touchY(0)
  override def getY(pointer:        Int):  Int     = touchY(pointer)
  override def isTouched():                Boolean = touchedPointers(0)
  override def justTouched():              Boolean = justTouchedFlag
  override def isTouched(pointer:   Int):  Boolean = touchedPointers(pointer)
  override def getPressure():              Float   = getPressure(0)
  override def getPressure(pointer: Int):  Float   = if (touchedPointers(pointer)) 1f else 0f

  override def isButtonPressed(button: Int): Boolean = scala.util.boundary {
    if (button != Input.Buttons.LEFT) scala.util.boundary.break(false)
    for (i <- touchedPointers.indices)
      if (touchedPointers(i)) scala.util.boundary.break(true)
    false
  }

  override def isButtonJustPressed(button: Int): Boolean =
    button == Input.Buttons.LEFT && justTouchedFlag

  override def isKeyPressed(key: Int): Boolean = scala.util.boundary {
    if (key == Input.Keys.ANY_KEY) scala.util.boundary.break(keyCount > 0)
    if (key < 0 || key > 255) scala.util.boundary.break(false)
    keys(key)
  }

  override def isKeyJustPressed(key: Int): Boolean = scala.util.boundary {
    if (key == Input.Keys.ANY_KEY) scala.util.boundary.break(keyJustPressed)
    if (key < 0 || key > 255) scala.util.boundary.break(false)
    justPressedKeys(key)
  }

  override def getTextInput(listener: Input.TextInputListener, title: String, text: String, hint: String): Unit =
    sde.input.getTextInput(listener, title, text, hint)

  override def getTextInput(listener: Input.TextInputListener, title: String, text: String, hint: String, `type`: OnscreenKeyboardType.OnscreenKeyboardType): Unit =
    sde.input.getTextInput(listener, title, text, hint, `type`)

  override def setOnscreenKeyboardVisible(visible: Boolean):                                                    Unit           = {}
  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType.OnscreenKeyboardType): Unit           = {}
  override def openTextInputField(configuration:   NativeInputConfiguration):                                   Unit           = {}
  override def closeTextInputField(sendReturn:     Boolean):                                                    Unit           = {}
  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver):                                     Unit           = {}
  override def vibrate(milliseconds:               Int):                                                        Unit           = {}
  override def vibrate(milliseconds:               Int, fallback:   Boolean):                                   Unit           = {}
  override def vibrate(milliseconds:               Int, amplitude:  Int, fallback: Boolean):                    Unit           = {}
  override def vibrate(vibrationType:              VibrationType.VibrationType):                                Unit           = {}
  override def getAzimuth():                                                                                    Float          = compass(0)
  override def getPitch():                                                                                      Float          = compass(1)
  override def getRoll():                                                                                       Float          = compass(2)
  override def setCatchKey(keycode:                Int, catchKey:   Boolean):                                   Unit           = {}
  override def isCatchKey(keycode:                 Int):                                                        Boolean        = false
  override def setInputProcessor(processor:        InputProcessor):                                             Unit           = this.processor = processor
  override def getInputProcessor():                                                                             InputProcessor = this.processor

  /** @return
    *   the IP addresses {@link RemoteSender} or gdx-remote should connect to. Most likely the LAN addresses if behind a NAT.
    */
  def getIPs(): Array[String] = ips

  override def isPeripheralAvailable(peripheral: Peripheral.Peripheral): Boolean =
    peripheral match {
      case Peripheral.Accelerometer    => true
      case Peripheral.Compass          => true
      case Peripheral.MultitouchScreen => multiTouch
      case _                           => false
    }

  override def getRotation():                           Int                     = 0
  override def getNativeOrientation():                  Orientation.Orientation = Orientation.Landscape
  override def setCursorCatched(catched: Boolean):      Unit                    = {}
  override def isCursorCatched():                       Boolean                 = false
  override def getDeltaX():                             Int                     = deltaX(0)
  override def getDeltaX(pointer:        Int):          Int                     = deltaX(pointer)
  override def getDeltaY():                             Int                     = deltaY(0)
  override def getDeltaY(pointer:        Int):          Int                     = deltaY(pointer)
  override def setCursorPosition(x:      Int, y: Int):  Unit                    = {}
  override def getCurrentEventTime():                   Long                    = 0L
  override def getRotationMatrix(matrix: Array[Float]): Unit                    = {}
}

object RemoteInput {
  final val MAX_TOUCHES     = 20
  val DEFAULT_PORT          = 8190
  val DEFAULT_SCREEN_WIDTH  = 800
  val DEFAULT_SCREEN_HEIGHT = 600
}
