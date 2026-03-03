/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/HttpParametersUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `final class` with static methods → `object`; `Map<String,String>` → `mutable.Map`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package net

import java.net.URLEncoder
import java.io.UnsupportedEncodingException
import scala.collection.mutable

/** Provides utility methods to work with the {@link HttpRequest} content and parameters. */
object HttpParametersUtils {

  var defaultEncoding:    String = "UTF-8"
  var nameValueSeparator: String = "="
  var parameterSeparator: String = "&"

  /** Useful method to convert a map of key,value pairs to a String to be used as part of a GET or POST content.
    * @param parameters
    *   A Map<String, String> with the parameters to encode.
    * @return
    *   The String with the parameters encoded.
    */
  def convertHttpParameters(parameters: mutable.Map[String, String]): String = {
    val keySet              = parameters.keySet
    val convertedParameters = new StringBuilder()
    for (name <- keySet) {
      convertedParameters.append(encode(name, defaultEncoding))
      convertedParameters.append(nameValueSeparator)
      convertedParameters.append(encode(parameters(name), defaultEncoding))
      convertedParameters.append(parameterSeparator)
    }
    if (convertedParameters.length > 0) convertedParameters.deleteCharAt(convertedParameters.length - 1)
    convertedParameters.toString()
  }

  private def encode(content: String, encoding: String): String =
    try
      URLEncoder.encode(content, encoding)
    catch {
      case e: UnsupportedEncodingException =>
        throw new IllegalArgumentException(e)
    }
}
