/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/XmlReader.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ InputStream, InputStreamReader, Reader }

import scala.util.boundary
import scala.util.boundary.break
import scala.xml.{ Elem, Node, XML }

/** Lightweight XML parser.
  *
  * Parses XML text into a tree of [[XmlReader.Element]] nodes. Uses scala-xml for parsing, then converts to the LibGDX-compatible tree structure.
  */
class XmlReader {

  /** Parses the given XML string into an Element tree. */
  def parse(xml: String): XmlReader.Element =
    XmlReader.fromScalaXml(XML.loadString(xml))

  /** Parses XML from a Reader. */
  def parse(reader: Reader): XmlReader.Element = {
    val sb     = new StringBuilder(1024)
    val buffer = new Array[Char](1024)
    try {
      var len = reader.read(buffer)
      while (len != -1) {
        sb.appendAll(buffer, 0, len)
        len = reader.read(buffer)
      }
    } finally
      try reader.close()
      catch { case _: Exception => () }
    parse(sb.toString)
  }

  /** Parses XML from an InputStream. */
  def parse(input: InputStream): XmlReader.Element =
    parse(new InputStreamReader(input, "UTF-8"))

  /** Parses XML from a FileHandle. */
  def parse(file: files.FileHandle): XmlReader.Element =
    parse(file.reader())
}

object XmlReader {

  /** An XML element with attributes, children, and text content. */
  class Element(val name: String, private var _parent: Nullable[Element]) {
    private var attributes: Nullable[ObjectMap[String, String]] = Nullable.empty
    private var children:   Nullable[DynamicArray[Element]]     = Nullable.empty
    private var _text:      Nullable[String]                    = Nullable.empty

    def getParent: Nullable[Element] = _parent

    def getAttributes: Nullable[ObjectMap[String, String]] = attributes

    /** @throws SgeError.InvalidInput if the attribute was not found. */
    def getAttribute(attrName: String): String = {
      if (attributes.isEmpty)
        throw SgeError.InvalidInput("Element " + name + " doesn't have attribute: " + attrName)
      val value = attributes.orNull.get(attrName)
      if (value.isEmpty)
        throw SgeError.InvalidInput("Element " + name + " doesn't have attribute: " + attrName)
      value.orNull
    }

    def getAttribute(attrName: String, defaultValue: Nullable[String]): Nullable[String] =
      if (attributes.isEmpty) defaultValue
      else {
        val value = attributes.orNull.get(attrName)
        if (value.isEmpty) defaultValue else value
      }

    def hasAttribute(attrName: String): Boolean =
      attributes.isDefined && attributes.orNull.containsKey(attrName)

    def setAttribute(attrName: String, value: String): Unit = {
      if (attributes.isEmpty) attributes = Nullable(ObjectMap[String, String](8))
      attributes.orNull.put(attrName, value)
    }

    def getChildCount: Int =
      if (children.isEmpty) 0 else children.orNull.size

    def getChildren: Nullable[DynamicArray[Element]] = children

    /** @throws SgeError.InvalidInput if the element has no children. */
    def getChild(index: Int): Element = {
      if (children.isEmpty)
        throw SgeError.InvalidInput("Element has no children: " + name)
      children.orNull(index)
    }

    def addChild(element: Element): Unit = {
      if (children.isEmpty) children = Nullable(DynamicArray[Element](8))
      children.orNull.add(element)
      element._parent = Nullable(this)
    }

    def getText: Nullable[String] = _text

    def setText(text: Nullable[String]): Unit = _text = text

    def removeChild(index: Int): Unit =
      if (children.isDefined) {
        val removedChild = children.orNull.removeIndex(index)
        removedChild._parent = Nullable.empty
      }

    def removeChild(child: Element): Unit =
      if (children.isDefined) {
        val removeSuccess = children.orNull.removeValueByRef(child)
        if (removeSuccess) child._parent = Nullable.empty
      }

    def remove(): Unit = {
      _parent.orNull.removeChild(this)
      _parent = Nullable.empty
    }

    /** Returns the first child having the given name or empty, does not recurse. */
    def getChildByName(childName: String): Nullable[Element] =
      if (children.isEmpty) Nullable.empty
      else
        boundary {
          var i = 0
          while (i < children.orNull.size) {
            val element = children.orNull(i)
            if (element.name == childName) break(Nullable(element))
            i += 1
          }
          Nullable.empty
        }

    def hasChild(childName: String): Boolean =
      children.isDefined && getChildByName(childName).isDefined

    /** Returns the first child having the given name or empty, recurses. */
    def getChildByNameRecursive(childName: String): Nullable[Element] =
      if (children.isEmpty) Nullable.empty
      else
        boundary {
          var i = 0
          while (i < children.orNull.size) {
            val element = children.orNull(i)
            if (element.name == childName) break(Nullable(element))
            val found = element.getChildByNameRecursive(childName)
            if (found.isDefined) break(found)
            i += 1
          }
          Nullable.empty
        }

    def hasChildRecursive(childName: String): Boolean =
      children.isDefined && getChildByNameRecursive(childName).isDefined

    /** Returns the children with the given name or an empty array. */
    def getChildrenByName(childName: String): DynamicArray[Element] = {
      val result = DynamicArray[Element](4)
      if (children.isDefined) {
        var i = 0
        while (i < children.orNull.size) {
          val child = children.orNull(i)
          if (child.name == childName) result.add(child)
          i += 1
        }
      }
      result
    }

    /** Returns the children with the given name or an empty array, recursive. */
    def getChildrenByNameRecursively(childName: String): DynamicArray[Element] = {
      val result = DynamicArray[Element](4)
      getChildrenByNameRecursivelyImpl(childName, result)
      result
    }

