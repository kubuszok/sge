// SGE — Desktop integration test: HTTP networking check
//
// Starts a local HTTP server on a random port, makes a request using
// java.net.http.HttpClient, and verifies the round-trip response.

package sge.it.desktop.checks

import sge.Sge
import sge.it.desktop.CheckResult

/** Verifies HTTP client/server round-trip on the JVM. */
object HttpCheck {

  def run()(using Sge): CheckResult =
    try {
      // Start a tiny HTTP server on a random port
      val server = com.sun.net.httpserver.HttpServer.create(
        new java.net.InetSocketAddress(0),
        0
      )
      server.createContext(
        "/test",
        exchange => {
          val response = "hello from sge"
          exchange.sendResponseHeaders(200, response.length.toLong)
          val os = exchange.getResponseBody
          os.write(response.getBytes)
          os.close()
        }
      )
      server.start()
      val port = server.getAddress.getPort

      // Make HTTP request using java.net.http.HttpClient
      val client   = java.net.http.HttpClient.newHttpClient()
      val request  = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(s"http://localhost:$port/test")).build()
      val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())

      server.stop(0)

      val ok = response.statusCode() == 200 && response.body() == "hello from sge"
      CheckResult("http_request", ok, if (ok) "HTTP roundtrip OK" else s"status=${response.statusCode()} body=${response.body()}")
    } catch {
      case e: Exception =>
        CheckResult("http_request", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
