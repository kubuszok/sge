/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/CameraGroupStrategy.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package decals

import sge.graphics.Camera
import sge.graphics.GL20
import sge.graphics.glutils.ShaderProgram
import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.ObjectMap
import sge.utils.Pool
import sge.utils.SgeError

/** <p> Minimalistic grouping strategy that splits decals into opaque and transparent ones enabling and disabling blending as needed. Opaque decals are rendered first (decal color is ignored in
  * opacity check).<br/> Use this strategy only if the vast majority of your decals are opaque and the few transparent ones are unlikely to overlap. </p> <p> Can produce invisible artifacts when
  * transparent decals overlap each other. </p> <p> Needs to be explicitly disposed as it might allocate a ShaderProgram when GLSL 2.0 is used. </p> <p> States (* = any, EV = entry value - same as
  * value before flush):<br/> <table> <tr> <td></td> <td>expects</td> <td>exits on</td> </tr> <tr> <td>glDepthMask</td> <td>true</td> <td>EV</td> </tr> <tr> <td>GL_DEPTH_TEST</td> <td>enabled</td>
  * <td>EV</td> </tr> <tr> <td>glDepthFunc</td> <td>GL_LESS | GL_LEQUAL</td> <td>EV</td> </tr> <tr> <td>GL_BLEND</td> <td>disabled</td> <td>EV | disabled</td> </tr> <tr> <td>glBlendFunc</td>
  * <td>*</td> <td>*</td> </tr> <tr> <td>GL_TEXTURE_2D</td> <td>*</td> <td>disabled</td> </tr> </table> </p>
  */
class CameraGroupStrategy(var camera: Camera, cameraSorter: Ordering[Decal])(using sge: Sge) extends GroupStrategy with AutoCloseable {

  private val arrayPool: Pool[DynamicArray[Decal]] = new Pool.Default[DynamicArray[Decal]](
    () => DynamicArray[Decal](),
    initialCapacity = 16
  )
  private val usedArrays:     DynamicArray[DynamicArray[Decal]]             = DynamicArray[DynamicArray[Decal]]()
  private val materialGroups: ObjectMap[DecalMaterial, DynamicArray[Decal]] = ObjectMap[DecalMaterial, DynamicArray[Decal]]()

  private var shader: ShaderProgram = scala.compiletime.uninitialized

  createDefaultShader()

  def this(camera: Camera)(using sge: Sge) =
    this(
      camera,
      Ordering.fromLessThan[Decal] { (o1, o2) =>
        val dist1 = camera.position.distance(o1.getPosition)
        val dist2 = camera.position.distance(o2.getPosition)
        Math.signum(dist2 - dist1).toInt > 0
      }
    )

  def setCamera(camera: Camera): Unit =
    this.camera = camera

  def getCamera: Camera = camera

  override def decideGroup(decal: Decal): Int =
    if (decal.getMaterial.isOpaque) CameraGroupStrategy.GROUP_OPAQUE else CameraGroupStrategy.GROUP_BLEND

  override def beforeGroup(group: Int, contents: DynamicArray[Decal]): Unit =
    if (group == CameraGroupStrategy.GROUP_BLEND) {
      sge.graphics.gl.glEnable(GL20.GL_BLEND)
      sge.graphics.gl.glDepthMask(false)
      contents.sort(cameraSorter)
    } else {
      var i = 0
      val n = contents.size
      while (i < n) {
        val decal         = contents(i)
        val materialGroup = materialGroups.get(decal.material).getOrElse {
          val mg = arrayPool.obtain()
          mg.clear()
          usedArrays.add(mg)
          materialGroups.put(decal.material, mg)
          mg
        }
        materialGroup.add(decal)
        i += 1
      }

      contents.clear()
      materialGroups.foreachValue { materialGroup =>
        contents.addAll(materialGroup)
      }

      materialGroups.clear()
      usedArrays.foreach(arrayPool.free)
      usedArrays.clear()
    }

  override def afterGroup(group: Int): Unit =
    if (group == CameraGroupStrategy.GROUP_BLEND) {
      sge.graphics.gl.glDisable(GL20.GL_BLEND)
      sge.graphics.gl.glDepthMask(true)
    }

  override def beforeGroups(): Unit = {
    sge.graphics.gl.glEnable(GL20.GL_DEPTH_TEST)
    shader.bind()
    shader.setUniformMatrix("u_projectionViewMatrix", camera.combined)
    shader.setUniformi("u_texture", 0)
  }

  override def afterGroups(): Unit =
    sge.graphics.gl.glDisable(GL20.GL_DEPTH_TEST)

  private def createDefaultShader(): Unit = {
    val vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
      + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
      + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
      + "uniform mat4 u_projectionViewMatrix;\n" //
      + "varying vec4 v_color;\n" //
      + "varying vec2 v_texCoords;\n" //
      + "\n" //
      + "void main()\n" //
      + "{\n" //
      + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
      + "   v_color.a = v_color.a * (255.0/254.0);\n" //
      + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
      + "   gl_Position =  u_projectionViewMatrix * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
      + "}\n"
    val fragmentShader = "#ifdef GL_ES\n" //
      + "precision mediump float;\n" //
      + "#endif\n" //
      + "varying vec4 v_color;\n" //
      + "varying vec2 v_texCoords;\n" //
      + "uniform sampler2D u_texture;\n" //
      + "void main()\n" //
      + "{\n" //
      + "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
      + "}"

    shader = new ShaderProgram(vertexShader, fragmentShader)
    if (!shader.isCompiled()) throw SgeError.GraphicsError("couldn't compile shader: " + shader.getLog())
  }

  override def getGroupShader(group: Int): Nullable[ShaderProgram] = Nullable(shader)

  def close(): Unit =
    Nullable(shader).foreach(_.close())
}

object CameraGroupStrategy {
  final private val GROUP_OPAQUE = 0
  final private val GROUP_BLEND  = 1
}
