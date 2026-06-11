/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/CameraGroupStrategy.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 * - Disposable -> AutoCloseable, dispose() -> close(): correct
 * - cameraSorter: Java uses camera.position.dst(o1.position) accessing protected field;
 *   Scala uses camera.position.distance(o1.position) using public getter — correct
 *   (dst renamed to distance in SGE Vector3)
 * - Default comparator: Java sets the final `cameraSorter` field to an anonymous Comparator
 *   whose compare reads CameraGroupStrategy.this.camera.position.dst(...) (lines 104-107) —
 *   the LIVE field — so it tracks setCamera (lines 119-121). Returns
 *   (int)Math.signum(dist2 - dist1): farther decals sort first (back-to-front), with the
 *   signum-zero (equal-distance) case comparing equal. The Scala port realises this with a
 *   defaultCameraSorter Ordering[Decal] (SAM) defined in the class body that reads the live
 *   `camera` var at compare time; the no-comparator constructor passes Nullable.empty through
 *   the primary constructor's Nullable[Ordering[Decal]] sentinel, which resolves to that
 *   default. A secondary-constructor SAM cannot reference `this`, so it would have captured the
 *   constructor parameter (frozen at construction) instead of the live field — fixed under
 *   ISS-497 bounce 1 (the earlier note overclaimed "faithful": it captured the ctor parameter
 *   and ignored later setCamera / direct `camera =` rebinding)
 * - arrayPool: Java Pool subclass -> Pool.Default lambda: correct
 * - arrayPool.freeAll(usedArrays) -> usedArrays.foreach(arrayPool.free): correct
 *   (DynamicArray not Iterable)
 * - contents.sort(cameraSorter): uses DynamicArray.sort(Ordering): correct
 * - materialGroups iteration: Java values() -> Scala foreachValue: correct
 * - getGroupShader: returns Nullable(shader) instead of bare shader: correct
 * - close(): Nullable(shader).foreach(_.close()): correct null-safe dispose
 * - Shader strings: identical to Java source
 * - Field visibility: Java package-private arrayPool/usedArrays/materialGroups/camera/shader;
 *   Scala: camera is public var (with getter/setter), shader is private, pools are private
 *   — slightly tighter, acceptable
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 204
 * Covenant-baseline-methods: CameraGroupStrategy,GROUP_BLEND,GROUP_OPAQUE,afterGroup,afterGroups,arrayPool,beforeGroup,beforeGroups,cameraSorter,close,createDefaultShader,decideGroup,defaultCameraSorter,fragmentShader,getCamera,getGroupShader,materialGroups,setCamera,shader,this,usedArrays,vertexShader
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/decals/CameraGroupStrategy.java
 * Covenant-verified: 2026-06-11
 *
 * upstream-commit: 2729e989ca16f1dba2da7ef8b754fd6c6a9c0394
 */
package sge
package graphics
package g3d
package decals

import sge.graphics.Camera
import sge.graphics.EnableCap
import sge.graphics.glutils.ShaderProgram
import lowlevel.util.DynamicArray
import lowlevel.Nullable
import lowlevel.util.ObjectMap
import sge.utils.Pool
import sge.utils.SgeError

/** <p> Minimalistic grouping strategy that splits decals into opaque and transparent ones enabling and disabling blending as needed. Opaque decals are rendered first (decal color is ignored in
  * opacity check).<br/> Use this strategy only if the vast majority of your decals are opaque and the few transparent ones are unlikely to overlap. </p> <p> Can produce invisible artifacts when
  * transparent decals overlap each other. </p> <p> Needs to be explicitly disposed as it might allocate a ShaderProgram when GLSL 2.0 is used. </p> <p> States (* = any, EV = entry value - same as
  * value before flush):<br/> <table> <tr> <td></td> <td>expects</td> <td>exits on</td> </tr> <tr> <td>glDepthMask</td> <td>true</td> <td>EV</td> </tr> <tr> <td>GL_DEPTH_TEST</td> <td>enabled</td>
  * <td>EV</td> </tr> <tr> <td>glDepthFunc</td> <td>GL_LESS | GL_LEQUAL</td> <td>EV</td> </tr> <tr> <td>GL_BLEND</td> <td>disabled</td> <td>EV | disabled</td> </tr> <tr> <td>glBlendFunc</td>
  * <td>*</td> <td>*</td> </tr> <tr> <td>GL_TEXTURE_2D</td> <td>*</td> <td>disabled</td> </tr> </table> </p>
  */
