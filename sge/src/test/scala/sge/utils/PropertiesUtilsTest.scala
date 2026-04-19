/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.io.{ StringReader, StringWriter }

class PropertiesUtilsTest extends munit.FunSuite {

  // ---- Loading ----

  test("load simple key=value pairs") {
    val input = "key1=value1\nkey2=value2\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.size, 2)
    assertEquals(props.get("key1").getOrElse(fail("expected non-empty")), "value1")
    assertEquals(props.get("key2").getOrElse(fail("expected non-empty")), "value2")
  }

  test("load with colon separator") {
    val input = "key1:value1\nkey2:value2\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key1").getOrElse(fail("expected non-empty")), "value1")
    assertEquals(props.get("key2").getOrElse(fail("expected non-empty")), "value2")
  }

  test("load with spaces around separator") {
    val input = "key1 = value1\nkey2 = value2\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key1").getOrElse(fail("expected non-empty")), "value1")
    assertEquals(props.get("key2").getOrElse(fail("expected non-empty")), "value2")
  }

  test("load ignores comment lines starting with #") {
    val input = "# this is a comment\nkey=value\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.size, 1)
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "value")
  }

  test("load ignores comment lines starting with !") {
    val input = "! this is a comment\nkey=value\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.size, 1)
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "value")
  }

  test("load handles empty value") {
    val input = "key=\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "")
  }

  test("load handles escaped newlines (line continuation)") {
    val input = "key=value1\\\n    value2\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "value1value2")
  }

  test("load handles unicode escape") {
    val input = "key=\\u0041\\u0042\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "AB")
  }

  test("load handles escape sequences") {
    val input = "key=hello\\nworld\\ttab\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "hello\nworld\ttab")
  }

  test("load with no trailing newline") {
    val input = "key=value"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "value")
  }

  test("load empty input produces empty map") {
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(""))
    assertEquals(props.size, 0)
  }

  test("load with blank lines between entries") {
    val input = "key1=val1\n\nkey2=val2\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.size, 2)
  }

  test("load key with spaces separator (no = or :)") {
    val input = "key value\n"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.get("key").getOrElse(fail("expected non-empty")), "value")
  }

  // ---- Storing ----

  test("store writes key=value pairs") {
    val props = ObjectMap[String, String]()
    props.put("myKey", "myValue")

    val writer = new StringWriter()
    PropertiesUtils.store(props, writer, null)
    val output = writer.toString

    assert(output.contains("myKey=myValue"), s"Expected output to contain 'myKey=myValue' but got: $output")
  }

  test("store escapes special characters in keys") {
    val props = ObjectMap[String, String]()
    props.put("my=key", "value")

    val writer = new StringWriter()
    PropertiesUtils.store(props, writer, null)
    val output = writer.toString

    assert(output.contains("my\\=key=value"), s"Expected output to contain 'my\\=key=value' but got: $output")
  }

  test("store with comment includes comment line") {
    val props = ObjectMap[String, String]()
    props.put("k", "v")

    val writer = new StringWriter()
    PropertiesUtils.store(props, writer, "my comment")
    val output = writer.toString

    assert(output.contains("#my comment"), s"Expected comment in output but got: $output")
  }

  // ---- Round-trip ----

  test("store then load round-trip") {
    val original = ObjectMap[String, String]()
    original.put("alpha", "one")
    original.put("beta", "two")
    original.put("gamma", "three")

    val writer = new StringWriter()
    PropertiesUtils.store(original, writer, null)

    val loaded = ObjectMap[String, String]()
    PropertiesUtils.load(loaded, new StringReader(writer.toString))

    assertEquals(loaded.size, original.size)
    original.foreachEntry { (k, v) =>
      assertEquals(loaded.get(k).getOrElse(fail(s"missing key $k")), v)
    }
  }

  test("load invalid unicode escape throws") {
    val input = "key=\\uXYZW\n"
    val props = ObjectMap[String, String]()
    intercept[IllegalArgumentException] {
      PropertiesUtils.load(props, new StringReader(input))
    }
  }

  test("load carriage return line ending") {
    val input = "key1=value1\rkey2=value2\r"
    val props = ObjectMap[String, String]()
    PropertiesUtils.load(props, new StringReader(input))
    assertEquals(props.size, 2)
    assertEquals(props.get("key1").getOrElse(fail("expected non-empty")), "value1")
    assertEquals(props.get("key2").getOrElse(fail("expected non-empty")), "value2")
  }
}
