/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import hearth.kindlings.jsoniterjson.{ JsonNumber, JsonObject }

/** ISS-515 bounce-2 red suite: pins [[LenientJson]] to libGDX `JsonReader`'s exact lenient grammar — round 2.
  *
  * Authority: `original-src/libgdx/gdx/res/com/badlogic/gdx/utils/JsonReader.rl` lines 301-312 (the Ragel grammar) and the generated
  * `original-src/libgdx/gdx/src/com/badlogic/gdx/utils/JsonReader.java` lines 541-568 (exception wrapping: every `RuntimeException` raised while the machine runs is captured at lines 543-544 and
  * re-thrown as a `SerializationException` at lines 557/568). Every expected value below was additionally cross-checked empirically against the real `com.badlogic.gdx.utils.JsonReader` (gdx 1.13.1)
  * on the JVM.
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
  * Deviation classes pinned here (found by the bounce-2 audit, all verified against gdx 1.13.1):
  *   1. a leading comma whose first element is ABSENT is ACCEPTED by libGDX;
  *   1. the newline-form `comma` consuming the optional ',' directly before the closer is REJECTED by libGDX;
  *   1. unquoted-value keyword typing (`true`/`false`/`null`) tests the UNESCAPED text, not the raw token;
  *   1. malformed `\u` escapes surface as [[SgeError.InvalidInput]], never as raw runtime exceptions.
  */
class LenientJsonGrammarRedSuite2 extends munit.FunSuite {

  private def obj(fields: (String, Json)*): Json = Json.Obj(JsonObject(fields.toVector))
  private def arr(values: Json*):           Json = Json.Arr(values.toVector)
  private def num(value:  Long):            Json = Json.Num(JsonNumber.fromLong(value))

  // ---------------------------------------------------------------------------------------------
  // (1) a leading comma with an ABSENT first element is accepted
  // ---------------------------------------------------------------------------------------------

  test("(1) lone comma in otherwise-empty containers is accepted: {,} [,] { , } [<LF>,]") {
    // JsonReader.rl lines 310-311: with nameValue?/value? absent and the (comma ...)** loop taken
    // zero times, the close production (','? ws* '}' / ','? ws* ']') still allows one optional
    // comma — so a lone ',' inside otherwise-empty braces/brackets is in-grammar.
    // Real JsonReader (gdx 1.13.1): {,} -> {}, [,] -> [], { , } -> {}, "[\n,]" -> [].
    assertEquals(LenientJson.parse("{,}"), obj())
    assertEquals(LenientJson.parse("[,]"), arr())
    assertEquals(LenientJson.parse("{ , }"), obj())
    assertEquals(LenientJson.parse("[\n,]"), arr())
  }

  test("(1) leading comma before the first array element is accepted: [,1]") {
    // JsonReader.rl line 311: array := ws* value? ws2* <: (comma ws* value ws2*)** :>> ...
    // With value? absent the machine may enter the (comma ws* value)** loop directly, so a
    // leading ',' followed by the first element parses as one loop iteration.
    // Real JsonReader (gdx 1.13.1): [,1] -> [1].
    assertEquals(LenientJson.parse("[,1]"), arr(num(1)))
  }

  test("(1) leading comma before the first object member is accepted: {,a:1} {,a:1,b:2}") {
    // JsonReader.rl line 310: object := ws* nameValue? ws2* <: (comma ws* nameValue ws2*)** :>> ...
    // Same shape as the array case: nameValue? absent, then comma+nameValue loop iterations.
    // Real JsonReader (gdx 1.13.1): {,a:1} -> {a:1}, {,a:1,b:2} -> {a:1,b:2}.
    assertEquals(LenientJson.parse("{,a:1}"), obj("a" -> num(1)))
    assertEquals(LenientJson.parse("{,a:1,b:2}"), obj("a" -> num(1), "b" -> num(2)))
  }

