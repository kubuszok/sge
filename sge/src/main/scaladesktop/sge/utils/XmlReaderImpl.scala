/*
 * XmlReader implementation for JVM and Native — uses scala-xml.
 */
package sge
package utils

import scala.xml.{ Elem, Node, XML }

private[utils] object XmlReaderImpl {

  def parse(xml: String): XmlElement =
    fromScalaXml(XML.loadString(xml))

  /** Converts a scala.xml.Elem to an XmlElement. */
  private def fromScalaXml(node: Elem, parent: Nullable[XmlElement] = Nullable.empty): XmlElement = {
    val element = XmlElement(node.label, parent)
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
    if (textParts.nonEmpty) element.text = Nullable(textParts.toString)
    element
  }
}
