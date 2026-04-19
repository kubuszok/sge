/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 196
 * Covenant-baseline-methods: MeshTangentSpaceGenerator,biNormal,computeNormalsImpl,computeTangentSpace,computeTangentsImpl,count,i,index,normal,normalOffset,posOffset,stride,tan1,tan2,tangent,tangentOffset,texCoordOffset,vab,vac,vertexCount,vu,vv
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package shared
package geometry

import sge.graphics.VertexAttributes
import sge.math.Vector3

object MeshTangentSpaceGenerator {

  def computeTangentSpace(
    vertices:        Array[Float],
    indices:         Array[Short],
    attributesGroup: VertexAttributes,
    computeNormals:  Boolean,
    computeTangents: Boolean,
    normalMapUVs:    sge.graphics.VertexAttribute
  ): Unit = {
    if (computeNormals) computeNormalsImpl(vertices, indices, attributesGroup)
    if (computeTangents) computeTangentsImpl(vertices, indices, attributesGroup, normalMapUVs)
  }

  private def computeNormalsImpl(vertices: Array[Float], indices: Array[Short], attributesGroup: VertexAttributes): Unit = {
    val posOffset    = attributesGroup.offset(VertexAttributes.Usage.Position)
    val normalOffset = attributesGroup.offset(VertexAttributes.Usage.Normal)
    val stride       = attributesGroup.vertexSize / 4

    val vab = new Vector3()
    val vac = new Vector3()
    if (indices != null) { // @nowarn — indices can be null for non-indexed meshes
      var index = 0
      val count = indices.length
      while (index < count) {
        val vIndexA = indices(index) & 0xffff; index += 1
        val ax      = vertices(vIndexA * stride + posOffset)
        val ay      = vertices(vIndexA * stride + posOffset + 1)
        val az      = vertices(vIndexA * stride + posOffset + 2)

        val vIndexB = indices(index) & 0xffff; index += 1
        val bx      = vertices(vIndexB * stride + posOffset)
        val by      = vertices(vIndexB * stride + posOffset + 1)
        val bz      = vertices(vIndexB * stride + posOffset + 2)

        val vIndexC = indices(index) & 0xffff; index += 1
        val cx      = vertices(vIndexC * stride + posOffset)
        val cy      = vertices(vIndexC * stride + posOffset + 1)
        val cz      = vertices(vIndexC * stride + posOffset + 2)

        vab.set(bx, by, bz).sub(ax, ay, az)
        vac.set(cx, cy, cz).sub(ax, ay, az)
        val n = vab.crs(vac).nor()

        vertices(vIndexA * stride + normalOffset) = n.x
        vertices(vIndexA * stride + normalOffset + 1) = n.y
        vertices(vIndexA * stride + normalOffset + 2) = n.z

        vertices(vIndexB * stride + normalOffset) = n.x
        vertices(vIndexB * stride + normalOffset + 1) = n.y
        vertices(vIndexB * stride + normalOffset + 2) = n.z

        vertices(vIndexC * stride + normalOffset) = n.x
        vertices(vIndexC * stride + normalOffset + 1) = n.y
        vertices(vIndexC * stride + normalOffset + 2) = n.z
      }
    } else {
      var index = 0
      val count = vertices.length / stride
      while (index < count) {
        val vIndexA = index; index += 1
        val ax      = vertices(vIndexA * stride + posOffset)
        val ay      = vertices(vIndexA * stride + posOffset + 1)
        val az      = vertices(vIndexA * stride + posOffset + 2)

        val vIndexB = index; index += 1
        val bx      = vertices(vIndexB * stride + posOffset)
        val by      = vertices(vIndexB * stride + posOffset + 1)
        val bz      = vertices(vIndexB * stride + posOffset + 2)

        val vIndexC = index; index += 1
        val cx      = vertices(vIndexC * stride + posOffset)
        val cy      = vertices(vIndexC * stride + posOffset + 1)
        val cz      = vertices(vIndexC * stride + posOffset + 2)

        vab.set(bx, by, bz).sub(ax, ay, az)
        vac.set(cx, cy, cz).sub(ax, ay, az)
        val n = vab.crs(vac).nor()

        vertices(vIndexA * stride + normalOffset) = n.x
        vertices(vIndexA * stride + normalOffset + 1) = n.y
        vertices(vIndexA * stride + normalOffset + 2) = n.z

        vertices(vIndexB * stride + normalOffset) = n.x
        vertices(vIndexB * stride + normalOffset + 1) = n.y
        vertices(vIndexB * stride + normalOffset + 2) = n.z

        vertices(vIndexC * stride + normalOffset) = n.x
        vertices(vIndexC * stride + normalOffset + 1) = n.y
        vertices(vIndexC * stride + normalOffset + 2) = n.z
      }
    }
  }

