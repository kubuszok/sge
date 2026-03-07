/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

class JsonValueTest extends munit.FunSuite {

  // ---- Construction from primitives ----

  test("construct string value") {
    val jv = new JsonValue(Nullable("hello"))
    assert(jv.isString)
    assertEquals(jv.asString().getOrElse(fail("expected non-empty")), "hello")
  }

  test("construct null string creates null value") {
    val jv = new JsonValue(Nullable.empty[String])
    assert(jv.isNull)
  }

  test("construct double value") {
    val jv = new JsonValue(3.14)
    assert(jv.isDouble)
    assert(jv.isNumber)
    assertEqualsDouble(jv.asDouble(), 3.14, 0.001)
  }

  test("construct long value") {
    val jv = new JsonValue(42L)
    assert(jv.isLong)
    assert(jv.isNumber)
    assertEquals(jv.asLong(), 42L)
  }

  test("construct double with string representation") {
    val jv = new JsonValue(3.14, Nullable("3.14"))
    assert(jv.isDouble)
    assertEquals(jv.asString().getOrElse(fail("expected non-empty")), "3.14")
  }

  test("construct long with string representation") {
    val jv = new JsonValue(100L, Nullable("100"))
    assert(jv.isLong)
    assertEquals(jv.asString().getOrElse(fail("expected non-empty")), "100")
  }

  test("construct boolean true") {
    val jv = new JsonValue(true)
    assert(jv.isBoolean)
    assert(jv.asBoolean())
  }

  test("construct boolean false") {
    val jv = new JsonValue(false)
    assert(jv.isBoolean)
    assert(!jv.asBoolean())
  }

  // ---- Type checking ----

  test("isObject") {
    val jv = new JsonValue(JsonValue.ValueType.`object`)
    assert(jv.isObject)
    assert(!jv.isArray)
    assert(!jv.isString)
    assert(!jv.isNumber)
    assert(!jv.isBoolean)
    assert(!jv.isNull)
  }

  test("isArray") {
    val jv = new JsonValue(JsonValue.ValueType.array)
    assert(jv.isArray)
    assert(!jv.isObject)
  }

  test("isValue returns true for primitives") {
    assert(new JsonValue(Nullable("s")).isValue)
    assert(new JsonValue(1.0).isValue)
    assert(new JsonValue(1L).isValue)
    assert(new JsonValue(true).isValue)
    assert(new JsonValue(Nullable.empty[String]).isValue) // null value
  }

  test("isValue returns false for containers") {
    assert(!new JsonValue(JsonValue.ValueType.`object`).isValue)
    assert(!new JsonValue(JsonValue.ValueType.array).isValue)
  }

  // ---- Value conversion (as* methods) ----

  test("asString from string") {
    val jv = new JsonValue(Nullable("hello"))
    assertEquals(jv.asString().getOrElse(fail("expected non-empty")), "hello")
  }

  test("asString from double") {
    val jv = new JsonValue(3.14)
    // Should return string representation of the double
    val s = jv.asString()
    assert(s.isDefined)
  }

  test("asString from long") {
    val jv = new JsonValue(42L)
    val s  = jv.asString()
    assert(s.isDefined)
    assertEquals(s.getOrElse(fail("expected non-empty")), "42")
  }

  test("asString from boolean") {
    assertEquals(new JsonValue(true).asString().getOrElse(fail("expected non-empty")), "true")
    assertEquals(new JsonValue(false).asString().getOrElse(fail("expected non-empty")), "false")
  }

  test("asString from null returns empty") {
    val jv = new JsonValue(Nullable.empty[String])
    assert(jv.asString().isEmpty)
  }

  test("asString from object throws") {
    val jv = new JsonValue(JsonValue.ValueType.`object`)
    intercept[IllegalStateException] {
      jv.asString()
    }
  }

  test("asInt from long") {
    val jv = new JsonValue(42L)
    assertEquals(jv.asInt(), 42)
  }

