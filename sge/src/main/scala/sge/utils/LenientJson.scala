/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/JsonReader.java
 *   (generated from com/badlogic/gdx/utils/JsonReader.rl — the Ragel grammar
 *   is the authoritative specification reproduced below)
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: faithful hand-written port of libGDX JsonReader's lenient
 *     grammar. The original is a Ragel-generated state machine; this is an
 *     equivalent recursive-descent reader producing the kindlings Json AST
 *     instead of JsonValue. It implements the JsonReader.rl productions
 *     (lines 301-312) faithfully:
 *       comment     = ('//' | '/' '*') @comment;
 *       ws          = [\r\n\t ] | comment;
 *       ws2         = [\t ] | comment;
 *       comma       = ',' | ([\r\n] ws* ','?);
 *       quotedString = '"' @quotedChars %string '"';
 *       nameString  = quotedString | ^[":,}/\r\n\t ] >unquotedChars %string;
 *       valueString = quotedString | ^[":,{[\]/\r\n\t ] >unquotedChars %string;
 *       value       = '{' @startObject | '[' @startArray | valueString;
 *       nameValue   = nameString >name ws* ':' ws* value;
 *       object := ws* nameValue? ws2* <: (comma ws* nameValue ws2*)** :>> (','? ws* '}' @endObject);
 *       array  := ws* value?    ws2* <: (comma ws* value    ws2*)** :>> (','? ws* ']' @endArray);
 *       main   := ws* value ws*;
 *     Consequences pinned by LenientJsonGrammarRedSuite and matched here:
 *       - ':' is the ONLY name/value separator; '=' is an ordinary name char
 *         (unquotedChars name mode terminates at ':','\r','\n' or comment).
 *       - members are separated by `comma` only: a literal ',' OR a newline
 *         ([\r\n] ws* ','?). No bare same-line adjacency and no DOUBLED
 *         commas ([,,1], {a:1,,b:2} are rejected). A LEADING comma whose
 *         first member is ABSENT is accepted, because nameValue?/value? is
 *         optional and the (comma ...)** loop / close's `','?` absorb it
 *         ({,} -> {}, [,1] -> [1], {,a:1} -> {a:1}). At most one optional
 *         trailing comma — but a NEWLINE-form comma ([\r\n] ws* ',') that
 *         consumed its `','?` commits to a following member, so a closer may
 *         not follow it ({a: 1\n,}, [1\n,] are rejected); a literal ',' or a
 *         bare newline before the closer is the close's own trailing comma
 *         and is accepted ({a: 1,\n}, {a: 1\n} are accepted).
 *       - ws (token skipping) is exactly [\r\n\t ]+comments; ws2 (post-value)
 *         is [\t ]+comments — the newline is reserved as a member separator.
 *       - trailing-trim of unquoted tokens uses Java's Character.isSpace
 *         ({' ','\t','\n','\f','\r'} — NOT 0x0B vertical tab); the trimmed
 *         trailing chars are pushed back and must be consumed by the
 *         following ws2/comma/close, so a form-feed (isSpace, but not in
 *         ws/ws2) trailing an unquoted value is REJECTED.
 *       - start classes are enforced: an unquoted value cannot start with
 *         ':' (or ',', '{', '[', ']', '/' or ws), e.g. [:a] is rejected.
 *       - unquoted-value typing unescapes FIRST (JsonReader.java line 113),
 *         so the keyword tests true/false/null (lines 123-134) see the
 *         UNESCAPED text ({a: \\u0074rue} -> true). ONLY the numeric
 *         classification loop (line 137 `for (int i = s; i < p; i++)`) walks
 *         the RAW token chars.
 *       - a malformed \\u escape (truncated or non-hex) in a quoted string or
 *         in unescape surfaces as SgeError.InvalidInput, mirroring libGDX
 *         wrapping the RuntimeException into a SerializationException
 *         (JsonReader.java lines 317-319/342, 428).
 *   Convention (f): libGDX accepts non-finite numeric tokens (e.g. 1e999)
 *     because Double.parseDouble yields Infinity without throwing, and
 *     JsonValue keeps the raw text alongside the double. The kindlings AST
 *     cannot hold Infinity (JsonNumber.fromDouble returns None for
 *     NaN/Infinity), so such tokens fall back to Json.Str of the raw token
 *     text — the closest faithful mapping through the Json-typed API.
 *   Convention (g): libGDX's parse("") returns the null JsonValue root
 *     without throwing (the state machine never executes, p == pe). Since
 *     parse returns a non-nullable Json, the null root maps to Json.Null —
 *     the only Json inhabitant carrying "no value". Whitespace-only input
 *     likewise yields Json.Null.
 *   Convention: GdxRuntimeException/SerializationException -> SgeError.InvalidInput
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 546
 * Covenant-baseline-methods: CommaResult,LenientJson,Parser,atEnd,isSpace,isWs,isWs2,length,numberOrString,parse,parseArray,parseComma,parseName,parseNameValue,parseObject,parseQuotedString,parseUnquotedValue,parseValue,parser,pos,readUnquoted,skipComment,skipWs,skipWs2,typeUnquotedValue,unescape
 * Covenant-source-reference: com/badlogic/gdx/utils/JsonReader.java
 * Covenant-verified: 2026-06-12
 */
package sge
package utils

import hearth.kindlings.jsoniterjson.{ JsonNumber, JsonObject }

import scala.util.boundary
import scala.util.boundary.break

/** Lenient JSON reader matching libGDX's `JsonReader` relaxed grammar, producing a kindlings [[Json]] AST.
  *
  * libGDX (and skins authored against it, e.g. VisUI) writes skin/UI JSON in a relaxed form: object keys and string values may be unquoted, members are separated by commas or newlines, and `//` /
  * block comments are allowed. The strict jsoniter reader used by [[readJson]] rejects that input, so skin loading parses through this reader instead.
  *
  * The grammar implemented is exactly libGDX's `JsonReader.rl` (productions reproduced in the file header). Unquoted value typing mirrors libGDX exactly (JsonReader.java lines 207-266):
  *   - `true` / `false` become booleans, `null` becomes a JSON null;
  *   - a token whose RAW characters are only `[0-9+-]` (optionally containing `.`, `e`, `E`) is parsed as a number — a long when integral, a double otherwise — falling back to a string on parse
  *     failure;
  *   - anything else is a string.
  */
object LenientJson {

  /** Parses a lenient-JSON string into a [[Json]] AST. The root may be an object or an array, or any scalar value (mirroring libGDX `main := ws* value ws*`).
    *
    * Empty or whitespace-only input yields [[Json.Null]] (libGDX returns the null root without error — see convention (g) in the file header).
    * @throws SgeError.InvalidInput
    *   if the input is malformed.
    */
  def parse(text: String): Json = {
    val parser = new Parser(text)
    parser.skipWs() // main := ws* ...
    // main requires a value, but libGDX never runs the machine for empty/ws-only
    // input, returning the null root without error (convention (g)).
    if (parser.atEnd) Json.Null
    else {
      val result = parser.parseValue()
      parser.skipWs() // main := ... ws*
      if (!parser.atEnd)
        throw SgeError.InvalidInput("Unexpected trailing content in JSON at offset " + parser.pos)
      result
    }
  }

  /** Outcome of [[Parser.parseComma]], distinguishing the two `comma` alternatives (JsonReader.rl line 304) so the close productions can tell a loop-`comma` (member mandatory) from the close's own
    * optional trailing `','?`.
    */
  private enum CommaResult {

    /** No separator matched at the current position. */
    case None

    /** A literal `,` — may be a loop `comma` OR the close production's optional trailing `','?`, so a closer may follow. */
    case Literal

    /** A bare `[\r\n] ws*` newline that consumed no trailing `,` — equally consumable by the close's `ws*`, so a closer may follow. */
    case Newline

    /** A `[\r\n] ws* ','` newline form that consumed the optional trailing `,` — this `,` belongs to `comma`, so a member is mandatory and a closer is rejected. */
    case NewlineComma
  }

  final private class Parser(data: String) {
    var pos: Int = 0
    private val length = data.length

    def atEnd: Boolean = pos >= length

    // ----- whitespace classes (JsonReader.rl lines 302-303) -----

    /** `ws` token-skipping class minus comments: exactly `[\r\n\t ]` (JsonReader.rl line 302). */
    private def isWs(c: Char): Boolean =
      c == '\r' || c == '\n' || c == '\t' || c == ' '

    /** `ws2` class minus comments: exactly `[\t ]` (JsonReader.rl line 303) — the newline is reserved as a member separator. */
    private def isWs2(c: Char): Boolean =
      c == '\t' || c == ' '

    /** Java's `Character.isSpace`: space, horizontal tab, LF, form-feed, CR — but NOT 0x0B vertical tab. This is the predicate libGDX uses to trim trailing whitespace from unquoted tokens
      * (JsonReader.rl line 277).
      */
    private def isSpace(c: Char): Boolean =
      c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r'

    /** If a `//` or block comment opens at `pos`, consume it (JsonReader.rl action `comment`, lines 213-225) and return true. */
    private def skipComment(): Boolean =
      if (pos + 1 < length && data.charAt(pos) == '/') {
        val next = data.charAt(pos + 1)
        if (next == '/') {
          pos += 2
          while (pos < length && data.charAt(pos) != '\n') pos += 1
          true
        } else if (next == '*') {
          pos += 2
          while (pos + 1 < length && !(data.charAt(pos) == '*' && data.charAt(pos + 1) == '/')) pos += 1
          pos += 2 // consume the closing comment terminator
          if (pos > length) pos = length
          true
        } else false
      } else false

    /** Skips `ws*` = `([\r\n\t ] | comment)*` (JsonReader.rl line 302). */
    def skipWs(): Unit = {
      var progressing = true
      while (progressing) {
        progressing = false
        while (pos < length && isWs(data.charAt(pos))) {
          pos += 1
          progressing = true
        }
        if (skipComment()) progressing = true
      }
    }

    /** Skips `ws2*` = `([\t ] | comment)*` (JsonReader.rl line 303). */
    private def skipWs2(): Unit = {
      var progressing = true
      while (progressing) {
        progressing = false
        while (pos < length && isWs2(data.charAt(pos))) {
          pos += 1
          progressing = true
        }
        if (skipComment()) progressing = true
      }
    }

    /** Parses any JSON value: `value = '{' | '[' | valueString` (JsonReader.rl line 308). */
    def parseValue(): Json = {
      if (atEnd) throw SgeError.InvalidInput("Unexpected end of JSON")
      data.charAt(pos) match {
        case '{' => parseObject()
        case '[' => parseArray()
        case '"' => Json.Str(parseQuotedString())
        case c   =>
          // valueString = quotedString | ^[":,{[\]/\r\n\t ] >unquotedChars (JsonReader.rl line 307):
          // the first unquoted-value char may not be any of these. ('{','[','"'
          // are dispatched above; the rest must error here.)
          if (c == ':' || c == ',' || c == ']' || c == '/' || isWs(c))
            throw SgeError.InvalidInput("Unexpected character '" + c + "' at start of JSON value at offset " + pos)
          parseUnquotedValue()
      }
    }

    /** Parses an object per `object := ws* nameValue? ws2* <: (comma ws* nameValue ws2*)** :>> (','? ws* '}' @endObject)` (JsonReader.rl line 310). */
    private def parseObject(): Json = {
      pos += 1 // consume '{'
      val fields = scala.collection.mutable.ArrayBuffer.empty[(String, Json)]
      skipWs() // ws*
      if (atEnd) throw SgeError.InvalidInput("Unterminated object in JSON")
      if (data.charAt(pos) == '}') {
        pos += 1 // empty object: nameValue? absent, then (','? ws* '}')
      } else {
        // nameValue? — the FIRST member is OPTIONAL. After `ws*`, the only remaining `comma`-starter
        // (JsonReader.rl line 304: comma = ',' | [\r\n]...) is a literal ',' (the [\r\n] forms were
        // already absorbed by the leading `ws*`). A leading ',' therefore means the first member is
        // absent and the close's `','?` / the (comma ws* nameValue)** loop handles it — so `{,a:1}`,
        // `{,}` and `{ , }` are in-grammar.
        if (data.charAt(pos) != ',') {
          parseNameValue(fields) // nameValue
          skipWs2() // ws2*
        }
        boundary {
          while (true) {
            if (atEnd) throw SgeError.InvalidInput("Unterminated object in JSON")
            if (data.charAt(pos) == '}') {
              pos += 1 // (','? ws* '}') with no trailing comma
              break()
            }
            // A separator is required before the next member or close.
            val comma = parseComma()
            if (comma == CommaResult.None)
              throw SgeError.InvalidInput("Expected ',' or '}' in object at offset " + pos)
            skipWs() // ws* after comma
            if (atEnd) throw SgeError.InvalidInput("Unterminated object in JSON")
            if (data.charAt(pos) == '}') {
              // A closer may follow a literal ',' (the close's own optional trailing ',') or a bare
              // newline-form comma; but a newline-form comma that consumed a trailing ',' (line 304's
              // `','?` inside `comma`) commits to a following nameValue, so `{a: 1\n,}` is rejected.
              if (comma == CommaResult.NewlineComma)
                throw SgeError.InvalidInput("Expected name/value pair after comma in object at offset " + pos)
              pos += 1 // the comma was the single optional trailing ',' (':>>' close)
              break()
            }
            parseNameValue(fields) // (comma ws* nameValue ...)
            skipWs2() // ws2*
          }
        }
      }
      Json.Obj(JsonObject(fields.toVector))
    }

    /** Parses one `nameValue = nameString >name ws* ':' ws* value` (JsonReader.rl line 309) and appends it. */
    private def parseNameValue(fields: scala.collection.mutable.ArrayBuffer[(String, Json)]): Unit = {
      val name = parseName()
      skipWs() // ws* before ':'
      if (atEnd || data.charAt(pos) != ':')
        throw SgeError.InvalidInput("Expected ':' after object name '" + name + "' at offset " + pos)
      pos += 1 // consume ':' — the ONLY name/value separator ('=' is an ordinary name char)
      skipWs() // ws* after ':'
      val value = parseValue()
      fields += (name -> value)
    }

    /** Parses an array per `array := ws* value? ws2* <: (comma ws* value ws2*)** :>> (','? ws* ']' @endArray)` (JsonReader.rl line 311). */
    private def parseArray(): Json = {
      pos += 1 // consume '['
      val elements = scala.collection.mutable.ArrayBuffer.empty[Json]
      skipWs() // ws*
      if (atEnd) throw SgeError.InvalidInput("Unterminated array in JSON")
      if (data.charAt(pos) == ']') {
        pos += 1 // empty array: value? absent, then (','? ws* ']')
      } else {
        // value? — the FIRST element is OPTIONAL. As in parseObject, after `ws*` the only remaining
        // `comma`-starter is a literal ',', so a leading ',' means the first element is absent and the
        // close's `','?` / the (comma ws* value)** loop handles it — `[,1]`, `[,]` and `[\n,]` are
        // in-grammar.
        if (data.charAt(pos) != ',') {
          elements += parseValue() // value
          skipWs2() // ws2*
        }
        boundary {
          while (true) {
            if (atEnd) throw SgeError.InvalidInput("Unterminated array in JSON")
            if (data.charAt(pos) == ']') {
              pos += 1 // (','? ws* ']') with no trailing comma
              break()
            }
            val comma = parseComma()
            if (comma == CommaResult.None)
              throw SgeError.InvalidInput("Expected ',' or ']' in array at offset " + pos)
            skipWs() // ws* after comma
            if (atEnd) throw SgeError.InvalidInput("Unterminated array in JSON")
            if (data.charAt(pos) == ']') {
              // As in parseObject: a newline-form comma that consumed a trailing ',' commits to a
              // following value, so `[1\n,]` is rejected; a literal ',' or a bare newline-form comma
              // before the closer is the close's optional trailing comma and is accepted.
              if (comma == CommaResult.NewlineComma)
                throw SgeError.InvalidInput("Expected value after comma in array at offset " + pos)
              pos += 1 // single optional trailing comma
              break()
            }
            elements += parseValue() // (comma ws* value ...)
            skipWs2() // ws2*
          }
        }
      }
      Json.Arr(elements.toVector)
    }

    /** Matches `comma = ',' | ([\r\n] ws* ','?)` (JsonReader.rl line 304) and reports which alternative was taken.
      *
      * The distinction is load-bearing for the close production `(','? ws* '}')` / `(','? ws* ']')`: in the newline alternative the trailing `','?` belongs to `comma`, so once it is consumed a member
      * is mandatory and the closer is unreachable (`{a: 1\n,}` is rejected). In the literal-`,` alternative the comma may instead be the close production's own optional `','?`, so a closer may
      * legitimately follow it (`{a: 1,\n}` is accepted). A bare `[\r\n]` newline that consumes no trailing `,` is also closer-compatible (`{a: 1\n}` is accepted), because that `[\r\n]` is equally
      * consumable by the close's `ws*`.
      */
    private def parseComma(): CommaResult =
      if (atEnd) CommaResult.None
      else {
        val c = data.charAt(pos)
        if (c == ',') {
          pos += 1
          CommaResult.Literal
        } else if (c == '\r' || c == '\n') {
          pos += 1 // exactly one [\r\n]
          skipWs() // ws*
          if (!atEnd && data.charAt(pos) == ',') {
            pos += 1 // optional single trailing ',' — once taken, a member is required (closer rejected)
            CommaResult.NewlineComma
          } else
            CommaResult.Newline // bare [\r\n] ws*: equally consumable by the close's ws*, so a closer may follow
        } else CommaResult.None
      }

    /** Parses a quoted string starting at the current `"`, applying libGDX-compatible JSON unescaping. */
    private def parseQuotedString(): String = {
      pos += 1 // consume opening quote
      val sb = new StringBuilder
      boundary {
        while (true) {
          if (atEnd) throw SgeError.InvalidInput("Unterminated string in JSON")
          val c = data.charAt(pos)
          if (c == '"') {
            pos += 1
            break()
          } else if (c == '\\') {
            pos += 1
            if (atEnd) throw SgeError.InvalidInput("Unterminated escape in JSON string")
            val e = data.charAt(pos)
            pos += 1
            e match {
              case 'u' =>
                // JsonReader wraps any RuntimeException raised while parsing (substring out-of-bounds or a
                // non-hex Integer.parseInt) into a SerializationException (JsonReader.java lines 317-319/342).
                // The port maps that to SgeError.InvalidInput per parse's documented @throws.
                if (pos + 4 > length) throw SgeError.InvalidInput("Truncated unicode escape in JSON string")
                val hex       = data.substring(pos, pos + 4)
                val codePoint =
                  try Integer.parseInt(hex, 16)
                  catch { case _: NumberFormatException => throw SgeError.InvalidInput("Malformed unicode escape '\\u" + hex + "' in JSON string") }
                pos += 4
                // appendAll (not append) so the chars are added, not the Array's toString.
                sb.appendAll(Character.toChars(codePoint))
              case '"'   => sb.append('"')
              case '\\'  => sb.append('\\')
              case '/'   => sb.append('/')
              case 'b'   => sb.append('\b')
              case 'f'   => sb.append('\f')
              case 'n'   => sb.append('\n')
              case 'r'   => sb.append('\r')
              case 't'   => sb.append('\t')
              case other => throw SgeError.InvalidInput("Illegal escaped character: \\" + other)
            }
          } else {
            sb.append(c)
            pos += 1
          }
        }
      }
      sb.toString
    }

    /** Reads an unquoted object name (`nameString` unquoted branch). The start class `^[":,}/\r\n\t ]` (JsonReader.rl line 306) is enforced here; the scanner terminates at `:`, `\r`, `\n`, or comment
      * start (JsonReader.rl lines 243-246); trailing whitespace is trimmed; `\\` escapes are honored.
      */
    private def parseName(): String =
      if (data.charAt(pos) == '"') parseQuotedString()
      else {
        // nameString start class: ^[":,}/\r\n\t ]. ('"' is the quoted branch above.)
        val c = data.charAt(pos)
        if (c == ':' || c == ',' || c == '}' || c == '/' || isWs(c))
          throw SgeError.InvalidInput("Unexpected character '" + c + "' at start of JSON name at offset " + pos)
        val (raw, needsUnescape) = readUnquoted(nameMode = true)
        if (needsUnescape) unescape(raw) else raw
      }

    /** Reads an unquoted value and applies libGDX's value typing. The scanner terminates at `}`, `]`, `,`, `\r`, `\n`, or comment start (JsonReader.rl lines 264-269); trailing whitespace is trimmed.
      * Typing classifies the RAW chars, before unescaping.
      */
    private def parseUnquotedValue(): Json = {
      val (raw, needsUnescape) = readUnquoted(nameMode = false)
      // Typing scans the RAW token (JsonReader.java line 222: `for (i = s; i < p; i++)`),
      // BEFORE any unescape is applied.
      typeUnquotedValue(raw, needsUnescape)
    }

    /** Core unquoted-token scanner shared by name and value modes (JsonReader.rl action `unquotedChars`, lines 226-279). Returns the trimmed RAW token text and whether it contained a backslash
      * escape. Trailing `Character.isSpace` characters are trimmed AND left unconsumed (pos rewound to just past the last token char) so the following grammar must account for them — a form feed
      * there is unconsumable by ws/ws2 and therefore rejected.
      */
    private def readUnquoted(nameMode: Boolean): (String, Boolean) = {
      val start         = pos
      var needsUnescape = false
      boundary {
        while (pos < length) {
          val c = data.charAt(pos)
          if (c == '\\') {
            needsUnescape = true
            pos += 1
          } else if (c == '/' && pos + 1 < length && (data.charAt(pos + 1) == '/' || data.charAt(pos + 1) == '*')) {
            break() // comment start terminates the token (without consuming it)
          } else if (nameMode) {
            if (c == ':' || c == '\r' || c == '\n') break()
            else pos += 1
          } else {
            if (c == '}' || c == ']' || c == ',' || c == '\r' || c == '\n') break()
            else pos += 1
          }
        }
      }
      // Trim trailing whitespace, matching libGDX's "while (Character.isSpace(data[p])) p--"
      // (JsonReader.rl line 277). The trimmed chars are NOT consumed: pos is rewound to the
      // first trailing-space char so the surrounding grammar (ws2*/comma/close) must consume
      // them — a form feed (isSpace but not ws/ws2) there has no transition and is rejected.
      var endExclusive = pos
      while (endExclusive > start && isSpace(data.charAt(endExclusive - 1))) endExclusive -= 1
      pos = endExclusive
      (data.substring(start, endExclusive), needsUnescape)
    }

    /** Applies libGDX's unquoted-value typing (JsonReader.java 111-186). Unescaping happens FIRST (line 113: `if (needsUnescape) value = unescape(value)`), so the keyword tests
      * `value.equals("true"/"false"/"null")` (lines 123-134) see the UNESCAPED text — only the numeric classification loop (lines 137-163) walks the RAW token characters (`for (int i = s; i < p;
      * i++)`).
      */
    private def typeUnquotedValue(rawValue: String, needsUnescape: Boolean): Json = {
      // JsonReader.java line 112-113: the value handed to the keyword tests, number() and string() is the
      // UNESCAPED text. (unescape may throw on a malformed \\u escape — that surfaces as InvalidInput.)
      val value = if (needsUnescape) unescape(rawValue) else rawValue
      // JsonReader.java lines 123-134: keyword recognition tests the UNESCAPED value, not the raw token.
      if (value == "true") Json.Bool(true)
      else if (value == "false") Json.Bool(false)
      else if (value == "null") Json.Null
      else if (rawValue.isEmpty) Json.Str(value)
      else {
        var couldBeDouble = false
        var couldBeLong   = true
        var i             = 0
        var decided       = false
        // JsonReader.java line 137: `for (int i = s; i < p; i++) switch (data[i])` — RAW token chars.
        while (i < rawValue.length && !decided) {
          rawValue.charAt(i) match {
            case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' | '+' => ()
            case '.' | 'e' | 'E'                                                       =>
              couldBeDouble = true
              couldBeLong = false
            case _ =>
              couldBeDouble = false
              couldBeLong = false
              decided = true
          }
          i += 1
        }
        if (couldBeDouble)
          numberOrString(value, _.toDouble, d => JsonNumber.fromDouble(d))
        else if (couldBeLong)
          numberOrString(value, _.toLong, l => Some(JsonNumber.fromLong(l)))
        else Json.Str(value)
      }
    }

    /** Parses `value` as a number, producing a [[Json.Num]] on success or a [[Json.Str]] on any failure (matching libGDX's NumberFormatException fallthrough to `string`). Non-finite doubles (1e999)
      * yield `None` from `JsonNumber.fromDouble` and likewise fall back to a string — see convention (f) in the file header.
      */
    private def numberOrString[A](value: String, parse: String => A, toNumber: A => Option[JsonNumber]): Json =
      try
        toNumber(parse(value)) match {
          case Some(n) => Json.Num(n)
          case None    => Json.Str(value)
        }
      catch { case _: NumberFormatException => Json.Str(value) }

    /** libGDX-compatible JSON string unescaping (JsonReader.java 732-774). */
    private def unescape(value: String): String = {
      val sb     = new StringBuilder(value.length + 16)
      val len    = value.length
      var i      = 0
      var broken = false
      while (i < len && !broken) {
        var c = value.charAt(i)
        i += 1
        if (c != '\\') sb.append(c)
        else if (i == len) broken = true
        else {
          c = value.charAt(i)
          i += 1
          if (c == 'u') {
            // JsonReader.java line 428 calls Integer.parseInt(value.substring(i, i + 4), 16) with no bounds
            // or hex check: a truncated escape raises StringIndexOutOfBoundsException and a non-hex one raises
            // NumberFormatException, both captured by the parse-time catch (lines 317-319) and re-thrown as a
            // SerializationException (line 342). The port maps that to SgeError.InvalidInput.
            if (i + 4 > len) throw SgeError.InvalidInput("Truncated unicode escape in JSON string")
            val hex       = value.substring(i, i + 4)
            val codePoint =
              try Integer.parseInt(hex, 16)
              catch { case _: NumberFormatException => throw SgeError.InvalidInput("Malformed unicode escape '\\u" + hex + "' in JSON string") }
            // appendAll (not append) so the chars are added, not the Array's toString.
            sb.appendAll(Character.toChars(codePoint))
            i += 4
          } else {
            c match {
              case '"' | '\\' | '/' => sb.append(c)
              case 'b'              => sb.append('\b')
              case 'f'              => sb.append('\f')
              case 'n'              => sb.append('\n')
              case 'r'              => sb.append('\r')
              case 't'              => sb.append('\t')
              case other            => throw SgeError.InvalidInput("Illegal escaped character: \\" + other)
            }
          }
        }
      }
      sb.toString
    }
  }
}
