package sge.input

import java.io.DataOutputStream
import java.net.Socket
import sge.InputProcessor

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
  private var out:       DataOutputStream = scala.compiletime.uninitialized
  private var connected: Boolean          = false

  // Constructor body
  try {
    val socket = new Socket(ip, port)
    socket.setTcpNoDelay(true)
    socket.setSoTimeout(3000)
    out = new DataOutputStream(socket.getOutputStream())
    out.writeBoolean(true) // placeholder
    connected = true
    sde.input.setInputProcessor(this)
  } catch {
    case e: Exception =>
      println("RemoteSender: couldn't connect to " + ip + ":" + port)
  }

  def sendUpdate(): Unit = {
    this.synchronized {
      if (!connected) return
    }
    try {
      out.writeInt(RemoteSender.ACCEL)
      out.writeFloat(sde.input.getAccelerometerX())
      out.writeFloat(sde.input.getAccelerometerY())
      out.writeFloat(sde.input.getAccelerometerZ())
      out.writeInt(RemoteSender.COMPASS)
      out.writeFloat(sde.input.getAzimuth())
      out.writeFloat(sde.input.getPitch())
      out.writeFloat(sde.input.getRoll())
      out.writeInt(RemoteSender.SIZE)
      out.writeFloat(800.0f) // placeholder width
      out.writeFloat(600.0f) // placeholder height
      out.writeInt(RemoteSender.GYRO)
      out.writeFloat(sde.input.getGyroscopeX())
      out.writeFloat(sde.input.getGyroscopeY())
      out.writeFloat(sde.input.getGyroscopeZ())
    } catch {
      case _: Throwable =>
        out = null
        connected = false
    }
  }

  override def keyDown(keycode: Int): Boolean = {
    this.synchronized {
      if (!connected) return false
    }

    try {
      out.writeInt(RemoteSender.KEY_DOWN)
      out.writeInt(keycode)
    } catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def keyUp(keycode: Int): Boolean = {
    this.synchronized {
      if (!connected) return false
    }

    try {
      out.writeInt(RemoteSender.KEY_UP)
      out.writeInt(keycode)
    } catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def keyTyped(character: Char): Boolean = {
    this.synchronized {
      if (!connected) return false
    }

    try {
      out.writeInt(RemoteSender.KEY_TYPED)
      out.writeChar(character)
    } catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean = {
    this.synchronized {
      if (!connected) return false
    }

    try {
      out.writeInt(RemoteSender.TOUCH_DOWN)
      out.writeInt(x)
      out.writeInt(y)
      out.writeInt(pointer)
    } catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean = {
    this.synchronized {
      if (!connected) return false
    }

    try {
      out.writeInt(RemoteSender.TOUCH_UP)
      out.writeInt(x)
      out.writeInt(y)
      out.writeInt(pointer)
    } catch {
      case _: Throwable =>
        this.synchronized {
          connected = false
        }
    }
    false
  }

  override def touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    touchUp(screenX, screenY, pointer, button)

  override def touchDragged(x: Int, y: Int, pointer: Int): Boolean = {
    this.synchronized {
      if (!connected) return false
    }

    try {
      out.writeInt(RemoteSender.TOUCH_DRAGGED)
      out.writeInt(x)
      out.writeInt(y)
      out.writeInt(pointer)
    } catch {
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
