/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/Socket.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `interface extends Disposable` → `trait extends AutoCloseable`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.io.{ InputStream, OutputStream }

/** A client socket that talks to a server socket via some {@link Protocol} . See {@link Net#newClientSocket(Protocol, String, int, SocketHints)} and
  * {@link Net#newServerSocket(Protocol, int, ServerSocketHints)} . </p>
  *
  * A socket has an {@link InputStream} used to send data to the other end of the connection, and an {@link OutputStream} to receive data from the other end of the connection. </p>
  *
  * A socket needs to be disposed if it is no longer used. Disposing also closes the connection.
  *
  * @author
  *   mzechner (original implementation)
  */
trait Socket extends AutoCloseable {

  /** @return whether the socket is connected */
  def isConnected(): Boolean

  /** @return the {@link InputStream} used to read data from the other end of the connection. */
  def getInputStream(): InputStream

  /** @return the {@link OutputStream} used to write data to the other end of the connection. */
  def getOutputStream(): OutputStream

  /** @return the RemoteAddress of the Socket as String */
  def getRemoteAddress(): String
}
