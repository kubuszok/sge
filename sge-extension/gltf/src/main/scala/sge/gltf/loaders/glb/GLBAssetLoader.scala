/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/loaders/glb/GLBAssetLoader.java
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: InternalFileHandleResolver -> FileHandleResolver.Internal;
 *     Array<AssetDescriptor> -> DynamicArray[AssetDescriptor[?]];
 *     null return -> Nullable.empty via DynamicArray()
 *   Idiom: split packages; (using Sge) propagation
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package glb

import sge.Sge
import sge.assets.{ AssetDescriptor, AssetManager }
import sge.assets.loaders.{ AsynchronousAssetLoader, FileHandleResolver }
import sge.files.FileHandle
import sge.gltf.loaders.shared.SceneAssetLoaderParameters
import sge.gltf.scene3d.scene.SceneAsset
import sge.utils.{ DynamicArray, Nullable }

class GLBAssetLoader(resolver: FileHandleResolver)(using Sge)
    extends AsynchronousAssetLoader[SceneAsset, SceneAssetLoaderParameters](resolver) {

  def this()(using sge: Sge) =
    this(new FileHandleResolver.Internal())

  override def loadAsync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: SceneAssetLoaderParameters
  ): Unit = {}

  override def loadSync(
    manager:   AssetManager,
    fileName:  String,
    file:      FileHandle,
    parameter: SceneAssetLoaderParameters
  ): SceneAsset = {
    val withData   = Nullable(parameter).exists(_.withData)
    val sceneAsset = new GLBLoader().load(file, withData)
    sceneAsset
  }

  override def getDependencies(
    fileName:  String,
    file:      FileHandle,
    parameter: SceneAssetLoaderParameters
  ): DynamicArray[AssetDescriptor[?]] =
    DynamicArray[AssetDescriptor[?]]()
}
