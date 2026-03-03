/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/GroupPlug.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 * - Java interface -> Scala trait: correct
 * - Array<Decal> -> DynamicArray[Decal]: correct (project-wide replacement)
 * - All methods faithfully ported; no behavioral differences
 * - Status: pass
 */
package sge
package graphics
package g3d
package decals

import sge.utils.DynamicArray

/** Handles a single group's pre and post render arrangements. Can be plugged into {@link PluggableGroupStrategy} to build modular {@link GroupStrategy GroupStrategies}.
  */
trait GroupPlug {

  def beforeGroup(contents: DynamicArray[Decal]): Unit

  def afterGroup(): Unit
}
