/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import hearth.kindlings.jsoniterjson.{ JsonNumber, JsonObject }

/** ISS-515 bounce red suite: pins [[LenientJson]] to libGDX `JsonReader`'s exact lenient grammar.
  *
  * Authority: `original-src/libgdx/gdx/res/com/badlogic/gdx/utils/JsonReader.rl` (the Ragel grammar) and the generated `original-src/libgdx/gdx/src/com/badlogic/gdx/utils/JsonReader.java`. Every
  * expected value below was additionally cross-checked empirically against the real `com.badlogic.gdx.utils.JsonReader` (gdx 1.13.1) on the JVM.
  *
  * Grammar rules cited throughout (JsonReader.rl):
  * {{{
  * 301  comment     = ('//' | '/' '*') @comment;   <- block-comment opener written split: Scala comments nest
  * 302  ws          = [\r\n\t ] | comment;
  * 303  ws2         = [\t ] | comment;
  * 304  comma       = ',' | ([\r\n] ws* ','?);
  * 306  nameString  = quotedString | ^[":,}/\r\n\t ] >unquotedChars %string;
  * 307  valueString = quotedString | ^[":,{[\]/\r\n\t ] >unquotedChars %string;
  * 309  nameValue   = nameString >name ws* ':' ws* value;
  * 310  object      := ws* nameValue? ws2* <: (comma ws* nameValue ws2*)** :>> (','? ws* '}' @endObject);
  * 311  array       := ws* value? ws2* <: (comma ws* value ws2*)** :>> (','? ws* ']' @endArray);
  * }}}
  *
  * Control characters under test are built with `11.toChar` (VT) and `12.toChar` (FF) so the difference stays visible in review — never as raw bytes hidden inside string literals.
  */
class LenientJsonGrammarRedSuite extends munit.FunSuite {

  private def obj(fields: (String, Json)*): Json = Json.Obj(JsonObject(fields.toVector))
  private def arr(values: Json*):           Json = Json.Arr(values.toVector)
  private def num(value:  Long):            Json = Json.Num(JsonNumber.fromLong(value))

  /** 0x0B vertical tab / 0x0C form feed — built programmatically so no raw control bytes hide in literals. */
  private val VT = 11.toChar.toString
  private val FF = 12.toChar.toString

  // ---------------------------------------------------------------------------------------------
  // (a) '=' is NOT a name separator
  // ---------------------------------------------------------------------------------------------

  test("(a) '=' is an ordinary name character, not a name/value separator") {
    // JsonReader.rl line 309: nameValue = nameString >name ws* ':' ws* value — only ':' separates.
    // JsonReader.rl lines 243-246 (action unquotedChars, name mode): an unquoted name terminates
    // only at ':', '\r', '\n' (or comment start) — '=' is NOT a terminator.
    // Real JsonReader: {a=b: 1} -> object with single member name "a=b", long value 1.
    assertEquals(LenientJson.parse("{a=b: 1}"), obj("a=b" -> num(1)))
  }

  // ---------------------------------------------------------------------------------------------
  // (b) member separator: a literal ',' OR a newline ([\r\n] ws* ','?) — nothing else
  // ---------------------------------------------------------------------------------------------

