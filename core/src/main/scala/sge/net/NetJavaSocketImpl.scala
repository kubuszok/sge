/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/NetJavaSocketImpl.java
 * Original authors: noblemaster
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `Disposable.dispose()` → `AutoCloseable.close()`
 *   Idiom: split packages; 2-arg constructor (existing socket) is separate from connecting constructor
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package net

import java.net.{ InetSocketAddress, Socket => JSocket }
import java.io.{ InputStream, OutputStream }
import sge.utils.Nullable

/** Socket implementation using java.net.Socket.
  *
  * @author
  *   noblemaster (original implementation)
  */
class NetJavaSocketImpl private (private var socket: JSocket) extends Socket {

  /** Creates a new socket and connects to the given host:port. */
  def this(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints) = {
    this(new JSocket())
    try {
      NetJavaSocketImpl.applyHints(socket, hints) // better to call BEFORE socket is connected!
      // and connect...
      val address = new InetSocketAddress(host, port)
      Nullable(hints).fold(socket.connect(address)) { h =>
        socket.connect(address, h.connectTimeout)
      }
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Error making a socket connection to $host:$port", e)
    }
  }

  /** Wraps an already-connected socket. */
  def this(socket: JSocket, hints: SocketHints) = {
    this(socket)
    NetJavaSocketImpl.applyHints(socket, hints)
  }

  override def isConnected(): Boolean =
    Nullable(socket).exists(_.isConnected())

  override def getInputStream(): InputStream =
    try
      socket.getInputStream()
    catch {
      case e: Exception =>
        throw new RuntimeException("Error getting input stream from socket.", e)
    }

  override def getOutputStream(): OutputStream =
    try
      socket.getOutputStream()
    catch {
      case e: Exception =>
        throw new RuntimeException("Error getting output stream from socket.", e)
    }

  override def getRemoteAddress(): String =
    socket.getRemoteSocketAddress().toString()

  override def close(): Unit =
    Nullable(socket).foreach { s =>
      try {
        s.close()
        socket = null // raw null at Java interop boundary
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error closing socket.", e)
      }
    }
}

object NetJavaSocketImpl {
  private def applyHints(socket: JSocket, hints: SocketHints): Unit =
    Nullable(hints).foreach { h =>
      try {
        socket.setPerformancePreferences(h.performancePrefConnectionTime, h.performancePrefLatency, h.performancePrefBandwidth)
        socket.setTrafficClass(h.trafficClass)
        socket.setTcpNoDelay(h.tcpNoDelay)
        socket.setKeepAlive(h.keepAlive)
        socket.setSendBufferSize(h.sendBufferSize)
        socket.setReceiveBufferSize(h.receiveBufferSize)
        socket.setSoLinger(h.linger, h.lingerDuration)
        socket.setSoTimeout(h.socketTimeout)
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error setting socket hints.", e)
      }
    }
}
