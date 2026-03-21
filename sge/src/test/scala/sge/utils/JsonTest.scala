/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import sge.utils.{ readFromString, writeToString }
import sge.utils.given

class JsonTest extends munit.FunSuite {

  // ---- Typed codec (JsonReader equivalent) ----

  final private case class SimpleModel(name: String, version: Int, active: Boolean)
  private given JsonCodec[SimpleModel] = JsonCodec.derive

  test("parse simple object via typed codec") {
    val json = """{"name":"sge","version":2,"active":true}"""
    val m    = readFromString[SimpleModel](json)
    assertEquals(m.name, "sge")
    assertEquals(m.version, 2)
    assert(m.active)
  }

  test("round-trip typed codec preserves values") {
    val original = SimpleModel("test", 42, active = false)
    val json     = writeToString(original)
    val restored = readFromString[SimpleModel](json)
    assertEquals(restored, original)
  }

  final private case class Nested(label: String, child: SimpleModel)
  private given JsonCodec[Nested] = JsonCodec.derive

  test("parse nested objects via typed codec") {
    val json = """{"label":"outer","child":{"name":"inner","version":1,"active":false}}"""
    val n    = readFromString[Nested](json)
    assertEquals(n.label, "outer")
    assertEquals(n.child.name, "inner")
    assertEquals(n.child.version, 1)
    assert(!n.child.active)
  }

  final private case class WithList(items: List[Int])
  private given JsonCodec[WithList] = JsonCodec.derive

  test("parse array via typed codec") {
    val json = """{"items":[10,20,30]}"""
    val w    = readFromString[WithList](json)
    assertEquals(w.items, List(10, 20, 30))
  }

  // ---- Json AST (JsonValue equivalent) ----

  private def parseJson(s: String): Json = readFromString[Json](s)

  test("Json AST: object field access and pattern matching") {
    val root = parseJson("""{"name":"sge","count":3,"enabled":true}""")
    root match {
      case Json.Obj(obj) =>
        var nameFound    = false
        var countFound   = false
        var enabledFound = false
        obj.fields.foreach { case (k, v) =>
          k match {
            case "name" =>
              nameFound = true
              assert(v.isInstanceOf[Json.Str])
              val Json.Str(s) = v: @unchecked
              assertEquals(s, "sge")
            case "count" =>
              countFound = true
              assert(v.isInstanceOf[Json.Num])
              val Json.Num(n) = v: @unchecked
              assertEquals(n.toDouble.map(_.toInt), Some(3))
            case "enabled" =>
              enabledFound = true
              assert(v.isInstanceOf[Json.Bool])
              val Json.Bool(b) = v: @unchecked
              assert(b)
            case other => fail(s"unexpected field: $other")
          }
        }
        assert(nameFound, "name field not found")
        assert(countFound, "count field not found")
        assert(enabledFound, "enabled field not found")
      case _ => fail("expected Json.Obj")
    }
  }

  test("Json AST: array iteration") {
    val root = parseJson("""[1,2,3]""")
    root match {
      case Json.Arr(values) =>
        assertEquals(values.length, 3)
        val ints = values.collect { case Json.Num(n) => n.toDouble.map(_.toInt).getOrElse(0) }
        assertEquals(ints.toList, List(1, 2, 3))
      case _ => fail("expected Json.Arr")
    }
  }

  test("Json AST: null value") {
    val root = parseJson("""{"x":null}""")
    root match {
      case Json.Obj(obj) =>
        obj.fields.foreach { case (k, v) =>
          assertEquals(k, "x")
          assert(v == Json.Null, s"expected Json.Null, got $v")
        }
      case _ => fail("expected Json.Obj")
    }
  }

  test("Json AST: empty object and array") {
    parseJson("{}") match {
      case Json.Obj(obj) => assert(obj.fields.isEmpty)
      case other         => fail(s"expected Json.Obj, got $other")
    }
    parseJson("[]") match {
      case Json.Arr(values) => assertEquals(values.length, 0)
      case other            => fail(s"expected Json.Arr, got $other")
    }
  }

  test("Json AST: nested structure") {
    val root = parseJson("""{"a":{"b":[1,true,"s"]}}""")
    root match {
      case Json.Obj(outer) =>
        var found = false
        outer.fields.foreach { case (k, v) =>
          assertEquals(k, "a")
          v match {
            case Json.Obj(inner) =>
              inner.fields.foreach { case (k2, v2) =>
                assertEquals(k2, "b")
                v2 match {
                  case Json.Arr(elems) =>
                    assertEquals(elems.length, 3)
                    found = true
                  case _ => fail("expected Json.Arr")
                }
              }
            case _ => fail("expected inner Json.Obj")
          }
        }
        assert(found, "did not reach nested array")
      case _ => fail("expected Json.Obj")
    }
  }

  test("Json AST: round-trip preserves structure") {
    val input    = """{"key":"value","nums":[1,2],"flag":false}"""
    val ast      = parseJson(input)
    val output   = writeToString(ast)
    val reparsed = parseJson(output)
    assertEquals(ast, reparsed)
  }
}
