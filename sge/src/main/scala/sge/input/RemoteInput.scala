/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/RemoteInput.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Gdx -> Sge; GdxRuntimeException -> SgeError.NetworkError; isTouched[] -> touchedPointers;
 *     justTouched -> justTouchedFlag; RemoteInputListener extracted as top-level trait
 *   Convention: Java constructor overloads -> default params + auxiliary constructors;
 *     listener: null -> Option; processor: null -> Nullable; Gdx singleton -> implicit Sge
 *   Idiom: boundary/break (6 return), Nullable (3 null), split packages
 *   Convention: anonymous (using Sge) + Sge() accessor
 *   TODOs: 1 — postRunnable not yet wired (EventTrigger.run() called directly)
 *   Convention: opaque Key/Button for key/button parameters
 *   Convention: opaque Pixels for TouchEvent x/y, getX/getY, getDeltaX/getDeltaY, setCursorPosition
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 380
 * Covenant-baseline-methods: DEFAULT_PORT,EventTrigger,KEY_DOWN,KEY_TYPED,KEY_UP,KeyEvent,MAX_TOUCHES,RemoteInput,RemoteInputListener,TOUCH_DOWN,TOUCH_DRAGGED,TOUCH_UP,TouchEvent,accel,accelerometerX,accelerometerY,accelerometerZ,azimuth,closeTextInputField,compass,connected,currentEventTime,cursorCatched,deltaX,deltaXArr,deltaY,deltaYArr,getRotationMatrix,getTextInput,gyrate,gyroscopeX,gyroscopeY,gyroscopeZ,iPs,ips,isButtonJustPressed,isButtonPressed,isCatchKey,isConnected,isKeyJustPressed,isKeyPressed,isPeripheralAvailable,isTouched,justPressedKeys,justTouched,justTouchedFlag,keyChar,keyCode,keyCount,keyJustPressed,keys,maxPointers,multiTouch,nativeOrientation,onConnected,onDisconnected,openTextInputField,pitch,pointer,pressure,processor,remoteHeight,remoteWidth,roll,rotation,run,serverSocket,setCatchKey,setCursorCatched,setCursorPosition,setInputProcessor,setKeyboardHeightObserver,setOnscreenKeyboardVisible,this,timeStamp,touchX,touchY,touched,touchedPointers,vibrate,x,y
 * Covenant-source-reference: com/badlogic/gdx/input/RemoteInput.java
 * Covenant-verified: 2026-04-19
 */
package sge
package input

import java.io.{ DataInputStream, IOException }
import java.net.{ InetAddress, ServerSocket }
import sge.utils.{ Nullable, SgeError }
import scala.annotation.nowarn
import sge.Input.{ Button, Key, KeyboardHeightObserver, OnscreenKeyboardType, Orientation, Peripheral, VibrationType }
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
class RemoteInput(port: Int = RemoteInput.DEFAULT_PORT, listener: Option[RemoteInputListener] = None)(using Sge) extends Runnable with Input {

  def this()(using Sge) = this(RemoteInput.DEFAULT_PORT, None)
  def this(listener: RemoteInputListener)(using Sge) = this(RemoteInput.DEFAULT_PORT, Some(listener))
  def this(port:     Int)(using Sge) = this(port, None)

  class KeyEvent {
    var timeStamp: Long = scala.compiletime.uninitialized
    var `type`:    Int  = scala.compiletime.uninitialized
    var keyCode:   Key  = scala.compiletime.uninitialized
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

