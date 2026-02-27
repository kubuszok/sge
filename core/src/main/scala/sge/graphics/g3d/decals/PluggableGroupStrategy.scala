/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/PluggableGroupStrategy.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package decals

import sge.utils.DynamicArray
import sge.utils.ObjectMap
import sge.utils.Nullable

/** This class in combination with the {@link GroupPlug GroupPlugs} allows you to build a modular {@link GroupStrategy} out of routines you already implemented.
  */
abstract class PluggableGroupStrategy extends GroupStrategy {

  private val plugs: ObjectMap[Int, GroupPlug] = ObjectMap[Int, GroupPlug]()

  override def beforeGroup(group: Int, contents: DynamicArray[Decal]): Unit =
    plugs.get(group).foreach(_.beforeGroup(contents))

  override def afterGroup(group: Int): Unit =
    plugs.get(group).foreach(_.afterGroup())

  /** Set the plug used for a specific group. The plug will automatically be invoked.
    * @param plug
    *   Plug to use
    * @param group
    *   Group the plug is for
    */
  def plugIn(plug: GroupPlug, group: Int): Unit =
    plugs.put(group, plug)

  /** Remove a plug from the strategy
    * @param group
    *   Group to remove the plug from
    * @return
    *   removed plug, Nullable.empty if there was none for that group
    */
  def unPlug(group: Int): Nullable[GroupPlug] =
    plugs.remove(group)
}
