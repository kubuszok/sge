/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/PropertiesUtils.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
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
