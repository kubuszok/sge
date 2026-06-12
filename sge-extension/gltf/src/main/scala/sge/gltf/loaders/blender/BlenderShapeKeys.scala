/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 80
 * Covenant-baseline-methods: BlenderShapeKeys,couldBeDouble,couldBeLong,elementToString,numberToString,parse
 * Covenant-source-reference: net/mgsx/gltf/loaders/blender/BlenderShapeKeys.java
 * Covenant-verified: 2026-06-12
 *
 * upstream-commit: 683054a88382f71e8472abbc1c29931277c1cf22
 */
package sge
package gltf
package loaders
package blender

import sge.gltf.data.geometry.GLTFMesh
import sge.utils.Json
import lowlevel.Nullable
import lowlevel.util.DynamicArray

import scala.util.boundary
import scala.util.boundary.break

/** Blender stores shape key names in mesh extras.
  *
  * Shape key names are stored in mesh extras as a "targetNames" array. The original Java reads them via LibGDX's JsonValue API; in SGE we use the JSON AST stored in GLTFExtras.
  */
object BlenderShapeKeys {

  /** Blender store shape key names in mesh extras.
    * {{{
    *  "meshes" : [
    *       {
    *         "name" : "Plane",
    *         "extras" : {
    *             "targetNames" : [
    *                 "Water",
    *                 "Mountains"
    *             ]
    *         },
    *         "primitives" : ...,
    *         "weights" : [0.6, 0.3]
    *       }
    *     ]
    * }}}
    */
  def parse(glMesh: GLTFMesh): Nullable[DynamicArray[String]] =
    // BlenderShapeKeys.java:28 — `if(glMesh.extras == null) return null;`
    glMesh.extras.flatMap { extras =>
      // BlenderShapeKeys.java:29 — `glMesh.extras.value.get("targetNames")`. The original dereferences
      // extras.value directly; SGE stores it as a Nullable[Json], and JsonValue.get(name) on a non-object
      // yields null, so we look the field up only when value is a present JSON object.
      val targetNames: Nullable[Json] =
        extras.value.flatMap { v =>
          v.fold(
            onNull = Nullable.empty[Json],
            onBoolean = _ => Nullable.empty[Json],
            onNumber = _ => Nullable.empty[Json],
            onString = _ => Nullable.empty[Json],
            onArray = _ => Nullable.empty[Json],
            onObject = obj => obj("targetNames").fold(Nullable.empty[Json])(Nullable(_))
          )
        }
      // BlenderShapeKeys.java:30 — `if(targetNames != null && targetNames.isArray())`
      targetNames.flatMap { tn =>
        tn.fold(
          onNull = Nullable.empty[DynamicArray[String]],
          onBoolean = _ => Nullable.empty[DynamicArray[String]],
          onNumber = _ => Nullable.empty[DynamicArray[String]],
          onString = _ => Nullable.empty[DynamicArray[String]],
          onObject = _ => Nullable.empty[DynamicArray[String]],
          onArray = elements => {
            // BlenderShapeKeys.java:31 — `return new Array<String>(targetNames.asStringArray());`
            // Mirror JsonValue.asStringArray (JsonValue.java:389-413): allocate `new String[size]` and assign
            // each converted element by index. A literal JSON null element (asStringArray's `case nullValue`)
            // leaves the array's pre-initialised null entry in place — exactly as the gdx Array<String> holds it
            // — so no explicit null is written here.
            val strings = new Array[String](elements.size)
            var i       = 0
            elements.foreach { element =>
              elementToString(element).foreach { s => strings(i) = s }
              i += 1
            }
            Nullable(DynamicArray.from(strings))
          }
        )
      }
    }
  // BlenderShapeKeys.java:33 — `return null;` (extras absent, no targetNames field, or not a JSON array)
  // is covered by the empty Nullable fallbacks above.

