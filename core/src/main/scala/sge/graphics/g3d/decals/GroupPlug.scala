/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/GroupPlug.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package decals

import scala.collection.mutable.ArrayBuffer

/** Handles a single group's pre and post render arrangements. Can be plugged into {@link PluggableGroupStrategy} to build modular {@link GroupStrategy GroupStrategies}.
  */
trait GroupPlug {

  def beforeGroup(contents: ArrayBuffer[Decal]): Unit

  def afterGroup(): Unit
}