// The primary constructor takes the sorter as a Nullable[Ordering[Decal]]: a present value is the
// caller-supplied comparator (Java's two-arg constructor, lines 113-117); an empty value selects the
// default sorter. Java sets the final `cameraSorter` field to an anonymous Comparator whose compare
// reads `CameraGroupStrategy.this.camera` (lines 104-107) — the LIVE field, so it tracks setCamera
// (lines 119-121). Scala cannot reference `this` from a secondary-constructor argument expression, so
// the default sorter (defaultCameraSorter below) lives in the class body and reads the live `camera`
// var at compare time, and the empty sentinel routes the no-comparator constructor to it.
class CameraGroupStrategy(var camera: Camera, cameraSorterOrDefault: Nullable[Ordering[Decal]])(using Sge) extends GroupStrategy with AutoCloseable {

  private val arrayPool: Pool[DynamicArray[Decal]] = new Pool.Default[DynamicArray[Decal]](
    () => DynamicArray[Decal](),
    initialCapacity = 16
  )
  private val usedArrays:     DynamicArray[DynamicArray[Decal]]             = DynamicArray[DynamicArray[Decal]]()
  private val materialGroups: ObjectMap[DecalMaterial, DynamicArray[Decal]] = ObjectMap[DecalMaterial, DynamicArray[Decal]]()

  /** Default decal comparator, equivalent to the anonymous Comparator Java builds in its single-argument constructor (lines 102-109). compare reads the LIVE `this.camera` field (Java lines 105-106),
    * so the sort follows setCamera / a direct `camera =` reassignment (Java lines 119-121). Returns (int)Math.signum(dist2 - dist1) (Java line 107): farther decals sort first (back-to-front), with
    * equal distances comparing equal via signum(0) == 0.
    */
  private val defaultCameraSorter: Ordering[Decal] =
    (o1: Decal, o2: Decal) => {
      val dist1 = camera.position.distance(o1.position)
      val dist2 = camera.position.distance(o2.position)
      Math.signum(dist2 - dist1).toInt
    }

  // Resolve the final sorter once, mirroring Java's final `cameraSorter` field: caller-supplied if present,
  // otherwise the live-field-reading default above.
  private val cameraSorter: Ordering[Decal] = cameraSorterOrDefault.getOrElse(defaultCameraSorter)

  private var shader: ShaderProgram = scala.compiletime.uninitialized

  createDefaultShader()

  def this(camera: Camera)(using Sge) =
    this(camera, Nullable.empty[Ordering[Decal]])

  def this(camera: Camera, sorter: Ordering[Decal])(using Sge) =
    this(camera, Nullable(sorter))

  def setCamera(camera: Camera): Unit =
    this.camera = camera

  def getCamera: Camera = camera

  override def decideGroup(decal: Decal): Int =
    if (decal.material.isOpaque) CameraGroupStrategy.GROUP_OPAQUE else CameraGroupStrategy.GROUP_BLEND

  override def beforeGroup(group: Int, contents: DynamicArray[Decal]): Unit =
    if (group == CameraGroupStrategy.GROUP_BLEND) {
      Sge().graphics.gl.glEnable(EnableCap.Blend)
      Sge().graphics.gl.glDepthMask(false)
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
      Sge().graphics.gl.glDisable(EnableCap.Blend)
      Sge().graphics.gl.glDepthMask(true)
    }

  override def beforeGroups(): Unit = {
    Sge().graphics.gl.glEnable(EnableCap.DepthTest)
    shader.bind()
    shader.setUniformMatrix("u_projectionViewMatrix", camera.combined)
    shader.setUniformi("u_texture", 0)
  }

  override def afterGroups(): Unit =
    Sge().graphics.gl.glDisable(EnableCap.DepthTest)

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

    shader = ShaderProgram(vertexShader, fragmentShader)
    if (!shader.compiled) throw SgeError.GraphicsError("couldn't compile shader: " + shader.log)
  }

  override def getGroupShader(group: Int): Nullable[ShaderProgram] = Nullable(shader)

  def close(): Unit =
    Nullable(shader).foreach(_.close())
}

object CameraGroupStrategy {
  final private val GROUP_OPAQUE = 0
  final private val GROUP_BLEND  = 1
}
