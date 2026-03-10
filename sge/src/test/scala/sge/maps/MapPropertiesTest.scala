/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package maps

import sge.utils.Nullable

class MapPropertiesTest extends munit.FunSuite {

  test("get returns Nullable.empty for missing key") {
    val props = MapProperties()
    assert(props.get("missing").isEmpty)
  }

  test("get returns Nullable value for present key") {
    val props = MapProperties()
    props.put("key", "value")
    assert(props.get("key").isDefined)
    assertEquals(props.get("key").get.asInstanceOf[String], "value")
  }

  test("getAs returns typed Nullable") {
    val props = MapProperties()
    props.put("width", Integer.valueOf(100))
    val result: Nullable[Integer] = props.getAs[Integer]("width")
    assert(result.isDefined)
    assertEquals(result.get.intValue(), 100)
  }

  test("getAs returns empty for missing key") {
    val props = MapProperties()
    val result: Nullable[Integer] = props.getAs[Integer]("missing")
    assert(result.isEmpty)
  }

  test("getAs with default returns value when present") {
    val props = MapProperties()
    props.put("x", Integer.valueOf(42))
    assertEquals(props.getAs[Integer]("x", Integer.valueOf(0)).intValue(), 42)
  }

  test("getAs with default returns default when missing") {
    val props = MapProperties()
    assertEquals(props.getAs[Integer]("x", Integer.valueOf(99)).intValue(), 99)
  }

  test("containsKey") {
    val props = MapProperties()
    props.put("a", "1")
    assert(props.containsKey("a"))
    assert(!props.containsKey("b"))
  }

  test("remove") {
    val props = MapProperties()
    props.put("a", "1")
    props.remove("a")
    assert(!props.containsKey("a"))
  }

  test("putAll") {
    val p1 = MapProperties()
    p1.put("a", "1")
    val p2 = MapProperties()
    p2.put("b", "2")
    p1.putAll(p2)
    assert(p1.containsKey("a"))
    assert(p1.containsKey("b"))
  }
}
