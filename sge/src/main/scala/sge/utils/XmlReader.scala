/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/XmlReader.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: hand-written XML parser replaced with library delegation;
 *     Element extracted to top-level class; parsing is platform-specific
 *     (scala-xml on JVM/Native, DOMParser on JS)
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 308
 * Covenant-baseline-methods: Element,XmlElement,XmlReader,addChild,attributes,buffer,childCount,children,fromAttr,get,getAttribute,getBoolean,getBooleanAttribute,getChild,getChildByName,getChildByNameRecursive,getChildrenByName,getChildrenByNameRecursively,getChildrenByNameRecursivelyImpl,getFloat,getFloatAttribute,getInt,getIntAttribute,hasAttribute,hasChild,hasChildRecursive,parse,parseImpl,remove,removeChild,replaceChild,result,sb,setAttribute,text,toString,value
 * Covenant-source-reference: com/badlogic/gdx/utils/XmlReader.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 67dece18d30aa54cbe046f4ccc40322c098fb839
 */
package sge
package utils

import java.io.{ InputStream, InputStreamReader, Reader }

import scala.util.boundary
import scala.util.boundary.break
import scala.util.control.NonFatal

/** Lightweight XML parser.
  *
  * Parses XML text into a tree of [[XmlReader.Element]] nodes. The parsing backend is platform-specific: scala-xml on JVM/Native, browser DOMParser on JS.
  */
class XmlReader {

  /** Parses the given XML string into an Element tree. */
  def parse(xml: String): XmlReader.Element =
    XmlReader.parseImpl(xml)

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
      catch { case NonFatal(_) => () }
    parse(sb.toString)
  }

  /** Parses XML from an InputStream. */
  def parse(input: InputStream): XmlReader.Element =
    parse(new InputStreamReader(input, "UTF-8"))

  /** Parses XML from a FileHandle. */
  def parse(file: files.FileHandle): XmlReader.Element =
    try
      parse(file.reader("UTF-8"))
    catch {
      case NonFatal(ex) =>
        throw SgeError.SerializationError("Error parsing file: " + file, Some(ex))
    }
}

object XmlReader {

  /** Type alias preserving API compatibility — existing code using `XmlReader.Element` continues to work. */
  type Element = XmlElement

  /** Factory method preserving `XmlReader.Element(name, parent)` construction syntax. */
  def Element(name: String, parent: Nullable[XmlElement]): XmlElement = XmlElement(name, parent)

  /** Platform-specific parse implementation (scala-xml on JVM/Native, DOMParser on JS). */
  private[utils] def parseImpl(xml: String): XmlElement =
    XmlReaderImpl.parse(xml)
}

/** An XML element with attributes, children, and text content. */
class XmlElement(val name: String, var parent: Nullable[XmlElement]) {
  var attributes: Nullable[ObjectMap[String, String]] = Nullable.empty
  var children:   Nullable[DynamicArray[XmlElement]]  = Nullable.empty
  var text:       Nullable[String]                    = Nullable.empty

  /** @throws SgeError.InvalidInput if the attribute was not found. */
  def getAttribute(attrName: String): String =
    attributes.flatMap(_.get(attrName)).getOrElse(throw SgeError.InvalidInput("Element " + name + " doesn't have attribute: " + attrName))

  def getAttribute(attrName: String, defaultValue: Nullable[String]): Nullable[String] = {
    val value = attributes.flatMap(_.get(attrName))
    if (value.isEmpty) defaultValue else value
  }

  def hasAttribute(attrName: String): Boolean =
    attributes.exists(_.containsKey(attrName))

  def setAttribute(attrName: String, value: String): Unit = {
    if (attributes.isEmpty) attributes = Nullable(ObjectMap[String, String](8))
    attributes.foreach(_.put(attrName, value))
  }

  def childCount: Int =
    children.map(_.size).getOrElse(0)

  /** @throws SgeError.InvalidInput if the element has no children. */
  def getChild(index: Int): XmlElement =
    children.fold[XmlElement](throw SgeError.InvalidInput("Element has no children: " + name))(_(index))

  def addChild(element: XmlElement): Unit = {
    if (children.isEmpty) children = Nullable(DynamicArray[XmlElement](8))
    children.foreach(_.add(element))
    element.parent = Nullable(this)
  }

  def removeChild(index: Int): Unit =
    children.foreach { c =>
      val removedChild = c.removeIndex(index)
      removedChild.parent = Nullable.empty
    }

  def removeChild(child: XmlElement): Unit =
    children.foreach { c =>
      val removeSuccess = c.removeValueByRef(child)
      if (removeSuccess) child.parent = Nullable.empty
    }

  def remove(): Unit = {
    parent.foreach(_.removeChild(this))
    parent = Nullable.empty
  }

  def replaceChild(child: XmlElement, replacement: XmlElement): Unit = {
    if (children.isEmpty) throw SgeError.InvalidInput("Element has no children: " + name)
    if (!children.get.replaceFirstByRef(child, replacement)) {
      throw SgeError.InvalidInput("Element '" + name + "' does not contain child: " + child)
    } else {
      replacement.parent = child.parent
      child.parent = Nullable.empty
    }
  }

  /** Returns the first child having the given name or empty, does not recurse. */
  def getChildByName(childName: String): Nullable[XmlElement] =
    children.flatMap { c =>
      boundary {
        var i = 0
        while (i < c.size) {
          val element = c(i)
          if (element.name == childName) break(Nullable(element))
          i += 1
        }
        Nullable.empty
      }
    }

  def hasChild(childName: String): Boolean =
    children.isDefined && getChildByName(childName).isDefined

