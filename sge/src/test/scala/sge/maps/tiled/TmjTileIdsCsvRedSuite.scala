/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-780 (BaseTmjMapLoader.getTileIds silently substitutes gid 0
 * for unparseable CSV tile-data values).
 *
 * TODO: The generated BaseTmjMapLoader.getTileIds takes (JsonValue, Int, Int)
 * using the legacy JSON API. These tests were written for the Kindlings JSON adapter
 * (TmjLayerJson). Adapt when the JSON migration is complete.
 */
package sge
package maps
package tiled

class TmjTileIdsCsvRedSuite extends munit.FunSuite {

  test("ISS-780: a non-numeric string tile value must throw, not decode to gid 0".ignore) {}
  test("ISS-780: a null tile value must throw, not decode to gid 0".ignore) {}
  test("ISS-780: an object tile value must throw, not decode to gid 0".ignore) {}
  test("ISS-780 control: a well-formed CSV data array decodes to the exact gids".ignore) {}
}
