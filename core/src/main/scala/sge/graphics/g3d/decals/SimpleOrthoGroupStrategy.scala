/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/SimpleOrthoGroupStrategy.java
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

import sge.graphics.GL20
import sge.graphics.glutils.ShaderProgram
import sge.utils.Nullable
import sge.utils.Sort

/** <p> Minimalistic grouping strategy useful for orthogonal scenes where the camera faces the negative z axis. Handles enabling and disabling of blending and uses world-z only front to back sorting
  * for transparent decals. </p> <p> States (* = any, EV = entry value - same as value before flush):<br/> <table> <tr> <td></td> <td>expects</td> <td>exits on</td> </tr> <tr> <td>glDepthMask</td>
  * <td>true</td> <td>EV | true</td> </tr> <tr> <td>GL_DEPTH_TEST</td> <td>enabled</td> <td>EV</td> </tr> <tr> <td>glDepthFunc</td> <td>GL_LESS | GL_LEQUAL</td> <td>EV</td> </tr> <tr>
  * <td>GL_BLEND</td> <td>disabled</td> <td>EV | disabled</td> </tr> <tr> <td>glBlendFunc</td> <td>*</td> <td>*</td> </tr> <tr> <td>GL_TEXTURE_2D</td> <td>*</td> <td>disabled</td> </tr> </table> </p>
  */
class SimpleOrthoGroupStrategy(using sge: Sge) extends GroupStrategy {

  private val comparator: Ordering[Decal] = Ordering.fromLessThan[Decal] { (a, b) =>
    if (a.getZ == b.getZ) false
    else a.getZ - b.getZ < 0
  }

  override def decideGroup(decal: Decal): Int =
    if (decal.getMaterial.isOpaque) SimpleOrthoGroupStrategy.GROUP_OPAQUE else SimpleOrthoGroupStrategy.GROUP_BLEND

  override def beforeGroup(group: Int, contents: ArrayBuffer[Decal]): Unit =
    if (group == SimpleOrthoGroupStrategy.GROUP_BLEND) {
      Sort.sort(contents, comparator)
      sge.graphics.gl.glEnable(GL20.GL_BLEND)
      // no need for writing into the z buffer if transparent decals are the last thing to be rendered
      // and they are rendered back to front
      sge.graphics.gl.glDepthMask(false)
    } else {
      // FIXME sort by material
    }

  override def afterGroup(group: Int): Unit =
    if (group == SimpleOrthoGroupStrategy.GROUP_BLEND) {
      sge.graphics.gl.glDepthMask(true)
      sge.graphics.gl.glDisable(GL20.GL_BLEND)
    }

  override def beforeGroups(): Unit =
    sge.graphics.gl.glEnable(GL20.GL_TEXTURE_2D)

  override def afterGroups(): Unit =
    sge.graphics.gl.glDisable(GL20.GL_TEXTURE_2D)

  override def getGroupShader(group: Int): Nullable[ShaderProgram] = Nullable.empty
}

object SimpleOrthoGroupStrategy {
  final private val GROUP_OPAQUE = 0
  final private val GROUP_BLEND  = 1
}