  test("asInt from double") {
    val jv = new JsonValue(3.9)
    assertEquals(jv.asInt(), 3)
  }

  test("asInt from boolean") {
    assertEquals(new JsonValue(true).asInt(), 1)
    assertEquals(new JsonValue(false).asInt(), 0)
  }

  test("asFloat from double") {
    val jv = new JsonValue(2.5)
    assertEqualsFloat(jv.asFloat(), 2.5f, 0.001f)
  }

  test("asFloat from long") {
    val jv = new JsonValue(10L)
    assertEqualsFloat(jv.asFloat(), 10f, 0.001f)
  }

  test("asFloat from boolean") {
    assertEqualsFloat(new JsonValue(true).asFloat(), 1f, 0.001f)
    assertEqualsFloat(new JsonValue(false).asFloat(), 0f, 0.001f)
  }

  test("asDouble from long") {
    val jv = new JsonValue(100L)
    assertEqualsDouble(jv.asDouble(), 100.0, 0.001)
  }

  test("asDouble from boolean") {
    assertEqualsDouble(new JsonValue(true).asDouble(), 1.0, 0.001)
    assertEqualsDouble(new JsonValue(false).asDouble(), 0.0, 0.001)
  }

  test("asLong from double") {
    val jv = new JsonValue(3.9)
    assertEquals(jv.asLong(), 3L)
  }

  test("asBoolean from long") {
    assert(new JsonValue(1L).asBoolean())
    assert(!new JsonValue(0L).asBoolean())
  }

  test("asBoolean from double") {
    assert(new JsonValue(1.0).asBoolean())
    assert(!new JsonValue(0.0).asBoolean())
  }

  test("asBoolean from string") {
    val jv = new JsonValue(Nullable("true"))
    assert(jv.asBoolean())
    assert(!new JsonValue(Nullable("false")).asBoolean())
  }

  test("asByte from long") {
    val jv = new JsonValue(42L)
    assertEquals(jv.asByte(), 42.toByte)
  }

  test("asShort from long") {
    val jv = new JsonValue(1000L)
    assertEquals(jv.asShort(), 1000.toShort)
  }

  test("asChar from string") {
    val jv = new JsonValue(Nullable("A"))
    assertEquals(jv.asChar(), 'A')
  }

  test("asChar from empty string returns 0") {
    val jv = new JsonValue(Nullable(""))
    assertEquals(jv.asChar(), 0.toChar)
  }

  // ---- Child navigation (object) ----

  private def makeObject(): JsonValue = {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    val a   = new JsonValue(Nullable("valueA"))
    a.name = Nullable("a")
    obj.addChild(a)
    val b = new JsonValue(42L)
    b.name = Nullable("b")
    obj.addChild(b)
    val c = new JsonValue(true)
    c.name = Nullable("c")
    obj.addChild(c)
    obj
  }

  test("get by name") {
    val obj   = makeObject()
    val child = obj.get("b")
    assert(child.isDefined)
    assertEquals(child.getOrElse(fail("expected non-empty")).asLong(), 42L)
  }

  test("get by name returns empty for missing") {
    val obj = makeObject()
    assert(obj.get("missing").isEmpty)
  }

  test("has by name") {
    val obj = makeObject()
    assert(obj.has("a"))
    assert(!obj.has("missing"))
  }

  test("child returns first child") {
    val obj = makeObject()
    assert(obj.child.isDefined)
    assertEquals(obj.child.getOrElse(fail("expected non-empty")).name.getOrElse(fail("expected non-empty")), "a")
  }

  test("next traversal") {
    val obj   = makeObject()
    val first = obj.child.getOrElse(fail("expected non-empty"))
    assertEquals(first.name.getOrElse(fail("expected non-empty")), "a")
    val second = first.next.getOrElse(fail("expected non-empty"))
    assertEquals(second.name.getOrElse(fail("expected non-empty")), "b")
    val third = second.next.getOrElse(fail("expected non-empty"))
    assertEquals(third.name.getOrElse(fail("expected non-empty")), "c")
    assert(third.next.isEmpty)
  }

