package sge
package textra
package utils

class CaseInsensitiveIntMapSuite extends munit.FunSuite {

  // ---------- construction ----------

  test("default constructor creates empty map") {
    val map = new CaseInsensitiveIntMap()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
    assert(!map.notEmpty)
  }

  test("constructor with initial capacity creates empty map") {
    val map = new CaseInsensitiveIntMap(100)
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  test("constructor with capacity and load factor") {
    val map = new CaseInsensitiveIntMap(100, 0.8f)
    assertEquals(map.size, 0)
    assertEquals(map.loadFactor, 0.8f)
  }

  test("constructor rejects invalid load factors") {
    intercept[IllegalArgumentException](new CaseInsensitiveIntMap(10, 0f))
    intercept[IllegalArgumentException](new CaseInsensitiveIntMap(10, 1f))
    intercept[IllegalArgumentException](new CaseInsensitiveIntMap(10, -0.5f))
  }

  test("constructor from arrays") {
    val keys   = Array("alpha", "beta", "gamma")
    val values = Array(1, 2, 3)
    val map    = new CaseInsensitiveIntMap(keys, values)
    assertEquals(map.size, 3)
    assertEquals(map.get("alpha", -1), 1)
    assertEquals(map.get("beta", -1), 2)
    assertEquals(map.get("gamma", -1), 3)
  }

  test("constructor from arrays with mismatched lengths uses shorter") {
    val keys   = Array("a", "b", "c", "d")
    val values = Array(1, 2)
    val map    = new CaseInsensitiveIntMap(keys, values)
    assertEquals(map.size, 2)
    assertEquals(map.get("a", -1), 1)
    assertEquals(map.get("b", -1), 2)
    assertEquals(map.get("c", -1), -1) // not added
  }

  test("copy constructor creates identical map") {
    val original = new CaseInsensitiveIntMap()
    original.put("hello", 10)
    original.put("world", 20)
    val copy = new CaseInsensitiveIntMap(original)
    assertEquals(copy.size, 2)
    assertEquals(copy.get("hello", -1), 10)
    assertEquals(copy.get("world", -1), 20)
  }

  test("copy constructor is independent of original") {
    val original = new CaseInsensitiveIntMap()
    original.put("key", 42)
    val copy = new CaseInsensitiveIntMap(original)
    original.put("key", 99)
    assertEquals(copy.get("key", -1), 42)
  }

  // ---------- put / get ----------

  test("put and get basic") {
    val map = new CaseInsensitiveIntMap()
    map.put("foo", 42)
    assertEquals(map.get("foo", -1), 42)
    assertEquals(map.size, 1)
  }

  test("put overwrites existing key") {
    val map = new CaseInsensitiveIntMap()
    map.put("key", 1)
    map.put("key", 2)
    assertEquals(map.get("key", -1), 2)
    assertEquals(map.size, 1)
  }

  test("put with default returns old value on overwrite") {
    val map = new CaseInsensitiveIntMap()
    val d1  = map.put("key", 10, -1)
    assertEquals(d1, -1) // default because key was new
    val d2 = map.put("key", 20, -1)
    assertEquals(d2, 10) // old value
    assertEquals(map.get("key", -1), 20)
  }

  test("get returns default for missing key") {
    val map = new CaseInsensitiveIntMap()
    assertEquals(map.get("missing", -999), -999)
  }

  // ---------- case insensitivity ----------

  test("get is case-insensitive") {
    val map = new CaseInsensitiveIntMap()
    map.put("Hello", 1)
    assertEquals(map.get("hello", -1), 1)
    assertEquals(map.get("HELLO", -1), 1)
    assertEquals(map.get("hElLo", -1), 1)
  }

  test("put with different case overwrites") {
    val map = new CaseInsensitiveIntMap()
    map.put("ABC", 10)
    map.put("abc", 20)
    assertEquals(map.size, 1)
    assertEquals(map.get("ABC", -1), 20)
  }

  test("containsKey is case-insensitive") {
    val map = new CaseInsensitiveIntMap()
    map.put("Test", 5)
    assert(map.containsKey("test"))
    assert(map.containsKey("TEST"))
    assert(map.containsKey("Test"))
    assert(!map.containsKey("other"))
  }

  // ---------- remove ----------

  test("remove returns value for existing key") {
    val map = new CaseInsensitiveIntMap()
    map.put("key", 42)
    val removed = map.remove("key", -1)
    assertEquals(removed, 42)
    assertEquals(map.size, 0)
    assert(!map.containsKey("key"))
  }

  test("remove returns default for missing key") {
    val map = new CaseInsensitiveIntMap()
    assertEquals(map.remove("nope", -1), -1)
  }

  test("remove is case-insensitive") {
    val map = new CaseInsensitiveIntMap()
    map.put("Hello", 10)
    val removed = map.remove("HELLO", -1)
    assertEquals(removed, 10)
    assertEquals(map.size, 0)
  }

  // ---------- containsValue / findKey ----------

  test("containsValue finds present values") {
    val map = new CaseInsensitiveIntMap()
    map.put("a", 100)
    map.put("b", 200)
    assert(map.containsValue(100))
    assert(map.containsValue(200))
    assert(!map.containsValue(300))
  }

  test("findKey returns key for value") {
    val map = new CaseInsensitiveIntMap()
    map.put("alpha", 42)
    val found = map.findKey(42)
    assertEquals(found, "alpha")
  }

  test("findKey returns null for missing value") {
    val map = new CaseInsensitiveIntMap()
    map.put("alpha", 42)
    val found = map.findKey(99)
    assertEquals(found, null)
  }

  // ---------- sentinel value: -1 vs 0 ----------

  test("value 0 is stored and retrieved correctly") {
    val map = new CaseInsensitiveIntMap()
    map.put("zero", 0)
    assertEquals(map.get("zero", -1), 0)
    assert(map.containsKey("zero"))
  }

  test("value -1 is stored and retrieved correctly") {
    val map = new CaseInsensitiveIntMap()
    map.put("neg", -1)
    // Use a different default to distinguish from missing
    assertEquals(map.get("neg", Int.MinValue), -1)
    assert(map.containsKey("neg"))
  }

  test("get with -1 default can be ambiguous without containsKey") {
    val map = new CaseInsensitiveIntMap()
    map.put("sentinel", -1)
    // get returns -1, but so would a missing key with -1 default
    assertEquals(map.get("sentinel", -1), -1)
    assertEquals(map.get("missing", -1), -1)
    // Must use containsKey to disambiguate
    assert(map.containsKey("sentinel"))
    assert(!map.containsKey("missing"))
  }

  // ---------- getAndIncrement ----------

  test("getAndIncrement on missing key inserts default+increment") {
    val map   = new CaseInsensitiveIntMap()
    val value = map.getAndIncrement("counter", 0, 1)
    assertEquals(value, 0) // returns default
    assertEquals(map.get("counter", -1), 1) // stored default+increment
  }

  test("getAndIncrement on existing key returns old and increments") {
    val map = new CaseInsensitiveIntMap()
    map.put("counter", 5)
    val old = map.getAndIncrement("counter", 0, 3)
    assertEquals(old, 5)
    assertEquals(map.get("counter", -1), 8)
  }

  // ---------- clear ----------

  test("clear empties the map") {
    val map = new CaseInsensitiveIntMap()
    map.put("a", 1)
    map.put("b", 2)
    map.clear()
    assertEquals(map.size, 0)
    assert(map.isEmpty)
    assert(!map.containsKey("a"))
  }

  test("clear on empty map is a no-op") {
    val map = new CaseInsensitiveIntMap()
    map.clear()
    assertEquals(map.size, 0)
  }

  test("clear with capacity") {
    val map = new CaseInsensitiveIntMap()
    map.put("a", 1)
    map.put("b", 2)
    map.clear(10)
    assertEquals(map.size, 0)
    assert(map.isEmpty)
  }

  // ---------- putAll ----------

  test("putAll from arrays") {
    val map = new CaseInsensitiveIntMap()
    map.putAll(Array("x", "y", "z"), Array(10, 20, 30))
    assertEquals(map.size, 3)
    assertEquals(map.get("x", -1), 10)
    assertEquals(map.get("y", -1), 20)
    assertEquals(map.get("z", -1), 30)
  }

  test("putAll from another map") {
    val source = new CaseInsensitiveIntMap()
    source.put("a", 1)
    source.put("b", 2)
    val target = new CaseInsensitiveIntMap()
    target.put("c", 3)
    target.putAll(source)
    assertEquals(target.size, 3)
    assertEquals(target.get("a", -1), 1)
    assertEquals(target.get("b", -1), 2)
    assertEquals(target.get("c", -1), 3)
  }

  // ---------- iteration ----------

  test("entries iterator visits all entries") {
    val map = new CaseInsensitiveIntMap()
    map.put("one", 1)
    map.put("two", 2)
    map.put("three", 3)
    val entries = map.entries()
    var count   = 0
    var sum     = 0
    while (entries.hasNext) {
      val e = entries.next()
      sum += e.value
      count += 1
    }
    assertEquals(count, 3)
    assertEquals(sum, 6)
  }

  test("keys iterator visits all keys") {
    val map = new CaseInsensitiveIntMap()
    map.put("alpha", 1)
    map.put("beta", 2)
    val keys      = map.keys()
    var collected = Set.empty[String]
    while (keys.hasNext) {
      collected += keys.next().toLowerCase
    }
    assertEquals(collected, Set("alpha", "beta"))
  }

  test("values iterator visits all values") {
    val map = new CaseInsensitiveIntMap()
    map.put("x", 10)
    map.put("y", 20)
    val values    = map.values()
    var collected = Set.empty[Int]
    while (values.hasNext) {
      collected += values.next()
    }
    assertEquals(collected, Set(10, 20))
  }

  test("values toArray returns all values") {
    val map = new CaseInsensitiveIntMap()
    map.put("a", 5)
    map.put("b", 10)
    map.put("c", 15)
    val arr = map.values().toArray()
    assertEquals(arr.size, 3)
    val sorted = (0 until arr.size).map(arr(_)).sorted.toList
    assertEquals(sorted, List(5, 10, 15))
  }

  test("keys toArray returns all keys") {
    val map = new CaseInsensitiveIntMap()
    map.put("foo", 1)
    map.put("bar", 2)
    val arr = map.keys().toArray()
    assertEquals(arr.size, 2)
    val sorted = (0 until arr.size).map(i => arr(i).toLowerCase).sorted.toList
    assertEquals(sorted, List("bar", "foo"))
  }

  // ---------- equals / hashCode ----------

  test("equals: identical maps are equal") {
    val m1 = new CaseInsensitiveIntMap()
    m1.put("a", 1)
    m1.put("b", 2)
    val m2 = new CaseInsensitiveIntMap()
    m2.put("a", 1)
    m2.put("b", 2)
    assert(m1.equals(m2))
    assert(m2.equals(m1))
  }

  test("equals: case-insensitive keys match") {
    val m1 = new CaseInsensitiveIntMap()
    m1.put("Hello", 1)
    val m2 = new CaseInsensitiveIntMap()
    m2.put("hello", 1)
    assert(m1.equals(m2))
  }

  test("equals: different values are not equal") {
    val m1 = new CaseInsensitiveIntMap()
    m1.put("a", 1)
    val m2 = new CaseInsensitiveIntMap()
    m2.put("a", 2)
    assert(!m1.equals(m2))
  }

  test("equals: different sizes are not equal") {
    val m1 = new CaseInsensitiveIntMap()
    m1.put("a", 1)
    val m2 = new CaseInsensitiveIntMap()
    m2.put("a", 1)
    m2.put("b", 2)
    assert(!m1.equals(m2))
  }

  test("equals: map equals itself") {
    val m = new CaseInsensitiveIntMap()
    m.put("x", 99)
    assert(m.equals(m))
  }

  test("equals: map does not equal non-map") {
    val m = new CaseInsensitiveIntMap()
    assert(!m.equals("not a map"))
  }

  test("hashCode is consistent for equal maps") {
    val m1 = new CaseInsensitiveIntMap()
    m1.put("abc", 10)
    m1.put("def", 20)
    val m2 = new CaseInsensitiveIntMap()
    m2.put("abc", 10)
    m2.put("def", 20)
    assertEquals(m1.hashCode(), m2.hashCode())
  }

  // ---------- toString ----------

  test("toString of empty map is {}") {
    val map = new CaseInsensitiveIntMap()
    assertEquals(map.toString(), "{}")
  }

  test("toString of single-entry map contains key=value") {
    val map = new CaseInsensitiveIntMap()
    map.put("test", 42)
    val s = map.toString()
    assert(s.startsWith("{"), s"Expected '{' prefix: $s")
    assert(s.endsWith("}"), s"Expected '}' suffix: $s")
    assert(s.contains("test=42"), s"Expected key=value: $s")
  }

  test("toString without braces") {
    val map = new CaseInsensitiveIntMap()
    map.put("k", 7)
    val s = map.toString(", ", braces = false)
    assert(!s.startsWith("{"))
    assert(s.contains("k=7"))
  }

  // ---------- shrink / ensureCapacity ----------

  test("shrink reduces table if larger than needed") {
    val map = new CaseInsensitiveIntMap(200)
    map.put("a", 1)
    map.shrink(2)
    assertEquals(map.size, 1)
    assertEquals(map.get("a", -1), 1)
  }

  test("shrink rejects negative capacity") {
    val map = new CaseInsensitiveIntMap()
    intercept[IllegalArgumentException](map.shrink(-1))
  }

  test("ensureCapacity allows many puts without issue") {
    val map = new CaseInsensitiveIntMap()
    map.ensureCapacity(1000)
    var i = 0
    while (i < 1000) {
      map.put(s"key$i", i)
      i += 1
    }
    assertEquals(map.size, 1000)
    assertEquals(map.get("key500", -1), 500)
  }

  // ---------- notEmpty / isEmpty ----------

  test("notEmpty and isEmpty reflect state") {
    val map = new CaseInsensitiveIntMap()
    assert(map.isEmpty)
    assert(!map.notEmpty)
    map.put("x", 1)
    assert(!map.isEmpty)
    assert(map.notEmpty)
    map.remove("x", -1)
    assert(map.isEmpty)
    assert(!map.notEmpty)
  }

  // ---------- tableSize ----------

  test("tableSize returns power of two") {
    val ts = CaseInsensitiveIntMap.tableSize(10, 0.6f)
    assert(ts > 0)
    assert((ts & (ts - 1)) == 0, s"Expected power of two, got $ts")
    assert(ts >= 10) // at least capacity
  }

  test("tableSize rejects negative capacity") {
    intercept[IllegalArgumentException](CaseInsensitiveIntMap.tableSize(-1, 0.6f))
  }

  // ---------- hashCodeIgnoreCase ----------

  test("hashCodeIgnoreCase same for different cases") {
    val h1 = CaseInsensitiveIntMap.hashCodeIgnoreCase("Hello World")
    val h2 = CaseInsensitiveIntMap.hashCodeIgnoreCase("hello world")
    val h3 = CaseInsensitiveIntMap.hashCodeIgnoreCase("HELLO WORLD")
    assertEquals(h1, h2)
    assertEquals(h2, h3)
  }

  test("hashCodeIgnoreCase different for different strings") {
    val h1 = CaseInsensitiveIntMap.hashCodeIgnoreCase("abc")
    val h2 = CaseInsensitiveIntMap.hashCodeIgnoreCase("xyz")
    assert(h1 != h2)
  }
}
