/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data

import scala.collection.mutable.ArrayBuffer
import sge.gltf.data.animation.GLTFAnimation
import sge.gltf.data.camera.GLTFCamera
import sge.gltf.data.data.{GLTFAccessor, GLTFBuffer, GLTFBufferView}
import sge.gltf.data.geometry.GLTFMesh
import sge.gltf.data.material.GLTFMaterial
import sge.gltf.data.scene.{GLTFNode, GLTFScene, GLTFSkin}
import sge.gltf.data.texture.{GLTFImage, GLTFSampler, GLTFTexture}
import sge.utils.Nullable

class GLTF extends GLTFObject {
  var asset: Nullable[GLTFAsset] = Nullable.empty
  var scene: Int = 0
  var scenes: Nullable[ArrayBuffer[GLTFScene]] = Nullable.empty
  var nodes: Nullable[ArrayBuffer[GLTFNode]] = Nullable.empty
  var cameras: Nullable[ArrayBuffer[GLTFCamera]] = Nullable.empty
  var meshes: Nullable[ArrayBuffer[GLTFMesh]] = Nullable.empty

  var images: Nullable[ArrayBuffer[GLTFImage]] = Nullable.empty
  var samplers: Nullable[ArrayBuffer[GLTFSampler]] = Nullable.empty
  var textures: Nullable[ArrayBuffer[GLTFTexture]] = Nullable.empty

  var animations: Nullable[ArrayBuffer[GLTFAnimation]] = Nullable.empty
  var skins: Nullable[ArrayBuffer[GLTFSkin]] = Nullable.empty

  var accessors: Nullable[ArrayBuffer[GLTFAccessor]] = Nullable.empty
  var materials: Nullable[ArrayBuffer[GLTFMaterial]] = Nullable.empty
  var bufferViews: Nullable[ArrayBuffer[GLTFBufferView]] = Nullable.empty
  var buffers: Nullable[ArrayBuffer[GLTFBuffer]] = Nullable.empty

  var extensionsUsed: Nullable[ArrayBuffer[String]] = Nullable.empty
  var extensionsRequired: Nullable[ArrayBuffer[String]] = Nullable.empty
}
