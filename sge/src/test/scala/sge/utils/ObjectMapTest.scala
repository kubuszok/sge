/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Includes stress tests ported from libGDX MixedPutRemoveTest.java
 */
package sge
package utils

class ObjectMapTest extends munit.FunSuite {

  // ---- Construction ----

  test("default construction creates empty map") {
    val map = ObjectMap[String, Int]()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("construction with capacity") {
    val map = ObjectMap[String, Int](128)
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("construction with capacity and load factor") {
    val map = ObjectMap[String, Int](64, 0.6f)
    assertEquals(map.size, 0)
    assertEquals(map.loadFactor, 0.6f)
  }

  test("invalid load factor throws") {
    intercept[IllegalArgumentException] {
      ObjectMap[String, Int](16, 0f)
    }
    intercept[IllegalArgumentException] {
      ObjectMap[String, Int](16, 1f)
    }
  }

  // ---- Put / Get / Remove ----

  test("put and get basic") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.put("c", 3)
    assertEquals(map.size, 3)
    assertEquals(map.get("a").getOrElse(fail("expected non-empty")), 1)
    assertEquals(map.get("b").getOrElse(fail("expected non-empty")), 2)
    assertEquals(map.get("c").getOrElse(fail("expected non-empty")), 3)
  }

  test("put overwrites existing key") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    val old = map.put("a", 99)
    assert(old.isDefined)
    assertEquals(old.getOrElse(fail("expected non-empty")), 1)
    assertEquals(map.get("a").getOrElse(fail("expected non-empty")), 99)
    assertEquals(map.size, 1)
  }

  test("put returns Nullable.empty for new key") {
    val map    = ObjectMap[String, Int]()
    val result = map.put("a", 1)
    assert(result.isEmpty)
  }

