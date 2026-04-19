/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/scene/SceneAsset.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * gdx view of an asset file: Model, Camera (as template), lights (as template), textures
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 69
 * Covenant-baseline-methods: SceneAsset,animations,close,data,maxBones,meshes,pixmaps,scene,scenes,textures
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package scene

import sge.graphics.{ Mesh, Pixmap, Texture }
import sge.graphics.g3d.model.Animation
import sge.utils.{ DynamicArray, Nullable }

class SceneAsset extends AutoCloseable {

  /** underlying GLTF data structure, null if loaded without "withData" option. */
  var data: AnyRef = scala.compiletime.uninitialized // @nowarn — GLTF data type from loaders module

  var scenes: Nullable[DynamicArray[SceneModel]] = Nullable.empty
  var scene:  Nullable[SceneModel]               = Nullable.empty

  var animations: Nullable[DynamicArray[Animation]] = Nullable.empty
  var maxBones:   Int                               = 0

  /** Keep track of loaded textures in order to dispose them. Textures handled by AssetManager are excluded. */
  var textures: Nullable[DynamicArray[Texture]] = Nullable.empty

  /** Keep track of loaded pixmaps in order to dispose them. Pixmaps handled by AssetManager are excluded. */
  var pixmaps: Nullable[DynamicArray[Pixmap]] = Nullable.empty

  /** Keep track of loaded meshes in order to dispose them. */
  var meshes: Nullable[DynamicArray[Mesh]] = Nullable.empty

  override def close(): Unit = {
    scenes.foreach { ss =>
      var i = 0
      while (i < ss.size) {
        ss(i).close()
        i += 1
      }
    }
    textures.foreach { ts =>
      var i = 0
      while (i < ts.size) {
        ts(i).close()
        i += 1
      }
    }
    pixmaps.foreach { ps =>
      var i = 0
      while (i < ps.size) {
        ps(i).close()
        i += 1
      }
    }
    meshes.foreach { ms =>
      var i = 0
      while (i < ms.size) {
        ms(i).close()
        i += 1
      }
    }
  }
}