  // inspired by: https://gamedev.stackexchange.com/questions/68612/how-to-compute-tangent-and-bitangent-vectors
  private def computeTangentsImpl(
    vertices:        Array[Float],
    indices:         Array[Short],
    attributesGroup: VertexAttributes,
    normalMapUVs:    sge.graphics.VertexAttribute
  ): Unit = {
    val posOffset      = attributesGroup.offset(VertexAttributes.Usage.Position)
    val normalOffset   = attributesGroup.offset(VertexAttributes.Usage.Normal)
    val tangentOffset  = attributesGroup.offset(VertexAttributes.Usage.Tangent)
    val texCoordOffset = normalMapUVs.offset / 4
    val stride         = attributesGroup.vertexSize / 4
    val vertexCount    = vertices.length / stride

    val vu   = new Vector3()
    val vv   = new Vector3()
    val tan1 = Array.fill(vertexCount)(new Vector3())
    val tan2 = Array.fill(vertexCount)(new Vector3())

    var index = 0
    val count = indices.length
    while (index < count) {
      val vIndexA = indices(index) & 0xffff; index += 1
      val ax      = vertices(vIndexA * stride + posOffset)
      val ay      = vertices(vIndexA * stride + posOffset + 1)
      val az      = vertices(vIndexA * stride + posOffset + 2)

      val vIndexB = indices(index) & 0xffff; index += 1
      val bx      = vertices(vIndexB * stride + posOffset)
      val by      = vertices(vIndexB * stride + posOffset + 1)
      val bz      = vertices(vIndexB * stride + posOffset + 2)

      val vIndexC = indices(index) & 0xffff; index += 1
      val cx      = vertices(vIndexC * stride + posOffset)
      val cy      = vertices(vIndexC * stride + posOffset + 1)
      val cz      = vertices(vIndexC * stride + posOffset + 2)

      val au = vertices(vIndexA * stride + texCoordOffset)
      val av = 1 - vertices(vIndexA * stride + texCoordOffset + 1)

      val bu = vertices(vIndexB * stride + texCoordOffset)
      val bv = 1 - vertices(vIndexB * stride + texCoordOffset + 1)

      val cu = vertices(vIndexC * stride + texCoordOffset)
      val cv = 1 - vertices(vIndexC * stride + texCoordOffset + 1)

      val dx1 = bx - ax; val dx2 = cx - ax
      val dy1 = by - ay; val dy2 = cy - ay
      val dz1 = bz - az; val dz2 = cz - az
      val du1 = bu - au; val du2 = cu - au
      val dv1 = bv - av; val dv2 = cv - av

      val r = 1f / (du1 * dv2 - du2 * dv1)

      vu.set((dv2 * dx1 - dv1 * dx2) * r, (dv2 * dy1 - dv1 * dy2) * r, (dv2 * dz1 - dv1 * dz2) * r)
      vv.set((du1 * dx2 - du2 * dx1) * r, (du1 * dy2 - du2 * dy1) * r, (du1 * dz2 - du2 * dz1) * r)

      tan1(vIndexA).add(vu); tan2(vIndexA).add(vv)
      tan1(vIndexB).add(vu); tan2(vIndexB).add(vv)
      tan1(vIndexC).add(vu); tan2(vIndexC).add(vv)
    }

    val tangent  = new Vector3()
    val normal   = new Vector3()
    val biNormal = new Vector3()
    var i        = 0
    while (i < vertexCount) {
      val nx = vertices(i * stride + normalOffset)
      val ny = vertices(i * stride + normalOffset + 1)
      val nz = vertices(i * stride + normalOffset + 2)
      normal.set(nx, ny, nz)

      val t1 = tan1(i)
      tangent.set(t1).mulAdd(normal, -normal.dot(t1)).nor()

      val t2 = tan2(i)
      biNormal.set(normal).crs(tangent)
      val tangentW = if (biNormal.dot(t2) < 0) -1f else 1f

      vertices(i * stride + tangentOffset) = tangent.x
      vertices(i * stride + tangentOffset + 1) = tangent.y
      vertices(i * stride + tangentOffset + 2) = tangent.z
      vertices(i * stride + tangentOffset + 3) = tangentW
      i += 1
    }
  }
}
