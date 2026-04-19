/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/StringUtils.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 333
 * Covenant-baseline-methods: StringUtils,appendUnsignedHex,begin,c,chars,data,decimal,end,floatFromDec,foundPoint,h,hexChar,hexCode,i,idx,indexAfter,intFromDec,intFromHex,isLowerCase,isUpperCase,items,join,len,length,lim,longFromDec,longFromHex,s,safeSubstring,shuffleWords,sign,unsignedHex,unsignedHexArray
 * Covenant-source-reference: com/github/tommyettinger/textra/utils/StringUtils.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra
package utils

import java.util.Random as JRandom

import scala.util.boundary
import scala.util.boundary.break

import sge.math.MathUtils

object StringUtils {

  def join(delimiter: CharSequence, items: CharSequence*): String =
    if (items == null || items.isEmpty) {
      ""
    } else {
      val sb = new StringBuilder
      sb.append(items.head)
      var i = 1
      while (i < items.length) {
        sb.append(delimiter).append(items(i))
        i += 1
      }
      sb.toString
    }

  /** Shuffles the words in text using a thread-local random, joins them with a space as the delimiter, and returns that String.
    *
    * @param text
    *   a String containing typically many whitespace-separated words
    * @return
    *   text with its words shuffled randomly
    */
  def shuffleWords(text: String): String =
    shuffleWords(text, MathUtils.randomGenerator)

  /** Shuffles the words in text using the given Random generator, joins them with a space as the delimiter, and returns that String. The generator can be seeded to get replicable results.
    *
    * @param text
    *   a String containing typically many whitespace-separated words
    * @param generator
    *   a java.util.Random used for shuffling
    * @return
    *   text with its words shuffled randomly
    */
  def shuffleWords(text: String, generator: JRandom): String = {
    val items  = text.split("\\s+")
    val length = items.length
    var i      = length - 1
    while (i > 0) {
      val ii   = generator.nextInt(i + 1)
      val temp = items(i)
      items(i) = items(ii)
      items(ii) = temp
      i -= 1
    }
    join(" ", items*)
  }

  /** An overly-permissive, but fast, way of looking up the numeric value of a hex digit provided as a char. */
  def hexCode(c: Char): Int = {
    val h = c & 64
    (c & 15) + (h >>> 3) + (h >>> 6)
  }

  @SuppressWarnings(Array("org.wartremover.warts.All"))
  def hexChar(number: Int): Char =
    (number + 48 + (9 - number >>> -3)).toChar

  @SuppressWarnings(Array("org.wartremover.warts.All"))
  def hexChar(number: Long): Char =
    (number + 48 + (9 - number >>> -3)).toChar

  def appendUnsignedHex(sb: StringBuilder, number: Int): StringBuilder = {
    var s = 28
    while (s >= 0) {
      sb.append(hexChar(number >>> s & 15))
      s -= 4
    }
    sb
  }

  def appendUnsignedHex(sb: StringBuilder, number: Long): StringBuilder = {
    var i = 60
    while (i >= 0) {
      sb.append(hexChar(number >>> i & 15))
      i -= 4
    }
    sb
  }

  /** Allocates a new 8-char array filled with the unsigned hex format of number, and returns it.
    *
    * @param number
    *   any int to write in hex format
    * @return
    *   a new char array holding the 8-character unsigned hex representation of number
    */
  def unsignedHexArray(number: Int): Array[Char] = {
    val chars = new Array[Char](8)
    var i     = 0
    var s     = 28
    while (i < 8) {
      chars(i) = hexChar(number >>> s & 15)
      i += 1
      s -= 4
    }
    chars
  }

  /** Allocates a new 16-char array filled with the unsigned hex format of number, and returns it.
    *
    * @param number
    *   any long to write in hex format
    * @return
    *   a new char array holding the 16-character unsigned hex representation of number
    */
  def unsignedHexArray(number: Long): Array[Char] = {
    val chars = new Array[Char](16)
    var i     = 0
    var s     = 60
    while (i < 16) {
      chars(i) = hexChar(number >>> s & 15)
      i += 1
      s -= 4
    }
    chars
  }

  def unsignedHex(number: Int): String = {
    val chars = new Array[Char](8)
    var i     = 0
    var s     = 28
    while (i < 8) {
      chars(i) = hexChar(number >>> s & 15)
      i += 1
      s -= 4
    }
    new String(chars)
  }

  def unsignedHex(number: Long): String = {
    val chars = new Array[Char](16)
    var i     = 0
    var s     = 60
    while (i < 16) {
      chars(i) = hexChar(number >>> s & 15)
      i += 1
      s -= 4
    }
    new String(chars)
  }

