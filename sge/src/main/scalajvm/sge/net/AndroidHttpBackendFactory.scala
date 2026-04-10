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
import java.nio.charset.StandardCharsets
import scala.concurrent.{ ExecutionContext, Future }
import scala.jdk.CollectionConverters.*

/** Android-compatible HTTP backend that uses [[java.net.HttpURLConnection]] instead of sttp's `DefaultFutureBackend`. The sttp JDK
  * backend fails Android's DEX verifier because it references `java.net.http.HttpClient` APIs not available on Android. This
  * implementation uses the older `HttpURLConnection` API which is fully supported on Android.
  */
private[net] object AndroidHttpBackendFactory extends HttpBackendFactory {

  private given ExecutionContext = ExecutionContext.global

  override def send(request: SttpRequest[Either[String, String]]): Future[SttpResponse[Either[String, String]]] =
    Future {
      @SuppressWarnings(Array("all"))
      val url  = new URL(request.uri.toString()) // @nowarn - URL(String) deprecated in JDK 20+ but required for Android
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

        // Read response
        val statusCode = conn.getResponseCode
        val statusText = Option(conn.getResponseMessage).getOrElse("")
        val stream     = if (statusCode >= 400) conn.getErrorStream else conn.getInputStream

        val body: Either[String, String] =
          if (stream == null) {
            if (statusCode >= 400) Left("") else Right("")
          } else {
            try {
              val baos   = new ByteArrayOutputStream()
              val buffer = new Array[Byte](4096)
              var n      = stream.read(buffer)
              while (n >= 0) {
                baos.write(buffer, 0, n)
                n = stream.read(buffer)
              }
              val text = baos.toString(StandardCharsets.UTF_8.name())
              if (statusCode >= 400) Left(text) else Right(text)
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
