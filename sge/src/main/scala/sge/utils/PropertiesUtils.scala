/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/PropertiesUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Character.isSpace replaced with Character.isWhitespace (no GWT constraint)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ BufferedReader, Reader, Writer }
import java.util.Date

/** Helper that allows loading and storing key/value pairs of an [[ObjectMap]] with the same line-oriented syntax supported by `java.util.Properties`.
  */
object PropertiesUtils {

  final private val NONE     = 0
  final private val SLASH    = 1
  final private val UNICODE  = 2
  final private val CONTINUE = 3
  final private val KEY_DONE = 4
  final private val IGNORE   = 5

  final private val LINE_SEPARATOR = "\n"

  /** Adds to the specified [[ObjectMap]] the key/value pairs loaded from the [[Reader]] in a simple line-oriented format compatible with `java.util.Properties`.
    *
    * The input stream remains open after this method returns.
    *
    * @param properties
    *   the map to be filled.
    * @param reader
    *   the input character stream reader.
    * @throws java.io.IOException
    *   if an error occurred when reading from the input stream.
    * @throws IllegalArgumentException
    *   if a malformed Unicode escape appears in the input.
    */
  def load(properties: ObjectMap[String, String], reader: Reader): Unit = {
    var mode      = NONE
    var unicode   = 0
    var count     = 0
    var buf       = new Array[Char](40)
    var offset    = 0
    var keyLength = -1
    var firstChar = true

    val br = BufferedReader(reader)

    var running = true
    while (running) {
      scala.util.boundary {
        val intVal = br.read()
        if (intVal == -1) {
          running = false
          scala.util.boundary.break(())
        }
        var nextChar = intVal.toChar

        if (offset == buf.length) {
          val newBuf = new Array[Char](buf.length * 2)
          System.arraycopy(buf, 0, newBuf, 0, offset)
          buf = newBuf
        }
        if (mode == UNICODE) {
          val digit = Character.digit(nextChar, 16)
          if (digit >= 0) {
            unicode = (unicode << 4) + digit
            count += 1
            if (count < 4) {
              scala.util.boundary.break(())
            }
          } else if (count <= 4) {
            throw IllegalArgumentException("Invalid Unicode sequence: illegal character")
          }
          mode = NONE
          buf(offset) = unicode.toChar
          offset += 1
          if (nextChar != '\n') {
            scala.util.boundary.break(())
          }
        }
        if (mode == SLASH) {
          mode = NONE
          (nextChar: @annotation.switch) match {
            case '\r' =>
              mode = CONTINUE
              scala.util.boundary.break(())
            case '\n' =>
              mode = IGNORE
              scala.util.boundary.break(())
            case 'b' => nextChar = '\b'
            case 'f' => nextChar = '\f'
            case 'n' => nextChar = '\n'
            case 'r' => nextChar = '\r'
            case 't' => nextChar = '\t'
            case 'u' =>
              mode = UNICODE
              unicode = 0
              count = 0
              scala.util.boundary.break(())
            case _ => ()
          }
        } else {
          (nextChar: @annotation.switch) match {
            case '#' | '!' =>
              if (firstChar) {
                var done = false
                while (!done) {
                  val iv = br.read()
                  if (iv == -1) {
                    done = true
                  } else {
                    val nc = iv.toChar
                    if (nc == '\r' || nc == '\n') done = true
                  }
                }
                scala.util.boundary.break(())
              }
            case '\n' =>
              if (mode == CONTINUE) {
                mode = IGNORE
                scala.util.boundary.break(())
              }
              // fall through to newline/cr handling below
              mode = NONE
              firstChar = true
              if (offset > 0 || (offset == 0 && keyLength == 0)) {
                if (keyLength == -1) keyLength = offset
                val temp = new String(buf, 0, offset)
                properties.put(temp.substring(0, keyLength), temp.substring(keyLength))
              }
              keyLength = -1
              offset = 0
              scala.util.boundary.break(())
            case '\r' =>
              mode = NONE
              firstChar = true
              if (offset > 0 || (offset == 0 && keyLength == 0)) {
                if (keyLength == -1) keyLength = offset
                val temp = new String(buf, 0, offset)
                properties.put(temp.substring(0, keyLength), temp.substring(keyLength))
              }
              keyLength = -1
              offset = 0
              scala.util.boundary.break(())
            case '\\' =>
              if (mode == KEY_DONE) keyLength = offset
              mode = SLASH
              scala.util.boundary.break(())
            case ':' | '=' =>
              if (keyLength == -1) {
                mode = NONE
                keyLength = offset
                scala.util.boundary.break(())
              }
            case _ => ()
          }
          if (Character.isWhitespace(nextChar)) {
            if (mode == CONTINUE) mode = IGNORE
            if (offset == 0 || offset == keyLength || mode == IGNORE) {
              scala.util.boundary.break(())
            }
            if (keyLength == -1) {
              mode = KEY_DONE
              scala.util.boundary.break(())
            }
          }
          if (mode == IGNORE || mode == CONTINUE) mode = NONE
        }
        firstChar = false
        if (mode == KEY_DONE) {
          keyLength = offset
          mode = NONE
        }
        buf(offset) = nextChar
        offset += 1
      }
    }
    // Post-loop finalization: handle last line without trailing newline
    if (mode == UNICODE && count <= 4) {
      throw IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx")
    }
    if (keyLength == -1 && offset > 0) keyLength = offset
    if (keyLength >= 0) {
      val temp  = new String(buf, 0, offset)
      val key   = temp.substring(0, keyLength)
      var value = temp.substring(keyLength)
      if (mode == SLASH) value += "\u0000"
      properties.put(key, value)
    }
  }