  test("(b) two members on one line without comma or newline are rejected") {
    // JsonReader.rl line 304: comma = ',' | ([\r\n] ws* ','?) — the only member separators.
    // JsonReader.rl line 310: object members after the first must be preceded by `comma`.
    // Quoted (or object) values make the missing separator observable — an unquoted value would
    // swallow the rest of the line as string content; see the positive pin below.
    // Real JsonReader: {a: "1" b: "2"} -> SerializationException ("Error parsing JSON on line 1").
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("{a: \"1\" b: \"2\"}")
    }
    // Real JsonReader: {a: {x: 1} b: 2} -> SerializationException — the rejection is about the
    // separator, not about quoting: after the closed object value, ws2* ([\t ]) then `comma` is
    // required before the next nameValue, and a bare 'b' is neither.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("{a: {x: 1} b: 2}")
    }
  }

  test("(b) leading and doubled commas are rejected") {
    // JsonReader.rl line 310: object := ws* nameValue? ws2* <: (comma ws* nameValue ws2*)** :>> (','? ws* '}')
    // A comma may only appear BETWEEN members (each comma must be followed by a nameValue) plus at
    // most one optional trailing ','. A leading ',' or ',,' has no nameValue to attach to.
    // Real JsonReader: {,,a:1,,b:2,,} -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("{,,a:1,,b:2,,}")
    }
  }

  test("(b) positive pin: newline-separated members without commas are accepted") {
    // JsonReader.rl line 304: comma = ',' | ([\r\n] ws* ','?) — a newline IS a member separator.
    assertEquals(LenientJson.parse("{a: 1\nb: 2}"), obj("a" -> num(1), "b" -> num(2)))
  }

  test("(b) positive pin: a single trailing comma is accepted") {
    // JsonReader.rl line 310: the object close is (','? ws* '}') — exactly one optional trailing ','.
    assertEquals(LenientJson.parse("{a: 1,}"), obj("a" -> num(1)))
    assertEquals(LenientJson.parse("{a: 1, b: 2,}"), obj("a" -> num(1), "b" -> num(2)))
  }

  test("(b) positive pin: with UNQUOTED values the rest of the line is string content, not a second member") {
    // JsonReader.rl lines 264-269 (action unquotedChars, value mode): an unquoted value terminates
    // only at '}', ']', ',', '\r', '\n' (or comment start) — a space does not end it. So the
    // finding's literal example {a: 1 b: 2} is NOT a separator error: libGDX parses it as a single
    // member "a" with string value "1 b: 2" (verified against gdx 1.13.1), and so must the port.
    assertEquals(LenientJson.parse("{a: 1 b: 2}"), obj("a" -> Json.Str("1 b: 2")))
  }

  // ---------------------------------------------------------------------------------------------
  // (c) whitespace classes: ws is exactly [\r\n\t ]; trailing trim is exactly Character.isSpace
  // ---------------------------------------------------------------------------------------------

  test("(c) vertical tab 0x0B between tokens is not whitespace — it starts the unquoted name") {
    // JsonReader.rl line 302: ws = [\r\n\t ] | comment — no 0x0B (VT).
    // JsonReader.rl line 306: nameString start class ^[":,}/\r\n\t ] does not exclude VT, so a VT
    // after '{' begins an unquoted name; the name scanner (lines 243-246) runs to ':' and only
    // TRAILING whitespace is trimmed, so the VT stays at the front of the name.
    // Real JsonReader: {<VT>a:1} -> object with single member name <VT>+"a", long 1.
    assertEquals(LenientJson.parse("{" + VT + "a:1}"), obj((VT + "a") -> num(1)))
  }

  test("(c) trailing trim uses Character.isSpace: vertical tab 0x0B is NOT trimmed from an unquoted value") {
    // JsonReader.rl line 277 (action unquotedChars): `while (Character.isSpace(data[p])) p--`.
    // Java's Character.isSpace recognizes exactly '\t', '\n', '\f', '\r', ' ' — NOT 0x0B (VT),
    // so the VT stays part of the value.
    // Real JsonReader: [a<VT>] -> array with single string "a"+<VT>.
    assertEquals(LenientJson.parse("[a" + VT + "]"), arr(Json.Str("a" + VT)))
  }

  test("(c) form feed 0x0C trailing an unquoted value is rejected") {
    // JsonReader.rl line 277: Character.isSpace('\f') is true, so the trailing form feed is trimmed
    // out of the token — but ws (line 302) does not contain '\f', so the state machine has no
    // transition that consumes it before ']' (line 311) and parsing fails.
    // Real JsonReader: [a<FF>] -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("[a" + FF + "]")
    }
  }

  // ---------------------------------------------------------------------------------------------
  // (d) token start classes
  // ---------------------------------------------------------------------------------------------

  test("(d) an unquoted value cannot start with ':'") {
    // JsonReader.rl line 307: valueString = quotedString | ^[":,{[\]/\r\n\t ] >unquotedChars — the
    // start class EXCLUDES ':'. (Only the FIRST character is restricted; later ':' chars are fine,
    // as pinned by the "1 b: 2" positive test above.) Inside an array, ':' is neither a value start
    // nor ']' nor a separator, so the machine errors.
    // Real JsonReader: [:a] -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("[:a]")
    }
  }

  // ---------------------------------------------------------------------------------------------
  // (e) unquoted-value typing classifies the RAW characters, before unescaping
  // ---------------------------------------------------------------------------------------------

  test("(e) escaped digits stay a string: typing runs over the raw chars, the value is unescaped") {
    // JsonReader.rl lines 112-113 (action string): the VALUE is unescaped — but the typing loop
    // (lines 136-163; generated JsonReader.java line 222: `for (int i = s; i < p; i++) switch (data[i])`)
    // walks the RAW buffer. The raw token 12 (backslashes literal) contains '\' and 'u',
    // so couldBeDouble and couldBeLong are both false and the member is a STRING whose unescaped
    // text is "12" (unescape: generated JsonReader.java lines 732-774).
    // Real JsonReader: {a: 12} (12 raw chars) -> string "12".
    assertEquals(LenientJson.parse("{a: \\u0031\\u0032}"), obj("a" -> Json.Str("12")))
    assertEquals(LenientJson.parse("[\\u0031\\u0032]"), arr(Json.Str("12")))
  }

  // ---------------------------------------------------------------------------------------------
  // (f) non-finite numbers — documented-convention pin (NOT a grammar deviation test)
  // ---------------------------------------------------------------------------------------------

  test("(f) documented convention: non-finite doubles parse as strings preserving the raw text") {
    // JsonReader.rl lines 164-170 (action string): '1e999' is couldBeDouble and
    // Double.parseDouble("1e999") yields Infinity WITHOUT a NumberFormatException, so the real
    // JsonReader produces a double value Infinity (verified against gdx 1.13.1).
    // The kindlings AST cannot represent that: JsonNumber.fromDouble returns None for NaN/Infinity
    // (JsonNumber is a JSON-grammar number literal, and JSON has no non-finite literals). The
    // faithful libGDX result is therefore unachievable through LenientJson's Json-typed API.
    // DOCUMENTED CONVENTION pinned here (green now, must stay green): non-finite numeric tokens
    // fall back to Json.Str of the raw token text — mirroring libGDX's retention of the raw text
    // in JsonValue.stringValue alongside the double.
    assertEquals(LenientJson.parse("[1e999]"), arr(Json.Str("1e999")))
    assertEquals(LenientJson.parse("[-1e999]"), arr(Json.Str("-1e999")))
  }

  // ---------------------------------------------------------------------------------------------
  // (g) empty input
  // ---------------------------------------------------------------------------------------------

  test("(g) empty and whitespace-only input yield Json.Null, not an error") {
    // Generated JsonReader.java lines 551-568: after the machine runs, errors are raised only when
    // p < pe, when elements remain, or when a RuntimeException was captured. For empty input
    // p == pe == 0 and the machine never executes, so parse("") returns the null root (line 570)
    // WITHOUT throwing — verified against gdx 1.13.1 (whitespace-only input likewise).
    // LenientJson.parse returns Json (not Nullable/Option), so the faithful mapping of libGDX's
    // null root is pinned as Json.Null — the only Json inhabitant carrying "no value".
    assertEquals(LenientJson.parse(""), Json.Null)
    assertEquals(LenientJson.parse("   "), Json.Null)
  }
}
