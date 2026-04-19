/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.io.{ ByteArrayInputStream, StringReader }

class XmlReaderTest extends munit.FunSuite {

  private val reader = XmlReader()

  // ---- Parsing ----

  test("parse simple element") {
    val elem = reader.parse("<root/>")
    assertEquals(elem.name, "root")
    assertEquals(elem.childCount, 0)
    assert(elem.text.isEmpty)
  }

  test("parse element with text") {
    val elem = reader.parse("<msg>Hello</msg>")
    assertEquals(elem.name, "msg")
    assertEquals(elem.text.getOrElse(fail("expected text")), "Hello")
  }

  test("parse element with attributes") {
    val elem = reader.parse("""<item id="42" name="sword"/>""")
    assertEquals(elem.name, "item")
    assert(elem.hasAttribute("id"))
    assertEquals(elem.getAttribute("id"), "42")
    assertEquals(elem.getAttribute("name"), "sword")
  }

  test("parse element with children") {
    val xml =
      """<root>
        |  <child1/>
        |  <child2/>
        |</root>""".stripMargin
    val elem = reader.parse(xml)
    assertEquals(elem.childCount, 2)
    assertEquals(elem.getChild(0).name, "child1")
    assertEquals(elem.getChild(1).name, "child2")
  }

  test("parse nested elements") {
    val xml = "<a><b><c/></b></a>"
    val a   = reader.parse(xml)
    assertEquals(a.name, "a")
    assertEquals(a.getChild(0).name, "b")
    assertEquals(a.getChild(0).getChild(0).name, "c")
  }

  test("parse from Reader") {
    val xml    = "<test>content</test>"
    val elem   = reader.parse(new StringReader(xml))
    assertEquals(elem.name, "test")
    assertEquals(elem.text.getOrElse(fail("expected text")), "content")
  }

  test("parse from InputStream") {
    val xml    = "<test>content</test>"
    val stream = new ByteArrayInputStream(xml.getBytes("UTF-8"))
    val elem   = reader.parse(stream)
    assertEquals(elem.name, "test")
    assertEquals(elem.text.getOrElse(fail("expected text")), "content")
  }

  // ---- Attribute access ----

  test("getAttribute throws for missing attribute") {
    val elem = reader.parse("<item/>")
    intercept[SgeError.InvalidInput] {
      elem.getAttribute("missing")
    }
  }

