package sge
package utils

import java.io.Reader
import scala.jdk.CollectionConverters._

object PropertiesUtils {
  def load(map: ObjectMap[String, String], reader: Reader): Unit = {
    val props = new java.util.Properties()
    props.load(reader)
    props.asScala.foreach { case (key, value) =>
      map.put(key, value)
    }
  }
}
