/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/RemoteSender.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.input

import java.io.DataOutputStream
import java.net.Socket
import sge.InputProcessor
import sge.utils.Nullable

object RemoteSender {
  final val KEY_DOWN  = 0
  final val KEY_UP    = 1
  final val KEY_TYPED = 2

  final val TOUCH_DOWN    = 3
  final val TOUCH_UP      = 4
  final val TOUCH_DRAGGED = 5

  final val ACCEL   = 6
  final val COMPASS = 7
  final val SIZE    = 8
  final val GYRO    = 9
}

/** Sends all inputs from touch, key, accelerometer and compass to a {@link RemoteInput} at the given ip/port. Instantiate this and call sendUpdate() periodically.
  *
  * @author
  *   mzechner (original implementation)
  */
class RemoteSender(ip: String, port: Int)(implicit sde: sge.Sge) extends InputProcessor {
  private var out:       Nullable[DataOutputStream] = Nullable.empty
  private var connected: Boolean                    = false

  // Constructor body
  try {
    val socket = new Socket(ip, port)
    socket.setTcpNoDelay(true)
    socket.setSoTimeout(3000)
    out = Nullable(new DataOutputStream(socket.getOutputStream()))
    out.foreach(_.writeBoolean(true)) // placeholder
    connected = true
    sde.input.setInputProcessor(this)
  } catch {
    case _: Exception =>
      println("RemoteSender: couldn't connect to " + ip + ":" + port)
  }

  def sendUpdate(): Unit = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(())
    }
    try
      out.foreach { o =>
        o.writeInt(RemoteSender.ACCEL)
        o.writeFloat(sde.input.getAccelerometerX())
        o.writeFloat(sde.input.getAccelerometerY())
        o.writeFloat(sde.input.getAccelerometerZ())
        o.writeInt(RemoteSender.COMPASS)
        o.writeFloat(sde.input.getAzimuth())
        o.writeFloat(sde.input.getPitch())
        o.writeFloat(sde.input.getRoll())
        o.writeInt(RemoteSender.SIZE)
        o.writeFloat(800.0f) // placeholder width
        o.writeFloat(600.0f) // placeholder height
        o.writeInt(RemoteSender.GYRO)
        o.writeFloat(sde.input.getGyroscopeX())
        o.writeFloat(sde.input.getGyroscopeY())
        o.writeFloat(sde.input.getGyroscopeZ())
      }
    catch {
      case _: Throwable =>
        out = Nullable.empty
        connected = false
    }
  }

  override def keyDown(keycode: Int): Boolean = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(false)
    }

    try
      out.foreach { o =>
        o.writeInt(RemoteSender.KEY_DOWN)
        o.writeInt(keycode)
      }
    catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def keyUp(keycode: Int): Boolean = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(false)
    }

    try
      out.foreach { o =>
        o.writeInt(RemoteSender.KEY_UP)
        o.writeInt(keycode)
      }
    catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def keyTyped(character: Char): Boolean = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(false)
    }

    try
      out.foreach { o =>
        o.writeInt(RemoteSender.KEY_TYPED)
        o.writeChar(character)
      }
    catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(false)
    }

    try
      out.foreach { o =>
        o.writeInt(RemoteSender.TOUCH_DOWN)
        o.writeInt(x)
        o.writeInt(y)
        o.writeInt(pointer)
      }
    catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(false)
    }

    try
      out.foreach { o =>
        o.writeInt(RemoteSender.TOUCH_UP)
        o.writeInt(x)
        o.writeInt(y)
        o.writeInt(pointer)
      }
    catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    touchUp(screenX, screenY, pointer, button)

  override def touchDragged(x: Int, y: Int, pointer: Int): Boolean = scala.util.boundary {
    this.synchronized {
      if (!connected) scala.util.boundary.break(false)
    }

    try
      out.foreach { o =>
        o.writeInt(RemoteSender.TOUCH_DRAGGED)
        o.writeInt(x)
        o.writeInt(y)
        o.writeInt(pointer)
      }
    catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def mouseMoved(x: Int, y: Int): Boolean =
    false

  override def scrolled(amountX: Float, amountY: Float): Boolean =
    false

  def isConnected(): Boolean =
    this.synchronized {
      connected
    }
}
