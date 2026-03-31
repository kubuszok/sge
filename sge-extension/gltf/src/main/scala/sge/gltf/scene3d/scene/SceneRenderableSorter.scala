/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/SceneRenderableSorter.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Advanced RenderableSorter providing hints and limit shader, material and mesh switches.
 * Useful for Skybox: should be rendered before transparent renderables but after all opaque renderables.
 */
package sge
package gltf
package scene3d
package scene

import scala.util.boundary
import scala.util.boundary.break

import java.util.Comparator

import sge.graphics.Camera
import sge.graphics.g3d.Renderable
import sge.graphics.g3d.attributes.BlendingAttribute
import sge.graphics.g3d.utils.RenderableSorter
import sge.math.{ Matrix4, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

class SceneRenderableSorter extends RenderableSorter with Comparator[Renderable] {

  private var camera: Camera  = scala.compiletime.uninitialized
  private val tmpV1:  Vector3 = Vector3()
  private val tmpV2:  Vector3 = Vector3()

  override def sort(camera: Nullable[Camera], renderables: DynamicArray[Renderable]): Unit =
    camera.foreach { cam =>
      this.camera = cam
      renderables.sort()(using Ordering.comparatorToOrdering(this))
    }

  private def getTranslation(worldTransform: Matrix4, center: Vector3, output: Vector3): Vector3 = {
    if (center.isZero) worldTransform.translation(output)
    else if (!worldTransform.hasRotationOrScaling()) worldTransform.translation(output).add(center)
    else output.set(center).mul(worldTransform)
    output
  }

  override def compare(o1: Renderable, o2: Renderable): Int = boundary {
    val b1 = o1.material.exists(_.has(BlendingAttribute.Type)) &&
      o1.material.flatMap(_.getAs[BlendingAttribute](BlendingAttribute.Type)).exists(_.blended)
    val b2 = o2.material.exists(_.has(BlendingAttribute.Type)) &&
      o2.material.flatMap(_.getAs[BlendingAttribute](BlendingAttribute.Type)).exists(_.blended)

    val h1 = o1.userData match {
      case h: Nullable.Impl[?] =>
        h.fold(null: SceneRenderableSorter.Hints) {
          case hh: SceneRenderableSorter.Hints => hh
          case _ => null // @nowarn
        }
      case _ => null // @nowarn — no hint
    }
    val h2 = o2.userData match {
      case h: Nullable.Impl[?] =>
        h.fold(null: SceneRenderableSorter.Hints) {
          case hh: SceneRenderableSorter.Hints => hh
          case _ => null // @nowarn
        }
      case _ => null // @nowarn — no hint
    }

    if (h1 ne h2) { // @nowarn — identity comparison
      if (h1 == SceneRenderableSorter.Hints.OPAQUE_LAST) {
        break(if (b2) -1 else 1)
      }
      if (h2 == SceneRenderableSorter.Hints.OPAQUE_LAST) {
        break(if (b1) 1 else -1)
      }
    }

    // solid models are grouped by context to minimize switches
    if (!b1 && !b2) {
      @scala.annotation.nowarn("msg=deprecated")
      val s1 = o1.shader.orNull
      @scala.annotation.nowarn("msg=deprecated")
      val s2            = o2.shader.orNull
      val shaderCompare = compareIdentityNullable(s1.asInstanceOf[AnyRef], s2.asInstanceOf[AnyRef])
      if (shaderCompare != 0) break(shaderCompare)

      @scala.annotation.nowarn("msg=deprecated")
      val e1 = o1.environment.orNull
      @scala.annotation.nowarn("msg=deprecated")
      val e2         = o2.environment.orNull
      val envCompare = compareIdentityNullable(e1.asInstanceOf[AnyRef], e2.asInstanceOf[AnyRef])
      if (envCompare != 0) break(envCompare)

      @scala.annotation.nowarn("msg=deprecated")
      val m1 = o1.material.orNull
      @scala.annotation.nowarn("msg=deprecated")
      val m2              = o2.material.orNull
      val materialCompare = compareIdentityNullable(m1.asInstanceOf[AnyRef], m2.asInstanceOf[AnyRef])
      if (materialCompare != 0) break(materialCompare)

      val meshCompare = compareIdentity(o1.meshPart.mesh, o2.meshPart.mesh)
      if (meshCompare != 0) break(meshCompare)
    } else if (b1 && !b2) {
      break(1)
    } else if (!b1 && b2) {
      break(-1)
    }

    // classic with distance: front to back for solid, back to front for transparent
    getTranslation(o1.worldTransform, o1.meshPart.center, tmpV1)
    getTranslation(o2.worldTransform, o2.meshPart.center, tmpV2)
    val d1     = camera.position.distance(tmpV1)
    val d2     = camera.position.distance(tmpV2)
    val dst    = java.lang.Float.compare(d1, d2)
    val result = if (dst < 0) -1 else if (dst > 0) 1 else 0
    if (b1) -result else result
  }

  private def compareIdentityNullable(o1: AnyRef, o2: AnyRef): Int =
    if ((o1 eq null) && (o2 eq null)) 0 // @nowarn — null comparison
    else if (o1 eq null) -1 // @nowarn
    else if (o2 eq null) 1 // @nowarn
    else compareIdentity(o1, o2)

  private def compareIdentity(o1: AnyRef, o2: AnyRef): Int =
    if (o1 eq o2) 0
    else Integer.compare(o1.hashCode(), o2.hashCode())
}

object SceneRenderableSorter {

  enum Hints extends java.lang.Enum[Hints] {
    case OPAQUE_LAST
  }
}
