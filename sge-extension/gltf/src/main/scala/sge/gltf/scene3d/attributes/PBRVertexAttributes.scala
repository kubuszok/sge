/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRVertexAttributes.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

/** Vertex attribute usages for PBR morph targets. */
object PBRVertexAttributes {

  // based on VertexAttributes maximum (biNormal = 256)
  object Usage {
    val PositionTarget: Int = 512
    val NormalTarget:   Int = 1024
    val TangentTarget:  Int = 2048
  }
}
