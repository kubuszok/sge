/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-780 (BaseTmjMapLoader.getTileIds silently substitutes gid 0
 * for unparseable CSV tile-data values).
 *
 * Root cause being reproduced: in sge/src/main/scala/sge/maps/tiled/
 * BaseTmjMapLoader.scala (object BaseTmjMapLoader.getTileIds, the csv / empty
 * encoding branch) the port writes
 *
 *   case Json.Num(n) => result(i) = n.toLong.map(_.toInt).getOrElse(n.toDouble.map(_.toInt).getOrElse(0))
 *   case _           => result(i) = 0
 *
 * so a data array element that is not a parseable number is silently turned
 * into an empty tile (gid 0) instead of failing loudly. A corrupt .tmj then
 * "loads successfully" with blank tiles.
 *
 * Original Java (original-src/libgdx/gdx/src/com/badlogic/gdx/maps/tiled/
 * BaseTmjMapLoader.java, getTileIds line 648) does `ids = data.asIntArray()`.
 * JsonValue.asIntArray (JsonValue.java lines 506-531) throws on non-numeric
 * data: a string value goes through `Integer.parseInt` (NumberFormatException
 * if non-numeric) and any object/array/null value hits the default case and
 * throws IllegalStateException("Value cannot be converted to int: " + type).
 * LibGDX never silently substitutes 0 — it fails loudly.
 *
 * These tests are written before the fix and encode the original Java
 * semantics, not the port's silent-zero behaviour.
 */
package sge
package maps
package tiled

import scala.util.{ Failure, Success, Try }

import sge.utils.Json

class TmjTileIdsCsvRedSuite extends munit.FunSuite {

  private def layer(data: Json): TmjLayerJson =
    TmjLayerJson(name = "layer1", tpe = "tilelayer", width = 2, height = 2, data = Some(data))

  private def assertLoud(description: String, data: Json): Unit =
    Try(BaseTmjMapLoader.getTileIds(layer(data))) match {
      case Success(ids) =>
        fail(
          s"getTileIds silently accepted $description; decoded ids = ${ids.toList} " +
            "(LibGDX JsonValue.asIntArray throws instead of substituting gid 0)"
        )
      case Failure(_) => () // any loud failure is acceptable (NumberFormatException / IllegalStateException)
    }

  test("ISS-780: a non-numeric string tile value must throw, not decode to gid 0") {
    assertLoud(
      "a non-numeric string element",
      Json.arr(Json.fromInt(1), Json.fromString("not-a-number"), Json.fromInt(3), Json.fromInt(4))
    )
  }

  test("ISS-780: a null tile value must throw, not decode to gid 0") {
    assertLoud(
      "a null element",
      Json.arr(Json.fromInt(1), Json.Null, Json.fromInt(3), Json.fromInt(4))
    )
  }

  test("ISS-780: an object tile value must throw, not decode to gid 0") {
    assertLoud(
      "an object element",
      Json.arr(Json.fromInt(1), Json.obj("x" -> Json.fromInt(0)), Json.fromInt(3), Json.fromInt(4))
    )
  }

  test("ISS-780 control: a well-formed CSV data array decodes to the exact gids") {
    val ids = BaseTmjMapLoader.getTileIds(
      layer(Json.arr(Json.fromInt(1), Json.fromInt(2), Json.fromInt(3), Json.fromInt(4)))
    )
    assertEquals(ids.toList, List(1, 2, 3, 4))
  }
}
