/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/NetJavaServerSocketImpl.java
 * Original authors: noblemaster
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `Disposable.dispose()` → `AutoCloseable.close()`
 *   Idiom: split packages
 *   Issues: secondary constructor double-initializes server socket (primary always calls `initializeServer`; Java 3-arg ctor delegates to 4-arg, so init runs once)
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.net.{ InetSocketAddress, ServerSocket => JServerSocket }
import sge.utils.Nullable

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
      val hintsOpt = Nullable(hints)
      hintsOpt.foreach { h =>
        server.setPerformancePreferences(h.performancePrefConnectionTime, h.performancePrefLatency, h.performancePrefBandwidth)
        server.setReuseAddress(h.reuseAddress)
        server.setSoTimeout(h.acceptTimeout)
        server.setReceiveBufferSize(h.receiveBufferSize)
      }

      // and bind the server...
      val address = hostname match {
        case Some(host) => new InetSocketAddress(host, port)
        case None       => new InetSocketAddress(port)
      }

      hintsOpt.fold(server.bind(address)) { h =>
        server.bind(address, h.backlog)
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
    Nullable(server).foreach { s =>
      try {
        s.close()
        server = null
      } catch {
        case e: Exception =>
          throw new RuntimeException("Error closing server.", e)
      }
    }
}
