package sge
package net

/** A server socket that accepts new incoming connections, returning {@link Socket} instances. The {@link #accept(SocketHints)} method should preferably be called in a separate thread as it is
  * blocking.
  *
  * @author
  *   mzechner (original implementation)
  * @author
  *   noblemaster (original implementation)
  */
trait ServerSocket extends AutoCloseable {

  /** @return the Protocol used by this socket */
  def getProtocol(): Net.Protocol

  /** Accepts a new incoming connection from a client {@link Socket} . The given hints will be applied to the accepted socket. Blocking, call on a separate thread.
    *
    * @param hints
    *   additional {@link SocketHints} applied to the accepted {@link Socket} . Input null to use the default setting provided by the system.
    * @return
    *   the accepted {@link Socket}
    * @throws GdxRuntimeException
    *   in case an error occurred
    */
  def accept(hints: SocketHints): Socket
}
