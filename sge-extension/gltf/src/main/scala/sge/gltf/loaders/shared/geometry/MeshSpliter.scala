/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 145
 * Covenant-baseline-methods: MeshSpliter,count,g,groups,i,lastGroup,lastVertices,maxGroup,maxIndex,primitiveIndices,remainingIndices,size,size16,split,stride,tmp,toProcess,vertexMaxSize
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package loaders
package shared
package geometry

import scala.collection.mutable.{ ArrayBuffer, HashMap }
import sge.graphics.VertexAttributes

/** Splits meshes with 32-bit indices into multiple meshes with 16-bit indices.
  */
private[geometry] object MeshSpliter {

  def split(
    splitVertices:        ArrayBuffer[Array[Float]],
    splitIndices:         ArrayBuffer[Array[Short]],
    vertices:             Array[Float],
    attributes:           VertexAttributes,
    indices:              Array[Int],
    verticesPerPrimitive: Int
  ): Unit = {
    // Values used by some graphics APIs as "primitive restart" values are disallowed.
    // Specifically, the value 65535 (in UINT16) cannot be used as a vertex index.
    val size16 = 65535

    val stride        = attributes.vertexSize / 4
    val vertexMaxSize = size16 * stride

    val primitiveIndices = ArrayBuffer[Int]()
    var remainingIndices = ArrayBuffer[Int]()

    val groups = HashMap[Int, ArrayBuffer[Int]]()
    var i      = 0
    val count  = indices.length
    while (i < count) {
      val index0 = indices(i); i += 1
      primitiveIndices += index0
      val group0    = index0 / size16
      var sameGroup = true
      var j         = 1
      while (j < verticesPerPrimitive) {
        val indexI = indices(i); i += 1
        primitiveIndices += indexI
        val groupI = indexI / size16
        if (groupI != group0) sameGroup = false
        j += 1
      }
      if (sameGroup) {
        val group = groups.getOrElseUpdate(group0, ArrayBuffer[Int]())
        j = 0
        while (j < verticesPerPrimitive) {
          group += (primitiveIndices(j) - group0 * size16)
          j += 1
        }
      } else {
        remainingIndices ++= primitiveIndices
      }
      primitiveIndices.clear()
    }

    var maxGroup = 0
    for ((key, _) <- groups)
      maxGroup = scala.math.max(maxGroup, key)

    val lastGroup = groups(maxGroup)
    var maxIndex  = 0
    for (idx <- lastGroup)
      maxIndex = scala.math.max(maxIndex, idx)

    var g = 0
    while (g <= maxGroup) {
      val groupVertices = new Array[Float](vertexMaxSize)
      val offset        = g * size16 * stride
      val size          = scala.math.min(vertices.length - offset, groupVertices.length)
      System.arraycopy(vertices, offset, groupVertices, 0, size)
      splitVertices += groupVertices
      g += 1
    }

    var lastVertices = splitVertices.last
    var toProcess    = ArrayBuffer[Int]()

    while (remainingIndices.nonEmpty) {
      if (maxIndex < 0 || maxIndex >= size16 - 1) {
        maxIndex = -1
        maxGroup += 1
        groups.put(maxGroup, ArrayBuffer[Int]())
        lastVertices = new Array[Float](vertexMaxSize)
        splitVertices += lastVertices
      }
      val currentGroup = groups(maxGroup)
      val reindex      = HashMap[Int, Int]()
      for (oindex <- remainingIndices) {
        val tindex = reindex.getOrElse(oindex, -1)
        if (tindex < 0) {
          val newIndex = maxIndex + 1
          if (newIndex >= size16) {
            toProcess += oindex
          } else {
            reindex.put(oindex, newIndex)
            maxIndex = newIndex
            currentGroup += newIndex
          }
        } else {
          currentGroup += tindex
        }
      }

      for ((key, value) <- reindex)
        System.arraycopy(vertices, key * stride, lastVertices, value * stride, stride)

      if (toProcess.isEmpty) {
        remainingIndices = ArrayBuffer.empty
      } else {
        remainingIndices = toProcess
        toProcess = ArrayBuffer[Int]()
        maxIndex = -1
      }
    }

    g = 0
    while (g <= maxGroup) {
      val group        = groups(g)
      val shortIndices = new Array[Short](group.size)
      var idx          = 0
      while (idx < group.size) {
        shortIndices(idx) = group(idx).toShort
        idx += 1
      }
      splitIndices += shortIndices
      g += 1
    }

    val size = (maxIndex + 1) * stride
    val tmp  = new Array[Float](size)
    System.arraycopy(lastVertices, 0, tmp, 0, size)
    splitVertices(splitIndices.size - 1) = tmp
  }
}
