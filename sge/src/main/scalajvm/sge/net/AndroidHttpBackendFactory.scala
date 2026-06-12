/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (Android HTTP backend via java.net.HttpURLConnection)
 *   Convention: platform-specific impl in scalajvm/ alongside AndroidNet
 *   Idiom: split packages
 */
package sge
package net

import java.io.{ ByteArrayOutputStream, OutputStream }
import java.net.{ HttpURLConnection, URL }
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters.*

/** Android-compatible HTTP backend that uses [[java.net.HttpURLConnection]] instead of sttp's `DefaultFutureBackend`. The sttp JDK backend fails Android's DEX verifier because it references
  * `java.net.http.HttpClient` APIs not available on Android. This implementation uses the older `HttpURLConnection` API which is fully supported on Android.
  */
private[net] object AndroidHttpBackendFactory extends HttpBackendFactory {

  private given ExecutionContext = ExecutionContext.global

  override def send(request: SttpRequest[Array[Byte]]): Future[SttpResponse[Array[Byte]]] =
    Future {
      @scala.annotation.nowarn("msg=deprecated") // URL(String) deprecated in JDK 20+ but required for Android
      val url  = new URL(request.uri.toString())
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]
      try {
        conn.setRequestMethod(request.method.method)
        conn.setInstanceFollowRedirects(true)
        conn.setConnectTimeout(30000)
        conn.setReadTimeout(30000)

        // Set headers
        for (header <- request.headers)
          conn.setRequestProperty(header.name, header.value)

        // Set body
        request.body match {
          case b: sttp.client4.StringBody =>
            conn.setDoOutput(true)
            val os: OutputStream = conn.getOutputStream
            try os.write(b.s.getBytes(b.encoding))
            finally os.close()
          case b: sttp.client4.ByteArrayBody =>
            conn.setDoOutput(true)
            val os: OutputStream = conn.getOutputStream
            try os.write(b.b)
            finally os.close()
          case _ => ()
        }

        // Read response — raw bytes on both success and error paths, mirroring
        // NetJavaImpl.getResult() copying the connection InputStream straight into
        // a byte[] with no charset (NetJavaImpl.java:62-78). Decoding to a String
        // here would corrupt binary downloads (ISS-521).
        val statusCode = conn.getResponseCode
        val statusText = Option(conn.getResponseMessage).getOrElse("")
        val stream     = if (statusCode >= 400) conn.getErrorStream else conn.getInputStream

        val body: Array[Byte] =
          if (stream == null) {
            Array.emptyByteArray
          } else {
            try {
              val baos   = new ByteArrayOutputStream()
              val buffer = new Array[Byte](4096)
              var n      = stream.read(buffer)
              while (n >= 0) {
                baos.write(buffer, 0, n)
                n = stream.read(buffer)
              }
              baos.toByteArray
            } finally stream.close()
          }

        // Collect response headers
        val responseHeaders = {
          val builder = Vector.newBuilder[SttpHeader]
          val fields  = conn.getHeaderFields
          if (fields != null) {
            fields.asScala.foreach { case (name, values) =>
              if (name != null) { // null key = status line
                values.asScala.foreach { value =>
                  builder += SttpHeader(name, value)
                }
              }
            }
          }
          builder.result()
        }

        SttpResponse(
          body = body,
          code = SttpStatusCode(statusCode),
          statusText = statusText,
          headers = responseHeaders,
          history = Nil,
          request = sttp.model.RequestMetadata(request.method, request.uri, request.headers)
        )
      } finally conn.disconnect()
    }

  override def close(): Unit = ()
}