    private def getChildrenByNameRecursivelyImpl(childName: String, result: DynamicArray[Element]): Unit =
      if (children.isDefined) {
        var i = 0
        while (i < children.orNull.size) {
          val child = children.orNull(i)
          if (child.name == childName) result.add(child)
          child.getChildrenByNameRecursivelyImpl(childName, result)
          i += 1
        }
      }

    /** @throws SgeError.InvalidInput if the attribute was not found. */
    def getFloatAttribute(attrName: String): Float =
      java.lang.Float.parseFloat(getAttribute(attrName))

    def getFloatAttribute(attrName: String, defaultValue: Float): Float = {
      val value = getAttribute(attrName, Nullable.empty)
      if (value.isEmpty) defaultValue else java.lang.Float.parseFloat(value.orNull)
    }

    /** @throws SgeError.InvalidInput if the attribute was not found. */
    def getIntAttribute(attrName: String): Int =
      java.lang.Integer.parseInt(getAttribute(attrName))

    def getIntAttribute(attrName: String, defaultValue: Int): Int = {
      val value = getAttribute(attrName, Nullable.empty)
      if (value.isEmpty) defaultValue else java.lang.Integer.parseInt(value.orNull)
    }

    /** @throws SgeError.InvalidInput if the attribute was not found. */
    def getBooleanAttribute(attrName: String): Boolean =
      java.lang.Boolean.parseBoolean(getAttribute(attrName))

    def getBooleanAttribute(attrName: String, defaultValue: Boolean): Boolean = {
      val value = getAttribute(attrName, Nullable.empty)
      if (value.isEmpty) defaultValue else java.lang.Boolean.parseBoolean(value.orNull)
    }

    /** Returns the attribute value with the specified name, or if no attribute is found, the text of a child with the name.
      * @throws SgeError.InvalidInput
      *   if no attribute or child was found.
      */
    def get(fieldName: String): String = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName)
      value.orNull
    }

    /** Returns the attribute value with the specified name, or if no attribute is found, the text of a child with the name. */
    def get(fieldName: String, defaultValue: Nullable[String]): Nullable[String] =
      if (attributes.isDefined) {
        val value = attributes.orNull.get(fieldName)
        if (value.isDefined) value
        else {
          val child = getChildByName(fieldName)
          if (child.isEmpty) defaultValue
          else {
            val t = child.orNull.getText
            if (t.isEmpty) defaultValue else t
          }
        }
      } else {
        val child = getChildByName(fieldName)
        if (child.isEmpty) defaultValue
        else {
          val t = child.orNull.getText
          if (t.isEmpty) defaultValue else t
        }
      }

    def getInt(fieldName: String): Int = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName)
      java.lang.Integer.parseInt(value.orNull)
    }

    def getInt(fieldName: String, defaultValue: Int): Int = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) defaultValue else java.lang.Integer.parseInt(value.orNull)
    }

    def getFloat(fieldName: String): Float = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName)
      java.lang.Float.parseFloat(value.orNull)
    }

    def getFloat(fieldName: String, defaultValue: Float): Float = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) defaultValue else java.lang.Float.parseFloat(value.orNull)
    }

    def getBoolean(fieldName: String): Boolean = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName)
      java.lang.Boolean.parseBoolean(value.orNull)
    }

    def getBoolean(fieldName: String, defaultValue: Boolean): Boolean = {
      val value = get(fieldName, Nullable.empty)
      if (value.isEmpty) defaultValue else java.lang.Boolean.parseBoolean(value.orNull)
    }

    override def toString(): String = toString("")

    def toString(indent: String): String = {
      val buffer = new StringBuilder(128)
      buffer.append(indent)
      buffer.append('<')
      buffer.append(name)
      if (attributes.isDefined) {
        attributes.orNull.foreachEntry { (k, v) =>
          buffer.append(' ')
          buffer.append(k)
          buffer.append("=\"")
          buffer.append(v)
          buffer.append('"')
        }
      }
      if (children.isEmpty && (_text.isEmpty || _text.orNull.isEmpty))
        buffer.append("/>")
      else {
        buffer.append(">\n")
        val childIndent = indent + "\t"
        if (_text.isDefined && _text.orNull.nonEmpty) {
          buffer.append(childIndent)
          buffer.append(_text.orNull)
          buffer.append('\n')
        }
        if (children.isDefined) {
          var i = 0
          while (i < children.orNull.size) {
            buffer.append(children.orNull(i).toString(childIndent))
            buffer.append('\n')
            i += 1
          }
        }
        buffer.append(indent)
        buffer.append("</")
        buffer.append(name)
        buffer.append('>')
      }
      buffer.toString
    }
  }

  /** Converts a scala.xml.Elem to an XmlReader.Element. */
  private def fromScalaXml(elem: Elem): Element = fromScalaXml(elem, Nullable.empty)

  private def fromScalaXml(node: Elem, parent: Nullable[Element]): Element = {
    val element = new Element(node.label, parent)
    // Attributes
    node.attributes.foreach { attr =>
      element.setAttribute(attr.key, attr.value.text)
    }
    // Children and text
    val textParts = new StringBuilder
    node.child.foreach {
      case e: Elem =>
        element.addChild(fromScalaXml(e, Nullable(element)))
      case t: Node if t.text.trim.nonEmpty =>
        textParts.append(t.text)
      case _ => ()
    }
    if (textParts.nonEmpty) element.setText(Nullable(textParts.toString))
    element
  }
}
