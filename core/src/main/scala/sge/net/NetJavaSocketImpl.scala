/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/NetJavaSocketImpl.java
 * Original authors: noblemaster
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.net.{ InetSocketAddress, Socket => JSocket }
import java.io.{ InputStream, OutputStream }
// import sge.utils.SgeError

/** Socket implementation using java.net.Socket.
  *
  * @author
  *   noblemaster (original implementation)
  */
class NetJavaSocketImpl(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints) extends Socket {

  /** Our socket or null for disposed, aka closed. */
  private var socket: JSocket = scala.compiletime.uninitialized

  try {
    // create the socket
    socket = new JSocket()
    applyHints(hints) // better to call BEFORE socket is connected!

    // and connect...
    val address = new InetSocketAddress(host, port)
    if (hints != null) {
      socket.connect(address, hints.connectTimeout)
    } else {
      socket.connect(address)
    }
  } catch {
    case e: Exception =>
      throw new RuntimeException(s"Error making a socket connection to $host:$port", e)
  }

  def this(socket: JSocket, hints: SocketHints) = {
    this(protocol = null, socket.getRemoteSocketAddress.toString, socket.getPort, hints)
    this.socket = socket
    applyHints(hints)
  }

  private def applyHints(hints: SocketHints): Unit =
    if (hints != null) {
      try {
        socket.setPerformancePreferences(hints.performancePrefConnectionTime, hints.performancePrefLatency, hints.performancePrefBandwidth)
        socket.setTrafficClass(hints.trafficClass)
        socket.setTcpNoDelay(hints.tcpNoDelay)
        socket.setKeepAlive(hints.keepAlive)
        socket.setSendBufferSize(hints.sendBufferSize)
        socket.setReceiveBufferSize(hints.receiveBufferSize)
        socket.setSoLinger(hints.linger, hints.lingerDuration)
        socket.setSoTimeout(hints.socketTimeout)
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error setting socket hints.", e)
      }
    }

  override def isConnected(): Boolean =
    if (socket != null) {
      socket.isConnected()
    } else {
      false
    }

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
    if (socket != null) {
      try {
        socket.close()
        socket = null
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error closing socket.", e)
      }
    }
}