  test("get with default value") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    assertEquals(map.get("a", -1), 1)
    assertEquals(map.get("missing", -1), -1)
  }

  test("get for missing key returns Nullable.empty") {
    val map = ObjectMap[String, Int]()
    assert(map.get("missing").isEmpty)
  }

  test("remove existing key") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    val removed = map.remove("a")
    assert(removed.isDefined)
    assertEquals(removed.getOrElse(fail("expected non-empty")), 1)
    assertEquals(map.size, 1)
    assert(map.get("a").isEmpty)
  }

  test("remove missing key returns Nullable.empty") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    val removed = map.remove("missing")
    assert(removed.isEmpty)
    assertEquals(map.size, 1)
  }

  // ---- containsKey / containsValue / findKey ----

  test("containsKey") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    assert(map.containsKey("a"))
    assert(!map.containsKey("b"))
  }

  test("containsValue") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    assert(map.containsValue(1))
    assert(map.containsValue(2))
    assert(!map.containsValue(99))
  }

  test("findKey") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    val key = map.findKey(2)
    assert(key.isDefined)
    assertEquals(key.getOrElse(fail("expected non-empty")), "b")
    assert(map.findKey(99).isEmpty)
  }

  // ---- Iteration ----

  test("foreachEntry visits all entries") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.put("c", 3)
    var count = 0
    var sum   = 0
    map.foreachEntry { (k, v) =>
      count += 1
      sum += v
    }
    assertEquals(count, 3)
    assertEquals(sum, 6)
  }

  test("foreachKey visits all keys") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    val keys = scala.collection.mutable.Set[String]()
    map.foreachKey(keys.add)
    assertEquals(keys.toSet, Set("a", "b"))
  }

  test("foreachValue visits all values") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    val values = scala.collection.mutable.Set[Int]()
    map.foreachValue(values.add)
    assertEquals(values.toSet, Set(1, 2))
  }

  // ---- Bulk operations ----

  test("putAll from another ObjectMap") {
    val map1 = ObjectMap[String, Int]()
    map1.put("a", 1)
    val map2 = ObjectMap[String, Int]()
    map2.put("b", 2)
    map2.put("c", 3)
    map1.putAll(map2)
    assertEquals(map1.size, 3)
    assertEquals(map1.get("b").getOrElse(fail("expected non-empty")), 2)
  }

  test("clear") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    map.put("b", 2)
    map.clear()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
    assert(map.get("a").isEmpty)
  }

  test("clear on empty map is no-op") {
    val map = ObjectMap[String, Int]()
    map.clear()
    assertEquals(map.size, 0)
  }

  test("clear with maximum capacity") {
    val map = ObjectMap[String, Int]()
    for (i <- 0 until 100)
      map.put(s"key$i", i)
    map.clear(10)
    assertEquals(map.size, 0)
  }

  test("ensureCapacity") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    // Should not throw
    map.ensureCapacity(1000)
    assertEquals(map.size, 1)
    assertEquals(map.get("a").getOrElse(fail("expected non-empty")), 1)
  }

  test("shrink reduces table size") {
    val map = ObjectMap[String, Int](1000)
    map.put("a", 1)
    map.shrink(2)
    assertEquals(map.size, 1)
    assertEquals(map.get("a").getOrElse(fail("expected non-empty")), 1)
  }

  test("shrink with negative capacity throws") {
    val map = ObjectMap[String, Int]()
    intercept[IllegalArgumentException] {
      map.shrink(-1)
    }
  }

  // ---- isEmpty / nonEmpty ----

  test("isEmpty and nonEmpty") {
    val map = ObjectMap[String, Int]()
    assert(map.isEmpty)
    assert(!map.nonEmpty)
    map.put("a", 1)
    assert(!map.isEmpty)
    assert(map.nonEmpty)
    map.remove("a")
    assert(map.isEmpty)
    assert(!map.nonEmpty)
  }

  // ---- equals and hashCode ----

  test("equal maps have same hashCode") {
    val map1 = ObjectMap[String, Int]()
    map1.put("a", 1)
    map1.put("b", 2)
    val map2 = ObjectMap[String, Int]()
    map2.put("a", 1)
    map2.put("b", 2)
    assertEquals(map1, map2)
    assertEquals(map1.hashCode(), map2.hashCode())
  }

  test("unequal maps by value") {
    val map1 = ObjectMap[String, Int]()
    map1.put("a", 1)
    val map2 = ObjectMap[String, Int]()
    map2.put("a", 2)
    assert(map1 != map2)
  }

  test("unequal maps by size") {
    val map1 = ObjectMap[String, Int]()
    map1.put("a", 1)
    val map2 = ObjectMap[String, Int]()
    map2.put("a", 1)
    map2.put("b", 2)
    assert(map1 != map2)
  }

  test("map does not equal non-map") {
    val map = ObjectMap[String, Int]()
    assert(!map.equals("not a map"))
  }

  test("map equals itself") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    assert(map == map)
  }

  // ---- from (copy constructor) ----

  test("from creates independent copy") {
    val original = ObjectMap[String, Int]()
    original.put("a", 1)
    original.put("b", 2)
    val copy = ObjectMap.from(original)
    assertEquals(copy.size, 2)
    assertEquals(copy.get("a").getOrElse(fail("expected non-empty")), 1)
    // Modifying copy does not affect original
    copy.put("a", 99)
    assertEquals(original.get("a").getOrElse(fail("expected non-empty")), 1)
  }

  // ---- toString ----

  test("toString for empty map") {
    val map = ObjectMap[String, Int]()
    assertEquals(map.toString, "{}")
  }

  test("toString for non-empty map contains entries") {
    val map = ObjectMap[String, Int]()
    map.put("a", 1)
    val s = map.toString
    assert(s.startsWith("{"))
    assert(s.endsWith("}"))
    assert(s.contains("a=1"))
  }

  // ---- Edge cases ----

  test("empty map operations") {
    val map = ObjectMap[String, Int]()
    assert(map.get("anything").isEmpty)
    assert(map.remove("anything").isEmpty)
    assert(!map.containsKey("anything"))
    assert(!map.containsValue(0))
    assert(map.findKey(0).isEmpty)
  }

  test("single entry operations") {
    val map = ObjectMap[String, Int]()
    map.put("only", 42)
    assertEquals(map.size, 1)
    assert(map.containsKey("only"))
    assert(map.containsValue(42))
    assertEquals(map.findKey(42).getOrElse(fail("expected non-empty")), "only")
    map.remove("only")
    assertEquals(map.size, 0)
  }

  test("many insertions trigger resize") {
    val map = ObjectMap[String, Int]()
    for (i <- 0 until 1000)
      map.put(s"key$i", i)
    assertEquals(map.size, 1000)
    for (i <- 0 until 1000)
      assertEquals(map.get(s"key$i").getOrElse(fail("expected non-empty")), i)
  }

  test("many removals after insertions") {
    val map = ObjectMap[String, Int]()
    for (i <- 0 until 500)
      map.put(s"key$i", i)
    for (i <- 0 until 250)
      map.remove(s"key$i")
    assertEquals(map.size, 250)
    for (i <- 250 until 500)
      assert(map.containsKey(s"key$i"))
  }

  // ---- Stress tests (ported from libGDX MixedPutRemoveTest) ----

  test("ObjectMap put stress test") {
    val sgeMap     = ObjectMap[Int, Int]()
    val jdkMap     = new java.util.HashMap[java.lang.Integer, java.lang.Integer]()
    var stateA     = 0L
    var stateB     = 1L
    var sgeRepeats = 0
    var jdkRepeats = 0

    for (i <- 0 until 0x100000) {
      // simple-ish RNG that repeats more than RandomXS128; we want repeats to test behavior
      stateA += 0xc6bc279692b5c323L
      val temp = (stateA ^ (stateA >>> 31)) * { stateB += 0x9e3779b97f4a7c16L; stateB }
      val item = (temp & (temp >>> 24)).toInt

      if (sgeMap.put(item, i).isDefined) sgeRepeats += 1
      if (jdkMap.put(item, i) != null) jdkRepeats += 1
      assertEquals(sgeMap.size, jdkMap.size())
    }
    assertEquals(sgeRepeats, jdkRepeats)
  }

  test("ObjectMap mixed put/remove stress test") {
    val sgeMap      = ObjectMap[Int, Int]()
    val jdkMap      = new java.util.HashMap[java.lang.Integer, java.lang.Integer]()
    var stateA      = 0L
    var stateB      = 1L
    var sgeRemovals = 0
    var jdkRemovals = 0

    for (i <- 0 until 0x100000) {
      // simple-ish RNG that repeats more than RandomXS128; we want repeats to test behavior
      stateA += 0xc6bc279692b5c323L
      val temp = (stateA ^ (stateA >>> 31)) * { stateB += 0x9e3779b97f4a7c16L; stateB }
      val item = (temp & (temp >>> 24)).toInt

      if (sgeMap.remove(item).isEmpty) {
        sgeMap.put(item, i)
      } else {
        sgeRemovals += 1
      }
      if (jdkMap.remove(item) == null) {
        jdkMap.put(item, i)
      } else {
        jdkRemovals += 1
      }
      assertEquals(sgeMap.size, jdkMap.size())
    }
    assertEquals(sgeRemovals, jdkRemovals)
  }

  test("ObjectMap sequential put and remove") {
    val map = ObjectMap[Int, Int]()
    // Put 1000 items
    for (i <- 0 until 1000)
      map.put(i, i * 10)
    assertEquals(map.size, 1000)

    // Remove odd keys
    for (i <- 0 until 1000 by 2) {
      val removed = map.remove(i)
      assert(removed.isDefined)
      assertEquals(removed.getOrElse(fail("expected non-empty")), i * 10)
    }
    assertEquals(map.size, 500)

    // Verify only odd keys remain
    for (i <- 0 until 1000)
      if (i % 2 == 0) {
        assert(map.get(i).isEmpty)
      } else {
        assertEquals(map.get(i).getOrElse(fail("expected non-empty")), i * 10)
      }
  }

  test("ObjectMap collision handling with same hash") {
    // Objects with the same hashCode to test collision resolution
    val map = ObjectMap[ObjectMapTest.BadHash, Int]()
    import ObjectMapTest.BadHash
    for (i <- 0 until 50)
      map.put(new BadHash(i), i)
    assertEquals(map.size, 50)
    for (i <- 0 until 50)
      assertEquals(map.get(new BadHash(i)).getOrElse(fail("expected non-empty")), i)
    // Remove half
    for (i <- 0 until 25)
      map.remove(new BadHash(i))
    assertEquals(map.size, 25)
    for (i <- 25 until 50)
      assertEquals(map.get(new BadHash(i)).getOrElse(fail("expected non-empty")), i)
  }
}

object ObjectMapTest {

  /** Class where all instances have the same hashCode, to test collision resolution. */
  class BadHash(val value: Int) {
    override def hashCode():       Int     = 42
    override def equals(obj: Any): Boolean = obj match {
      case other: BadHash => value == other.value
      case _ => false
    }
  }
}
