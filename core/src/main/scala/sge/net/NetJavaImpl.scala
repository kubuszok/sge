/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/NetJavaImpl.java
 * Original authors: acoppes
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.net.{ HttpURLConnection, URL }
import java.io.{ InputStream, OutputStream, OutputStreamWriter }
import java.util.concurrent.{ Future, LinkedBlockingQueue, SynchronousQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit, atomic }
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import sge.utils.{ Nullable, StreamUtils }

import scala.language.implicitConversions

/** Implements part of the {@link Net} API using {@link HttpURLConnection} , to be easily reused between the Android and Desktop backends.
  * @author
  *   acoppes (original implementation)
  */
class NetJavaImpl(maxThreads: Int = Int.MaxValue) {

  class HttpClientResponse(connection: HttpURLConnection) extends Net.HttpResponse {
    private val status: HttpStatus =
      try
        new HttpStatus(connection.getResponseCode())
      catch {
        case _: java.io.IOException => new HttpStatus(-1)
      }

    override def getResult(): Array[Byte] = {
      val input = getInputStream()

      // If the response does not contain any content, input will be null.
      if (input == null) {
        return StreamUtils.EMPTY_BYTES
      }

      try
        StreamUtils.copyStreamToByteArray(input, connection.getContentLength())
      catch {
        case _: java.io.IOException => StreamUtils.EMPTY_BYTES
      } finally
        StreamUtils.closeQuietly(input)
    }

    override def getResultAsString(): String = {
      val input = getInputStream()

      // If the response does not contain any content, input will be null.
      if (input == null) {
        return ""
      }

      try
        StreamUtils.copyStreamToString(input, connection.getContentLength(), "UTF8")
      catch {
        case _: java.io.IOException => ""
      } finally
        StreamUtils.closeQuietly(input)
    }

    override def getResultAsStream(): InputStream = getInputStream()

    override def getStatus(): HttpStatus = status

    override def getHeader(name: String): String = connection.getHeaderField(name)

    override def getHeaders(): java.util.Map[String, java.util.List[String]] = connection.getHeaderFields()

    private def getInputStream(): InputStream =
      try
        connection.getInputStream()
      catch {
        case _: java.io.IOException => connection.getErrorStream()
      }
  }

  private val executorService: ThreadPoolExecutor = {
    val isCachedPool = maxThreads == Int.MaxValue
    val executor     = new ThreadPoolExecutor(
      if (isCachedPool) 0 else maxThreads,
      maxThreads,
      60L,
      TimeUnit.SECONDS,
      if (isCachedPool) new SynchronousQueue[Runnable]() else new LinkedBlockingQueue[Runnable](),
      new ThreadFactory() {
        val threadID = new AtomicInteger()

        override def newThread(r: Runnable): Thread = {
          val thread = new Thread(r, "NetThread" + threadID.getAndIncrement())
          thread.setDaemon(true)
          thread
        }
      }
    )
    executor.allowCoreThreadTimeOut(!isCachedPool)
    executor
  }

  private val connections = mutable.Map[Net.HttpRequest, HttpURLConnection]()
  private val listeners   = mutable.Map[Net.HttpRequest, Net.HttpResponseListener]()
  private val tasks       = mutable.Map[Net.HttpRequest, Future[?]]()