  /** Writes the key/value pairs of the specified [[ObjectMap]] to the output character stream in a simple line-oriented format compatible with `java.util.Properties`.
    *
    * @param properties
    *   the map.
    * @param writer
    *   an output character stream writer.
    * @param comment
    *   an optional comment to be written, or null.
    */
  def store(properties: ObjectMap[String, String], writer: Writer, comment: String): Unit =
    storeImpl(properties, writer, comment, escapeUnicode = false)

  private def storeImpl(
    properties:    ObjectMap[String, String],
    writer:        Writer,
    comment:       String,
    escapeUnicode: Boolean
  ): Unit = {
    if (comment != null) { // @nowarn - Java interop, comment may be null from Java callers
      writeComment(writer, comment)
    }
    writer.write("#")
    writer.write(new Date().toString)
    writer.write(LINE_SEPARATOR)

    val sb = new StringBuilder(200)
    properties.foreachEntry { (key, value) =>
      dumpString(sb, key, escapeSpace = true, escapeUnicode)
      sb.append('=')
      dumpString(sb, value, escapeSpace = false, escapeUnicode)
      writer.write(LINE_SEPARATOR)
      writer.write(sb.toString())
      sb.setLength(0)
    }
    writer.flush()
  }

  private def dumpString(outBuffer: StringBuilder, string: String, escapeSpace: Boolean, escapeUnicode: Boolean): Unit = {
    val len = string.length
    var i   = 0
    while (i < len) {
      val ch = string.charAt(i)
      // Handle common case first
      if (ch > 61 && ch < 127) {
        if (ch == '\\') outBuffer.append("\\\\")
        else outBuffer.append(ch)
      } else {
        ch match {
          case ' ' =>
            if (i == 0 || escapeSpace) outBuffer.append("\\ ")
            else outBuffer.append(ch)
          case '\n'                  => outBuffer.append("\\n")
          case '\r'                  => outBuffer.append("\\r")
          case '\t'                  => outBuffer.append("\\t")
          case '\f'                  => outBuffer.append("\\f")
          case '=' | ':' | '#' | '!' =>
            outBuffer.append('\\').append(ch)
          case _ =>
            if ((ch < 0x0020 || ch > 0x007e) && escapeUnicode) {
              val hex = Integer.toHexString(ch.toInt)
              outBuffer.append("\\u")
              var j = 0
              while (j < 4 - hex.length) {
                outBuffer.append('0')
                j += 1
              }
              outBuffer.append(hex)
            } else {
              outBuffer.append(ch)
            }
        }
      }
      i += 1
    }
  }

  private def writeComment(writer: Writer, comment: String): Unit = {
    writer.write("#")
    val len       = comment.length
    var curIndex  = 0
    var lastIndex = 0
    while (curIndex < len) {
      val c = comment.charAt(curIndex)
      if (c > '\u00ff' || c == '\n' || c == '\r') {
        if (lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex))
        if (c > '\u00ff') {
          val hex = Integer.toHexString(c.toInt)
          writer.write("\\u")
          var j = 0
          while (j < 4 - hex.length) {
            writer.write("0")
            j += 1
          }
          writer.write(hex)
        } else {
          writer.write(LINE_SEPARATOR)
          if (c == '\r' && curIndex != len - 1 && comment.charAt(curIndex + 1) == '\n') {
            curIndex += 1
          }
          if (curIndex == len - 1 || (comment.charAt(curIndex + 1) != '#' && comment.charAt(curIndex + 1) != '!')) {
            writer.write("#")
          }
        }
        lastIndex = curIndex + 1
      }
      curIndex += 1
    }
    if (lastIndex != curIndex) writer.write(comment.substring(lastIndex, curIndex))
    writer.write(LINE_SEPARATOR)
  }
}
