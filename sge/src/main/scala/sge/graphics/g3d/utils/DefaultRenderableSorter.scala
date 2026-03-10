/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/DefaultRenderableSorter.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - implements RenderableSorter, Comparator<Renderable> -> extends RenderableSorter with given Ordering[Renderable]
 *   - compare() extracted to private compareRenderables() used by given Ordering
 *   - material access uses Nullable.fold (material can be null in Java)
 *   - camera field: null -> Nullable[Camera]
 *   - camera.position.dst2() -> camera.position.distanceSq() (renamed in SGE)
 *   - Minor: Java casts int on each dst2 call separately: `(int)(1000f * dst2)`;
 *     Scala version casts after multiply: `(1000f * distanceSq).toInt` -- equivalent
 *   - All methods (sort, getTranslation, compare) fully ported
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import sge.graphics.g3d.attributes.BlendingAttribute
import sge.math.{ Matrix4, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

class DefaultRenderableSorter extends RenderableSorter {
  private var camera: Nullable[Camera] = Nullable.empty
  private val tmpV1:  Vector3          = Vector3()
  private val tmpV2:  Vector3          = Vector3()

  private given Ordering[Renderable] = (o1: Renderable, o2: Renderable) => compareRenderables(o1, o2)

  override def sort(camera: Nullable[Camera], renderables: DynamicArray[Renderable]): Unit = {
    this.camera = camera
    renderables.sort()
  }

  private def getTranslation(worldTransform: Matrix4, center: Vector3, output: Vector3): Vector3 = {
    if (center.isZero)
      worldTransform.getTranslation(output)
    else if (!worldTransform.hasRotationOrScaling())
      worldTransform.getTranslation(output).add(center)
    else
      output.set(center).mul(worldTransform)
    output
  }

  private def compareRenderables(o1: Renderable, o2: Renderable): Int = {
    val b1 = o1.material.exists { mat =>
      mat.has(BlendingAttribute.Type) &&
      mat.get(BlendingAttribute.Type).exists(_.asInstanceOf[BlendingAttribute].blended)
    }
    val b2 = o2.material.exists { mat =>
      mat.has(BlendingAttribute.Type) &&
      mat.get(BlendingAttribute.Type).exists(_.asInstanceOf[BlendingAttribute].blended)
    }
    if (b1 != b2) { if (b1) 1 else -1 }
    else {
      // FIXME implement better sorting algorithm
      // final boolean same = o1.shader == o2.shader && o1.mesh == o2.mesh && (o1.lights == null) == (o2.lights == null) &&
      // o1.material.equals(o2.material);
      getTranslation(o1.worldTransform, o1.meshPart.center, tmpV1)
      getTranslation(o2.worldTransform, o2.meshPart.center, tmpV2)
      camera.fold(0) { cam =>
        val dst    = (1000f * cam.position.distanceSq(tmpV1)).toInt - (1000f * cam.position.distanceSq(tmpV2)).toInt
        val result = if (dst < 0) -1 else if (dst > 0) 1 else 0
        if (b1) -result else result
      }
    }
  }
}
