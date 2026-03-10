/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.util.Locale

class TextFormatterTest extends munit.FunSuite {

  private val fmt = TextFormatter(Locale.US, false)

  test("no placeholders returns same instance") {
    val pattern = "hello world"
    val result  = fmt.format(pattern)
    assert(result eq pattern)
  }

  test("single placeholder") {
    assertEquals(fmt.format("hello {0}", "world"), "hello world")
  }

  test("multiple placeholders") {
    assertEquals(fmt.format("{0} + {1} = {2}", "1", "2", "3"), "1 + 2 = 3")
  }

  test("repeated placeholder") {
    assertEquals(fmt.format("{0}{0}{0}", "ab"), "ababab")
  }

  test("multi-digit index") {
    val args = (0 to 10).map(i => Integer.valueOf(i).asInstanceOf[AnyRef])
    assertEquals(fmt.format("{10}", args*), "10")
  }

  test("escaped left brace") {
    assertEquals(fmt.format("a {{b}} c"), "a {b}} c")
  }

  test("escaped brace with placeholder") {
    assertEquals(fmt.format("{{0}} is {0}", "x"), "{0}} is x")
  }

  test("empty args no placeholders") {
    assertEquals(fmt.format("abc"), "abc")
  }

  test("index out of bounds throws") {
    intercept[IllegalArgumentException] {
      fmt.format("{1}", "only-zero")
    }
  }

  test("empty braces throws") {
    intercept[IllegalArgumentException] {
      fmt.format("{}", "x")
    }
  }

  test("non-digit in placeholder throws") {
    intercept[IllegalArgumentException] {
      fmt.format("{a}", "x")
    }
  }

  test("unmatched brace throws") {
    intercept[IllegalArgumentException] {
      fmt.format("{0", "x")
    }
  }

  test("null argument renders as null") {
    assertEquals(fmt.format("{0}", null), "null")
  }
}