  test("prev traversal") {
    val obj  = makeObject()
    val last = obj.lastChild.getOrElse(fail("expected non-empty"))
    assertEquals(last.name.getOrElse(fail("expected non-empty")), "c")
    val middle = last.prev.getOrElse(fail("expected non-empty"))
    assertEquals(middle.name.getOrElse(fail("expected non-empty")), "b")
    val first = middle.prev.getOrElse(fail("expected non-empty"))
    assertEquals(first.name.getOrElse(fail("expected non-empty")), "a")
    assert(first.prev.isEmpty)
  }

  test("size of object") {
    val obj = makeObject()
    assertEquals(obj.size, 3)
  }

  test("require by name returns child") {
    val obj   = makeObject()
    val child = obj.require("a")
    assertEquals(child.asString().getOrElse(fail("expected non-empty")), "valueA")
  }

  test("require by name throws for missing") {
    val obj = makeObject()
    intercept[IllegalArgumentException] {
      obj.require("missing")
    }
  }

  // ---- Child navigation (array) ----

  private def makeArray(): JsonValue = {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(Nullable("first")))
    arr.addChild(new JsonValue(Nullable("second")))
    arr.addChild(new JsonValue(Nullable("third")))
    arr
  }

  test("get by index") {
    val arr = makeArray()
    assertEquals(arr.get(0).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "first")
    assertEquals(arr.get(1).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "second")
    assertEquals(arr.get(2).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "third")
  }

  test("get last index uses lastChild shortcut") {
    val arr = makeArray()
    // Index == size-1 should use the lastChild optimization
    assertEquals(arr.get(2).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "third")
  }

  test("get out of bounds returns empty") {
    val arr = makeArray()
    assert(arr.get(10).isEmpty)
  }

  test("require by index") {
    val arr = makeArray()
    assertEquals(arr.require(0).asString().getOrElse(fail("expected non-empty")), "first")
  }

  test("require by index throws for missing") {
    val arr = makeArray()
    intercept[IllegalArgumentException] {
      arr.require(99)
    }
  }

  // ---- Array iteration ----

  test("forEach on array nodes") {
    val arr    = makeArray()
    val values = scala.collection.mutable.ListBuffer[String]()
    arr.foreach(child => values += child.asString().getOrElse(fail("expected non-empty")))
    assertEquals(values.toList, List("first", "second", "third"))
  }

  test("iterator on array") {
    val arr = makeArray()
    val it  = arr.iterator
    assert(it.hasNext)
    assertEquals(it.next().asString().getOrElse(fail("expected non-empty")), "first")
    assertEquals(it.next().asString().getOrElse(fail("expected non-empty")), "second")
    assertEquals(it.next().asString().getOrElse(fail("expected non-empty")), "third")
    assert(!it.hasNext)
  }

  test("iterator on empty array") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    assert(!arr.iterator.hasNext)
  }

  test("iterator next on exhausted throws") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    val it  = arr.iterator
    intercept[java.util.NoSuchElementException] {
      it.next()
    }
  }

  // ---- Mutation ----

  test("addChild adds to end") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(Nullable("a")))
    arr.addChild(new JsonValue(Nullable("b")))
    assertEquals(arr.size, 2)
    assertEquals(arr.get(0).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "a")
    assertEquals(arr.get(1).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "b")
  }

  test("addChild with name for object") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    obj.addChild("key", new JsonValue(Nullable("val")))
    assertEquals(obj.size, 1)
    assertEquals(obj.get("key").getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "val")
  }

  test("addChildFirst adds to beginning") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(Nullable("b")))
    arr.addChildFirst(new JsonValue(Nullable("a")))
    assertEquals(arr.get(0).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "a")
    assertEquals(arr.get(1).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "b")
  }

  test("remove by index") {
    val arr     = makeArray()
    val removed = arr.remove(1)
    assert(removed.isDefined)
    assertEquals(removed.getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "second")
    assertEquals(arr.size, 2)
  }

  test("remove by name") {
    val obj     = makeObject()
    val removed = obj.remove("b")
    assert(removed.isDefined)
    assertEquals(removed.getOrElse(fail("expected non-empty")).asLong(), 42L)
    assertEquals(obj.size, 2)
    assert(!obj.has("b"))
  }

  test("remove missing returns empty") {
    val obj = makeObject()
    assert(obj.remove("missing").isEmpty)
    assertEquals(obj.size, 3)
  }

  test("setChild replaces existing or adds new") {
    val obj         = makeObject()
    val replacement = new JsonValue(Nullable("replaced"))
    obj.setChild("a", replacement)
    assertEquals(obj.get("a").getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "replaced")
    assertEquals(obj.size, 3) // size unchanged, replaced in-place

    val newChild = new JsonValue(Nullable("new"))
    obj.setChild("d", newChild)
    assertEquals(obj.size, 4)
    assertEquals(obj.get("d").getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "new")
  }

  // ---- Named child getters (with default) ----

  test("getString with default") {
    val obj = makeObject()
    assertEquals(obj.getString("a", Nullable("default")).getOrElse(fail("expected non-empty")), "valueA")
    assertEquals(obj.getString("missing", Nullable("default")).getOrElse(fail("expected non-empty")), "default")
  }

  test("getInt with default") {
    val obj = makeObject()
    assertEquals(obj.getInt("b", -1), 42)
    assertEquals(obj.getInt("missing", -1), -1)
  }

  test("getBoolean with default") {
    val obj = makeObject()
    assertEquals(obj.getBoolean("c", false), true)
    assertEquals(obj.getBoolean("missing", false), false)
  }

  test("getFloat with default") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    val f   = new JsonValue(2.5)
    f.name = Nullable("f")
    obj.addChild(f)
    assertEqualsFloat(obj.getFloat("f", 0f), 2.5f, 0.001f)
    assertEqualsFloat(obj.getFloat("missing", 9.9f), 9.9f, 0.001f)
  }

  test("getDouble with default") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    val d   = new JsonValue(3.14)
    d.name = Nullable("d")
    obj.addChild(d)
    assertEqualsDouble(obj.getDouble("d", 0.0), 3.14, 0.001)
    assertEqualsDouble(obj.getDouble("missing", 9.9), 9.9, 0.001)
  }

  test("getLong with default") {
    val obj = makeObject()
    assertEquals(obj.getLong("b", -1L), 42L)
    assertEquals(obj.getLong("missing", -1L), -1L)
  }

  // ---- Named child getters (required) ----

  test("getString required") {
    val obj = makeObject()
    assertEquals(obj.getString("a").getOrElse(fail("expected non-empty")), "valueA")
  }

  test("getString required throws for missing") {
    val obj = makeObject()
    intercept[IllegalArgumentException] {
      obj.getString("missing")
    }
  }

  test("getInt required") {
    val obj = makeObject()
    assertEquals(obj.getInt("b"), 42)
  }

  test("getBoolean required") {
    val obj = makeObject()
    assertEquals(obj.getBoolean("c"), true)
  }

  // ---- Nested structures ----

  test("nested object") {
    val root   = new JsonValue(JsonValue.ValueType.`object`)
    val nested = new JsonValue(JsonValue.ValueType.`object`)
    val inner  = new JsonValue(Nullable("innerValue"))
    inner.name = Nullable("key")
    nested.addChild(inner)
    nested.name = Nullable("nested")
    root.addChild(nested)

    val result = root.get("nested").getOrElse(fail("expected non-empty")).get("key")
    assert(result.isDefined)
    assertEquals(result.getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "innerValue")
  }

  test("array of objects") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    for (i <- 0 until 3) {
      val obj  = new JsonValue(JsonValue.ValueType.`object`)
      val name = new JsonValue(Nullable(s"item$i"))
      name.name = Nullable("name")
      obj.addChild(name)
      arr.addChild(obj)
    }
    assertEquals(arr.size, 3)
    assertEquals(
      arr.get(1).getOrElse(fail("expected non-empty")).get("name").getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")),
      "item1"
    )
  }

  test("object with array child") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.name = Nullable("items")
    arr.addChild(new JsonValue(1L))
    arr.addChild(new JsonValue(2L))
    arr.addChild(new JsonValue(3L))
    obj.addChild(arr)

    val items = obj.get("items")
    assert(items.isDefined)
    assertEquals(items.getOrElse(fail("expected non-empty")).size, 3)
    assertEquals(items.getOrElse(fail("expected non-empty")).get(1).getOrElse(fail("expected non-empty")).asLong(), 2L)
  }

  // ---- hasChild / getChild ----

  test("hasChild checks for grandchildren") {
    val obj    = new JsonValue(JsonValue.ValueType.`object`)
    val parent = new JsonValue(JsonValue.ValueType.`object`)
    parent.name = Nullable("parent")
    val child = new JsonValue(Nullable("value"))
    child.name = Nullable("child")
    parent.addChild(child)
    obj.addChild(parent)

    assert(obj.hasChild("parent"))
    assert(!obj.hasChild("missing"))
  }

  test("getChild returns first child of named child") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.name = Nullable("list")
    arr.addChild(new JsonValue(Nullable("first")))
    arr.addChild(new JsonValue(Nullable("second")))
    obj.addChild(arr)

    val firstChild = obj.getChild("list")
    assert(firstChild.isDefined)
    assertEquals(firstChild.getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "first")
    assert(obj.getChild("missing").isEmpty)
  }

  // ---- isEmpty / notEmpty ----

  test("isEmpty and notEmpty for containers") {
    val empty = new JsonValue(JsonValue.ValueType.array)
    assert(empty.isEmpty)
    assert(!empty.notEmpty)

    empty.addChild(new JsonValue(1L))
    assert(!empty.isEmpty)
    assert(empty.notEmpty)
  }

  // ---- Set methods ----

  test("set from another JsonValue") {
    val source = new JsonValue(Nullable("hello"))
    val target = new JsonValue(42L)
    target.set(source)
    assert(target.isString)
    assertEquals(target.asString().getOrElse(fail("expected non-empty")), "hello")
  }

  test("set to null") {
    val jv = new JsonValue(Nullable("hello"))
    jv.setNull()
    assert(jv.isNull)
  }

  test("set string value") {
    val jv = new JsonValue(42L)
    jv.set(Nullable("changed"))
    assert(jv.isString)
    assertEquals(jv.asString().getOrElse(fail("expected non-empty")), "changed")
  }

  test("set double value") {
    val jv = new JsonValue(42L)
    jv.set(3.14, Nullable.empty[String])
    assert(jv.isDouble)
    assertEqualsDouble(jv.asDouble(), 3.14, 0.001)
  }

  test("set long value") {
    val jv = new JsonValue(3.14)
    jv.set(99L, Nullable.empty[String])
    assert(jv.isLong)
    assertEquals(jv.asLong(), 99L)
  }

  test("set boolean value") {
    val jv = new JsonValue(42L)
    jv.set(true)
    assert(jv.isBoolean)
    assert(jv.asBoolean())
  }

  // ---- Comparison helpers ----

  test("equalsString") {
    val jv = new JsonValue(Nullable("hello"))
    assert(jv.equalsString("hello"))
    assert(!jv.equalsString("world"))
  }

  test("nameEquals") {
    val jv = new JsonValue(Nullable("val"))
    jv.name = Nullable("myName")
    assert(jv.nameEquals("myName"))
    assert(!jv.nameEquals("other"))
  }

  test("nameEquals with no name") {
    val jv = new JsonValue(Nullable("val"))
    assert(!jv.nameEquals("anything"))
  }

  // ---- toString ----

  test("toString for value without name") {
    val jv = new JsonValue(Nullable("hello"))
    assertEquals(jv.toString, "hello")
  }

  test("toString for value with name") {
    val jv = new JsonValue(Nullable("world"))
    jv.name = Nullable("greeting")
    assertEquals(jv.toString, "greeting: world")
  }

  test("toString for null value") {
    val jv = new JsonValue(Nullable.empty[String])
    assertEquals(jv.toString, "null")
  }

  test("toString for boolean") {
    assertEquals(new JsonValue(true).toString, "true")
    assertEquals(new JsonValue(false).toString, "false")
  }

  // ---- toJson ----

  test("toJson for simple values") {
    assertEquals(new JsonValue(Nullable("hello")).toJson(), "hello")
    assertEquals(new JsonValue(true).toJson(), "true")
    assertEquals(new JsonValue(false).toJson(), "false")
    assertEquals(new JsonValue(Nullable.empty[String]).toJson(), "null")
  }

  test("toJson for object") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    val a   = new JsonValue(Nullable("val"))
    a.name = Nullable("key")
    obj.addChild(a)
    val json = obj.toJson()
    assert(json.contains("\"key\":\"val\""))
  }

  test("toJson for array") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(1L))
    arr.addChild(new JsonValue(2L))
    arr.addChild(new JsonValue(3L))
    assertEquals(arr.toJson(), "[1,2,3]")
  }

  // ---- Nullable handling for missing keys ----

  test("get on empty object returns empty") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    assert(obj.get("anything").isEmpty)
  }

  test("parent is set when addChild is called") {
    val obj   = new JsonValue(JsonValue.ValueType.`object`)
    val child = new JsonValue(Nullable("val"))
    child.name = Nullable("key")
    obj.addChild(child)
    assert(child.parent.isDefined)
  }

  // ---- trace ----

  test("trace for root") {
    val obj = new JsonValue(JsonValue.ValueType.`object`)
    assertEquals(obj.trace(), "{}")
  }

  test("trace for root array") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    assertEquals(arr.trace(), "[]")
  }

  // ---- removeFromParent ----

  test("removeFromParent") {
    val arr    = makeArray()
    val middle = arr.get(1).getOrElse(fail("expected non-empty"))
    middle.removeFromParent()
    assertEquals(arr.size, 2)
    assertEquals(arr.get(0).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "first")
    assertEquals(arr.get(1).getOrElse(fail("expected non-empty")).asString().getOrElse(fail("expected non-empty")), "third")
  }

  // ---- Array conversion methods ----

  test("asStringArray") {
    val arr    = makeArray()
    val result = arr.asStringArray()
    assertEquals(result.toSeq, Seq("first", "second", "third"))
  }

  test("asIntArray") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(1L))
    arr.addChild(new JsonValue(2L))
    arr.addChild(new JsonValue(3L))
    assertEquals(arr.asIntArray().toSeq, Seq(1, 2, 3))
  }

  test("asFloatArray") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(1.5))
    arr.addChild(new JsonValue(2.5))
    val result = arr.asFloatArray()
    assertEqualsFloat(result(0), 1.5f, 0.001f)
    assertEqualsFloat(result(1), 2.5f, 0.001f)
  }

  test("asDoubleArray") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(1.5))
    arr.addChild(new JsonValue(2.5))
    val result = arr.asDoubleArray()
    assertEqualsDouble(result(0), 1.5, 0.001)
    assertEqualsDouble(result(1), 2.5, 0.001)
  }

  test("asLongArray") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(100L))
    arr.addChild(new JsonValue(200L))
    assertEquals(arr.asLongArray().toSeq, Seq(100L, 200L))
  }

  test("asBooleanArray") {
    val arr = new JsonValue(JsonValue.ValueType.array)
    arr.addChild(new JsonValue(true))
    arr.addChild(new JsonValue(false))
    assertEquals(arr.asBooleanArray().toSeq, Seq(true, false))
  }

  test("asStringArray on non-array throws") {
    val jv = new JsonValue(Nullable("notArray"))
    intercept[IllegalStateException] {
      jv.asStringArray()
    }
  }
}
