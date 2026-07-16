/*
 * SGE Gauntlet — net: sge HTTP client against a local JDK loopback server (JVM-only).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.Net.{ HttpMethod, HttpResponse, HttpResponseListener }

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import scala.collection.mutable.ListBuffer

/** Spins a JDK loopback HTTP server and exercises the sge HTTP client (`Sge().net.httpClient`) against it: GET status+body and POST request-body echo. */
object NetHttpLoopbackProbe extends FeatureProbe {

  override def id: String = "net/http-loopback"

  override def area: String = "net"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  private final class RecordingListener(record: (String, Int) => Unit, done: CountDownLatch) extends HttpResponseListener {

    override def handleHttpResponse(httpResponse: HttpResponse): Unit = {
      record(httpResponse.resultAsString, httpResponse.status.statusCode)
      done.countDown()
    }

    override def failed(t: Throwable): Unit = {
      record(s"<failed: $t>", -1)
      done.countDown()
    }

    override def cancelled(): Unit = {
      record("<cancelled>", -2)
      done.countDown()
    }
  }

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext(
      "/hello",
      exchange => {
        val response = "hello from gauntlet"
        exchange.sendResponseHeaders(200, response.length.toLong)
        val os = exchange.getResponseBody
        os.write(response.getBytes("UTF-8"))
        os.close()
      }
    )
    server.createContext(
      "/echo",
      exchange => {
        val body     = new String(exchange.getRequestBody.readAllBytes(), "UTF-8")
        val response = s"echo:$body"
        exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length.toLong)
        val os = exchange.getResponseBody
        os.write(response.getBytes("UTF-8"))
        os.close()
      }
    )
    server.start()
    val port = server.getAddress.getPort
    ctx.log(s"loopback http server on port $port")

    try {
      val client = ctx.sgeCtx.net.httpClient

      var getBody   = ""
      var getStatus = 0
      val getDone   = new CountDownLatch(1)
      val getReq    = client.obtainRequest()
      getReq.withMethod(HttpMethod.GET).withUrl(s"http://127.0.0.1:$port/hello")
      client.send(
        getReq,
        lowlevel.Nullable(new RecordingListener((b, s) => { getBody = b; getStatus = s }, getDone))
      )
      val getInTime = getDone.await(10, TimeUnit.SECONDS)
      checks += Check.cond("get-completed", getInTime, "response within 10s", if (getInTime) "completed" else "timed out")
      checks += Check.eq("get-status", 200, getStatus)
      checks += Check.eq("get-body", "hello from gauntlet", getBody)

      var postBody   = ""
      var postStatus = 0
      val postDone   = new CountDownLatch(1)
      val postReq    = client.obtainRequest()
      postReq.withMethod(HttpMethod.POST).withUrl(s"http://127.0.0.1:$port/echo").withContent("ping-42")
      client.send(
        postReq,
        lowlevel.Nullable(new RecordingListener((b, s) => { postBody = b; postStatus = s }, postDone))
      )
      val postInTime = postDone.await(10, TimeUnit.SECONDS)
      checks += Check.cond("post-completed", postInTime, "response within 10s", if (postInTime) "completed" else "timed out")
      checks += Check.eq("post-status", 200, postStatus)
      checks += Check.eq("post-echo", "echo:ping-42", postBody)
    } finally server.stop(0)
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
