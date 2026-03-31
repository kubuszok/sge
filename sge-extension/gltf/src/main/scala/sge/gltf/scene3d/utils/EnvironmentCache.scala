/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/EnvironmentCache.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package utils

import sge.graphics.g3d.{ Attribute, Environment }

class EnvironmentCache extends Environment {

  /** fast way to copy only references */
  def setCache(env: Environment): Unit = {
    this.mask = env.getMask
    this.attributes.clear()
    env.foreach(a => this.attributes.add(a))
    this.shadowMap = env.shadowMap
    this.sorted = true
  }

  /** fast way to replace an attribute without sorting */
  def replaceCache(attribute: Attribute): Unit = {
    val idx = indexOf(attribute.`type`)
    this.attributes(idx) = attribute
  }
}