  def longFromDec(cs: CharSequence, start: Int, endIn: Int): Long = boundary {
    var end = endIn
    if (cs == null || start < 0 || end <= 0) break(0L)
    end = Math.min(end, cs.length())
    if (end - start <= 0) break(0L)
    var c    = cs.charAt(start)
    var sign = 1
    var h    = 0
    var lim  = 20
    if (c == '-') {
      sign = -1; h = 0; lim = 21
    } else if (c == '+') {
      sign = 1; h = 0; lim = 21
    } else if (c < '0' || c > '9') {
      break(0L)
    } else {
      lim = 20; h = c - '0'
    }
    var data = h.toLong
    var i    = start + 1
    while (i < end && i < start + lim) {
      c = cs.charAt(i)
      if (c < '0' || c > '9') break(data * sign)
      data = data * 10 + (c - '0')
      i += 1
    }
    data * sign
  }

  def intFromDec(cs: CharSequence, start: Int, endIn: Int): Int = boundary {
    var end = endIn
    if (cs == null || start < 0 || end <= 0) break(0)
    end = Math.min(end, cs.length())
    if (end - start <= 0) break(0)
    var c    = cs.charAt(start)
    var sign = 1
    var h    = 0
    var lim  = 10
    if (c == '-') {
      sign = -1; h = 0; lim = 11
    } else if (c == '+') {
      sign = 1; h = 0; lim = 11
    } else if (c < '0' || c > '9') {
      break(0)
    } else {
      lim = 10; h = c - '0'
    }
    var data = h
    var i    = start + 1
    while (i < end && i < start + lim) {
      c = cs.charAt(i)
      if (c < '0' || c > '9') break(data * sign)
      data = data * 10 + (c - '0')
      i += 1
    }
    data * sign
  }

  def intFromHex(cs: CharSequence, start: Int, endIn: Int): Int = boundary {
    var end = endIn
    if (cs == null || start < 0 || end <= 0) break(0)
    end = Math.min(end, cs.length())
    if (end - start <= 0) break(0)
    var c    = cs.charAt(start)
    var sign = 1
    var h    = 0
    var lim  = 8
    if (c == '-') {
      sign = -1; h = 0; lim = 9
    } else if (c == '+') {
      sign = 1; h = 0; lim = 9
    } else if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
      break(0)
    } else {
      lim = 8; h = hexCode(c)
    }
    var data = h
    var i    = start + 1
    while (i < end && i < start + lim) {
      c = cs.charAt(i)
      if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
        break(data * sign)
      }
      data = (data << 4) | hexCode(c)
      i += 1
    }
    data * sign
  }

  def longFromHex(cs: CharSequence, start: Int, endIn: Int): Long = boundary {
    var end = endIn
    if (cs == null || start < 0 || end <= 0) break(0L)
    end = Math.min(end, cs.length())
    if (end - start <= 0) break(0L)
    var c    = cs.charAt(start)
    var sign = 1
    var h    = 0
    var lim  = 16
    if (c == '-') {
      sign = -1; h = 0; lim = 17
    } else if (c == '+') {
      sign = 1; h = 0; lim = 17
    } else if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
      break(0L)
    } else {
      lim = 16; h = hexCode(c)
    }
    var data = h.toLong
    var i    = start + 1
    while (i < end && i < start + lim) {
      c = cs.charAt(i)
      if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
        break(data * sign)
      }
      data = (data << 4) | hexCode(c)
      i += 1
    }
    data * sign
  }

  def floatFromDec(cs: CharSequence, start: Int, endIn: Int): Float = boundary {
    if (cs == null || start < 0 || endIn <= 0 || endIn - start <= 0) break(0f)
    val len = cs.length()
    if (len - start <= 0 || endIn > len) break(0f)
    val end        = endIn
    var decimal    = 1f
    var foundPoint = false
    var c          = cs.charAt(start)
    var sign       = 1
    var h          = 0
    if (c == '-') {
      sign = -1; h = 0
    } else if (c == '+') {
      sign = 1; h = 0
    } else if (c < '0' || c > '9') {
      break(0f)
    } else {
      h = hexCode(c)
    }
    var data = h
    var i    = start + 1
    while (i < end) {
      c = cs.charAt(i)
      if (c == '.') { foundPoint = true; i += 1 }
      else {
        if (c < '0' || c > '9') break(data * sign / decimal)
        h = hexCode(c)
        if (foundPoint) decimal *= 10f
        data = data * 10 + h
        i += 1
      }
    }
    data * sign / decimal
  }

  /** Returns the next index just after the end of `search` starting at `from` in `text`. */
  def indexAfter(text: String, search: String, from: Int): Int = {
    val idx = text.indexOf(search, from)
    if (idx < 0) text.length else idx + search.length
  }

  /** Like String.substring but returns "" instead of throwing. */
  def safeSubstring(source: String, beginIndex: Int, endIndex: Int): String = {
    if (source == null || source.isEmpty) return ""
    val begin = if (beginIndex < 0) 0 else beginIndex
    val end   = if (endIndex < 0 || endIndex > source.length) source.length else endIndex
    if (begin >= end) "" else source.substring(begin, end)
  }

  def isLowerCase(c: Char): Boolean = Character.isLowerCase(c)
  def isUpperCase(c: Char): Boolean = Character.isUpperCase(c)
}