  test("getAttribute with default returns default for missing") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getAttribute("missing", Nullable("default")).getOrElse(fail("expected non-empty")), "default")
  }

  test("hasAttribute") {
    val elem = reader.parse("""<item id="1"/>""")
    assert(elem.hasAttribute("id"))
    assert(!elem.hasAttribute("name"))
  }

  test("setAttribute adds new attribute") {
    val elem = reader.parse("<item/>")
    elem.setAttribute("color", "red")
    assertEquals(elem.getAttribute("color"), "red")
  }

  test("getFloatAttribute") {
    val elem = reader.parse("""<item x="1.5"/>""")
    assertEquals(elem.getFloatAttribute("x"), 1.5f)
  }

  test("getFloatAttribute with default") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getFloatAttribute("x", 2.0f), 2.0f)
  }

  test("getIntAttribute") {
    val elem = reader.parse("""<item count="10"/>""")
    assertEquals(elem.getIntAttribute("count"), 10)
  }

  test("getIntAttribute with default") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getIntAttribute("count", 5), 5)
  }

  test("getBooleanAttribute") {
    val elem = reader.parse("""<item visible="true"/>""")
    assertEquals(elem.getBooleanAttribute("visible"), true)
  }

  test("getBooleanAttribute with default") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getBooleanAttribute("visible", false), false)
  }

  // ---- Child access ----

  test("getChildByName returns first matching child") {
    val xml  = "<root><a/><b/><a/></root>"
    val root = reader.parse(xml)
    val a    = root.getChildByName("a")
    assert(a.isDefined)
    assertEquals(a.getOrElse(fail("expected child")).name, "a")
  }

  test("getChildByName returns empty for non-existing") {
    val root = reader.parse("<root><a/></root>")
    assert(root.getChildByName("missing").isEmpty)
  }

  test("hasChild") {
    val root = reader.parse("<root><a/></root>")
    assert(root.hasChild("a"))
    assert(!root.hasChild("b"))
  }

  test("getChildByNameRecursive finds nested child") {
    val xml  = "<root><a><b><target/></b></a></root>"
    val root = reader.parse(xml)
    val t    = root.getChildByNameRecursive("target")
    assert(t.isDefined)
    assertEquals(t.getOrElse(fail("expected child")).name, "target")
  }

  test("hasChildRecursive") {
    val xml  = "<root><a><b/></a></root>"
    val root = reader.parse(xml)
    assert(root.hasChildRecursive("b"))
    assert(!root.hasChildRecursive("missing"))
  }

  test("getChildrenByName returns all matching children") {
    val xml      = "<root><a/><b/><a/><c/><a/></root>"
    val root     = reader.parse(xml)
    val children = root.getChildrenByName("a")
    assertEquals(children.size, 3)
  }

  test("getChildrenByNameRecursively returns all matching at any depth") {
    val xml      = "<root><a/><b><a/></b><a/></root>"
    val root     = reader.parse(xml)
    val children = root.getChildrenByNameRecursively("a")
    assertEquals(children.size, 3)
  }

  // ---- Child manipulation ----

  test("addChild increases child count and sets parent") {
    val root  = reader.parse("<root/>")
    val child = XmlElement("child", Nullable.empty)
    root.addChild(child)
    assertEquals(root.childCount, 1)
    assert(child.parent.isDefined)
  }

  test("removeChild by index") {
    val root = reader.parse("<root><a/><b/></root>")
    root.removeChild(0)
    assertEquals(root.childCount, 1)
    assertEquals(root.getChild(0).name, "b")
  }

  test("removeChild by reference") {
    val root  = reader.parse("<root><a/><b/></root>")
    val child = root.getChild(0)
    root.removeChild(child)
    assertEquals(root.childCount, 1)
    assertEquals(root.getChild(0).name, "b")
  }

  test("remove removes self from parent") {
    val root  = reader.parse("<root><a/><b/></root>")
    val child = root.getChild(0)
    child.remove()
    assertEquals(root.childCount, 1)
    assert(child.parent.isEmpty)
  }

  test("replaceChild replaces existing child") {
    val root        = reader.parse("<root><old/></root>")
    val oldChild    = root.getChild(0)
    val replacement = XmlElement("new", Nullable.empty)
    root.replaceChild(oldChild, replacement)
    assertEquals(root.childCount, 1)
    assertEquals(root.getChild(0).name, "new")
    assert(oldChild.parent.isEmpty)
  }

  test("replaceChild throws for non-existing child") {
    val root        = reader.parse("<root><a/></root>")
    val notAChild   = XmlElement("notachild", Nullable.empty)
    val replacement = XmlElement("new", Nullable.empty)
    intercept[SgeError.InvalidInput] {
      root.replaceChild(notAChild, replacement)
    }
  }

  // ---- get (attribute or child text) ----

  test("get returns attribute value if present") {
    val elem = reader.parse("""<item name="sword"/>""")
    assertEquals(elem.get("name"), "sword")
  }

  test("get falls back to child text") {
    val xml  = "<item><name>sword</name></item>"
    val elem = reader.parse(xml)
    assertEquals(elem.get("name"), "sword")
  }

  test("get throws for missing attribute and child") {
    val elem = reader.parse("<item/>")
    intercept[SgeError.InvalidInput] {
      elem.get("missing")
    }
  }

  test("get with default returns default for missing") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.get("missing", Nullable("def")).getOrElse(fail("expected non-empty")), "def")
  }

  test("getInt from attribute") {
    val elem = reader.parse("""<item count="5"/>""")
    assertEquals(elem.getInt("count"), 5)
  }

  test("getInt with default") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getInt("count", 10), 10)
  }

  test("getFloat from attribute") {
    val elem = reader.parse("""<item x="3.14"/>""")
    assertEqualsFloat(elem.getFloat("x"), 3.14f, 0.001f)
  }

  test("getFloat with default") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getFloat("x", 1.0f), 1.0f)
  }

  test("getBoolean from attribute") {
    val elem = reader.parse("""<item active="true"/>""")
    assertEquals(elem.getBoolean("active"), true)
  }

  test("getBoolean with default") {
    val elem = reader.parse("<item/>")
    assertEquals(elem.getBoolean("active", false), false)
  }

  // ---- toString ----

  test("toString for self-closing element") {
    val elem = reader.parse("<item/>")
    val str  = elem.toString
    assert(str.contains("<item"), s"Expected '<item' in: $str")
    assert(str.contains("/>"), s"Expected '/>' in: $str")
  }

  test("toString for element with children") {
    val xml  = "<root><child/></root>"
    val elem = reader.parse(xml)
    val str  = elem.toString
    assert(str.contains("<root>"), s"Expected '<root>' in: $str")
    assert(str.contains("</root>"), s"Expected '</root>' in: $str")
    assert(str.contains("<child"), s"Expected '<child' in: $str")
  }

  test("toString for element with text") {
    val elem = reader.parse("<msg>Hello</msg>")
    val str  = elem.toString
    assert(str.contains("Hello"), s"Expected 'Hello' in: $str")
    assert(str.contains("</msg>"), s"Expected '</msg>' in: $str")
  }

  test("toString for element with attributes") {
    val elem = reader.parse("""<item id="1" name="test"/>""")
    val str  = elem.toString
    assert(str.contains("id=\"1\""), s"Expected attribute in: $str")
    assert(str.contains("name=\"test\""), s"Expected attribute in: $str")
  }

  // ---- Edge cases ----

  test("getChild throws for element with no children") {
    val elem = reader.parse("<item/>")
    intercept[SgeError.InvalidInput] {
      elem.getChild(0)
    }
  }

  test("childCount is 0 for leaf element") {
    val elem = reader.parse("<leaf/>")
    assertEquals(elem.childCount, 0)
  }

  test("parent is set for child elements") {
    val root  = reader.parse("<root><child/></root>")
    val child = root.getChild(0)
    assert(child.parent.isDefined)
    assertEquals(child.parent.getOrElse(fail("expected parent")).name, "root")
  }
}