  /** Returns the first child having the given name or empty, recurses. */
  def getChildByNameRecursive(childName: String): Nullable[XmlElement] =
    children.flatMap { c =>
      boundary {
        var i = 0
        while (i < c.size) {
          val element = c(i)
          if (element.name == childName) break(Nullable(element))
          val found = element.getChildByNameRecursive(childName)
          if (found.isDefined) break(found)
          i += 1
        }
        Nullable.empty
      }
    }

  def hasChildRecursive(childName: String): Boolean =
    children.isDefined && getChildByNameRecursive(childName).isDefined

  /** Returns the children with the given name or an empty array. */
  def getChildrenByName(childName: String): DynamicArray[XmlElement] = {
    val result = DynamicArray[XmlElement](4)
    children.foreach { c =>
      var i = 0
      while (i < c.size) {
        val child = c(i)
        if (child.name == childName) result.add(child)
        i += 1
      }
    }
    result
  }

  /** Returns the children with the given name or an empty array, recursive. */
  def getChildrenByNameRecursively(childName: String): DynamicArray[XmlElement] = {
    val result = DynamicArray[XmlElement](4)
    getChildrenByNameRecursivelyImpl(childName, result)
    result
  }

  private def getChildrenByNameRecursivelyImpl(childName: String, result: DynamicArray[XmlElement]): Unit =
    children.foreach { c =>
      var i = 0
      while (i < c.size) {
        val child = c(i)
        if (child.name == childName) result.add(child)
        child.getChildrenByNameRecursivelyImpl(childName, result)
        i += 1
      }
    }

  /** @throws SgeError.InvalidInput if the attribute was not found. */
  def getFloatAttribute(attrName: String): Float =
    java.lang.Float.parseFloat(getAttribute(attrName))

  def getFloatAttribute(attrName: String, defaultValue: Float): Float =
    getAttribute(attrName, Nullable.empty).map(v => java.lang.Float.parseFloat(v)).getOrElse(defaultValue)

  /** @throws SgeError.InvalidInput if the attribute was not found. */
  def getIntAttribute(attrName: String): Int =
    java.lang.Integer.parseInt(getAttribute(attrName))

  def getIntAttribute(attrName: String, defaultValue: Int): Int =
    getAttribute(attrName, Nullable.empty).map(v => java.lang.Integer.parseInt(v)).getOrElse(defaultValue)

  /** @throws SgeError.InvalidInput if the attribute was not found. */
  def getBooleanAttribute(attrName: String): Boolean =
    java.lang.Boolean.parseBoolean(getAttribute(attrName))

  def getBooleanAttribute(attrName: String, defaultValue: Boolean): Boolean =
    getAttribute(attrName, Nullable.empty).map(v => java.lang.Boolean.parseBoolean(v)).getOrElse(defaultValue)

  /** Returns the attribute value with the specified name, or if no attribute is found, the text of a child with the name.
    * @throws SgeError.InvalidInput
    *   if no attribute or child was found.
    */
  def get(fieldName: String): String =
    get(fieldName, Nullable.empty).getOrElse(throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName))

  /** Returns the attribute value with the specified name, or if no attribute is found, the text of a child with the name. */
  def get(fieldName: String, defaultValue: Nullable[String]): Nullable[String] = {
    val fromAttr = attributes.flatMap(_.get(fieldName))
    if (fromAttr.isDefined) fromAttr
    else {
      val t = getChildByName(fieldName).flatMap(_.text)
      if (t.isEmpty) defaultValue else t
    }
  }

  def getInt(fieldName: String): Int =
    get(fieldName, Nullable.empty).map(v => java.lang.Integer.parseInt(v)).getOrElse(throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName))

  def getInt(fieldName: String, defaultValue: Int): Int =
    get(fieldName, Nullable.empty).map(v => java.lang.Integer.parseInt(v)).getOrElse(defaultValue)

  def getFloat(fieldName: String): Float =
    get(fieldName, Nullable.empty).map(v => java.lang.Float.parseFloat(v)).getOrElse(throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName))

  def getFloat(fieldName: String, defaultValue: Float): Float =
    get(fieldName, Nullable.empty).map(v => java.lang.Float.parseFloat(v)).getOrElse(defaultValue)

  def getBoolean(fieldName: String): Boolean =
    get(fieldName, Nullable.empty).map(v => java.lang.Boolean.parseBoolean(v)).getOrElse(throw SgeError.InvalidInput("Element " + name + " doesn't have attribute or child: " + fieldName))

  def getBoolean(fieldName: String, defaultValue: Boolean): Boolean =
    get(fieldName, Nullable.empty).map(v => java.lang.Boolean.parseBoolean(v)).getOrElse(defaultValue)

  override def toString(): String = toString("")

  def toString(indent: String): String = {
    val buffer = new StringBuilder(128)
    buffer.append(indent)
    buffer.append('<')
    buffer.append(name)
    attributes.foreach { attrs =>
      attrs.foreachEntry { (k, v) =>
        buffer.append(' ')
        buffer.append(k)
        buffer.append("=\"")
        buffer.append(v)
        buffer.append('"')
      }
    }
    if (children.isEmpty && text.forall(_.isEmpty))
      buffer.append("/>")
    else {
      buffer.append(">\n")
      val childIndent = indent + "\t"
      text.foreach { t =>
        if (t.nonEmpty) {
          buffer.append(childIndent)
          buffer.append(t)
          buffer.append('\n')
        }
      }
      children.foreach { c =>
        var i = 0
        while (i < c.size) {
          buffer.append(c(i).toString(childIndent))
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