  /** Mirrors `com.badlogic.gdx.utils.JsonValue.asStringArray`'s per-element type switch (JsonValue.java:393-410): string → itself (JsonValue.java:394-396); boolean → "true"/"false"
    * (JsonValue.java:403-405); null → null (JsonValue.java:406-408); object/array → IllegalStateException (JsonValue.java:409-410); number → its textual form.
    *
    * Number handling is the subtle case. In gdx a parsed array element is a `longValue`/`doubleValue` JsonValue whose own `stringValue` is the source token, but asStringArray (JsonValue.java:398/401)
    * reads the *array's* `stringValue` — which is null for a parsed array — so it never returns the source token and always renders via `Long.toString(longValue)` (JsonValue.java:401) or
    * `Double.toString(doubleValue)` (JsonValue.java:398) of the value JsonReader parsed. JsonReader chooses long-vs-double by scanning the token text (JsonReader.java:220-263): characters limited to
    * digits/sign keep `couldBeLong`; any of `.`/`e`/`E` flips to `couldBeDouble`; any other character abandons both; a long that overflows `Long.parseLong` or a double that fails `Double.parseDouble`
    * falls through to a verbatim string element (JsonReader.java:256-266).
    *
    * The kindlings AST does not store the original token: `JsonCodec` decodes every number through `readBigDecimal` and stores `BigDecimal.toString` in `JsonNumber.value` (e.g. `1e2` → "1E+2",
    * `10.50` → "10.50", `-0.0` → "0.0" — the BigDecimal normaliser drops the negative-zero sign before this method ever runs). To reproduce gdx's `Long/Double.toString` output we therefore re-run
    * JsonReader's token classification over `JsonNumber.value` and emit `Long.toString`/`Double.toString` of the re-parsed value, falling back to the raw string when both parses fail (mirroring gdx's
    * string fallback at JsonReader.java:256-266). This makes `5` → "5", `0.6` → "0.6", `1e2` → "100.0" and `10.50` → "10.5" match gdx. Two residual divergences are caused upstream by the shared
    * codec, not here: `-0.0` renders "0.0" (BigDecimal normalisation drops the sign), and a token gdx would render VERBATIM via its string fallback — an integer literal beyond `Long`'s range AND
    * beyond DECIMAL128's 34 significant digits (e.g. a 40-digit integer) — is rounded by `readBigDecimal` into a `BigDecimal.toString` exponent form (`1.234…235E+39`) before this method sees it, so
    * the port routes it through `Double.parseDouble`/`Double.toString` (→ "1.2345678901234568E39") instead of emitting the original digits. Decimal tokens with a `.` beyond 34 significant digits do
    * NOT diverge: both gdx and the port collapse them through `Double.parseDouble` to the same byte-for-byte double form. Both residual divergences are properties of `JsonCodec`/`readBigDecimal`,
    * outside BlenderShapeKeys.
    */
  private def elementToString(element: Json): Nullable[String] =
    element.fold(
      onNull = Nullable.empty[String],
      onBoolean = b => Nullable(if (b) "true" else "false"),
      onNumber = n => Nullable(numberToString(n.value)),
      onString = s => Nullable(s),
      onArray = _ => throw new IllegalStateException("Value cannot be converted to string: array"),
      onObject = _ => throw new IllegalStateException("Value cannot be converted to string: object")
    )

  /** Replicates JsonReader's token classification (JsonReader.java:220-263) over a numeric token, returning the `Long.toString`/`Double.toString` form gdx's JsonValue.asStringArray would produce for
    * it. The scan walks the token (JsonReader.java:222-248): digits and `+`/`-` keep `couldBeLong`; `.`/`e`/`E` set `couldBeDouble` and clear `couldBeLong`; any other character clears both and stops
    * (the `break outer2` at JsonReader.java:246). `couldBeDouble` parses as double (JsonReader.java:249-255), else `couldBeLong` parses as long (JsonReader.java:256-263); a failed parse — e.g. an
    * integer beyond `Long`'s range — leaves the token as a verbatim string element exactly as gdx falls through to `string(valueName, value)` (JsonReader.java:264-266).
    */
  private def numberToString(value: String): String = {
    var couldBeDouble = false
    var couldBeLong   = true
    boundary {
      var i = 0
      while (i < value.length) {
        value.charAt(i) match {
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' | '+' =>
          case '.' | 'e' | 'E'                                                       =>
            couldBeDouble = true
            couldBeLong = false
          case _ =>
            couldBeDouble = false
            couldBeLong = false
            break()
        }
        i += 1
      }
    }
    if (couldBeDouble) {
      try java.lang.Double.toString(java.lang.Double.parseDouble(value))
      catch { case _: NumberFormatException => value }
    } else if (couldBeLong) {
      try java.lang.Long.toString(java.lang.Long.parseLong(value))
      catch { case _: NumberFormatException => value }
    } else value
  }
}
