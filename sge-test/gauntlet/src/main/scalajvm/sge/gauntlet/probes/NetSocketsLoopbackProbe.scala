/*
 * SGE Gauntlet — net: sge TCP server+client socket loopback echo (JVM-only).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.Net.Protocol
import sge.net.{ ServerSocketHints, SocketHints }

import java.io.{ BufferedReader, InputStreamReader, PrintWriter }
import java.util.concurrent.TimeUnit

import scala.collection.mutable.ListBuffer

/** Opens an sge TCP ServerSocket, connects an sge client Socket to it, and round-trips an echo line — both socket ends go through the sge Net API. Skips gracefully (capability check, still passing)
  * if the platform reports TCP sockets as unsupported.
  */
object NetSocketsLoopbackProbe extends FeatureProbe {

  override def id: String = "net/sockets-loopback"

  override def area: String = "net"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val net = ctx.sgeCtx.net

    // find a free port
    val portProbe = new java.net.ServerSocket(0)
    val port      = portProbe.getLocalPort
    portProbe.close()

    try {
      val serverHints = new ServerSocketHints()
      val server      = net.newServerSocket(Protocol.TCP, "127.0.0.1", port, serverHints)
      try {
        val accepted = new java.util.concurrent.CountDownLatch(1)
        @volatile var serverSideOk = false
        val serverThread = new Thread(
          () => {
            val socket = server.accept(new SocketHints())
            try {
              val in   = new BufferedReader(new InputStreamReader(socket.inputStream, "UTF-8"))
              val out  = new PrintWriter(socket.outputStream, true)
              val line = in.readLine()
              out.println(s"echo:$line")
              serverSideOk = line == "ping-gauntlet"
            } finally {
              socket.close()
              accepted.countDown()
            }
          },
          "gauntlet-socket-server"
        )
        serverThread.setDaemon(true)
        serverThread.start()

        val client = net.newClientSocket(Protocol.TCP, "127.0.0.1", port, new SocketHints())
        try {
          checks += Check.eq("client-connected", true, client.isConnected)
          val out = new PrintWriter(client.outputStream, true)
          val in  = new BufferedReader(new InputStreamReader(client.inputStream, "UTF-8"))
          out.println("ping-gauntlet")
          val reply    = in.readLine()
          val serverIn = accepted.await(10, TimeUnit.SECONDS)
          checks += Check.cond("server-accepted", serverIn, "accept within 10s", if (serverIn) "accepted" else "timed out")
          checks += Check.eq("server-received-line", true, serverSideOk)
          checks += Check.eq("echo-roundtrip", "echo:ping-gauntlet", reply)
        } finally client.close()
      } finally server.close()
    } catch {
      case e: UnsupportedOperationException =>
        // Platform without TCP socket support: capability-skip, not a failure.
        ctx.log(s"TCP sockets unsupported on this platform: ${e.getMessage}")
        checks += Check.cond("tcp-supported", passed = true, "TCP sockets or graceful unsupported signal", s"unsupported: ${e.getMessage}")
    }
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
