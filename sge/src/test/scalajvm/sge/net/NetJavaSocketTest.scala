package sge
package net

import scala.language.implicitConversions

class NetJavaSocketTest extends munit.FunSuite {

  private val TestPort = 19876

  test("server socket binds and accepts a connection") {
    val hints = new ServerSocketHints()
    hints.acceptTimeout = 3000 // 3 seconds
    val server = new NetJavaServerSocketImpl(Net.Protocol.TCP, TestPort, hints)
    try {
      assertEquals(server.protocol, Net.Protocol.TCP)

      // Connect a client
      val clientHints = new SocketHints()
      clientHints.connectTimeout = 3000
      val client = new NetJavaSocketImpl(Net.Protocol.TCP, "127.0.0.1", TestPort, clientHints)
      try {
        assert(client.isConnected, "client should be connected")
        assert(
          client.remoteAddress.contains("127.0.0.1"),
          s"remote address should contain 127.0.0.1, got ${client.remoteAddress}"
        )

        // Accept the connection on the server side
        val accepted = server.accept(new SocketHints())
        try
          assert(accepted.isConnected, "accepted socket should be connected")
        finally
          accepted.close()
      } finally
        client.close()
    } finally
      server.close()
  }

  test("server socket with hostname binds successfully") {
    val hints = new ServerSocketHints()
    hints.acceptTimeout = 1000
    val server = new NetJavaServerSocketImpl(Net.Protocol.TCP, "127.0.0.1", TestPort + 1, hints)
    try
      assertEquals(server.protocol, Net.Protocol.TCP)
    finally
      server.close()
  }

  test("client and server can exchange data") {
    val hints = new ServerSocketHints()
    hints.acceptTimeout = 3000
    val server = new NetJavaServerSocketImpl(Net.Protocol.TCP, TestPort + 2, hints)
    try {
      val clientHints = new SocketHints()
      clientHints.connectTimeout = 3000
      val client = new NetJavaSocketImpl(Net.Protocol.TCP, "127.0.0.1", TestPort + 2, clientHints)
      try {
        val accepted = server.accept(new SocketHints())
        try {
          // Send from client
          val message = "hello sge"
          client.outputStream.write(message.getBytes("UTF-8"))
          client.outputStream.flush()

          // Read on server side
          val buf  = new Array[Byte](message.length)
          var read = 0
          while (read < buf.length) {
            val n = accepted.inputStream.read(buf, read, buf.length - read)
            assert(n > 0, "expected to read data")
            read += n
          }
          assertEquals(new String(buf, "UTF-8"), message)
        } finally
          accepted.close()
      } finally
        client.close()
    } finally
      server.close()
  }

  test("wrapping an existing socket applies hints") {
    val hints = new ServerSocketHints()
    hints.acceptTimeout = 3000
    val server = new NetJavaServerSocketImpl(Net.Protocol.TCP, TestPort + 3, hints)
    try {
      // Create a raw java socket
      val rawSocket = new java.net.Socket("127.0.0.1", TestPort + 3)
      try {
        val socketHints = new SocketHints()
        socketHints.tcpNoDelay = true
        socketHints.keepAlive = true
        // Wrap the existing socket — this should not create a new socket
        val wrapped = new NetJavaSocketImpl(rawSocket, socketHints)
        try
          assert(wrapped.isConnected, "wrapped socket should be connected")
        finally
          wrapped.close()
      } finally
        if (!rawSocket.isClosed) rawSocket.close()
    } finally
      server.close()
  }

  test("server close makes socket unusable") {
    val hints = new ServerSocketHints()
    hints.acceptTimeout = 1000
    val server = new NetJavaServerSocketImpl(Net.Protocol.TCP, TestPort + 4, hints)
    server.close()
    // After close, accepting should throw
    intercept[RuntimeException] {
      server.accept(new SocketHints())
    }
  }
}
