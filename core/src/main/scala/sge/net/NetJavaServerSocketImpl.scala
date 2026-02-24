/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/NetJavaServerSocketImpl.java
 * Original authors: noblemaster
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.net.{ InetSocketAddress, ServerSocket => JServerSocket }

/** Server socket implementation using java.net.ServerSocket.
  *
  * @author
  *   noblemaster (original implementation)
  */
class NetJavaServerSocketImpl(val protocol: Net.Protocol, port: Int, hints: ServerSocketHints) extends ServerSocket {

  /** Our server or null for disposed, aka closed. */
  private var server: JServerSocket = scala.compiletime.uninitialized

  def this(protocol: Net.Protocol, hostname: String, port: Int, hints: ServerSocketHints) = {
    this(protocol, port, hints)
    initializeServer(Some(hostname), port, hints)
  }

  // Initialize in primary constructor
  initializeServer(None, port, hints)

  private def initializeServer(hostname: Option[String], port: Int, hints: ServerSocketHints): Unit =
    // create the server socket
    try {
      // initialize
      server = new JServerSocket()
      if (hints != null) {
        server.setPerformancePreferences(hints.performancePrefConnectionTime, hints.performancePrefLatency, hints.performancePrefBandwidth)
        server.setReuseAddress(hints.reuseAddress)
        server.setSoTimeout(hints.acceptTimeout)
        server.setReceiveBufferSize(hints.receiveBufferSize)
      }

      // and bind the server...
      val address = hostname match {
        case Some(host) => new InetSocketAddress(host, port)
        case None       => new InetSocketAddress(port)
      }

      if (hints != null) {
        server.bind(address, hints.backlog)
      } else {
        server.bind(address)
      }
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Cannot create a server socket at port $port.", e)
    }

  override def getProtocol(): Net.Protocol = protocol

  override def accept(hints: SocketHints): Socket =
    try
      new NetJavaSocketImpl(server.accept(), hints)
    catch {
      case e: Exception =>
        throw new RuntimeException("Error accepting socket.", e)
    }

  override def close(): Unit =
    if (server != null) {
      try {
        server.close()
        server = null
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error closing server.", e)
      }
    }
}