  test("(1) negative pin: doubled commas remain rejected: [,,1] {a:1,,b:2}") {
    // JsonReader.rl lines 310-311: each `comma` in the ** loop must be followed by ws* and then a
    // nameValue/value — two adjacent same-line commas leave the second one with nothing to attach
    // to, and the close production allows at most ONE optional ','.
    // Real JsonReader (gdx 1.13.1): [,,1] -> SerializationException, {a:1,,b:2} -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("[,,1]")
    }
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("{a:1,,b:2}")
    }
  }

  // ---------------------------------------------------------------------------------------------
  // (2) the newline-form comma may NOT consume the optional ',' directly before the closer
  // ---------------------------------------------------------------------------------------------

  test("(2) newline then comma directly before '}' is rejected: {a: 1<LF>,}") {
    // JsonReader.rl line 304: comma = ',' | ([\r\n] ws* ','?). Once a newline is consumed, the
    // DFA takes the optional ',' greedily as part of THAT comma (the `<:`/`:>>` guards on line 310
    // give the loop entry priority over the close), committing to a following nameValue — the
    // close production's own ','? (line 310) is no longer reachable, so '}' right after fails.
    // Real JsonReader (gdx 1.13.1): "{a: 1\n,}" -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("{a: 1\n,}")
    }
  }

  test("(2) newline then comma directly before ']' is rejected: [1<LF>,]") {
    // JsonReader.rl line 304 + line 311: same commitment as the object case — after [\r\n] ws* ','
    // the machine requires a value, and ']' is not one.
    // Real JsonReader (gdx 1.13.1): "[1\n,]" -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("[1\n,]")
    }
  }

  test("(2) newline, comma, space before '}' is rejected: {a: 1<LF>, }") {
    // JsonReader.rl lines 304/310: the trailing ' ' after the committed newline-form comma is
    // consumed by ws*, but a nameValue must follow and '}' is not one. The space does not
    // re-open the close production.
    // Real JsonReader (gdx 1.13.1): "{a: 1\n, }" -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("{a: 1\n, }")
    }
  }

  test("(2) positive pin: trailing newline / comma+newline / newline+comma+member stay accepted") {
    // JsonReader.rl line 310: close = (','? ws* '}') — ws (line 302) includes '\n', so a bare
    // trailing newline before '}' is fine, and ','-then-'\n' is the optional close comma followed
    // by ws*. A newline-form comma followed by an actual member ({a: 1\n, b: 2}) is the loop case.
    // Real JsonReader (gdx 1.13.1): all four accepted.
    assertEquals(LenientJson.parse("{a: 1\n}"), obj("a" -> num(1)))
    assertEquals(LenientJson.parse("{a: 1,\n}"), obj("a" -> num(1)))
    assertEquals(LenientJson.parse("[1,\n]"), arr(num(1)))
    assertEquals(LenientJson.parse("{a: 1\n, b: 2}"), obj("a" -> num(1), "b" -> num(2)))
  }

  // ---------------------------------------------------------------------------------------------
  // (3) keyword typing (true/false/null) tests the UNESCAPED value text
  // ---------------------------------------------------------------------------------------------

  test("(3) escaped keyword as object member value types as boolean: {a: \\u0074rue} -> true") {
    // JsonReader.rl action string, lines 112-113: `if (needsUnescape) value = unescape(value)`
    // runs BEFORE the equals("true")/equals("false")/equals("null") checks at lines 123-134, so
    // keyword recognition sees the UNESCAPED text. Only the numeric classification loop
    // (lines 137-163) walks the raw buffer chars.
    // Real JsonReader (gdx 1.13.1): {a: true} (raw backslash-u in the input) -> boolean true.
    assertEquals(LenientJson.parse("{a: \\u0074rue}"), obj("a" -> Json.Bool(true)))
  }

  test("(3) escaped keywords as array elements type as null/false/true") {
    // JsonReader.rl lines 112-113 + 123-134 again: unescape first, then keyword checks —
    // covering all three keywords and an escape in a non-leading position.
    // Real JsonReader (gdx 1.13.1): [null] -> [null], [false] -> [false],
    // [true] -> [true] (raw backslash-u in the input text in each case).
    assertEquals(LenientJson.parse("[\\u006Eull]"), arr(Json.Null))
    assertEquals(LenientJson.parse("[\\u0066alse]"), arr(Json.Bool(false)))
    assertEquals(LenientJson.parse("[tru\\u0065]"), arr(Json.Bool(true)))
  }

  // ---------------------------------------------------------------------------------------------
  // (4) malformed \u escapes surface as SgeError.InvalidInput, never as raw runtime exceptions
  // ---------------------------------------------------------------------------------------------

  test("(4) non-hex digits in a quoted \\u escape throw SgeError.InvalidInput: [\"\\uzzzz\"]") {
    // Generated JsonReader.java lines 541-568: the whole machine body is wrapped in
    // `catch (RuntimeException ex)` (lines 543-544) and any captured exception is re-thrown as a
    // SerializationException (lines 557/568) — unescape's Integer.parseInt("zzzz", 16)
    // NumberFormatException included. LenientJson.parse documents `@throws SgeError.InvalidInput
    // if the input is malformed`, the port's mapping of SerializationException.
    // Real JsonReader (gdx 1.13.1): ["\uzzzz"] -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("[\"\\uzzzz\"]")
    }
  }

  test("(4) truncated \\u escape in an unquoted value throws SgeError.InvalidInput: [a\\u12]") {
    // Generated JsonReader.java lines 541-568 again: unescape's substring(i, i + 4) past the end
    // of the token (generated lines 732-774) raises StringIndexOutOfBoundsException, captured at
    // lines 543-544 and re-thrown as SerializationException at line 568. The port must map it to
    // SgeError.InvalidInput per its documented @throws contract — not leak the raw runtime
    // exception.
    // Real JsonReader (gdx 1.13.1): [a\u12] -> SerializationException.
    intercept[SgeError.InvalidInput] {
      LenientJson.parse("[a\\u12]")
    }
  }
}