  class EventTrigger(touchEvent: Nullable[TouchEvent], keyEvent: Nullable[KeyEvent]) extends Runnable {
    override def run(): Unit = {
      justTouchedFlag = false
      if (keyJustPressed) {
        keyJustPressed = false
        for (i <- justPressedKeys.indices)
          justPressedKeys(i) = false
      }

      processor.fold {
        touchEvent.foreach { te =>
          te.`type` match {
            case TouchEvent.TOUCH_DOWN =>
              deltaXArr(te.pointer) = 0
              deltaYArr(te.pointer) = 0
              touchedPointers(te.pointer) = true
              justTouchedFlag = true
            case TouchEvent.TOUCH_UP =>
              deltaXArr(te.pointer) = 0
              deltaYArr(te.pointer) = 0
              touchedPointers(te.pointer) = false
            case TouchEvent.TOUCH_DRAGGED =>
              deltaXArr(te.pointer) = te.x - touchX(te.pointer)
              deltaYArr(te.pointer) = te.y - touchY(te.pointer)
          }
          touchX(te.pointer) = te.x
          touchY(te.pointer) = te.y
        }
        keyEvent.foreach { ke =>
          if (ke.`type` == KeyEvent.KEY_DOWN) {
            if (!keys(ke.keyCode.toInt)) {
              keyCount += 1
              keys(ke.keyCode.toInt) = true
            }
            keyJustPressed = true
            justPressedKeys(ke.keyCode.toInt) = true
          }
          if (ke.`type` == KeyEvent.KEY_UP) {
            if (keys(ke.keyCode.toInt)) {
              keyCount -= 1
              keys(ke.keyCode.toInt) = false
            }
          }
        }
      } { p =>
        touchEvent.foreach { te =>
          te.`type` match {
            case TouchEvent.TOUCH_DOWN =>
              deltaXArr(te.pointer) = 0
              deltaYArr(te.pointer) = 0
              p.touchDown(Pixels(te.x), Pixels(te.y), te.pointer, Input.Buttons.LEFT)
              touchedPointers(te.pointer) = true
              justTouchedFlag = true
            case TouchEvent.TOUCH_UP =>
              deltaXArr(te.pointer) = 0
              deltaYArr(te.pointer) = 0
              p.touchUp(Pixels(te.x), Pixels(te.y), te.pointer, Input.Buttons.LEFT)
              touchedPointers(te.pointer) = false
            case TouchEvent.TOUCH_DRAGGED =>
              deltaXArr(te.pointer) = te.x - touchX(te.pointer)
              deltaYArr(te.pointer) = te.y - touchY(te.pointer)
              p.touchDragged(Pixels(te.x), Pixels(te.y), te.pointer)
          }
          touchX(te.pointer) = te.x
          touchY(te.pointer) = te.y
        }
        keyEvent.foreach { ke =>
          ke.`type` match {
            case KeyEvent.KEY_DOWN =>
              p.keyDown(ke.keyCode)
              if (!keys(ke.keyCode.toInt)) {
                keyCount += 1
                keys(ke.keyCode.toInt) = true
              }
              keyJustPressed = true
              justPressedKeys(ke.keyCode.toInt) = true
            case KeyEvent.KEY_UP =>
              p.keyUp(ke.keyCode)
              if (keys(ke.keyCode.toInt)) {
                keyCount -= 1
                keys(ke.keyCode.toInt) = false
              }
            case KeyEvent.KEY_TYPED =>
              p.keyTyped(ke.keyChar)
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

  var keyCount:        Int                      = 0
  val keys:            Array[Boolean]           = Array.ofDim[Boolean](256)
  var keyJustPressed:  Boolean                  = false
  val justPressedKeys: Array[Boolean]           = Array.ofDim[Boolean](256)
  val deltaXArr:       Array[Int]               = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val deltaYArr:       Array[Int]               = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val touchX:          Array[Int]               = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val touchY:          Array[Int]               = Array.ofDim[Int](RemoteInput.MAX_TOUCHES)
  val touchedPointers: Array[Boolean]           = Array.ofDim[Boolean](RemoteInput.MAX_TOUCHES)
  var justTouchedFlag: Boolean                  = false
  var processor:       Nullable[InputProcessor] = Nullable.empty
  val ips:             Array[String]            =
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
        throw SgeError.NetworkError(s"Couldn't open listening socket at port '$port'", Some(e))
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
          var keyEvent:   Nullable[KeyEvent]   = Nullable.empty
          var touchEvent: Nullable[TouchEvent] = Nullable.empty
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
              val ke = KeyEvent()
              ke.keyCode = Key(in.readInt())
              ke.`type` = KeyEvent.KEY_DOWN
              keyEvent = Nullable(ke)
            case RemoteSender.KEY_UP =>
              val ke = KeyEvent()
              ke.keyCode = Key(in.readInt())
              ke.`type` = KeyEvent.KEY_UP
              keyEvent = Nullable(ke)
            case RemoteSender.KEY_TYPED =>
              val ke = KeyEvent()
              ke.keyChar = in.readChar()
              ke.`type` = KeyEvent.KEY_TYPED
              keyEvent = Nullable(ke)
            case RemoteSender.TOUCH_DOWN =>
              val te = TouchEvent()
              te.x = ((in.readInt() / remoteWidth) * Sge().graphics.width.toFloat).toInt
              te.y = ((in.readInt() / remoteHeight) * Sge().graphics.height.toFloat).toInt
              te.pointer = in.readInt()
              te.`type` = TouchEvent.TOUCH_DOWN
              touchEvent = Nullable(te)
            case RemoteSender.TOUCH_UP =>
              val te = TouchEvent()
              te.x = ((in.readInt() / remoteWidth) * Sge().graphics.width.toFloat).toInt
              te.y = ((in.readInt() / remoteHeight) * Sge().graphics.height.toFloat).toInt
              te.pointer = in.readInt()
              te.`type` = TouchEvent.TOUCH_UP
              touchEvent = Nullable(te)
            case RemoteSender.TOUCH_DRAGGED =>
              val te = TouchEvent()
              te.x = ((in.readInt() / remoteWidth) * Sge().graphics.width.toFloat).toInt
              te.y = ((in.readInt() / remoteHeight) * Sge().graphics.height.toFloat).toInt
              te.pointer = in.readInt()
              te.`type` = TouchEvent.TOUCH_DRAGGED
              touchEvent = Nullable(te)
          }

          Sge().application.postRunnable(() => EventTrigger(touchEvent, keyEvent).run())
        }
      } catch {
        case e: IOException =>
          e.printStackTrace()
      }

  def isConnected(): Boolean = connected

  override def accelerometerX:          Float   = accel(0)
  override def accelerometerY:          Float   = accel(1)
  override def accelerometerZ:          Float   = accel(2)
  override def gyroscopeX:              Float   = gyrate(0)
  override def gyroscopeY:              Float   = gyrate(1)
  override def gyroscopeZ:              Float   = gyrate(2)
  override def maxPointers:             Int     = RemoteInput.MAX_TOUCHES
  override def x:                       Pixels  = Pixels(touchX(0))
  override def x(pointer:         Int): Pixels  = Pixels(touchX(pointer))
  override def y:                       Pixels  = Pixels(touchY(0))
  override def y(pointer:         Int): Pixels  = Pixels(touchY(pointer))
  override def touched:                 Boolean = touchedPointers(0)
  override def justTouched():           Boolean = justTouchedFlag
  override def isTouched(pointer: Int): Boolean = touchedPointers(pointer)
  override def pressure:                Float   = pressure(0)
  override def pressure(pointer:  Int): Float   = if (touchedPointers(pointer)) 1f else 0f

  override def isButtonPressed(button: Button): Boolean = scala.util.boundary {
    if (button != Input.Buttons.LEFT) scala.util.boundary.break(false)
    for (i <- touchedPointers.indices)
      if (touchedPointers(i)) scala.util.boundary.break(true)
    false
  }

  override def isButtonJustPressed(button: Button): Boolean =
    button == Input.Buttons.LEFT && justTouchedFlag

  override def isKeyPressed(key: Key): Boolean = scala.util.boundary {
    if (key == Input.Keys.ANY_KEY) scala.util.boundary.break(keyCount > 0)
    if (key.toInt < 0 || key.toInt > 255) scala.util.boundary.break(false)
    keys(key.toInt)
  }

  override def isKeyJustPressed(key: Key): Boolean = scala.util.boundary {
    if (key == Input.Keys.ANY_KEY) scala.util.boundary.break(keyJustPressed)
    if (key.toInt < 0 || key.toInt > 255) scala.util.boundary.break(false)
    justPressedKeys(key.toInt)
  }

  override def getTextInput(listener: Input.TextInputListener, title: String, text: String, hint: String): Unit =
    Sge().input.getTextInput(listener, title, text, hint)

  override def getTextInput(listener: Input.TextInputListener, title: String, text: String, hint: String, `type`: OnscreenKeyboardType): Unit =
    Sge().input.getTextInput(listener, title, text, hint, `type`)

  override def setOnscreenKeyboardVisible(visible: Boolean):                                 Unit    = {}
  override def setOnscreenKeyboardVisible(visible: Boolean, `type`: OnscreenKeyboardType):   Unit    = {}
  override def openTextInputField(configuration:   NativeInputConfiguration):                Unit    = {}
  override def closeTextInputField(sendReturn:     Boolean):                                 Unit    = {}
  override def setKeyboardHeightObserver(observer: KeyboardHeightObserver):                  Unit    = {}
  override def vibrate(milliseconds:               Int):                                     Unit    = {}
  override def vibrate(milliseconds:               Int, fallback:   Boolean):                Unit    = {}
  override def vibrate(milliseconds:               Int, amplitude:  Int, fallback: Boolean): Unit    = {}
  override def vibrate(vibrationType:              VibrationType):                           Unit    = {}
  override def azimuth:                                                                      Float   = compass(0)
  override def pitch:                                                                        Float   = compass(1)
  override def roll:                                                                         Float   = compass(2)
  override def setCatchKey(keycode:                Key, catchKey:   Boolean):                Unit    = {}
  override def isCatchKey(keycode:                 Key):                                     Boolean = false
  override def setInputProcessor(processor:        InputProcessor):                          Unit    = this.processor = Nullable(processor)
  // Input trait returns InputProcessor (not Nullable) — orNull needed at API boundary
  @nowarn("msg=deprecated") override def inputProcessor: InputProcessor = this.processor.orNull

  /** @return
    *   the IP addresses {@link RemoteSender} or gdx-remote should connect to. Most likely the LAN addresses if behind a NAT.
    */
  def iPs: Array[String] = ips

  override def isPeripheralAvailable(peripheral: Peripheral): Boolean =
    peripheral match {
      case Peripheral.Accelerometer    => true
      case Peripheral.Compass          => true
      case Peripheral.MultitouchScreen => multiTouch
      case _                           => false
    }

  override def rotation:                                     Int             = 0
  override def nativeOrientation:                            Orientation     = Orientation.Landscape
  override def setCursorCatched(catched: Boolean):           Unit            = {}
  override def cursorCatched:                                Boolean         = false
  override def deltaX:                                       Pixels          = Pixels(deltaXArr(0))
  override def deltaX(pointer:           Int):               Pixels          = Pixels(deltaXArr(pointer))
  override def deltaY:                                       Pixels          = Pixels(deltaYArr(0))
  override def deltaY(pointer:           Int):               Pixels          = Pixels(deltaYArr(pointer))
  override def setCursorPosition(x:      Pixels, y: Pixels): Unit            = {}
  override def currentEventTime:                             sge.utils.Nanos = sge.utils.Nanos.zero
  override def getRotationMatrix(matrix: Array[Float]):      Unit            = {}
}

object RemoteInput {
  final val MAX_TOUCHES = 20
  val DEFAULT_PORT      = 8190
}
