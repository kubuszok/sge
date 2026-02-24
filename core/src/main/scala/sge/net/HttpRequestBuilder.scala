/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/HttpRequestBuilder.java
 * Original authors: Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.io.InputStream
import scala.collection.mutable

/** A builder for {@link HttpRequest} s.
  *
  * Make sure to call {@link #newRequest()} first, then set the request up and obtain it via {@link #build()} when you are done.
  *
  * It also offers a few utility methods to deal with content encoding and HTTP headers.
  *
  * @author
  *   Daniel Holderbaum (original implementation)
  */
class HttpRequestBuilder {

  private var httpRequest: Net.HttpRequest = scala.compiletime.uninitialized

  /** Initializes the builder and sets it up to build a new {@link HttpRequest} . */
  def newRequest(): HttpRequestBuilder = {
    if (httpRequest != null) {
      throw new IllegalStateException("A new request has already been started. Call HttpRequestBuilder.build() first.")
    }

    // TODO: Fix when proper instantiation method is available
    // httpRequest = new Net.HttpRequest()
    // httpRequest.setTimeOut(HttpRequestBuilder.defaultTimeout)
    this
  }

  /** @see HttpRequest#setMethod(String) */
  def method(httpMethod: String): HttpRequestBuilder = {
    validate()
    httpRequest.setMethod(httpMethod)
    this
  }

  /** The {@link #baseUrl} will automatically be added as a prefix to the given URL.
    *
    * @see
    *   HttpRequest#setUrl(String)
    */
  def url(url: String): HttpRequestBuilder = {
    validate()
    httpRequest.setUrl(HttpRequestBuilder.baseUrl + url)
    this
  }

  /** If this method is not called, the {@link #defaultTimeout} will be used.
    *
    * @see
    *   HttpRequest#setTimeOut(int)
    */
  def timeout(timeOut: Int): HttpRequestBuilder = {
    validate()
    httpRequest.setTimeOut(timeOut)
    this
  }

  /** @see HttpRequest#setFollowRedirects(boolean) */
  def followRedirects(followRedirects: Boolean): HttpRequestBuilder = {
    validate()
    httpRequest.setFollowRedirects(followRedirects)
    this
  }

  /** @see HttpRequest#setIncludeCredentials(boolean) */
  def includeCredentials(includeCredentials: Boolean): HttpRequestBuilder = {
    validate()
    httpRequest.setIncludeCredentials(includeCredentials)
    this
  }

  /** @see HttpRequest#setHeader(String, String) */
  def header(name: String, value: String): HttpRequestBuilder = {
    validate()
    httpRequest.setHeader(name, value)
    this
  }

  /** @see HttpRequest#setContent(String) */
  def content(content: String): HttpRequestBuilder = {
    validate()
    httpRequest.setContent(content)
    this
  }

  /** @see HttpRequest#setContent(java.io.InputStream, long) */
  def content(contentStream: InputStream, contentLength: Long): HttpRequestBuilder = {
    validate()
    httpRequest.setContent(contentStream, contentLength)
    this
  }

  /** Sets the correct {@code ContentType} and encodes the given parameter map, then sets it as the content. */
  def formEncodedContent(content: mutable.Map[String, String]): HttpRequestBuilder = {
    validate()
    httpRequest.setHeader(HttpRequestHeader.ContentType, "application/x-www-form-urlencoded")
    val formEncodedContent = HttpParametersUtils.convertHttpParameters(content)
    httpRequest.setContent(formEncodedContent)
    this
  }

  /** Sets the correct {@code ContentType} and encodes the given content object via {@link #json} , then sets it as the content.
    */
  def jsonContent(content: Any): HttpRequestBuilder = {
    validate()
    httpRequest.setHeader(HttpRequestHeader.ContentType, "application/json")
    // TODO: Implement proper JSON serialization when available
    val jsonContent = content.toString // Placeholder implementation
    httpRequest.setContent(jsonContent)
    this
  }

  /** Sets the {@code Authorization} header via the Base64 encoded username and password. */
  def basicAuthentication(username: String, password: String): HttpRequestBuilder = {
    validate()
    // TODO: Implement proper Base64 encoding when available
    httpRequest.setHeader(HttpRequestHeader.Authorization, "Basic " + java.util.Base64.getEncoder.encodeToString((username + ":" + password).getBytes))
    this
  }

  /** Returns the {@link HttpRequest} that has been setup by this builder so far. After using the request, it should be returned to the pool via {@code Pools.free(request)} .
    */
  def build(): Net.HttpRequest = {
    validate()
    val request = httpRequest
    httpRequest = null
    request
  }

  private def validate(): Unit =
    if (httpRequest == null) {
      throw new IllegalStateException("A new request has not been started yet. Call HttpRequestBuilder.newRequest() first.")
    }
}

object HttpRequestBuilder {

  /** Will be added as a prefix to each URL when {@link #url(String)} is called. Empty by default. */
  var baseUrl: String = ""

  /** Will be set for each new HttpRequest. By default set to {@code 1000}. Can be overwritten via {@link #timeout(int)}. */
  var defaultTimeout: Int = 1000

  /** Will be used for the object serialization in case {@link #jsonContent(Object)} is called. */
  // TODO: Add proper Json implementation when available
  // var json: sge.utils.Json = new sge.utils.Json()
}