  def sendHttpRequest(httpRequest: Net.HttpRequest, httpResponseListener: Option[Net.HttpResponseListener]): Unit = scala.util.boundary {
    if (httpRequest.getUrl() == null) {
      httpResponseListener.foreach(_.failed(new RuntimeException("can't process a HTTP request without URL set")))
      scala.util.boundary.break()
    }

    try {
      val method = httpRequest.getMethod()
      val url: URL = {
        val doInput = !method.equalsIgnoreCase(Net.HttpMethods.HEAD)
        // should be enabled to upload data.
        val doingOutPut = method.equalsIgnoreCase(Net.HttpMethods.POST) ||
          method.equalsIgnoreCase(Net.HttpMethods.PUT) ||
          method.equalsIgnoreCase(Net.HttpMethods.PATCH)

        if (method.equalsIgnoreCase(Net.HttpMethods.GET) || method.equalsIgnoreCase(Net.HttpMethods.HEAD)) {
          var queryString = ""
          val value       = httpRequest.getContent()
          if (value != null && value != "") queryString = "?" + value
          java.net.URI(httpRequest.getUrl() + queryString).toURL()
        } else {
          java.net.URI(httpRequest.getUrl()).toURL()
        }
      }

      val connection  = url.openConnection().asInstanceOf[HttpURLConnection]
      val doInput     = !method.equalsIgnoreCase(Net.HttpMethods.HEAD)
      val doingOutPut = method.equalsIgnoreCase(Net.HttpMethods.POST) ||
        method.equalsIgnoreCase(Net.HttpMethods.PUT) ||
        method.equalsIgnoreCase(Net.HttpMethods.PATCH)

      connection.setDoOutput(doingOutPut)
      connection.setDoInput(doInput)
      connection.setRequestMethod(method)
      HttpURLConnection.setFollowRedirects(httpRequest.getFollowRedirects())

      putIntoConnectionsAndListeners(httpRequest, httpResponseListener.orNull, connection)

      // Headers get set regardless of the method
      for ((key, value) <- httpRequest.getHeaders())
        connection.addRequestProperty(key, value)

      // Set Timeouts
      connection.setConnectTimeout(httpRequest.getTimeOut())
      connection.setReadTimeout(httpRequest.getTimeOut())

      val task = executorService.submit(
        new Runnable() {
          override def run(): Unit =
            try {
              // Set the content for POST and PUT (GET has the information embedded in the URL)
              if (doingOutPut) {
                // we probably need to use the content as stream here instead of using it as a string.
                val contentAsString = httpRequest.getContent()
                if (contentAsString != null) {
                  val writer = new OutputStreamWriter(connection.getOutputStream(), "UTF8")
                  try
                    writer.write(contentAsString)
                  finally
                    StreamUtils.closeQuietly(writer)
                } else {
                  val contentAsStream = httpRequest.getContentStream()
                  if (contentAsStream != null) {
                    val os = connection.getOutputStream()
                    try
                      StreamUtils.copyStream(contentAsStream, os)
                    finally
                      StreamUtils.closeQuietly(os)
                  }
                }
              }

              connection.connect()

              val clientResponse = new HttpClientResponse(connection)
              try {
                val listener = getFromListeners(httpRequest)

                if (listener != null) {
                  listener.handleHttpResponse(clientResponse)
                }
              } finally {
                removeFromConnectionsAndListeners(httpRequest)
                connection.disconnect()
              }
            } catch {
              case e: Exception =>
                connection.disconnect()
                try
                  httpResponseListener.foreach(_.failed(e))
                finally
                  removeFromConnectionsAndListeners(httpRequest)
            }
        }
      )

      tasks.synchronized {
        tasks.put(httpRequest, task)
      }
    } catch {
      case e: Exception =>
        try
          httpResponseListener.foreach(_.failed(e))
        finally
          removeFromConnectionsAndListeners(httpRequest)
    }
  }

  def cancelHttpRequest(httpRequest: Net.HttpRequest): Unit = {
    val httpResponseListener = getFromListeners(httpRequest)

    if (httpResponseListener != null) {
      httpResponseListener.cancelled()
      cancelTask(httpRequest)
      removeFromConnectionsAndListeners(httpRequest)
    }
  }

  def isHttpRequestPending(httpRequest: Net.HttpRequest): Boolean =
    getFromListeners(httpRequest) != null

  private def cancelTask(httpRequest: Net.HttpRequest): Unit =
    tasks.synchronized {
      val task = tasks.get(httpRequest)
      task.foreach(_.cancel(false))
    }

  private def removeFromConnectionsAndListeners(httpRequest: Net.HttpRequest): Unit = {
    connections.synchronized {
      connections.remove(httpRequest)
    }
    listeners.synchronized {
      listeners.remove(httpRequest)
    }
    tasks.synchronized {
      tasks.remove(httpRequest)
    }
  }

  private def putIntoConnectionsAndListeners(httpRequest: Net.HttpRequest, httpResponseListener: Nullable[Net.HttpResponseListener], connection: HttpURLConnection): Unit = {
    connections.synchronized {
      connections.put(httpRequest, connection)
    }
    listeners.synchronized {
      httpResponseListener.foreach(listeners.put(httpRequest, _))
    }
  }

  private def getFromListeners(httpRequest: Net.HttpRequest): Net.HttpResponseListener =
    listeners.synchronized {
      listeners.get(httpRequest).orNull
    }
}
