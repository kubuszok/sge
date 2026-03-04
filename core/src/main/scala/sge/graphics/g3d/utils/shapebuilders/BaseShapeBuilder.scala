/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/BaseShapeBuilder.java
 * Original authors: realitix, xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.Color
import sge.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import sge.math.{ Matrix4, Vector3 }
import sge.utils.Pool

/** This class allows to reduce the static allocation needed for shape builders. It contains all the objects used internally by shape builders.
  * @author
  *   realitix, xoppa
  */
object BaseShapeBuilder {
  /* Color */
  protected[shapebuilders] val tmpColor0: Color = new Color()
  protected[shapebuilders] val tmpColor1: Color = new Color()
  protected[shapebuilders] val tmpColor2: Color = new Color()
  protected[shapebuilders] val tmpColor3: Color = new Color()
  protected[shapebuilders] val tmpColor4: Color = new Color()

  /* Vector3 */
  protected[shapebuilders] val tmpV0: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV1: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV2: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV3: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV4: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV5: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV6: Vector3 = new Vector3()
  protected[shapebuilders] val tmpV7: Vector3 = new Vector3()

  /* VertexInfo */
  protected[shapebuilders] val vertTmp0: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp1: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp2: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp3: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp4: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp5: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp6: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp7: VertexInfo = new VertexInfo()
  protected[shapebuilders] val vertTmp8: VertexInfo = new VertexInfo()

  /* Matrix4 */
  protected[shapebuilders] val matTmp1: Matrix4 = new Matrix4()

  private class FlushableVectorPool extends Pool.Flushable[Vector3] {
    override protected val max:             Int     = Int.MaxValue
    override protected val initialCapacity: Int     = 16
    override protected def newObject():     Vector3 = new Vector3()
  }

  private class FlushableMatrixPool extends Pool.Flushable[Matrix4] {
    override protected val max:             Int     = Int.MaxValue
    override protected val initialCapacity: Int     = 16
    override protected def newObject():     Matrix4 = new Matrix4()
  }

  private val vectorPool:    FlushableVectorPool = new FlushableVectorPool()
  private val matrices4Pool: FlushableMatrixPool = new FlushableMatrixPool()

  /** Obtain a temporary {@link Vector3} object, must be free'd using {@link #freeAll()}. */
  protected[shapebuilders] def obtainV3(): Vector3 = vectorPool.obtain()

  /** Obtain a temporary {@link Matrix4} object, must be free'd using {@link #freeAll()}. */
  protected[shapebuilders] def obtainM4(): Matrix4 = {
    val result = matrices4Pool.obtain()
    result
  }

  /** Free all objects obtained using one of the `obtainXX` methods. */
  protected[shapebuilders] def freeAll(): Unit = {
    vectorPool.flush()
    matrices4Pool.flush()
  }
}
