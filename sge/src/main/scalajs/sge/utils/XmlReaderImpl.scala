/*
 * XmlReader implementation for Scala.js — uses the browser's DOMParser.
 */
package sge
package utils

import org.scalajs.dom.{ DOMParser, Element as DomElement, MIMEType }

private[utils] object XmlReaderImpl {

  // Node type constants for pattern matching (scalajs-dom defines them as non-stable vals)
  final private val ElementNode = 1
  final private val TextNode    = 3
  final private val CDataNode   = 4

  def parse(xml: String): XmlElement = {
    val parser = new DOMParser()
    val doc    = parser.parseFromString(xml, MIMEType.`text/xml`)
    val root   = doc.documentElement
    // DOMParser signals errors via a <parsererror> element (no exception thrown)
    if (root.getElementsByTagName("parsererror").length > 0)
      throw SgeError.InvalidInput("XML parse error: " + root.getElementsByTagName("parsererror").item(0).textContent)
    fromDom(root)
  }

  /** Converts a browser DOM Element to an XmlElement. */
  private def fromDom(node: DomElement, parent: Nullable[XmlElement] = Nullable.empty): XmlElement = {
    val element = XmlElement(node.tagName, parent)
    // Attributes
    val attrs = node.attributes
    var i     = 0
    while (i < attrs.length) {
      val attr = attrs.item(i)
      element.setAttribute(attr.name, attr.value)
      i += 1
    }
    // Children and text
    val textParts = new StringBuilder
    val children  = node.childNodes
    var j         = 0
    while (j < children.length) {
      val child = children.item(j)
      child.nodeType match {
        case ElementNode =>
          element.addChild(fromDom(child.asInstanceOf[DomElement], Nullable(element)))
        case TextNode | CDataNode =>
          val text = child.textContent
          if (text != null && text.trim.nonEmpty) textParts.append(text) // scalastyle:ignore null
        case _ => ()
      }
      j += 1
    }
    if (textParts.nonEmpty) element.text = Nullable(textParts.toString)
    element
  }
}
