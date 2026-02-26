/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/JsonReader.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ InputStream, InputStreamReader, Reader }

import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import hearth.kindlings.jsoniterjson.Json
import hearth.kindlings.jsoniterjson.codec.JsonCodec.given

/** Lightweight JSON parser.
  *
  * Parses JSON text into a tree of [[JsonValue]] nodes. Uses jsoniter-scala + kindlings for parsing, then converts to the
  * LibGDX-compatible linked-list tree structure.
  */
class JsonReader extends BaseJsonReader {

  /** Parses the given JSON string into a JsonValue tree. */
  def parse(json: String): JsonValue = {
    val ast = readFromString[Json](json)
    JsonValue.fromJson(ast)
  }

  /** Parses JSON from a Reader. */
  def parse(reader: Reader): JsonValue = {
    val sb     = new StringBuilder(1024)
    val buffer = new Array[Char](1024)
    try {
      var len = reader.read(buffer)
      while (len != -1) {
        sb.appendAll(buffer, 0, len)
        len = reader.read(buffer)
      }
    } finally {
      try { reader.close() }
      catch { case _: Exception => () }
    }
    parse(sb.toString)
  }

  /** Parses JSON from an InputStream. */
  override def parse(input: InputStream): JsonValue =
    parse(new InputStreamReader(input, "UTF-8"))

  /** Parses JSON from a FileHandle. */
  override def parse(file: files.FileHandle): JsonValue =
    parse(file.reader())
}
