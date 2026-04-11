/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package exporters

import scala.annotation.targetName
import sge.graphics.Color
import sge.graphics.g3d.attributes.ColorAttribute
import sge.math.{ Quaternion, Vector3 }
import sge.utils.Nullable

private[exporters] object GLTFExportTypes {

  @targetName("rgbAttr")
  def rgb(a: Nullable[ColorAttribute]): Nullable[Array[Float]] =
    a.flatMap(attr => rgb(Nullable(attr.color)))

  @targetName("rgbaAttr")
  def rgba(a: Nullable[ColorAttribute]): Nullable[Array[Float]] =
    a.flatMap(attr => rgba(Nullable(attr.color)))

  @targetName("rgbColor")
  def rgb(color: Nullable[Color]): Nullable[Array[Float]] =
    color.map(c => Array(c.r, c.g, c.b))

  @targetName("rgbaColor")
  def rgba(color: Nullable[Color]): Nullable[Array[Float]] =
    color.map(c => Array(c.r, c.g, c.b, c.a))

  @targetName("rgbColorDefault")
  def rgb(color: Color, nullColor: Color): Nullable[Array[Float]] =
    if (color.equals(nullColor)) Nullable.empty
    else rgb(Nullable(color))

  def toArray(v: Vector3): Array[Float] =
    Array(v.x, v.y, v.z)

  def toArray(v: Quaternion): Array[Float] =
    Array(v.x, v.y, v.z, v.w)
}
